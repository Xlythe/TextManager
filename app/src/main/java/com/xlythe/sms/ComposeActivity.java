package com.xlythe.sms;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.xlythe.sms.util.MessageUtils;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;

public class ComposeActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_CONTACT = 10001;

    private EditText mContacts;
    private TextView mMessage;
    private Activity mActivity;
    private TextManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mManager = TextManager.getInstance(getBaseContext());
        mContacts = (EditText) findViewById(R.id.contacts);
        mMessage = (TextView) findViewById(R.id.message);
        mActivity = this;

        mContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ContactSearchActivity.class);
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(mActivity,
                            Pair.create((View) mContacts, "edit_text"));
                    startActivityForResult(intent, REQUEST_CODE_CONTACT, options.toBundle());
                } else {
                    startActivityForResult(intent, REQUEST_CODE_CONTACT);
                }
            }
        });

        // Hack to force the up arrow to be white
        final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        if (savedInstanceState == null) {
            // This is the first time this Activity is launched. Lets check the intent to prepopulate the message.
            Intent intent = getIntent();
            String action = intent.getAction();
            if (Intent.ACTION_SEND.equals(action)
                    || Intent.ACTION_SENDTO.equals(action)
                    || Intent.ACTION_VIEW.equals(action)) {
                String[] recipients = MessageUtils.getRecipients(intent);
                String body = MessageUtils.getBody(intent);


                mContacts.setText(TextUtils.join(";", recipients));
                mMessage.setText(body);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_CONTACT) {
            if (resultCode == RESULT_OK) {
                String number = intent.getStringExtra(ContactSearchActivity.EXTRA_NUMBER);
                Contact contact = intent.getParcelableExtra(ContactSearchActivity.EXTRA_CONTACT);

                if (contact != null) {
                    mContacts.setText(contact.getDisplayName());
                } else {
                    mContacts.setText(PhoneNumberUtils.formatNumber(number));
                }
                mContacts.setTag(number);
                mContacts.setSelection(mContacts.getText().length());
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
                mManager.send(new Text.Builder()
                        .message(mMessage.getText().toString())
                        .addRecipient(this, mContacts.getTag().toString())
                        .build());
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
