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
import android.widget.EditText;
import android.widget.TextView;

import com.xlythe.sms.adapter.ContactAdapter;
import com.xlythe.textmanager.text.TextManager;

public class ContactSearchActivity extends AppCompatActivity {
    public static final String EXTRA_NUMBER = "number";

    private TextManager mManager;

    private EditText mInputField;
    private RecyclerView mRecyclerView;

    private ContactAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_search);
        setResult(RESULT_CANCELED);

        mManager = TextManager.getInstance(getBaseContext());

        mInputField = (EditText) findViewById(R.id.field);
        mInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_NUMBER, mInputField.getText().toString());
                    setResult(RESULT_OK, intent);
                    finish();
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
                mAdapter.swapCursor(mManager.getContactCursor(s.toString()));
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ContactAdapter(this, mManager.getContactCursor(""));
        mRecyclerView.setAdapter(mAdapter);
    }
}