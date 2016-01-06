package com.xlythe.sms;

import android.content.Intent;
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
import com.xlythe.textmanager.text.Thread;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements SimpleAdapter.SimpleViewHolder.ClickListener {
    private RecyclerView mRecyclerView;
    private SimpleAdapter mAdapter;
    private TextManager mManager;
    private List<Thread> mThreads;
    private ActionModeCallback mActionModeCallback = new ActionModeCallback();
    private ActionMode mActionMode;
    private AppBarLayout mAppbar;

    private static final long ONE_MINUTE = 60 * 1000;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    private static final long ONE_DAY = 24 * ONE_HOUR;
    private static final long ONE_WEEK = 7 * ONE_DAY;
    private static final long ONE_MONTH = 4 * ONE_WEEK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAppbar = (AppBarLayout) findViewById(R.id.appbar);

        mManager = TextManager.getInstance(getBaseContext());
        mThreads = mManager.getThreads();

        ArrayList<Section> headers = new ArrayList<>();

        Section section = new Section(0, "");
        for (int i = 0; i < mThreads.size(); i++) {
            long date = mThreads.get(i).getLatestMessage().getTimestamp();
            long time = System.currentTimeMillis() - date;
            if (time < ONE_DAY && !section.mTitle.equals("Today")) {
                section = new Section(i+headers.size(),"Today");
                headers.add(section);
                // TODO: think of a better way for headers
                mThreads.add(mThreads.get(0));
            } else if (time > ONE_DAY && time < 2 * ONE_DAY && !section.mTitle.equals("Yesterday")) {
                section = new Section(i+headers.size(),"Yesterday");
                headers.add(section);
                mThreads.add(mThreads.get(0));
            } else if (time > 2 * ONE_DAY && time < ONE_WEEK && !section.mTitle.equals("This week")) {
                section = new Section(i+headers.size(),"This week");
                headers.add(section);
                mThreads.add(mThreads.get(0));
            } else if (time > ONE_WEEK && time < ONE_MONTH && !section.mTitle.equals("This month")) {
                section = new Section(i+headers.size(),"This month");
                headers.add(section);
                mThreads.add(mThreads.get(0));
            }
        }

        //Your RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecorationRes(this, R.drawable.divider));
        mAdapter = new SimpleAdapter(this, mThreads, headers);

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
            Intent i = new Intent(getBaseContext(), MessageActivity.class);
            i.putExtra(MessageActivity.EXTRA_THREAD, mThreads.get(position));
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
