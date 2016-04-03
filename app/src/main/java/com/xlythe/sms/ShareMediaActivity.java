package com.xlythe.sms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.xlythe.sms.adapter.ContactAdapter;
import com.xlythe.sms.decoration.DividerItemDecoration;
import com.xlythe.sms.view.ContactEditText;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.TextManager;

import java.util.ArrayList;

public class ShareMediaActivity extends AppCompatActivity implements ContactAdapter.ClickListener {
    private static final String TAG = ShareMediaActivity.class.getSimpleName();

    public static final String EXTRA_CONTACTS = "contacts";
    public static final String EXTRA_CURSOR = "cursor";

    private TextManager mManager;

    private ContactEditText mInputField;
    private RecyclerView mRecyclerView;

    private ContactAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_search);
        setResult(RESULT_OK);

        mManager = TextManager.getInstance(getBaseContext());

        mInputField = (ContactEditText) findViewById(R.id.field);
        mInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    setResult();
                    finish();
                    return true;
                }
                return false;
            }
        });
        mInputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                mAdapter.swapCursor(mManager.getContactCursor(mInputField.getPendingText()));
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ContactAdapter(this, mManager.getContactCursor(""));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        if (getIntent().hasExtra(EXTRA_CONTACTS)) {
            ArrayList<Contact> contacts = getIntent().getParcelableArrayListExtra(EXTRA_CONTACTS);
            mInputField.setContacts(contacts);
        }

        if (getIntent().hasExtra(EXTRA_CURSOR)) {
            mInputField.setSelection(getIntent().getIntExtra(EXTRA_CURSOR, mInputField.getText().length()));
        }
    }

    @Override
    public void onItemClicked(Contact contact) {
        mInputField.insert(contact);
    }

    @Override
    public void onBackPressed() {
        setResult();
        super.onBackPressed();
    }

    private void setResult() {
        // Insert any remaining text
        mInputField.insertPendingText();

        // Send result
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(EXTRA_CONTACTS, mInputField.getContacts());
        intent.putExtra(EXTRA_CURSOR, mInputField.getSelectionStart());
        setResult(RESULT_OK, intent);
    }
}