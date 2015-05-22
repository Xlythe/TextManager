package com.xlythe.sms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.text.TextManager;

import java.io.Serializable;

public class ManagerActivity extends Activity {
    private ThreadAdapter mArrayAdapter;
    private Button mCompose;
    private ListView mListView;
    private TextManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mManager = TextManager.getInstance(getBaseContext());
        mCompose = (Button) findViewById(R.id.compose);
        mListView = (ListView) findViewById(R.id.listView);

        // Start Compose activity.
        mCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), ComposeActivity.class);
                startActivity(i);
            }
        });

        // Fill adapter
        mArrayAdapter = new ThreadAdapter(getBaseContext(), mManager.getThreads());
        mListView.setAdapter(mArrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                Intent i = new Intent(getBaseContext(), ThreadActivity.class);
                i.putExtra(ThreadActivity.EXTRA_THREAD, mManager.getThreads().get(position));
                startActivity(i);
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