package com.xlythe.sms;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.TextThread;
import com.xlythe.textmanager.text.TextUser;

import java.net.URI;


public class ThreadActivity extends Activity {
    public static String EXTRA_THREAD = "thread";

    private CursorTextAdapter mTextAdapter;
    private ListView mListView;
    private ImageButton mSend;
    private EditText mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);

        mListView = (ListView) findViewById(R.id.messages);
        mSend = (ImageButton) findViewById(R.id.send);
        mMessage = (EditText) findViewById(R.id.message);

        // Get thread that was clicked.
        final TextThread mThread = (TextThread) getIntent().getSerializableExtra(EXTRA_THREAD);

        // Color bars to match thread color.
        Window window = getWindow();
        window.setStatusBarColor(ColorUtils.getDarkColor(mThread.getThreadId()));
        getActionBar().setBackgroundDrawable(new ColorDrawable(ColorUtils.getColor(mThread.getThreadId())));
        getActionBar().setTitle(mThread.getAddress());

        // Populate Adapter with list of texts.
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mTextAdapter = new CursorTextAdapter(getBaseContext(), mThread.getTextCursor(getBaseContext()));
                mListView.setAdapter(mTextAdapter);
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                String clausole = "_ID = ";
                Text n = mThread.getMessages(getBaseContext()).get(pos);
                clausole = clausole + n.getId();
                Uri uri = Uri.parse("content://mms-sms/conversations/" + mThread.getThreadId());
                Log.v("long clicked", "pos: " + clausole + " URI=" + uri);
                getContentResolver().delete(uri, clausole, null);
                return true;
            }
        });

        // Send message.
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextManager manager = TextManager.getInstance(getBaseContext());
                manager.send(new Text.Builder(getBaseContext())
                                .message(mMessage.getText().toString())
                                .recipient(TextUser.get(mThread.getAddress()))
                                .build()
                );
                mMessage.setText("");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
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
