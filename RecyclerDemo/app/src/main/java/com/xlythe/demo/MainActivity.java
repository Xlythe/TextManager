package com.xlythe.demo;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements SimpleAdapter.SimpleViewHolder.ClickListener {
    RecyclerView mRecyclerView;
    SimpleAdapter mAdapter;

    private ActionModeCallback mActionModeCallback = new ActionModeCallback();
    private ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ArrayList<Thread> list = new ArrayList<>();
        list.add(new Thread("Will Harmon", "How\'s it going, did you get the ...", "10 min", null, 6, getColor(R.color.icon)));
        Thread oriana = new Thread("Oriana", "picture", "2:17pm", null, 0, getColor(R.color.icon));
        oriana.mDrawable = BitmapFactory.decodeResource(getResources(), R.drawable.oriana);
        list.add(oriana);
        list.add(new Thread("Mom", "Random message to show this off ...", "6:22pm", null, 0, getColor(R.color.purple)));
        list.add(new Thread("(216) 283-3928", "Hopefully Will likes this new design ...", "1:05pm", null, 1, getColor(R.color.pink)));
        list.add(new Thread("Josh Cheston", "Make nick stop", "10 min", null, 0, getColor(R.color.icon)));
        list.add(new Thread("Alex Goldstein", "hi", "10 min", null, 0, getColor(R.color.icon)));
        list.add(new Thread("Natalie", "The language!", "10 min", null, 0, getColor(R.color.icon)));
        list.add(new Thread("Tim Nerozzi", "My only big gripe is that Chewbacca ...", "10 min", null, 0, getColor(R.color.icon)));
        list.add(new Thread("Alex Bourdakos", "I agree", "10 min", null, 0, getColor(R.color.icon)));
        list.add(new Thread("Cyrus Basseri", "Unless you just want it to be on your ...", "10 min", null, 0, getColor(R.color.icon)));
        list.add(new Thread("Mark Steffl", "Noice", "10 min", null, 0, getColor(R.color.icon)));

        ArrayList<Section> headers = new ArrayList<>();
        headers.add(new Section(0+headers.size(),"Today"));
        headers.add(new Section(2+headers.size(),"Yesterday"));
        headers.add(new Section(7+headers.size(),"November"));
        headers.add(new Section(list.size()+headers.size(),""));

        //Your RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecorationRes(this, R.drawable.divider));
        mAdapter = new SimpleAdapter(this, list, headers);

//        List<ManagerAdapter.Section> sections = new ArrayList<>();
//        sections.add(new ManagerAdapter.Section(0,"Today"));
//        sections.add(new ManagerAdapter.Section(2,"Yesterday"));
//        sections.add(new ManagerAdapter.Section(7,"November"));
//        sections.add(new ManagerAdapter.Section(list.size(), ""));

        mRecyclerView.setAdapter(mAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Compose not yet added", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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
            mAdapter.clearSelection();
            mActionMode = null;
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
