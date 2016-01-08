package com.xlythe.sms;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Window;

import com.xlythe.sms.util.ColorUtils;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

public class MessageActivity extends AppCompatActivity implements MessageAdapter.FailedHolder.ClickListener {
    public static final String EXTRA_THREAD = "thread";

    private Thread mThread;
    private TextManager mManager;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mManager = TextManager.getInstance(getBaseContext());
        mThread = getIntent().getParcelableExtra(EXTRA_THREAD);

        getSupportActionBar().setTitle(mThread.getLatestMessage().getSender().getDisplayName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ColorDrawable colorDrawable = new ColorDrawable(ColorUtils.getColor(Long.parseLong(mThread.getId())));
        getSupportActionBar().setBackgroundDrawable(colorDrawable);

        Window window = this.getWindow();
        window.setStatusBarColor(ColorUtils.getDarkColor(Long.parseLong(mThread.getId())));

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        MessageAdapter adapter = new MessageAdapter(this, mManager.getMessageCursor(mThread));
        adapter.setOnClickListener(this);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClicked(Text text) {
        mManager.downloadAttachment(text);
    }
}
