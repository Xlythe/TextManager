package com.xlythe.sms;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.sms.util.ActionBarUtils;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Thread;

import java.util.Set;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle(ColorUtils.color(0x212121, "Info"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ActionBarUtils.grayUpArrow(this);

        Thread thread = getIntent().getParcelableExtra(MessageActivity.EXTRA_THREAD);
        Set<Contact> contacts = thread.getLatestMessage(this).get().getMembersExceptMe(this).get();

        final TextView name = (TextView) findViewById(R.id.name);
        final ImageView icon = (ImageView) findViewById(R.id.icon);

        if (contacts.size() == 1 && contacts.iterator().next().hasName()) {
            icon.setImageDrawable(new ProfileDrawable(this, contacts));
            name.setText(contacts.iterator().next().getDisplayName());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
