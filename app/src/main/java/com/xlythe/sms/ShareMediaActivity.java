package com.xlythe.sms;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.xlythe.sms.adapter.ContactIconAdapter;
import com.xlythe.sms.adapter.ShareMediaAdapter;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.util.MessageUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.xlythe.sms.ContactSearchActivity.EXTRA_CONTACTS;

public class ShareMediaActivity extends AppCompatActivity {
    private static final String TAG = ShareMediaActivity.class.getSimpleName();
    private static final int REQUEST_CODE_CONTACT = 10001;

    private TextManager mManager;
    private RecyclerView mRecyclerView;
    private RecyclerView mIconRecyclerView;
    private ShareMediaAdapter mAdapter;
    private ContactIconAdapter mContactAdapter;
    private ImageView mImageView;
    private EditText mEditText;
    private ImageView mSend;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_media);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mManager = TextManager.getInstance(getBaseContext());

        mRecyclerView = findViewById(R.id.list);
        mIconRecyclerView = findViewById(R.id.icon_list);
        mImageView = findViewById(R.id.media);
        mEditText = findViewById(R.id.message);
        mSend = findViewById(R.id.send);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ShareMediaAdapter(this, mManager.getThreadCursor());
        mAdapter.setOnClickListener(contacts -> {
            // Toggles the original adapters data so everything is in sync
            mAdapter.toggleSelection(contacts);
            mContactAdapter.updateData(mAdapter.getSelectedItems());
        });
        mRecyclerView.setAdapter(mAdapter);

        mIconRecyclerView.setHasFixedSize(true);
        //TODO: maybe change to true for RTL
        mIconRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mContactAdapter = new ContactIconAdapter(this, mAdapter.getSelectedItems());
        mContactAdapter.setOnClickListener(contacts -> {
            // Toggles the original adapters data so everything is in sync
            mAdapter.toggleSelection(contacts);
            mContactAdapter.updateData(mAdapter.getSelectedItems());
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
            Glide.with(getBaseContext())
                    .load(attachment.getUri())
                    .into(mImageView);
        }

        mSend.setOnClickListener(v -> {
            String message = mEditText.getText().toString();
            for (Set<Contact> contacts: mAdapter.getSelectedItems()) {
                if (message.isEmpty() && attachment != null) {
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
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_CONTACT) {
            if (resultCode == RESULT_OK) {
                // Yay! We got contacts back! Lets add them to our EditText
                ArrayList<Contact> contacts = intent.getParcelableArrayListExtra(EXTRA_CONTACTS);
                for (Contact contact : contacts) {
                    Set<Contact> set = new HashSet<>();
                    set.add(contact);
                    mAdapter.toggleSelection(set);
                    mContactAdapter.updateData(mAdapter.getSelectedItems());
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share_media, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            startContactSearch();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startContactSearch() {
        Intent intent = new Intent(getApplicationContext(), ContactSearchActivity.class);
        startActivityForResult(intent, REQUEST_CODE_CONTACT);
    }
}