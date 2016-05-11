package com.xlythe.sms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.xlythe.sms.adapter.ContactIconAdapter;
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
    private RecyclerView mIconRecyclerView;
    private ShareMediaAdapter mAdapter;
    private ContactIconAdapter mContactAdapter;
    private ImageView mImageView;
    private EditText mEditText;
    private ImageButton mSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_media);

        mManager = TextManager.getInstance(getBaseContext());

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mIconRecyclerView = (RecyclerView) findViewById(R.id.icon_list);
        mImageView = (ImageView) findViewById(R.id.media);
        mEditText = (EditText) findViewById(R.id.message);
        mSend = (ImageButton) findViewById(R.id.send);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ShareMediaAdapter(this, mManager.getThreadCursor());
        mAdapter.setOnClickListener(new ShareMediaAdapter.OnClickListener() {
            @Override
            public void onClick(Set<Contact> contacts) {
                // Toggles the original adapters data so everything is in sync
                mAdapter.toggleSelection(contacts);
                mContactAdapter.updateData(mAdapter.getSelectedItems());
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        mIconRecyclerView.setHasFixedSize(true);
        //TODO: maybe change to true for RTL
        mIconRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mContactAdapter = new ContactIconAdapter(this, mAdapter.getSelectedItems());
        mContactAdapter.setOnClickListener(new ContactIconAdapter.OnClickListener() {
            @Override
            public void onClick(Set<Contact> contacts) {
                // Toggles the original adapters data so everything is in sync
                mAdapter.toggleSelection(contacts);
                mContactAdapter.updateData(mAdapter.getSelectedItems());
            }
        });
        mIconRecyclerView.setAdapter(mContactAdapter);

        final Attachment attachment = MessageUtils.getAttachment(getIntent());
        final String incoming = MessageUtils.getBody(getIntent());

        if (incoming != null) {
            mEditText.setText(incoming);
        }

        if (attachment == null) {
            mImageView.setVisibility(View.GONE);
        } else {
            mImageView.setImageURI(attachment.getUri());
        }

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mEditText.getText().toString();
                for (Set<Contact> contacts: mAdapter.getSelectedItems()) {
                    if (message.equals("") && attachment != null) {
                        mManager.send(attachment).to(contacts);
                    } else if (attachment != null) {
                        mManager.send(message, attachment).to(contacts);
                    } else {
                        mManager.send(message).to(contacts);
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