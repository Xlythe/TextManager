package com.xlythe.sms;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.xlythe.sms.adapter.MessageAdapter;
import com.xlythe.sms.fragment.CameraFragment;
import com.xlythe.sms.fragment.FaceFragment;
import com.xlythe.sms.fragment.MicFragment;
import com.xlythe.sms.fragment.PhotoFragment;
import com.xlythe.sms.fragment.ScreenSlidePageFragment;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

public class MessageActivity extends AppCompatActivity implements MessageAdapter.FailedViewHolder.ClickListener {
    private static final String TAG = TextManager.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final String EXTRA_THREAD = "thread";
    private boolean mSendable;
    private View mAttachView;
    private EditText mEditText;
    private ImageView mSendButton;
    private Thread mThread;
    private TextManager mManager;
    private RecyclerView mRecyclerView;
    private MessageAdapter mAdapter;
    private final MessageObserver mMessageObserver = new MessageObserver() {
        @Override
        public void notifyDataChanged() {
            mAdapter.swapCursor(mManager.getMessageCursor(mThread));
            mManager.markAsRead(mThread);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAttachView = findViewById(R.id.fragment_container);
        mEditText = (EditText) findViewById(R.id.edit_text);
        mSendButton = (ImageView) findViewById(R.id.send);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSendable) {
                    mManager.send(new Text.Builder(getBaseContext())
                                    .message(mEditText.getText().toString())
                                    .recipient(mThread.getLatestMessage().getSender().getNumber())
                                    .build()
                    );
                    mEditText.setText("");
                    setSendable(false);
                }
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                log("before:" + s + ", " + start + ", " + "" + count + ", " + after);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                log("on:" + s + ", " + start + ", " + "" + before + ", " + count);
                // Thought I could use count, but it resets to 1 when you add a space
                // so ends up being zero when you delete the space
                if (s.length() > 0) {
                    setSendable(true);
                } else {
                    setSendable(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                log("after:" + s.toString());
            }
        });

        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    Log.d("MessageActivity", "dismiss attachview");
                    mAttachView.setVisibility(View.GONE);
                }
            }
        });

        mManager = TextManager.getInstance(getBaseContext());
        mThread = getIntent().getParcelableExtra(EXTRA_THREAD);
        final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(getResources().getColor(R.color.icon_color), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
        getSupportActionBar().setTitle(Html.fromHtml("<font color='#212121'>"+
                mThread.getLatestMessage().getSender().getDisplayName()
                + " </font>"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(ColorUtils.getDarkColor(Long.parseLong(mThread.getId())));
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
    }

    public void setSendable(boolean sendable){
        mSendable = sendable;
        if (sendable) {
            mSendButton.setColorFilter(ColorUtils.getColor(Long.parseLong(mThread.getId())), PorterDuff.Mode.SRC_ATOP);
        } else {
            mSendButton.clearColorFilter();
        }
    }

    public void attachClick(View view){
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        mEditText.clearFocus();
        if (mAttachView.getVisibility() == View.GONE)
            mAttachView.setVisibility(View.VISIBLE);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        switch (view.getId()){
            case R.id.photo:
                transaction.replace(R.id.fragment_container, new ScreenSlidePageFragment()).commit();
                log("photo");
                break;
            case R.id.camera:
                transaction.replace(R.id.fragment_container, new CameraFragment()).commit();
                log("camera");
                break;
            case R.id.sticker:
                transaction.replace(R.id.fragment_container, new FaceFragment()).commit();
                log("sticker");
                break;
            case R.id.mic:
                transaction.replace(R.id.fragment_container, new MicFragment()).commit();
                log("mic");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        mAdapter.destroy();
        mManager.unregisterObserver(mMessageObserver);
        super.onDestroy();
    }

    @Override
    public void onItemClicked(Text text) {
        if (text.isMms()) {
            if (!text.getAttachments().isEmpty()) {
                log("Open attachment");
                Intent i = new Intent(getBaseContext(), MediaActivity.class);
                i.putExtra(MediaActivity.EXTRA_TEXT, text);
                startActivity(i);
                return;
            }
            log("Re-download attachment");
            mManager.downloadAttachment(text);
            return;
        }
        log("Do nothing");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mManager.markAsRead(mThread);
    }

    public void log(String message){
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }
}
