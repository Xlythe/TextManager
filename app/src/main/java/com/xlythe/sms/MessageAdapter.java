package com.xlythe.sms;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.sms.util.DateFormatter;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.util.SimpleLruCache;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = MessageAdapter.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final int CACHE_SIZE = 50;

    private Text.TextCursor mCursor;
    private Context mContext;
    private FailedHolder.ClickListener mClickListener;
    private final SimpleLruCache<Integer, Text> mTextLruCache = new SimpleLruCache<>(CACHE_SIZE);

    // Duration between considering a text to be part of the same message, or split into different messages
    private static final long SPLIT_DURATION = 60 * 1000;

    private static final int TYPE_TOP_RIGHT    = 0;
    private static final int TYPE_MIDDLE_RIGHT = 1;
    private static final int TYPE_BOTTOM_RIGHT = 2;
    private static final int TYPE_SINGLE_RIGHT = 3;
    private static final int TYPE_TOP_LEFT     = 4;
    private static final int TYPE_MIDDLE_LEFT  = 5;
    private static final int TYPE_BOTTOM_LEFT  = 6;
    private static final int TYPE_SINGLE_LEFT  = 7;
    private static final int TYPE_ATTACHMENT   = 8;
    private static final int TYPE_FAILED       = 9;

    public static abstract class TextViewHolder extends RecyclerView.ViewHolder {
        private Text mText;

        public TextViewHolder(View v) {
            super(v);
        }

        public void setText(Text text) {
            mText = text;
        }

        public Text getText() {
            return mText;
        }
    }

    public static abstract class ViewHolder extends TextViewHolder {
        public TextView mTextView;

        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
        }

        public void setText(Text text) {
            super.setText(text);
            setText(text.getBody());
        }

        public void setText(String body) {
            mTextView.setText(body);
        }
    }

    public static abstract class LeftHolder extends ViewHolder {
        public LeftHolder(View v) {
            super(v);
        }

        @Override
        public void setText(Text text) {
            super.setText(text);
        }

        public void setColor(int color) {
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

    public static abstract class ProfileViewHolder extends LeftHolder {
        private CircleImageView mProfile;

        public ProfileViewHolder(View v) {
            super(v);
            mProfile = (CircleImageView) v.findViewById(R.id.profile_image);
        }

        public void setProfile(Context context){
            ProfileDrawable border = new ProfileDrawable(context,
                    getText().getSender().getDisplayName().charAt(0),
                    ColorUtils.getColor(Long.parseLong(getText().getThreadId())),
                    getText().getSender().getPhotoUri());
            mProfile.setImageDrawable(border);
        }
    }

    public static class TopLeftViewHolder extends ProfileViewHolder {
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

    public static class SingleLeftViewHolder extends ProfileViewHolder {
        public SingleLeftViewHolder(View v) {
            super(v);
        }
    }

    public static class AttachmentHolder extends TextViewHolder {
        ImageView mImageView;

        public AttachmentHolder(View v) {
            super(v);
            mImageView = (ImageView) v.findViewById(R.id.image);
        }

        public void setImage(Context context, Text text) {
            Picasso.with(context).load(text.getAttachments().get(0).getUri()).into(mImageView);
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
        public void setText(Text text) {
            super.setText(text);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onItemClicked(getText());
            }
        }

        public interface ClickListener {
            void onItemClicked(Text text);
        }
    }

    public MessageAdapter(Context context, Text.TextCursor cursor) {
        mCursor = cursor;
        mContext = context;
    }

    public void setOnClickListener(FailedHolder.ClickListener onClickListener) {
        mClickListener = onClickListener;
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
        Text text = getText(position);

        Text prevText = null;
        if (position > 0) {
            prevText = getText(position - 1);
        }

        Text nextText = null;
        if (position + 1 < mCursor.getCount()) {
            nextText = getText(position + 1);
        }

        if (text.isMms()) {
            if (!text.getAttachments().isEmpty()) {
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
        if (prevText != null) {
            datePrevious = prevText.getTimestamp();
            userPrevious = prevText.isIncoming();
        }

        // Check if next message exists, then get the date and sender.
        if (nextText != null) {
            dateNext = nextText.getTimestamp();
            userNext = nextText.isIncoming();
        }

        // Calculate time gap.
        boolean largePC = dateCurrent - datePrevious > SPLIT_DURATION;
        boolean largeCN = dateNext - dateCurrent > SPLIT_DURATION;

        if (DEBUG) {
            Log.d(TAG, String.format(
                    "userCurrent=%s, userPrevious=%s, userNext=%s," +
                    "dateCurrent=%s, datePrevious=%s, dateNext=%s," +
                    "largePC=%s, largeCN=%s",
                    userCurrent, userPrevious, userNext,
                    dateCurrent, datePrevious, dateNext,
                    largePC, largeCN));
        }

        if (!userCurrent && (userPrevious || largePC) && (!userNext && !largeCN)) {
            return TYPE_TOP_RIGHT;
        } else if (!userCurrent && (!userPrevious && !largePC) && (!userNext && !largeCN)) {
            return TYPE_MIDDLE_RIGHT;
        } else if (!userCurrent && (!userPrevious && !largePC)) {
            return TYPE_BOTTOM_RIGHT;
        } else if (!userCurrent) {
            return TYPE_SINGLE_RIGHT;
        } else if ((!userPrevious || largePC) && (userNext && !largeCN)) {
            return TYPE_TOP_LEFT;
        } else if ((userPrevious && !largePC) && (userNext && !largeCN)) {
            return TYPE_MIDDLE_LEFT;
        } else if (userPrevious && !largePC) {
            return TYPE_BOTTOM_LEFT;
        } else {
            return TYPE_SINGLE_LEFT;
        }
    }

    private Text getText(int position) {
        Text text = mTextLruCache.get(position);
        if (text == null) {
            mCursor.moveToPosition(position);
            text = mCursor.getText();
            mTextLruCache.add(position, text);
        }
        return text;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Text text = getText(position);

        if (holder instanceof ViewHolder) {
            ((ViewHolder) holder).setText(text.getBody());
            if (holder instanceof FailedHolder) {
                // this is just temporary
                ((LeftHolder) holder).setText("New attachment to download");
            }
            if (holder instanceof LeftHolder) {
                ((LeftHolder) holder).setColor(ColorUtils.getColor(Long.parseLong(text.getThreadId())));
            }
            if (holder instanceof ProfileViewHolder) {
                ((ProfileViewHolder) holder).setText(text);
                ((ProfileViewHolder) holder).setProfile(mContext);
            }
        } else if (holder instanceof AttachmentHolder) {
            ((AttachmentHolder) holder).setImage(mContext, text);
        }
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }
}