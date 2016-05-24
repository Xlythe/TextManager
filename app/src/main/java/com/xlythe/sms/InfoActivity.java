package com.xlythe.sms;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.sms.adapter.MessageAttachmentAdapter;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InfoActivity extends AppCompatActivity {
    private ViewGroup mBlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextManager manager =  TextManager.getInstance(this);

        Thread thread = getIntent().getParcelableExtra(MessageActivity.EXTRA_THREAD);
        Set<Contact> contacts = manager.getMembersExceptMe(thread.getLatestMessage()).get();

        final TextView name = (TextView) findViewById(R.id.name);
        final ImageView icon = (ImageView) findViewById(R.id.icon);
        mBlock = (ViewGroup) findViewById(R.id.block);

        // TODO: add this back in when feature is implemented
        mBlock.setVisibility(View.GONE);

        if (contacts.size() == 1 && contacts.iterator().next().hasName()) {
            icon.setVisibility(View.VISIBLE);
            name.setVisibility(View.VISIBLE);
            icon.setImageDrawable(new ProfileDrawable(this, contacts));
            name.setText(contacts.iterator().next().getDisplayName());
        } else {
            icon.setVisibility(View.GONE);
            name.setVisibility(View.GONE);
        }

        // Add individual contacts
        // Maybe use another recycler view...?
        // Don't want to though
        for (Contact contact : contacts) {
            LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = vi.inflate(R.layout.info_stub, null);

            // fill in any details dynamically here
            TextView textView = (TextView) v.findViewById(R.id.number);
            textView.setText(contact.getNumber(this).get());

            // insert into main view
            ViewGroup insertPoint = (ViewGroup) findViewById(R.id.holder);
            insertPoint.addView(v, -1, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setHasFixedSize(true);

        List<Attachment> attachments = manager.getAttachments(thread);
        Collections.reverse(attachments);

        MessageAttachmentAdapter adapter = new MessageAttachmentAdapter(this, attachments);
        recyclerView.setAdapter(adapter);

        adapter.setOnClickListener(new MessageAttachmentAdapter.OnClickListener() {
            @Override
            public void onClick(Attachment attachment) {
                Intent i = new Intent(getBaseContext(), MediaActivity.class);
                i.putExtra(MediaActivity.EXTRA_ATTACHMENT, attachment);
                startActivity(i);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
