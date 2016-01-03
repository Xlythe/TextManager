package com.xlythe.sms;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xlythe.textmanager.text.Text;

import java.util.List;

/**
 * Created by Niko on 1/2/16.
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Text> mTexts;

    // Duration between considering a text to be part of the same message, or split into different messages
    private static final long SPLIT_DURATION = 60 * 1000;

    private static final int TYPE_TOP_RIGHT = 0;
    private static final int TYPE_MIDDLE_RIGHT = 1;
    private static final int TYPE_BOTTOM_RIGHT = 2;
    private static final int TYPE_SINGLE_RIGHT = 3;
    private static final int TYPE_TOP_LEFT = 4;
    private static final int TYPE_MIDDLE_LEFT = 5;
    private static final int TYPE_BOTTOM_LEFT = 6;
    private static final int TYPE_SINGLE_LEFT = 7;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class TopRightViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public TopRightViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }
    }

    public static class MiddleRightViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public MiddleRightViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }
    }

    public static class BottomRightViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public BottomRightViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }
    }

    public static class SingleRightViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public SingleRightViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }
    }

    public static class TopLeftViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public TopLeftViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }
    }

    public static class MiddleLeftViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public MiddleLeftViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }
    }

    public static class BottomLeftViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public BottomLeftViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }
    }

    public static class SingleLeftViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public SingleLeftViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MessageAdapter(List<Text> texts) {
        mTexts = texts;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_TOP_RIGHT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_top, parent, false);
            return new TopRightViewHolder(v);
        } else if (viewType == TYPE_MIDDLE_RIGHT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_middle, parent, false);
            return new MiddleRightViewHolder(v);
        } else if (viewType == TYPE_BOTTOM_RIGHT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_bottom, parent, false);
            return new BottomRightViewHolder(v);
        } else if (viewType == TYPE_SINGLE_RIGHT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.right_single, parent, false);
            return new SingleRightViewHolder(v);
        } else if (viewType == TYPE_TOP_LEFT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_top, parent, false);
            return new TopLeftViewHolder(v);
        } else if (viewType == TYPE_MIDDLE_LEFT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_middle, parent, false);
            return new MiddleLeftViewHolder(v);
        } else if (viewType == TYPE_BOTTOM_LEFT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_bottom, parent, false);
            return new BottomLeftViewHolder(v);
        } else if (viewType == TYPE_SINGLE_LEFT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_single, parent, false);
            return new SingleLeftViewHolder(v);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        Text text = mTexts.get(position);

        // Get the date of the current, previous and next message.
        long dateCurrent = text.getTimestamp();
        long datePrevious = 0;
        long dateNext = 0;

        // Get the sender of the current, previous and next message. (returns true if you)
        boolean userCurrent = text.isIncoming();
        boolean userPrevious = text.isIncoming();
        boolean userNext = !text.isIncoming();

        // Check if previous message exists, then get the date and sender.
        if (position != 0) {
            Text nextText = mTexts.get(position - 1);
            datePrevious = nextText.getTimestamp();
            userPrevious = nextText.isIncoming();
        }

        // Check if next message exists, then get the date and sender.
        if (position != mTexts.size()-1) {
            Text nextText = mTexts.get(position + 1);
            dateNext = nextText.getTimestamp();
            userNext = nextText.isIncoming();
        }

        // Calculate time gap.
        boolean largePC = dateCurrent - datePrevious > SPLIT_DURATION;
        boolean largeCN = dateNext - dateCurrent > SPLIT_DURATION;

        if (!userCurrent && (userPrevious || largePC) && (!userNext && !largeCN)) {
            return TYPE_TOP_RIGHT;
        } else if (!userCurrent && (!userPrevious && !largePC) && (!userNext && !largeCN)){
            return TYPE_MIDDLE_RIGHT;
        } else if (!userCurrent && (!userPrevious && !largePC)){
            return TYPE_BOTTOM_RIGHT;
        } else if (!userCurrent) {
            return TYPE_SINGLE_RIGHT;
        } else if ((!userPrevious || largePC) && (userNext && !largeCN)) {
            return TYPE_TOP_LEFT;
        } else if ((userPrevious && !largePC) && (userNext && !largeCN)){
            return TYPE_MIDDLE_LEFT;
        } else if (userPrevious && !largePC){
            return TYPE_BOTTOM_LEFT;
        } else {
            return TYPE_SINGLE_LEFT;
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if (holder instanceof TopRightViewHolder) {
            TopRightViewHolder trvh = (TopRightViewHolder) holder;
            trvh.mTextView.setText(mTexts.get(position).getBody());
        } else if (holder instanceof MiddleRightViewHolder) {
            MiddleRightViewHolder trvh = (MiddleRightViewHolder) holder;
            trvh.mTextView.setText(mTexts.get(position).getBody());
        } else if (holder instanceof BottomRightViewHolder) {
            BottomRightViewHolder trvh = (BottomRightViewHolder) holder;
            trvh.mTextView.setText(mTexts.get(position).getBody());
        } else if (holder instanceof SingleRightViewHolder) {
            SingleRightViewHolder trvh = (SingleRightViewHolder) holder;
            trvh.mTextView.setText(mTexts.get(position).getBody());
        } else if (holder instanceof TopLeftViewHolder) {
            TopLeftViewHolder trvh = (TopLeftViewHolder) holder;
            trvh.mTextView.setText(mTexts.get(position).getBody());
        } else if (holder instanceof MiddleLeftViewHolder) {
            MiddleLeftViewHolder trvh = (MiddleLeftViewHolder) holder;
            trvh.mTextView.setText(mTexts.get(position).getBody());
        } else if (holder instanceof BottomLeftViewHolder) {
            BottomLeftViewHolder trvh = (BottomLeftViewHolder) holder;
            trvh.mTextView.setText(mTexts.get(position).getBody());
        } else if (holder instanceof SingleLeftViewHolder) {
            SingleLeftViewHolder trvh = (SingleLeftViewHolder) holder;
            trvh.mTextView.setText(mTexts.get(position).getBody());
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mTexts.size();
    }
}