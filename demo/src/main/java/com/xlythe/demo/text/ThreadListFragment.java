package com.xlythe.demo.text;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.xlythe.textmanager.MessageManager;
import com.xlythe.textmanager.MessageThread;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.TextThread;

public class ThreadListFragment extends ListFragment {

    // An adapter links a list of items to the ui to show
    private ArrayAdapter<MessageThread> mAdapter;

    // A message manager is from our library
    private MessageManager mManager;

    /**
     * Called when this fragment is attached to an activity.
     * */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Some voodoo magic to make LitFragment a bit easier to use.
        mAdapter = new ArrayAdapter<MessageThread>(getActivity(), R.layout.list_item, R.id.title);
        setListAdapter(null);
        getListView().setAdapter(mAdapter);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                onItemSelected(mAdapter.getItem(position));
            }
        });

        // We create a TextManager (this could be anything - even Facebook)

        mManager = new TextManager(getActivity()); //used to create TextThread.java

        // And then we update the ui with all the threads
        mAdapter.addAll(mManager.getThreads());
    }

    protected void onItemSelected(MessageThread item) {
        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, TextListFragment.getInstance(item))
                .commit();
    }
}
