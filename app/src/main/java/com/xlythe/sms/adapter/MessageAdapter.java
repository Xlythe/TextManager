package com.xlythe.sms.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.xlythe.sms.ProfileDrawable;
import com.xlythe.sms.R;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.sms.util.DateFormatter;
import com.xlythe.textmanager.text.Text;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final String TAG = MessageAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int CACHE_SIZE = 50;

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

    private static final SparseIntArray LAYOUT_MAP = new SparseIntArray();

    static {
        LAYOUT_MAP.put(TYPE_TOP_RIGHT, R.layout.right_top);
        LAYOUT_MAP.put(TYPE_MIDDLE_RIGHT,R.layout.right_middle);
        LAYOUT_MAP.put(TYPE_BOTTOM_RIGHT,R.layout.right_bottom);
        LAYOUT_MAP.put(TYPE_SINGLE_RIGHT,R.layout.right_single);
        LAYOUT_MAP.put(TYPE_TOP_LEFT,R.layout.left_top);
        LAYOUT_MAP.put(TYPE_MIDDLE_LEFT,R.layout.left_middle);
        LAYOUT_MAP.put(TYPE_BOTTOM_LEFT,R.layout.left_bottom);
        LAYOUT_MAP.put(TYPE_SINGLE_LEFT,R.layout.left_single);
        LAYOUT_MAP.put(TYPE_ATTACHMENT,R.layout.attachment);
        LAYOUT_MAP.put(TYPE_FAILED,R.layout.left_single);
    };

    private Text.TextCursor mCursor;
    private Context mContext;
    private FailedViewHolder.ClickListener mClickListener;
    private final LruCache<Integer, Text> mTextLruCache = new LruCache<>(CACHE_SIZE);

    public static abstract class MessageViewHolder extends RecyclerView.ViewHolder {
        private Text mText;
        private Context mContext;

        public MessageViewHolder(View v) {
            super(v);
        }

        public void setMessage(Context context, Text text) {
            mText = text;
            mContext = context;
        }

        public Text getMessage() {
            return mText;
        }

        public Context getContext() {
            return mContext;
        }
    }

    public static class ViewHolder extends MessageViewHolder {
        public TextView mTextView;
        public TextView mDate;

        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.message);
            mDate = (TextView) v.findViewById(R.id.date);
        }

        public void setMessage(Context context, Text text) {
            super.setMessage(context, text);
            setBodyText(text.getBody());
            setDateText(DateFormatter.getFormattedDate(text));
        }

        public void setBodyText(String body) {
            mTextView.setText(body);
        }
        public void setDateText(String dateText) {
            if (mDate != null) {
                mDate.setText(dateText);
            }
        }
    }

    public static class LeftViewHolder extends ViewHolder {
        public FrameLayout mFrame;
        private CircleImageView mProfile;

        public LeftViewHolder(View v) {
            super(v);
            mFrame = (FrameLayout) v.findViewById(R.id.frame);
            mProfile = (CircleImageView) v.findViewById(R.id.profile_image);
        }

        @Override
        public void setMessage(Context context, Text text) {
            super.setMessage(context, text);
            setColor(ColorUtils.getColor(text.getThreadIdAsLong()));
            setProfile();
        }

        public void setColor(int color) {
            mFrame.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }

        public void setProfile() {
            if (mProfile != null) {
                ProfileDrawable border = new ProfileDrawable(getContext(),
                        getMessage().getSender().getDisplayName().charAt(0),
                        ColorUtils.getColor(getMessage().getThreadIdAsLong()),
                        getMessage().getSender().getPhotoUri());
                mProfile.setImageDrawable(border);
            }
        }
    }

    public static class AttachmentViewHolder extends MessageViewHolder {
        ImageView mImageView;

        public AttachmentViewHolder(View v) {
            super(v);
            mImageView = (ImageView) v.findViewById(R.id.image);
        }

        @Override
        public void setMessage(Context context, Text text) {
            super.setMessage(context, text);
            setImage();
        }

        public void setImage() {
            Picasso.with(getContext()).load(getMessage().getAttachments().get(0).getUri()).into(mImageView);
        }
    }

    public static class FailedViewHolder extends LeftViewHolder implements View.OnClickListener {
        private ClickListener mListener;

        public FailedViewHolder(View v, ClickListener listener) {
            super(v);
            mListener = listener;
            v.setOnClickListener(this);
        }

        @Override
        public void setMessage(Context context, Text text) {
            super.setMessage(context, text);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onItemClicked(getMessage());
            }
        }

        public interface ClickListener {
            void onItemClicked(Text text);
        }
    }

    public MessageAdapter(Context context, Text.TextCursor cursor) {
        mCursor = cursor;
        mContext = context;

        mCursor.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                invalidateDataSet();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                invalidateDataSet();
            }
        });
    }

    public void setOnClickListener(FailedViewHolder.ClickListener onClickListener) {
        mClickListener = onClickListener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch(viewType) {
            case TYPE_TOP_RIGHT:
            case TYPE_MIDDLE_RIGHT:
            case TYPE_BOTTOM_RIGHT:
            case TYPE_SINGLE_RIGHT:
                return new ViewHolder(LayoutInflater.from(mContext).inflate(LAYOUT_MAP.get(viewType), parent, false));
            case TYPE_TOP_LEFT:
            case TYPE_MIDDLE_LEFT:
            case TYPE_BOTTOM_LEFT:
            case TYPE_SINGLE_LEFT:
                return new LeftViewHolder(LayoutInflater.from(mContext).inflate(LAYOUT_MAP.get(viewType), parent, false));
            case TYPE_ATTACHMENT:
                return new AttachmentViewHolder(LayoutInflater.from(mContext).inflate(LAYOUT_MAP.get(viewType), parent, false));
            default:
                return new FailedViewHolder(LayoutInflater.from(mContext).inflate(LAYOUT_MAP.get(viewType), parent, false), mClickListener);
        }
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
            mTextLruCache.put(position, text);
        }
        return text;
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        holder.setMessage(mContext, getText(position));
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    private void invalidateDataSet() {
        mTextLruCache.evictAll();
        notifyDataSetChanged();
    }

    public Cursor getCursor() {
        return mCursor;
    }
}