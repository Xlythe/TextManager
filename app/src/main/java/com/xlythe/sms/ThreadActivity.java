package com.xlythe.sms;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.xlythe.textmanager.text.CustomTextCursor;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.TextUser;


public class ThreadActivity extends Activity {
    public static String EXTRA_THREAD_ID = "threadId";

    private CursorTextAdapter mTextAdapter;
    private ListView mListView;
    private ImageButton mSend;
    private EditText mMessage;
    private TextManager mManager;
    private String mAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);

        mManager = TextManager.getInstance(getBaseContext());
        mListView = (ListView) findViewById(R.id.messages);
        mSend = (ImageButton) findViewById(R.id.send);
        mMessage = (EditText) findViewById(R.id.message);

        // Get threadId that was clicked.
        final long mThreadId = getIntent().getLongExtra(EXTRA_THREAD_ID, -1);

        // Get address.
        mAddress = mManager.getFirstMessage(mThreadId).getText().getAddress();

        // Color bars to match thread color.
        Window window = getWindow();
        window.setStatusBarColor(ColorUtils.getDarkColor(mThreadId));
        getActionBar().setBackgroundDrawable(new ColorDrawable(ColorUtils.getColor(mThreadId)));
        getActionBar().setTitle(mAddress);

        // Populate Adapter with list of texts.
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mTextAdapter = new CursorTextAdapter(getBaseContext(), mManager.getTextCursor(mThreadId));
                mListView.setAdapter(mTextAdapter);
            }
        });

        // Delete a message on long press.
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
                Text text = (Text) v.getTag();
                //mManager.delete(text);
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
                                .recipient(TextUser.get(mAddress))
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
