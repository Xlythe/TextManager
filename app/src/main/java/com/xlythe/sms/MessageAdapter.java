package com.xlythe.sms;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.textmanager.Attachment;
import com.xlythe.textmanager.text.ImageAttachment;
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
    private static final int TYPE_ATTACHMENT = 8;
    private static final int TYPE_FAILED = 9;

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }
        public void setText(String text){
            mTextView.setText(text);
        }
    }

    public static abstract class LeftHolder extends ViewHolder {
        public LeftHolder(View v) {
            super(v);
        }
        public void setColor(int color){
            mTextView.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }
    }

    public static class TopRightViewHolder extends ViewHolder {
        public TopRightViewHolder(View v) {
            super(v);
        }
    }

    public static class MiddleRightViewHolder extends ViewHolder {
        public MiddleRightViewHolder(View v) {
            super(v);
        }
    }

    public static class BottomRightViewHolder extends ViewHolder {
        public BottomRightViewHolder(View v) {
            super(v);
        }
    }

    public static class SingleRightViewHolder extends ViewHolder {
        public SingleRightViewHolder(View v) {
            super(v);
        }
    }

    public static class TopLeftViewHolder extends LeftHolder {
        public TopLeftViewHolder(View v) {
            super(v);
        }
    }

    public static class MiddleLeftViewHolder extends LeftHolder {
        public MiddleLeftViewHolder(View v) {
            super(v);
        }
    }

    public static class BottomLeftViewHolder extends LeftHolder {
        public BottomLeftViewHolder(View v) {
            super(v);
        }
    }

    public static class SingleLeftViewHolder extends LeftHolder {
        public SingleLeftViewHolder(View v) {
            super(v);
        }
    }

    public static class AttachmentHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;
        public AttachmentHolder(View v) {
            super(v);
            mImageView = (ImageView) v.findViewById(R.id.image);
        }
        public void setImage(Text text){
            mImageView.setImageURI(Uri.parse(text.getAttachment().getUri()));
        }
    }

    public static class FailedHolder extends LeftHolder implements View.OnClickListener {
        private ClickListener mListener;

        public FailedHolder(View v, ClickListener listener) {
            super(v);
            mListener = listener;
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onItemClicked(getAdapterPosition());
            }
        }

        public interface ClickListener {
            void onItemClicked(int position);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)

    private FailedHolder.ClickListener mClickListener;
    public MessageAdapter(Context context, List<Text> texts) {
        mClickListener = (FailedHolder.ClickListener) context;
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
        } else if (viewType == TYPE_ATTACHMENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.attachment, parent, false);
            return new AttachmentHolder(v);
        } else if (viewType == TYPE_FAILED) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.left_single, parent, false);
            return new FailedHolder(v, mClickListener);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        Text text = mTexts.get(position);

        if (text.isMms()){
            if (text.getAttachment() != null) {
                return TYPE_ATTACHMENT;
            }
            return TYPE_FAILED;
        }

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
        if (holder instanceof ViewHolder) {
            ((ViewHolder) holder).setText(mTexts.get(position).getBody());
            if (holder instanceof FailedHolder) {
                ((LeftHolder) holder).setColor(Color.BLUE);
                ((LeftHolder) holder).setText("New attachment to download");
            }
            if (holder instanceof LeftHolder) {
                ((LeftHolder) holder).setColor(Color.BLUE);
            }
        } else if (holder instanceof AttachmentHolder) {
            ((AttachmentHolder) holder).setImage(mTexts.get(position));
        }



    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mTexts.size();
    }
}