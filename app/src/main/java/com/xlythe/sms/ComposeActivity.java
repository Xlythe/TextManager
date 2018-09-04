package com.xlythe.sms;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.xlythe.sms.view.ContactEditText;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.util.MessageUtils;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static com.xlythe.sms.ContactSearchActivity.EXTRA_CONTACTS;
import static com.xlythe.sms.ContactSearchActivity.EXTRA_CURSOR;

public class ComposeActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_CONTACT = 10001;

    private ContactEditText mContacts;
    private TextView mMessage;
    private Activity mActivity;
    private TextManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_compose);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mManager = TextManager.getInstance(getBaseContext());
        mContacts = (ContactEditText) findViewById(R.id.contacts);
        mMessage = (TextView) findViewById(R.id.message);
        mActivity = this;

        mContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startContactSearch();
            }
        });
        mContacts.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            private boolean initialRun;

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!initialRun) {
                    initialRun = true;
                    return;
                }

                if (hasFocus) {
                    startContactSearch();
                }
            }
        });

        if (savedInstanceState == null) {
            // This is the first time this Activity is launched. Lets check the intent to prepopulate the message.
            Intent intent = getIntent();
            String action = intent.getAction();
            if (Intent.ACTION_SEND.equals(action)
                    || Intent.ACTION_SENDTO.equals(action)
                    || Intent.ACTION_VIEW.equals(action)) {
                String[] recipients = MessageUtils.getRecipients(intent);
                String body = MessageUtils.getBody(intent);

                if (recipients != null) {
                    String address = TextUtils.join(";", recipients);
                    if (!TextUtils.isEmpty(address) && !address.endsWith(";")) {
                        address += ";";
                    }
                    mContacts.setText(address);
                    mContacts.setSelection(mContacts.getText().length());
                }
                mMessage.setText(body);
            }
        }
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @TargetApi(21)
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
                        return true;
                    }
                });
    }

    @TargetApi(21)
    @Override
    public void onActivityReenter(int resultCode, Intent intent) {
        super.onActivityReenter(resultCode, intent);
        intent.setExtrasClassLoader(getClassLoader());
        if (intent.hasExtra(EXTRA_CONTACTS)) {
            ArrayList<Contact> contacts = intent.getParcelableArrayListExtra(EXTRA_CONTACTS);
            mContacts.setContacts(contacts);
            int cursor = intent.getIntExtra(EXTRA_CURSOR, mContacts.getText().length());
            mContacts.setSelection(cursor);
        }
        postponeEnterTransition();
        scheduleStartPostponedTransition(mContacts);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_CONTACT) {
            if (resultCode == RESULT_OK) {
                // Yay! We got contacts back! Lets add them to our EditText
                ArrayList<Contact> contacts = intent.getParcelableArrayListExtra(EXTRA_CONTACTS);
                mContacts.setContacts(contacts);
                int cursor = intent.getIntExtra(EXTRA_CURSOR, mContacts.getText().length());
                mContacts.setSelection(cursor);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_send) {
            if (!TextUtils.isEmpty(mMessage.getText().toString()) && !TextUtils.isEmpty(mContacts.getText().toString())) {
                mManager.send(mMessage.getText().toString()).to(mContacts.getContacts());
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startContactSearch() {
        Intent intent = new Intent(getApplicationContext(), ContactSearchActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_CONTACTS, mContacts.getContacts());
        intent.putExtra(EXTRA_CURSOR, mContacts.getSelectionStart());
        if (Build.VERSION.SDK_INT >= 21) {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(mActivity,
                    Pair.create((View) mContacts, "edit_text"));
            startActivityForResult(intent, REQUEST_CODE_CONTACT, options.toBundle());
        } else {
            startActivityForResult(intent, REQUEST_CODE_CONTACT);
        }
    }
}
