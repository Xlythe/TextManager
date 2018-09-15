package com.xlythe.sms;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.xlythe.sms.adapter.MessageAdapter;
import com.xlythe.sms.fragment.CameraFragment;
import com.xlythe.sms.fragment.GalleryFragment;
import com.xlythe.sms.fragment.MicFragment;
import com.xlythe.sms.fragment.StickerFragment;
import com.xlythe.sms.notification.MessageBasedNotificationManager;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.swap.SwapEditText;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;
import com.xlythe.textmanager.text.util.MessageUtils;
import com.xlythe.textmanager.text.util.Utils;

import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MessageActivity extends AppCompatActivity implements MessageAdapter.OnClickListener {
    private static final String TAG = TextManager.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final String EXTRA_THREAD = "thread";
    /* ChooserTargetService cannot send Parcelables, so we make do with just the id */
    public static final String EXTRA_THREAD_ID = "thread_id";

    private static Thread sActiveThread = null;

    public static boolean isVisible(String threadId) {
        if (sActiveThread != null) {
            return threadId.equals(sActiveThread.getId());
        }
        return false;
    }

    private AppBarLayout mAppbar;
    private SwapEditText mEditText;
    private ImageView mSendButton;
    private Thread mThread;
    private TextManager mManager;
    private RecyclerView mRecyclerView;
    private MessageAdapter mAdapter;
    private final MessageObserver mMessageObserver = new MessageObserver() {
        @Override
        public void notifyDataChanged() {
            boolean scroll = isScrolledToBottom(mRecyclerView);
            mAdapter.swapCursor(mManager.getMessageCursor(mThread));
            mManager.markAsRead(mThread);
            if (scroll) {
                mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
            }
        }

        private boolean isScrolledToBottom(RecyclerView recyclerView) {
            if (recyclerView.getAdapter().getItemCount() != 0) {
                int lastVisibleItemPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                if (lastVisibleItemPosition != RecyclerView.NO_POSITION) {
                    if (lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1) {
                        // The last item is visible. Fully (or at least mostly) scrolled.
                        return true;
                    }
                }
            }
            return false;
        }
    };

    private ActionModeCallback mActionModeCallback = new ActionModeCallback();
    private ActionMode mActionMode;

    private ImageView mGalleryAttachments;
    private ImageView mCameraAttachments;
    private ImageView mStickerAttachments;
    private ImageView mMicAttachments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAppbar = findViewById(R.id.appbar);

        mEditText = findViewById(R.id.edit_text);
        mSendButton = findViewById(R.id.send);

        setSendable(false);
        mSendButton.setOnClickListener(v -> send());

        mEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (mSendButton.isEnabled()) {
                    send();
                }
                return true;
            }
            return false;
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setSendable(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mEditText.setOnFragmentHiddenListener(this::clearAttachmentSelection);

        mManager = TextManager.getInstance(getBaseContext());
        mThread = getIntent().getParcelableExtra(EXTRA_THREAD);
        if (mThread == null) {
            String id = getIntent().getStringExtra(EXTRA_THREAD_ID);
            mThread = mManager.getThread(id).get();
        }
        if (DEBUG) {
            Log.d(TAG, "Opening Activity for thread " + mThread);
        }

        String name = Utils.join(", ", mManager.getMembersExceptMe(mThread.getLatestMessage()).get(), Contact::getDisplayName);

        getSupportActionBar().setTitle(name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(ColorUtils.getDarkColor(mThread.getIdAsLong()));
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(false);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        // maybe add transcript mode, and show a notification of new messages
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new MessageAdapter(this, mManager.getMessageCursor(mThread));
        mAdapter.setOnClickListener(this);
        mRecyclerView.setAdapter(mAdapter);

        mManager.registerObserver(mMessageObserver);

        // It bothers me when the keyboard doesnt collapse, remove it if you want
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState > 0) {
                    mEditText.hideKeyboard();
                }
            }
        });

        mGalleryAttachments = findViewById(R.id.gallery);
        mCameraAttachments = findViewById(R.id.camera);
        mStickerAttachments = findViewById(R.id.sticker);
        mMicAttachments = findViewById(R.id.mic);

        // Hide the camera fragment if the device has no camera
        mCameraAttachments.setVisibility(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ? View.VISIBLE : View.GONE);
        // TODO: Unhide when support is ready
        mMicAttachments.setVisibility(View.GONE);

        if (savedInstanceState == null) {
            // This is the first time this Activity is launched. Lets check the intent to prepopulate the message.
            Text text = MessageUtils.parse(this, getIntent());
            if (text != null) {
                if (text.getAttachment() != null) {
                    // The text has an attachment. Send immediately.
                    mManager.send(text);
                } else {
                    mEditText.setText(text.getBody());
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        clearAttachmentSelection();
        mEditText.setEnabled(mManager.isDefaultSmsPackage());
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEditText.hideKeyboard();
    }

    public void setSendable(boolean sendable){
        mSendButton.setEnabled(sendable && mManager.isDefaultSmsPackage());
        if (sendable && mManager.isDefaultSmsPackage()) {
            mSendButton.setColorFilter(ColorUtils.getColor(mThread.getIdAsLong()), PorterDuff.Mode.SRC_ATOP);
        } else {
            mSendButton.clearColorFilter();
            int color = getResources().getColor(R.color.button);
            mSendButton.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public void onAttachmentClicked(View view){
        clearAttachmentSelection();
        int color = ColorUtils.getColor(mThread.getIdAsLong());
        ((ImageView) view).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        Text text = mThread.getLatestMessage();

        view.setEnabled(false);
        switch (view.getId()) {
            case R.id.gallery:
                mEditText.showFragment(GalleryFragment.newInstance(text, color));
                break;
            case R.id.camera:
                mEditText.showFragment(CameraFragment.newInstance(text));
                break;
            case R.id.sticker:
                mEditText.showFragment(StickerFragment.newInstance(text));
                break;
            case R.id.mic:
                mEditText.showFragment(new MicFragment());
                break;
        }
    }

    private void clearAttachmentSelection() {
        mGalleryAttachments.clearColorFilter();
        mCameraAttachments.clearColorFilter();
        mStickerAttachments.clearColorFilter();
        mMicAttachments.clearColorFilter();
        int color = getResources().getColor(R.color.date_text_color);
        mGalleryAttachments.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        mCameraAttachments.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        mStickerAttachments.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        mMicAttachments.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        mGalleryAttachments.setEnabled(mManager.isDefaultSmsPackage());
        mCameraAttachments.setEnabled(mManager.isDefaultSmsPackage());
        mStickerAttachments.setEnabled(mManager.isDefaultSmsPackage());
        mMicAttachments.setEnabled(mManager.isDefaultSmsPackage());
    }

    @Override
    protected void onDestroy() {
        mAdapter.destroy();
        mManager.unregisterObserver(mMessageObserver);
        super.onDestroy();
    }

    @Override
    public void onItemClicked(Text text) {
        if (mActionMode != null) {
            toggleSelection(text);
        } else if (MessageAdapter.hasFailed(text)) {
            if (text.isIncoming()) {
                mManager.downloadAttachment(text);
            } else {
                mManager.send(text);
            }
        }
    }

    @Override
    public void onAttachmentClicked(Text text) {
        if (MessageAdapter.hasFailed(text)) {
            if (text.isIncoming()) {
                mManager.downloadAttachment(text);
            } else {
                mManager.send(text);
            }
        } else if (text.getAttachment() != null
                && (text.getAttachment().getType() == Attachment.Type.IMAGE
                || text.getAttachment().getType() == Attachment.Type.VIDEO)) {
            Intent i = new Intent(getBaseContext(), MediaActivity.class);
            i.putExtra(MediaActivity.EXTRA_ATTACHMENT, text.getAttachment());
            startActivity(i);
        }
    }

    @Override
    public void onShareClicked(Text text) {
        if (text.getAttachment() != null && text.getAttachment().getType() == Attachment.Type.IMAGE) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, text.getAttachment().getUri());
            startActivity(Intent.createChooser(share, "Share Image"));
        } else if (text.getAttachment() != null && text.getAttachment().getType() == Attachment.Type.VIDEO) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("video/mpeg");
            share.putExtra(Intent.EXTRA_STREAM, text.getAttachment().getUri());
            startActivity(Intent.createChooser(share, "Share Video"));
        }

    }

    @Override
    public boolean onItemLongClicked(Text text) {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
        toggleSelection(text);
        return true;
    }

    private void toggleSelection(Text text) {
        mAdapter.toggleSelection(text);
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
            mode.getMenuInflater().inflate (R.menu.menu_message, menu);
            menu.findItem(R.id.menu_remove).setVisible(mManager.isDefaultSmsPackage());
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
            Set<Text> texts = mAdapter.getSelectedItems();
            switch (item.getItemId()) {
                case R.id.menu_remove:
                    mManager.delete(texts);
                    mAdapter.clearSelection();
                    mode.finish();
                    return true;
                case R.id.menu_copy:
                    StringBuilder copy = new StringBuilder();
                    for (Text text : texts) {
                        if (copy.length() > 0) {
                            copy.append("\n");
                        }
                        if (!TextUtils.isEmpty(text.getBody())) {
                            copy.append(text.getBody());
                        }
                    }
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(getString(R.string.app_name), copy);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getBaseContext(), R.string.message_text_copied, Toast.LENGTH_SHORT).show();
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
            mRecyclerView.setNestedScrollingEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        if (mEditText.getFragmentVisibility()) {
            mEditText.hideKeyboard();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MessageBasedNotificationManager.from(this).cancelThread(mThread.getId());
        mManager.markAsRead(mThread);
        sActiveThread = mThread;
    }

    @Override
    protected void onPause() {
        sActiveThread = null;
        super.onPause();
    }

    public void log(String message){
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    protected void send() {
        final String message = mEditText.getText().toString();
        mManager.send(message).to(mThread);
        mEditText.setText(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_messages, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_info) {
            Intent i = new Intent(this, InfoActivity.class);
            i.putExtra(EXTRA_THREAD, mThread);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
