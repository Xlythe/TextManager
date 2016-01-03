package com.xlythe.sms;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

public class MessageActivity extends AppCompatActivity {
    public static final String EXTRA_THREAD = "thread";
    private Thread mThread;
    private MessageAdapter mAdapter;
    private TextManager mManager;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mManager = TextManager.getInstance(getBaseContext());
        mThread = (Thread) getIntent().getSerializableExtra(EXTRA_THREAD);
        getSupportActionBar().setTitle(mThread.getLatestMessage().getSender().getDisplayName());

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MessageAdapter(mManager.getMessages(mThread));

        mRecyclerView.setAdapter(mAdapter);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
