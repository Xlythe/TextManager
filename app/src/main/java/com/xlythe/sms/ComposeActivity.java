package com.xlythe.sms;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.TextUser;


public class ComposeActivity extends Activity {

    private Button mSend;
    private EditText mNumber;
    private EditText mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        mSend = (Button) findViewById(R.id.send);
        mNumber = (EditText) findViewById(R.id.number);
        mMessage = (EditText) findViewById(R.id.message);

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextManager manager = TextManager.getInstance(getBaseContext());
                manager.send(new Text.Builder(getBaseContext())
                                .message(mMessage.getText().toString())
                                .recipient(TextUser.get(mNumber.getText().toString()))
                                .build()
                );
                mMessage.setText("");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
