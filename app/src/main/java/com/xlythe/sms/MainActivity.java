package com.xlythe.sms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.xlythe.manager.notifications.messages.MessageBasedNotificationManager;
import com.xlythe.sms.adapter.ThreadAdapter;
import com.xlythe.sms.decoration.HeadersDecoration;
import com.xlythe.sms.decoration.ThreadsItemDecoration;
import com.xlythe.sms.service.FetchChooserTargetService;
import com.xlythe.sms.util.ArrayUtils;
import com.xlythe.sms.util.ShortcutUtils;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.text.Mock.Telephony;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.xlythe.sms.util.PermissionUtils.hasPermissions;

public class MainActivity extends AppCompatActivity implements ThreadAdapter.OnClickListener {
    static final String[] REQUIRED_PERMISSIONS;
    private static final String[] OPTIONAL_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= 33) {
            REQUIRED_PERMISSIONS = new String[] {
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_PHONE_NUMBERS,
            };
            OPTIONAL_PERMISSIONS = new String[] {
                    Manifest.permission.POST_NOTIFICATIONS,
            };
        } else if (Build.VERSION.SDK_INT >= 26) {
            REQUIRED_PERMISSIONS = new String[] {
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_PHONE_NUMBERS,
            };
            OPTIONAL_PERMISSIONS = new String[0];
        } else {
            REQUIRED_PERMISSIONS = new String[] {
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CONTACTS,
            };
            OPTIONAL_PERMISSIONS = new String[0];
        }
    }

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final int REQUEST_CODE_DEFAULT_SMS = 1001;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1002;
    private static final int REQUEST_CODE_BUBBLE_NOTIFICATIONS = 1003;

    private static final int TOOLBAR_SCROLL_FLAGS =
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP;

    private static final int TOOLBAR_SCROLL_FLAGS_SELECT = 0;

    private static final String KEY_REQUEST_PERMISSIONS_FLAG = "request_permissions";
    private static final String KEY_REQUEST_BUBBLE_PERMISSIONS_FLAG = "request_bubble_permissions";

    private TextManager mManager;
    private Thread.ThreadCursor mThreads;

    private ViewGroup mEmptyState;
    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private RecyclerView mRecyclerView;
    private ThreadAdapter mAdapter;
    private final MessageObserver mMessageObserver = new MessageObserver() {
        @SuppressLint("InlinedApi")
        @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
        @Override
        public void notifyDataChanged() {
            mThreads = mManager.getThreadCursor();
            mAdapter.swapCursor(mThreads);
            updateShortcuts();
        }
    };

    private final ActionModeCallback mActionModeCallback = new ActionModeCallback();
    private ActionMode mActionMode;

    private boolean mActionBarCollapsed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MessageBasedNotificationManager.from(this).cancelAll();

        mManager = TextManager.getInstance(getApplicationContext());

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ((AppBarLayout) findViewById(R.id.appbar)).addOnOffsetChangedListener((appBarLayout, verticalOffset) -> mActionBarCollapsed = verticalOffset < 0);

        mRecyclerView = findViewById(R.id.list);
        mEmptyState = findViewById(R.id.empty_state);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFab = findViewById(R.id.fab);
        mFab.setOnClickListener(v -> startActivity(new Intent(this, ComposeActivity.class)));
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();

        if (mManager.isDefaultSmsPackage() && hasPermissions(this, REQUIRED_PERMISSIONS)) {
            loadThreads();
        } else if (!mManager.isDefaultSmsPackage()) {
            defaultSmsNotGranted();
        } else if (hasRequestedPermissions()) {
            requiredPermissionsNotGranted();
        } else {
            markHasRequestedPermissions();
            ActivityCompat.requestPermissions(this, ArrayUtils.concat(REQUIRED_PERMISSIONS, OPTIONAL_PERMISSIONS), REQUEST_CODE_REQUIRED_PERMISSIONS);
            // We do this on 6.0 because this issue is resolved in 6.0.1
            if (Build.VERSION.RELEASE.equals("6.0") && !Settings.System.canWrite(this)) {
                startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName())),
                        REQUEST_CODE_WRITE_SETTINGS);
            }
        }

        // Apps cannot show notifications as bubbles by default. This will prompt the user to enable the feature.
        MessageBasedNotificationManager notificationManager = MessageBasedNotificationManager.from(this);
        boolean hasNotificationPermissions = Build.VERSION.SDK_INT < 33 || hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS);
        if (Build.VERSION.SDK_INT >= 30
                && hasNotificationPermissions
                && !notificationManager.canDisplayBubbles()
                && !hasRequestedBubblePermissions()) {
            Snackbar.make(findViewById(R.id.list), R.string.error_bubble_permissions_not_granted, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.error_bubble_permissions_resolve, v -> {
                        markHasRequestedBubblePermissions();
                        startActivityForResult(
                                new Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()),
                                REQUEST_CODE_BUBBLE_NOTIFICATIONS);
                    })
                    .show();
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

    @SuppressLint("InlinedApi")
    @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
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
        updateShortcuts();
    }

    private void defaultSmsNotGranted() {
        final View errorBox = findViewById(R.id.layout_default_sms);
        errorBox.setVisibility(View.VISIBLE);

        Button button = errorBox.findViewById(R.id.request_default_sms);
        button.setOnClickListener(v -> {
            errorBox.setVisibility(View.GONE);
            startActivityForResult(Telephony.Sms.Intents.requestDefault(this), REQUEST_CODE_DEFAULT_SMS);
        });

        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        params.setScrollFlags(0);
        mToolbar.setLayoutParams(params);

        mFab.setEnabled(false);
        mRecyclerView.setVisibility(View.GONE);
    }

    private void requiredPermissionsNotGranted() {
        final View errorBox = findViewById(R.id.layout_permissions);
        errorBox.setVisibility(View.VISIBLE);

        Button button = errorBox.findViewById(R.id.request_permissions);
        button.setOnClickListener(v -> {
            errorBox.setVisibility(View.GONE);
            ActivityCompat.requestPermissions(this, ArrayUtils.concat(REQUIRED_PERMISSIONS, OPTIONAL_PERMISSIONS), REQUEST_CODE_REQUIRED_PERMISSIONS);
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

            if (mActionBarCollapsed) {
                mRecyclerView.scrollBy(0, mToolbar.getHeight());
            }
        }
    }

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.selected_menu, menu);
            return true;
        }

        @SuppressLint("WrongConstant")
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
            params.setScrollFlags(TOOLBAR_SCROLL_FLAGS_SELECT);
            mToolbar.setLayoutParams(params);
            mRecyclerView.setNestedScrollingEnabled(false);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.menu_remove) {
                Set<Thread> threads = mAdapter.getSelectedItems();
                mManager.delete(threads.toArray(new Thread[0]));
                mAdapter.clearSelection();
                mode.finish();
                return true;
            }
            return false;
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

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        prefs.edit().putBoolean(KEY_REQUEST_PERMISSIONS_FLAG, true).apply();
    }

    /**
     * Returns true if we've already asked for permissions
     * */
    private boolean hasRequestedPermissions() {
        boolean shouldShowRationale = false;
        for (String permission : REQUIRED_PERMISSIONS) {
            shouldShowRationale = shouldShowRationale || ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
        }
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        return shouldShowRationale || prefs.getBoolean(KEY_REQUEST_PERMISSIONS_FLAG, false);
    }

    /**
     * Set a flag so that we don't ask for bubble permissions every time we're launched
     * */
    private void markHasRequestedBubblePermissions() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_REQUEST_BUBBLE_PERMISSIONS_FLAG, true).apply();
    }

    /**
     * Returns true if we've already asked for bubble permissions
     * */
    private boolean hasRequestedBubblePermissions() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_REQUEST_BUBBLE_PERMISSIONS_FLAG, false);
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
    private void updateShortcuts() {
        if (Build.VERSION.SDK_INT < 25) {
            return;
        }

        List<ShortcutInfoCompat> shortcutInfo = new ArrayList<>();
        for (Thread thread : getRecentThreads()) {
            shortcutInfo.add(ShortcutUtils.createShortcutInfo(this, thread));
        }
        ShortcutManagerCompat.addDynamicShortcuts(this, shortcutInfo);
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
    private List<Thread> getRecentThreads() {
        List<Thread> recentThreads = new ArrayList<>(FetchChooserTargetService.SIZE);
        if (mThreads.moveToFirst()) {
            do {
                if (mManager.getMembersExceptMe(mThreads.getThread().getLatestMessage()).get().size() == 0) {
                    continue;
                }
                recentThreads.add(mThreads.getThread());
            } while (mThreads.moveToNext() && recentThreads.size() < FetchChooserTargetService.SIZE);
        }
        return recentThreads;
    }
}
