package com.xlythe.sms;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;

import com.xlythe.sms.adapter.MessageAdapter;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.textmanager.MessageObserver;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

public class MessageActivity extends AppCompatActivity implements MessageAdapter.FailedViewHolder.ClickListener {
    public static final String EXTRA_THREAD = "thread";

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

    @Override
    protected void onDestroy() {
        mAdapter.destroy();
        mManager.unregisterObserver(mMessageObserver);
        super.onDestroy();
    }

    @Override
    public void onItemClicked(Text text) {
        mManager.downloadAttachment(text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mManager.markAsRead(mThread);
    }
}
