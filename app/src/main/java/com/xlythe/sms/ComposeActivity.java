package com.xlythe.sms;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;

public class ComposeActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_CONTACT = 10001;

    private TextView mContacts;
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
        mContacts = (TextView) findViewById(R.id.contacts);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_CONTACT) {
            if (resultCode == RESULT_OK) {
                mContacts.setText(intent.getStringExtra(ContactSearchActivity.EXTRA_NUMBER));
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

        if (id == R.id.action_send && mMessage.getText() != null && mContacts.getText() != null) {
            mManager.send(new Text.Builder()
                            .message(mMessage.getText().toString())
                            .addRecipient(this, mContacts.getText().toString())
                            .build()
            );
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
