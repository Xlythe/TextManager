package com.xlythe.sms;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.xlythe.sms.adapter.ThreadAdapter;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.text.Mock;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

public class MainActivity extends AppCompatActivity implements ThreadAdapter.ThreadViewHolder.ClickListener {
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
    };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private static final int TOOLBAR_SCROLL_FLAGS =
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP;

    private static final String KEY_REQUEST_PERMISSIONS_FLAG = "request_permissions";

    private TextManager mManager;
    private Thread.ThreadCursor mThreads;

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

        mManager = TextManager.getInstance(getBaseContext());

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mAppbar = (AppBarLayout) findViewById(R.id.appbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
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
            Snackbar.make(findViewById(android.R.id.content), R.string.msg_not_set_as_default, Snackbar.LENGTH_INDEFINITE)
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

        if (hasPermissions(REQUIRED_PERMISSIONS)) {
            loadThreads();
        } else if (hasRequestedPermissions()) {
            requiredPermissionsNotGranted();
        } else {
            markHasRequestedPermissions();
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    @Override
    protected void onDestroy() {
        mManager.unregisterObserver(mMessageObserver);
        mAdapter.destroy();
        super.onDestroy();
    }

    private void loadThreads() {
        mManager.registerObserver(mMessageObserver);
        mThreads = mManager.getThreadCursor();
        mAdapter = new ThreadAdapter(this, mThreads);
        mRecyclerView.addItemDecoration(new HeadersDecoration(mAdapter));
        mRecyclerView.addItemDecoration(new DividerItemDecorationRes(this, R.drawable.divider));
        mRecyclerView.setAdapter(mAdapter);

        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        params.setScrollFlags(TOOLBAR_SCROLL_FLAGS);
        mToolbar.setLayoutParams(params);

        mFab.setEnabled(true);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void requiredPermissionsNotGranted() {
        final View errorBox = findViewById(R.id.permission_error);
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
    public void onProfileClicked(int position) {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
        toggleSelection(position);
    }

    @Override
    public void onItemClicked(int position) {
        if (mActionMode != null) {
            toggleSelection(position);
        } else {
            Intent i = new Intent(getBaseContext(), MessageActivity.class);
            mThreads.moveToPosition(position);
            i.putExtra(MessageActivity.EXTRA_THREAD, mThreads.getThread());
            startActivity(i);
        }
    }

    @Override
    public boolean onItemLongClicked(int position) {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
        toggleSelection(position);
        return true;
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(String.valueOf(count)+" selected");
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
            mRecyclerView.setNestedScrollingEnabled(false);
            mAppbar.setExpanded(true);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_remove:
                    mAdapter.removeItems(mAdapter.getSelectedItems());
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.removeItems(null);
            mode.finish();
            mActionMode = null;
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
            if (hasPermissions(REQUIRED_PERMISSIONS)) {
                loadThreads();
            } else {
                requiredPermissionsNotGranted();
            }
        }
    }

    /**
     * Returns true if all given permissions are available
     * */
    protected boolean hasPermissions(String... permissions) {
        boolean ok = true;
        for (String permission : permissions) {
            ok = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
            if (!ok) break;
        }
        return ok;
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
