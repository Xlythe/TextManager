package com.xlythe.sms.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.makeramen.roundedimageview.RoundedImageView;

import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.sms.R;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.sms.util.DateFormatter;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Status;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.concurrency.Future;

import java.util.HashSet;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends SelectableAdapter<Text, MessageAdapter.MessageViewHolder> {
    private static final String TAG = MessageAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int CACHE_SIZE = 50;

    // Duration between considering a text to be part of the same message, or split into different messages
    private static final long SPLIT_DURATION = 60 * 1000;
    private static final long TIMEOUT = 10 * 1000;

    private static final int TYPE_TOP_RIGHT                 = 0;
    private static final int TYPE_MIDDLE_RIGHT              = 1;
    private static final int TYPE_BOTTOM_RIGHT              = 2;
    private static final int TYPE_SINGLE_RIGHT              = 3;
    private static final int TYPE_TOP_LEFT                  = 4;
    private static final int TYPE_MIDDLE_LEFT               = 5;
    private static final int TYPE_BOTTOM_LEFT               = 6;
    private static final int TYPE_SINGLE_LEFT               = 7;
    private static final int TYPE_ATTACHMENT_TOP_LEFT       = 8;
    private static final int TYPE_ATTACHMENT_MIDDLE_LEFT    = 9;
    private static final int TYPE_ATTACHMENT_BOTTOM_LEFT    = 10;
    private static final int TYPE_ATTACHMENT_TOP_RIGHT      = 11;
    private static final int TYPE_ATTACHMENT_MIDDLE_RIGHT   = 12;
    private static final int TYPE_ATTACHMENT_BOTTOM_RIGHT   = 13;
    private static final int TYPE_FAILED_TOP_LEFT           = 14;
    private static final int TYPE_FAILED_MIDDLE_LEFT        = 15;
    private static final int TYPE_FAILED_BOTTOM_LEFT        = 16;
    private static final int TYPE_FAILED_SINGLE_LEFT        = 16;
    private static final int TYPE_ATTACHMENT_SINGLE_LEFT    = 17;
    private static final int TYPE_ATTACHMENT_SINGLE_RIGHT   = 18;

    // TODO:
    // need to account for group messages, they'll show up as failed
    // as long with messages with a subject, video, and audio
    // These should not ever happen, but they'll stay for now
    private static final int TYPE_FAILED_TOP_RIGHT          = 19;
    private static final int TYPE_FAILED_MIDDLE_RIGHT       = 20;
    private static final int TYPE_FAILED_BOTTOM_RIGHT       = 21;
    private static final int TYPE_FAILED_SINGLE_RIGHT       = 22;

    private static final SparseIntArray LAYOUT_MAP = new SparseIntArray();

    static {
        LAYOUT_MAP.put(TYPE_TOP_RIGHT, R.layout.right_top);
        LAYOUT_MAP.put(TYPE_MIDDLE_RIGHT, R.layout.right_middle);
        LAYOUT_MAP.put(TYPE_BOTTOM_RIGHT, R.layout.right_bottom);
        LAYOUT_MAP.put(TYPE_SINGLE_RIGHT, R.layout.right_single);
        LAYOUT_MAP.put(TYPE_TOP_LEFT, R.layout.left_top);
        LAYOUT_MAP.put(TYPE_MIDDLE_LEFT, R.layout.left_middle);
        LAYOUT_MAP.put(TYPE_BOTTOM_LEFT, R.layout.left_bottom);
        LAYOUT_MAP.put(TYPE_SINGLE_LEFT, R.layout.left_single);
        LAYOUT_MAP.put(TYPE_ATTACHMENT_TOP_LEFT, R.layout.left_attachment_top);
        LAYOUT_MAP.put(TYPE_ATTACHMENT_MIDDLE_LEFT, R.layout.left_attachment_middle);
        LAYOUT_MAP.put(TYPE_ATTACHMENT_BOTTOM_LEFT, R.layout.left_attachment_bottom);
        LAYOUT_MAP.put(TYPE_ATTACHMENT_SINGLE_LEFT, R.layout.left_attachment_single);
        LAYOUT_MAP.put(TYPE_ATTACHMENT_TOP_RIGHT, R.layout.right_attachment_top);
        LAYOUT_MAP.put(TYPE_ATTACHMENT_MIDDLE_RIGHT, R.layout.right_attachment_middle);
        LAYOUT_MAP.put(TYPE_ATTACHMENT_BOTTOM_RIGHT, R.layout.right_attachment_bottom);
        LAYOUT_MAP.put(TYPE_ATTACHMENT_SINGLE_RIGHT, R.layout.right_attachment_single);
        LAYOUT_MAP.put(TYPE_FAILED_TOP_LEFT, R.layout.left_top);
        LAYOUT_MAP.put(TYPE_FAILED_MIDDLE_LEFT, R.layout.left_middle);
        LAYOUT_MAP.put(TYPE_FAILED_BOTTOM_LEFT, R.layout.left_bottom);
        LAYOUT_MAP.put(TYPE_FAILED_SINGLE_LEFT, R.layout.left_single);
        LAYOUT_MAP.put(TYPE_FAILED_TOP_RIGHT, R.layout.right_top);
        LAYOUT_MAP.put(TYPE_FAILED_MIDDLE_RIGHT, R.layout.right_middle);
        LAYOUT_MAP.put(TYPE_FAILED_BOTTOM_RIGHT, R.layout.right_bottom);
        LAYOUT_MAP.put(TYPE_FAILED_SINGLE_RIGHT, R.layout.right_single);
    }

    private Text.TextCursor mCursor;
    private Context mContext;
    private MessageAdapter.OnClickListener mClickListener;
    private final LruCache<Integer, Text> mTextLruCache = new LruCache<>(CACHE_SIZE);

    public static boolean failed(Text text) {
        // This is kinda hacky because is the app force closes then the message status isnt updated
        return text.getStatus() == Status.FAILED
                || (text.getStatus() == Status.PENDING
                && System.currentTimeMillis() - text.getTimestamp() > TIMEOUT);
    }

    public static abstract class MessageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private Text mText;
        private Context mContext;
        private MessageAdapter.OnClickListener mListener;
        public TextView mDate;

        public MessageViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v);
            mListener = listener;
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            mDate = (TextView) v.findViewById(R.id.date);
        }

        public void setMessage(Context context, Text text, boolean selected) {
            mText = text;
            mContext = context;
            setDateText(DateFormatter.getFormattedDate(text));
        }

        public void setDateText(String dateText) {
            if (mDate != null) {
                mDate.setText(dateText);
            }
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onItemClicked(getMessage());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            return mListener != null && mListener.onItemLongClicked(getMessage());
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
        public FrameLayout mFrame;

        public ViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v, listener);
            mFrame = (FrameLayout) v.findViewById(R.id.frame);
            mTextView = (TextView) v.findViewById(R.id.message);
        }

        public void setMessage(Context context, Text text, boolean selected) {
            super.setMessage(context, text, selected);
            setBodyText(text.getBody());
            if (selected) {
                setColor(tintColor(Color.WHITE));
            } else if (text.getStatus() == Status.FAILED) {
                setColor(context.getResources().getColor(android.R.color.holo_red_light));
            } else {
                setColor(context.getResources().getColor(android.R.color.white));
            }
        }

        public void setColor(int color) {
            mFrame.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }

        public void setBodyText(String body) {
            mTextView.setText(body);
        }
    }

    public static class LeftViewHolder extends ViewHolder {
        public FrameLayout mFrame;
        private CircleImageView mProfile;

        public LeftViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v, listener);
            mFrame = (FrameLayout) v.findViewById(R.id.frame);
            mProfile = (CircleImageView) v.findViewById(R.id.profile_image);
        }

        @Override
        public void setMessage(Context context, Text text, boolean selected) {
            super.setMessage(context, text, selected);
            if (selected) {
                setColor(tintColor(ColorUtils.getColor(text.getThreadIdAsLong())));
            } else if (failed(text)) {
                setColor(context.getResources().getColor(android.R.color.holo_red_light));
            } else {
                setColor(ColorUtils.getColor(text.getThreadIdAsLong()));
            }
            setProfile();
        }

        public void setColor(int color) {
            mFrame.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }

        public void setProfile() {
            if (mProfile != null) {
                getMessage().getSender(getContext()).get(new Future.Callback<Contact>() {
                    @Override
                    public void get(Contact instance) {
                        mProfile.setImageDrawable(new ProfileDrawable(getContext(), instance));
                    }
                });
            }
        }
    }

    public static class AttachmentViewHolder extends MessageViewHolder {
        private RoundedImageView mImageView;
        private ImageView mVideoLabel;

        public AttachmentViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v, listener);
            mImageView = (RoundedImageView) v.findViewById(R.id.image);
            mVideoLabel = (ImageView) v.findViewById(R.id.video_label);
        }

        @Override
        public void setMessage(Context context, Text text, boolean selected) {
            super.setMessage(context, text, selected);
            setImage();
            if (selected) {
                setColor(context.getResources().getColor(R.color.select_tint));
            }  else if (failed(text)) {
                setColor(context.getResources().getColor(android.R.color.holo_red_light));
            } else {
                mImageView.clearColorFilter();
            }
        }

        public void setColor(int color) {
            mImageView.setColorFilter(color);
        }

        public void setImage() {
            if (getMessage().getAttachment().getType() == Attachment.Type.VIDEO){
                mVideoLabel.setVisibility(View.VISIBLE);
            } else {
                mVideoLabel.setVisibility(View.GONE);
            }
            Glide.with(getContext())
                    .load(getMessage().getAttachment().getUri())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontAnimate()
                    .placeholder(R.color.loading)
                    .into(mImageView);
        }
    }

    public static class LeftAttachmentViewHolder extends AttachmentViewHolder {
        private CircleImageView mProfile;

        public LeftAttachmentViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v, listener);
            mProfile = (CircleImageView) v.findViewById(R.id.profile_image);
        }

        @Override
        public void setMessage(Context context, Text text, boolean selected) {
            super.setMessage(context, text, selected);
            setProfile();
        }

        public void setProfile() {
            if (mProfile != null) {
                getMessage().getSender(getContext()).get(new Future.Callback<Contact>() {
                    @Override
                    public void get(Contact instance) {
                        mProfile.setImageDrawable(new ProfileDrawable(getContext(), instance));
                    }
                });
            }
        }
    }

    public static class FailedViewHolder extends LeftViewHolder {

        public FailedViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v, listener);
        }

        @Override
        public void setMessage(Context context, Text text, boolean selected) {
            super.setMessage(context, text, selected);
            mTextView.setText("New attachment to download");
        }
    }

    public MessageAdapter(Context context, Text.TextCursor cursor) {
        mCursor = cursor;
        mContext = context;
    }

    public void setOnClickListener(MessageAdapter.OnClickListener onClickListener) {
        mClickListener = onClickListener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(LAYOUT_MAP.get(viewType), parent, false);
        switch(viewType) {
            case TYPE_TOP_RIGHT:
            case TYPE_MIDDLE_RIGHT:
            case TYPE_BOTTOM_RIGHT:
            case TYPE_SINGLE_RIGHT:
                return new ViewHolder(layout, mClickListener);
            case TYPE_TOP_LEFT:
            case TYPE_MIDDLE_LEFT:
            case TYPE_BOTTOM_LEFT:
            case TYPE_SINGLE_LEFT:
                return new LeftViewHolder(layout, mClickListener);
            case TYPE_ATTACHMENT_TOP_LEFT:
            case TYPE_ATTACHMENT_MIDDLE_LEFT:
            case TYPE_ATTACHMENT_BOTTOM_LEFT:
            case TYPE_ATTACHMENT_SINGLE_LEFT:
                return new LeftAttachmentViewHolder(layout, mClickListener);
            case TYPE_ATTACHMENT_TOP_RIGHT:
            case TYPE_ATTACHMENT_MIDDLE_RIGHT:
            case TYPE_ATTACHMENT_BOTTOM_RIGHT:
            case TYPE_ATTACHMENT_SINGLE_RIGHT:
                return new AttachmentViewHolder(layout, mClickListener);
            case TYPE_FAILED_TOP_LEFT:
            case TYPE_FAILED_MIDDLE_LEFT:
            case TYPE_FAILED_BOTTOM_LEFT:
            default:
                return new FailedViewHolder(layout, mClickListener);
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
            if (text.isMms()) {
                if (text.getAttachment() != null) {
                    return TYPE_ATTACHMENT_TOP_RIGHT;
                }
//                return TYPE_FAILED_TOP_RIGHT;
            }
            return TYPE_TOP_RIGHT;
        } else if (!userCurrent && (!userPrevious && !largePC) && (!userNext && !largeCN)) {
            if (text.isMms()) {
                if (text.getAttachment() != null) {
                    return TYPE_ATTACHMENT_MIDDLE_RIGHT;
                }
//                return TYPE_FAILED_MIDDLE_RIGHT;
            }
            return TYPE_MIDDLE_RIGHT;
        } else if (!userCurrent && (!userPrevious && !largePC)) {
            if (text.isMms()) {
                if (text.getAttachment() != null) {
                    return TYPE_ATTACHMENT_BOTTOM_RIGHT;
                }
//                return TYPE_FAILED_BOTTOM_RIGHT;
            }
            return TYPE_BOTTOM_RIGHT;
        } else if (!userCurrent) {
            if (text.isMms()) {
                if (text.getAttachment() != null) {
                    return TYPE_ATTACHMENT_SINGLE_RIGHT;
                }
//                return TYPE_FAILED_SINGLE_RIGHT;
            }
            return TYPE_SINGLE_RIGHT;
        } else if ((!userPrevious || largePC) && (userNext && !largeCN)) {
            if (text.isMms()) {
                if (text.getAttachment() != null) {
                    return TYPE_ATTACHMENT_TOP_LEFT;
                }
//                return TYPE_FAILED_TOP_LEFT;
            }
            return TYPE_TOP_LEFT;
        } else if ((userPrevious && !largePC) && (userNext && !largeCN)) {
            if (text.isMms()) {
                if (text.getAttachment() != null) {
                    return TYPE_ATTACHMENT_MIDDLE_LEFT;
                }
//                return TYPE_FAILED_MIDDLE_LEFT;
            }
            return TYPE_MIDDLE_LEFT;
        } else if (userPrevious && !largePC) {
            if (text.isMms()) {
                if (text.getAttachment() != null) {
                    return TYPE_ATTACHMENT_BOTTOM_LEFT;
                }
//                return TYPE_FAILED_BOTTOM_LEFT;
            }
            return TYPE_BOTTOM_LEFT;
        } else {
            if (text.isMms()) {
                if (text.getAttachment() != null) {
                    return TYPE_ATTACHMENT_SINGLE_LEFT;
                }
//                return TYPE_FAILED_SINGLE_LEFT;
            }
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
        boolean selected = isSelected(getText(position));
        holder.setMessage(mContext, getText(position), selected);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void destroy() {
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
    }

    public static int tintColor(int color) {
        int red = color >> 16 & 0x0000ff;
        int green = color >> 8 & 0x0000ff;
        int blue = color & 0x0000ff;
        red = (int)((double)red * 0.7);
        green = (int)((double)green * 0.7);
        blue = (int)((double)blue * 0.7);
        return Color.argb(255, red, green, blue);
    }

    public void swapCursor(Text.TextCursor cursor) {
        if (!mCursor.isClosed()) {
            mCursor.close();
        }

        mCursor = cursor;
        mTextLruCache.evictAll();
        notifyDataSetChanged();
    }

    public interface OnClickListener {
        void onItemClicked(Text text);
        boolean onItemLongClicked(Text text);
    }
}