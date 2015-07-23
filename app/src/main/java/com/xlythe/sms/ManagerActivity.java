package com.xlythe.sms;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.xlythe.textmanager.text.*;
import com.xlythe.textmanager.text.Thread;

import java.io.IOException;

public class ManagerActivity extends Activity {
    private ThreadAdapter mThreadAdapter;
    private ImageButton mCompose;
    private ListView mListView;
    private TextManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

//        TestMms test = new TestMms(getBaseContext());
//        try {
//            Log.d("main","trying");
//            byte[] resp = test.getPdu("http://snq2mosget.msg.eng.t-mobile.com/mms/wapenc?T=mavodi-1-13b-34d-2-66-626adf8");
////            String resp2 = new String(resp);
////            Log.d("resp", resp2+"");
//            RetrieveConf retrieveConf = (RetrieveConf) new PduParser(resp, true).parse();
//            if (null == retrieveConf) {
//                Log.d("receiver","failed");
//            }
//            PduPersister persister = PduPersister.getPduPersister(getBaseContext());
//            Uri msgUri;
//            try {
//                msgUri = persister.persist(retrieveConf, Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
//
//                // Use local time instead of PDU time
//                ContentValues values = new ContentValues(1);
//                values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
//                getBaseContext().getContentResolver().update(
//                        msgUri, values, null, null);
//            } catch (Exception e){
//
//            }
//        }catch (IOException ioe){
//
//        }

        mManager = TextManager.getInstance(getBaseContext());
        mCompose = (ImageButton) findViewById(R.id.compose);
        mListView = (ListView) findViewById(R.id.listView);

        // Start a new Message.
        mCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), ComposeActivity.class);
                startActivity(i);
            }
        });

        // Start Thread Activity.
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                Intent i = new Intent(getBaseContext(), ThreadActivity.class);
                i.putExtra(ThreadActivity.EXTRA_THREAD_ID, ((Thread) av.getItemAtPosition(position)).getThreadId());
                i.putExtra(ThreadActivity.EXTRA_ADDRESS, mManager.getSender((Thread) av.getItemAtPosition(position)).getDisplayName());
                i.putExtra(ThreadActivity.EXTRA_NUMBER, ((Thread) av.getItemAtPosition(position)).getAddress());
                startActivity(i);
            }
        });

        // Populate Adapter with list of threads.
        mThreadAdapter = new ThreadAdapter(getBaseContext(), R.layout.list_item_threads, mManager.getThreads());
        mListView.setAdapter(mThreadAdapter);
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