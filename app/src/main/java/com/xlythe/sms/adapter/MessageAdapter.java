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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.makeramen.roundedimageview.RoundedImageView;
import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.sms.util.DateFormatter;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Status;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.concurrency.Future;

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
    private static final int TYPE_ATTACHMENT_SINGLE_LEFT    = 17;
    private static final int TYPE_ATTACHMENT_SINGLE_RIGHT   = 18;

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
    }

    private Text.TextCursor mCursor;
    private Context mContext;
    private MessageAdapter.OnClickListener mClickListener;
    private final LruCache<Integer, Text> mTextLruCache = new LruCache<>(CACHE_SIZE);
    private final LruCache<String, Contact> mNumberLruCache;
    private int mMemberSize = -1;

    // TODO: this is for debugging REMOVE!!
    private int i = 0;

    public static boolean hasFailed(Text text) {
        // This is kinda hacky because if the app force closes then the message status isn't updated
        return text.getStatus() == Status.FAILED
                || (text.getStatus() == Status.PENDING
                && System.currentTimeMillis() - text.getTimestamp() > TIMEOUT);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private Text mText;
        private Context mContext;
        MessageAdapter.OnClickListener mListener;
        public TextView mDate;
        public TextView mTextView;
        public FrameLayout mFrame;

        public MessageViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v);
            mListener = listener;
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            mDate = (TextView) v.findViewById(R.id.date);
            mFrame = (FrameLayout) v.findViewById(R.id.frame);
            mTextView = (TextView) v.findViewById(R.id.message);
        }

        public void setMessage(Context context, Text text, boolean selected) {
            mText = text;
            mContext = context;
            if (mText.getStatus() == Status.PENDING) {
                setDateText(getContext().getString(R.string.message_pending));
            } else {
                setDateText(DateFormatter.getFormattedDate(text));
            }

            if (hasFailed(text)) {
                mTextView.setLinksClickable(false);
            } else {
                mTextView.setLinksClickable(true);
            }

            if (mDate != null) {
                if (hasFailed(text)) {
                    mDate.setTextColor(context.getResources().getColor(android.R.color.holo_red_light));
                    mDate.setText(R.string.message_failed_to_send);
                } else {
                    mDate.setTextColor(context.getResources().getColor(R.color.date_text_color));
                }
            }

            setBodyText(text.getBody());
            if (selected) {
                setColor(tintColor(Color.WHITE));
            } else {
                setColor(context.getResources().getColor(android.R.color.white));
            }

            // We set this here because of attachments
            if (hasFailed(text)) {
                mFrame.setAlpha(0.4f);
            } else {
                mFrame.setAlpha(1f);
            }

        }

        public void setColor(int color) {
            mFrame.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }

        public void setBodyText(String body) {
            if (body == null) {
                mFrame.setVisibility(View.GONE);
            } else {
                mFrame.setVisibility(View.VISIBLE);
                mTextView.setText(body);
            }
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

        public TextManager getManager() {
            return TextManager.getInstance(mContext);
        }
    }

    public static class LeftViewHolder extends MessageViewHolder {
        private ImageView mProfile;

        public LeftViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v, listener);
            mProfile = (ImageView) v.findViewById(R.id.profile_image);
        }

        @Override
        public void setMessage(Context context, Text text, boolean selected) {
            super.setMessage(context, text, selected);
            if (selected) {
                setColor(tintColor(ColorUtils.getColor(text.getThreadIdAsLong())));
            } else {
                setColor(ColorUtils.getColor(text.getThreadIdAsLong()));
            }

            // This is if a message failed to download
            if (mDate != null) {
                if (hasFailed(text)) {
                    mDate.setText("Message failed to download");
                }
            }

            if (mProfile != null) {
                getManager().getSender(getMessage()).get(new Future.Callback<Contact>() {
                    @Override
                    public void get(Contact instance) {
                        mProfile.setImageDrawable(new ProfileDrawable(getContext(), instance));
                    }
                });
            }
        }

        public void setBodyText(String body) {
            if (body == null) {
                if (hasFailed(getMessage())) {
                    mFrame.setVisibility(View.VISIBLE);
                    mTextView.setText("Tap to retry");
                } else {
                    mFrame.setVisibility(View.GONE);
                }
            } else {
                mFrame.setVisibility(View.VISIBLE);
                mTextView.setText(body);
            }
        }
    }

    public static class AttachmentViewHolder extends MessageViewHolder {
        private RoundedImageView mImageView;
        private ImageButton mShare;
        private ImageView mVideoLabel;

        public AttachmentViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v, listener);
            mImageView = (RoundedImageView) v.findViewById(R.id.image);
            mShare = (ImageButton) v.findViewById(R.id.share);
            mVideoLabel = (ImageView) v.findViewById(R.id.video_label);

            mShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onShareClicked(getMessage());
                    }
                }
            });

            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onAttachmentClicked(getMessage());
                    }
                }
            });
        }

        @Override
        public void setMessage(Context context, Text text, boolean selected) {
            super.setMessage(context, text, selected);
            setImage();
            if (selected) {
                setColor(context.getResources().getColor(R.color.select_tint));
            } else {
                mImageView.clearColorFilter();
            }

            if (selected) {
                setColorText(tintColor(Color.WHITE));
            } else {
                setColorText(context.getResources().getColor(android.R.color.white));
            }
        }

        public void setColorText(int color) {
            mFrame.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }

        @Override
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
        private ImageView mProfile;

        public LeftAttachmentViewHolder(View v, MessageAdapter.OnClickListener listener) {
            super(v, listener);
            mProfile = (ImageView) v.findViewById(R.id.profile_image);
        }

        @Override
        public void setMessage(Context context, Text text, boolean selected) {
            super.setMessage(context, text, selected);
            if (selected) {
                setColorText(tintColor(ColorUtils.getColor(text.getThreadIdAsLong())));
            } else {
                setColorText(ColorUtils.getColor(text.getThreadIdAsLong()));
            }

            if (mProfile != null) {
                getManager().getSender(getMessage()).get(new Future.Callback<Contact>() {
                    @Override
                    public void get(Contact instance) {
                        mProfile.setImageDrawable(new ProfileDrawable(getContext(), instance));
                    }
                });
            }
        }

    }

    public MessageAdapter(Context context, Text.TextCursor cursor) {
        mCursor = cursor;
        mContext = context;
        mNumberLruCache = new LruCache<>(mCursor.getCount());
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
                return new MessageViewHolder(layout, mClickListener);
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
            default:
                return new MessageViewHolder(layout, mClickListener);
        }
    }

    private Contact getSender(Text text) {
        Contact contact = mNumberLruCache.get(text.getId());
        if (contact == null) {
            i++;
            Log.d(TAG,  i + "");
            Log.d(TAG,  getItemCount() + "");
            TextManager manager = TextManager.getInstance(mContext);
            contact = manager.getSender(text).get();
            mNumberLruCache.put(text.getId(), contact);
        }
        return contact;
    }

    @Override
    public int getItemViewType(int position) {

        TextManager manager = TextManager.getInstance(mContext);
        Text text = getText(position);

        if (mMemberSize == -1) {
            mMemberSize = manager.getMembers(text).get().size();
            Log.d(TAG, "Member size: " + mMemberSize);
        }

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

        // This should improve speed
        Contact contactCurrent;
        if (!text.isIncoming()) {
            contactCurrent = manager.getSelf();
        } else if (mMemberSize <= 2) {
            contactCurrent = null;
        } else {
            contactCurrent = getSender(text);
        }
        Contact contactPrevious = null;
        Contact contactNext = null;

        // Check if previous message exists, then get the date and sender.
        if (prevText != null) {
            datePrevious = prevText.getTimestamp();
            if (!prevText.isIncoming()) {
                contactPrevious = manager.getSelf();
            } else if (mMemberSize <= 2) {
                contactPrevious = null;
            } else {
                contactPrevious = getSender(prevText);
            }
        }

        // Check if next message exists, then get the date and sender.
        if (nextText != null) {
            dateNext = nextText.getTimestamp();
            if (!nextText.isIncoming()) {
                contactNext = manager.getSelf();
            } else if (mMemberSize <= 2) {
                contactNext = null;
            } else {
                contactNext = getSender(nextText);
            }
        }

        boolean topEqualsCurrent;
        boolean bottomEqualsCurrent;
        if (contactCurrent == null) {
            topEqualsCurrent = null == contactPrevious;
            bottomEqualsCurrent = null == contactNext;
        } else {
            topEqualsCurrent = contactCurrent.equals(contactPrevious);
            bottomEqualsCurrent = contactCurrent.equals(contactNext);
        }

        // Calculate time gap.
        boolean largePC = dateCurrent - datePrevious > SPLIT_DURATION;
        boolean largeCN = dateNext - dateCurrent > SPLIT_DURATION;

        // If the message above does not equal the current message or the time gap is large
        boolean largeTopGap = !topEqualsCurrent || largePC;

        // If the message below equals the current message and the time gap is small
        boolean smallBottomGap = bottomEqualsCurrent && !largeCN;

        // If the message isn't incoming its you and should be on the right
        if (!text.isIncoming()) {
            if (largeTopGap) {
                if (smallBottomGap) {
                    if (text.getAttachment() != null) {
                        return TYPE_ATTACHMENT_TOP_RIGHT;
                    }
                    return TYPE_TOP_RIGHT;
                } else {
                    if (text.getAttachment() != null) {
                        return TYPE_ATTACHMENT_SINGLE_RIGHT;
                    }
                    return TYPE_SINGLE_RIGHT;
                }
            } else {
                if (smallBottomGap) {
                    if (text.getAttachment() != null) {
                        return TYPE_ATTACHMENT_MIDDLE_RIGHT;
                    }
                    return TYPE_MIDDLE_RIGHT;
                } else {
                    if (text.getAttachment() != null) {
                        return TYPE_ATTACHMENT_BOTTOM_RIGHT;
                    }
                    return TYPE_BOTTOM_RIGHT;
                }
            }
        }

        // Otherwise it should be on the left
        else {
            if (largeTopGap) {
                if (smallBottomGap) {
                    if (text.getAttachment() != null) {
                        return TYPE_ATTACHMENT_TOP_LEFT;
                    }
                    return TYPE_TOP_LEFT;
                }
                else {
                    if (text.getAttachment() != null) {
                        return TYPE_ATTACHMENT_SINGLE_LEFT;
                    }
                    return TYPE_SINGLE_LEFT;
                }
            } else {
                if (smallBottomGap) {
                    if (text.getAttachment() != null) {
                        return TYPE_ATTACHMENT_MIDDLE_LEFT;
                    }
                    return TYPE_MIDDLE_LEFT;
                } else {
                    if (text.getAttachment() != null) {
                        return TYPE_ATTACHMENT_BOTTOM_LEFT;
                    }
                    return TYPE_BOTTOM_LEFT;
                }
            }
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
        void onAttachmentClicked(Text text);
        void onShareClicked(Text text);
    }
}