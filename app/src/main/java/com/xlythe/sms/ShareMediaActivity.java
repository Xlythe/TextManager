package com.xlythe.sms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.xlythe.sms.adapter.ShareMediaAdapter;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.util.MessageUtils;

import java.util.Set;

public class ShareMediaActivity extends AppCompatActivity {
    private static final String TAG = ShareMediaActivity.class.getSimpleName();

    private TextManager mManager;
    private RecyclerView mRecyclerView;
    private ShareMediaAdapter mAdapter;
    private ImageView mImageView;
    private EditText mEditText;
    private ImageButton mSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_media);

        mManager = TextManager.getInstance(getBaseContext());

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mImageView = (ImageView) findViewById(R.id.media);
        mEditText = (EditText) findViewById(R.id.message);
        mSend = (ImageButton) findViewById(R.id.send);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ShareMediaAdapter(this, mManager.getThreadCursor());
        mAdapter.setOnClickListener(new ShareMediaAdapter.OnClickListener() {
            @Override
            public void onClick(Set<Contact> contacts) {
                mAdapter.toggleSelection(contacts);
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        final Attachment attachment = MessageUtils.getAttachment(getIntent());

        mImageView.setImageURI(attachment.getUri());

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mEditText.getText().toString();
                for (Set<Contact> contacts: mAdapter.getSelectedItems()) {
                    if (message.equals("")) {
                        mManager.send(attachment).to(contacts);
                    } else {
                        mManager.send(message, attachment).to(contacts);
                    }
                }
                if (mAdapter.getSelectedItemCount() == 1) {
                    // TODO: go to the thread instead
                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
                    startActivity(intent);
                } else if (mAdapter.getSelectedItemCount() > 1) {
                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
                    startActivity(intent);
                }
            }
        });

    }

}