package com.xlythe.sms;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;
import com.xlythe.sms.adapter.ShareMediaAdapter;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.util.MessageUtils;

import java.util.ArrayList;

public class ShareMediaActivity extends AppCompatActivity {
    private static final String TAG = ShareMediaActivity.class.getSimpleName();

    private TextManager mManager;
    private RecyclerView mRecyclerView;
    private ShareMediaAdapter mAdapter;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_media);

        mManager = TextManager.getInstance(getBaseContext());

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mImageView = (ImageView)  findViewById(R.id.media);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ShareMediaAdapter(this, mManager.getThreadCursor());
        mRecyclerView.setAdapter(mAdapter);

        mImageView.setImageURI(MessageUtils.getAttachment(getIntent()).getUri());

    }

}