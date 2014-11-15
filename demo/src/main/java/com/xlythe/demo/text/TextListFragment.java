package com.xlythe.demo.text;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageThread;

public class TextListFragment extends ListFragment {
    public static TextListFragment getInstance(MessageThread thread) {
        TextListFragment f = new TextListFragment();
        f.mThread = thread;
        return f;
    }

    // An adapter links a list of items to the ui to show
    private ArrayAdapter<Message> mAdapter;

    // The conversation thread
    private MessageThread mThread;

    /**
     * Called when this fragment is attached to an activity.
     * */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Create the adapter
        mAdapter = new ArrayAdapter<Message>(getActivity(), R.layout.list_item, R.id.title);
        setListAdapter(mAdapter);

        // And then we update the ui with all the threads
        mAdapter.addAll(mThread.getMessages());
    }
}
