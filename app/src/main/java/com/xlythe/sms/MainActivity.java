package com.xlythe.sms;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.xlythe.textmanager.text.TextManager;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  implements SimpleAdapter.SimpleViewHolder.ClickListener {
    private RecyclerView mRecyclerView;
    private SimpleAdapter mAdapter;
    private TextManager mManager;
    private ActionModeCallback mActionModeCallback = new ActionModeCallback();
    private ActionMode mActionMode;
    private AppBarLayout mAppbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAppbar = (AppBarLayout) findViewById(R.id.appbar);

        mManager = TextManager.getInstance(getBaseContext());

        ArrayList<Section> headers = new ArrayList<>();
//        headers.add(new Section(0+headers.size(),"Today"));
//        headers.add(new Section(2+headers.size(),"Yesterday"));
//        headers.add(new Section(7+headers.size(),"November"));
//        headers.add(new Section(list.size()+headers.size(),""));

        //Your RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecorationRes(this, R.drawable.divider));
        mAdapter = new SimpleAdapter(this, mManager.getThreads(), headers);

        mRecyclerView.setAdapter(mAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ComposeActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    public void onProfileClicked(int position) {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
        toggleSelection(position);
    }

    @Override
    public void onItemClicked(int position) {
        if (mActionMode != null) {
            toggleSelection(position);
        } else {
            Log.d("List","I was pressed!");
            Intent i = new Intent(getBaseContext(), MessageActivity.class);
            i.putExtra(MessageActivity.EXTRA_THREAD, mManager.getThreads().get(position));
            startActivity(i);
        }
    }

    @Override
    public boolean onItemLongClicked(int position) {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
        toggleSelection(position);
        return true;
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            mActionMode.finish();
        } else {
            mActionMode.setTitle(String.valueOf(count)+" selected");
            mActionMode.invalidate();
        }
    }

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate (R.menu.selected_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mRecyclerView.setNestedScrollingEnabled(false);
            mAppbar.setExpanded(true);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_remove:
                    mAdapter.removeItems(mAdapter.getSelectedItems());
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.removeItems(null);
            mode.finish();
            mActionMode = null;
            mRecyclerView.setNestedScrollingEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            Snackbar.make(findViewById(R.id.list), "Search not yet added", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
