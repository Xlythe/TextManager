package com.xlythe.sms;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.xlythe.sms.adapter.ThreadAdapter;
import com.xlythe.sms.decoration.HeadersDecoration;
import com.xlythe.sms.decoration.ThreadsItemDecoration;
import com.xlythe.sms.receiver.Notifications;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.text.Mock;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

import java.util.Set;

import static com.xlythe.sms.util.PermissionUtils.hasPermissions;

public class MainActivity extends AppCompatActivity implements ThreadAdapter.OnClickListener {
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
    };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1001;

    private static final int TOOLBAR_SCROLL_FLAGS =
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP;

    private static final int TOOLBAR_SCROLL_FLAGS_SELECT = 0;

    private static final String KEY_REQUEST_PERMISSIONS_FLAG = "request_permissions";

    private TextManager mManager;
    private Thread.ThreadCursor mThreads;

    private ViewGroup mEmptyState;
    private AppBarLayout mAppbar;
    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private RecyclerView mRecyclerView;
    private ThreadAdapter mAdapter;
    private final MessageObserver mMessageObserver = new MessageObserver() {
        @Override
        public void notifyDataChanged() {
            mThreads = mManager.getThreadCursor();
            mAdapter.swapCursor(mThreads);
        }
    };

    private ActionModeCallback mActionModeCallback = new ActionModeCallback();
    private ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Notifications.dismissAllNotifications(getApplicationContext());

        mManager = TextManager.getInstance(getApplicationContext());

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mAppbar = (AppBarLayout) findViewById(R.id.appbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mEmptyState = (ViewGroup) findViewById(R.id.empty_state);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ComposeActivity.class);
                startActivity(i);
            }
        });

        if (!mManager.isDefaultSmsPackage()) {
            Snackbar.make(findViewById(R.id.list), R.string.msg_not_set_as_default, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.btn_change, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Mock.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                            intent.putExtra(Mock.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                            startActivity(intent);
                        }
                    })
                    .show();
        }

        if (hasPermissions(this, REQUIRED_PERMISSIONS)) {
            loadThreads();
        } else if (hasRequestedPermissions()) {
            requiredPermissionsNotGranted();
        } else {
            markHasRequestedPermissions();
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            // We do this on 6.0 because this issue is resolved in 6.0.1
            if (Build.VERSION.RELEASE.equals("6.0") && !Settings.System.canWrite(this)) {
                startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName())),
                        REQUEST_CODE_WRITE_SETTINGS);
            }
        }
    }

    @Override
    protected void onDestroy() {
        mManager.unregisterObserver(mMessageObserver);
        if (mAdapter != null) {
            mAdapter.destroy();
            mAdapter = null;
        }
        super.onDestroy();
    }

    private void loadThreads() {
        mManager.registerObserver(mMessageObserver);
        mThreads = mManager.getThreadCursor();
        mAdapter = new ThreadAdapter(this, mThreads);
        mAdapter.setHasStableIds(true);
        mRecyclerView.addItemDecoration(new HeadersDecoration(mAdapter));
        mRecyclerView.addItemDecoration(new ThreadsItemDecoration(this, R.drawable.divider));
        mRecyclerView.setAdapter(mAdapter);

        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        params.setScrollFlags(TOOLBAR_SCROLL_FLAGS);
        mToolbar.setLayoutParams(params);

        mFab.setEnabled(true);
        mRecyclerView.setVisibility(View.VISIBLE);

        if (mThreads.getCount() == 0) {
            mEmptyState.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mEmptyState.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void requiredPermissionsNotGranted() {
        final View errorBox = findViewById(R.id.layout_permissions);
        errorBox.setVisibility(View.VISIBLE);

        Button button = (Button) errorBox.findViewById(R.id.request_permissions);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorBox.setVisibility(View.GONE);
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        });

        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        params.setScrollFlags(0);
        mToolbar.setLayoutParams(params);

        mFab.setEnabled(false);
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onProfileClicked(Thread thread) {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
        toggleSelection(thread);
    }

    @Override
    public void onAttachmentClicked(Thread thread) {
        Text text = thread.getLatestMessage();
        if (text.getAttachment() != null) {
            Intent i = new Intent(getBaseContext(), MediaActivity.class);
            i.putExtra(MediaActivity.EXTRA_ATTACHMENT, text.getAttachment());
            startActivity(i);
        }
    }

    @Override
    public void onItemClicked(Thread thread) {
        if (mActionMode != null) {
            toggleSelection(thread);
        } else {
            Intent i = new Intent(getBaseContext(), MessageActivity.class);
            i.putExtra(MessageActivity.EXTRA_THREAD, thread);
            startActivity(i);
        }
    }

    @Override
    public boolean onItemLongClicked(Thread thread) {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
        toggleSelection(thread);
        return true;
    }

    private void toggleSelection(Thread thread) {
        mAdapter.toggleSelection(thread);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(getString(R.string.title_selection, count));
            mActionMode.invalidate();
        }
    }

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate (R.menu.selected_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
            params.setScrollFlags(TOOLBAR_SCROLL_FLAGS_SELECT);
            mToolbar.setLayoutParams(params);
            mRecyclerView.setNestedScrollingEnabled(false);
            mAppbar.setExpanded(true);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_remove:
                    Set<Thread> threads = mAdapter.getSelectedItems();
                    mManager.delete(threads.toArray(new Thread[threads.size()]));
                    mAdapter.clearSelection();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mode.finish();
            mAdapter.clearSelection();
            mActionMode = null;
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
            params.setScrollFlags(TOOLBAR_SCROLL_FLAGS);
            mToolbar.setLayoutParams(params);
            mRecyclerView.setNestedScrollingEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            Snackbar.make(findViewById(R.id.list), "Search not yet added", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            if (hasPermissions(this, REQUIRED_PERMISSIONS)) {
                loadThreads();
            } else {
                requiredPermissionsNotGranted();
            }
        }
    }

    /**
     * Set a flag so that we don't ask for permissions every time we're launched
     * */
    private void markHasRequestedPermissions() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_REQUEST_PERMISSIONS_FLAG, true).commit();
    }

    /**
     * Returns true if we've already asked for permissions
     * */
    private boolean hasRequestedPermissions() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_REQUEST_PERMISSIONS_FLAG, false);
    }
}
