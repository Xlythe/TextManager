package com.xlythe.sms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class EditTextActivity extends AppCompatActivity {
    public static final String EXTRA_ITEM_ID = "ITEM_ID";
    int result = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_text);

        final EditText field = (EditText) findViewById(R.id.field);

        field.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_ITEM_ID, field.getText().toString());
                    setResult(result , intent);
                    finish();
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent();
        intent.putExtra("ITEM_ID", "");
        setResult(result, intent);
    }
}