package com.xlythe.sms.adapter;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;
import com.xlythe.sms.R;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.sms.util.DateFormatter;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;
import com.xlythe.textmanager.text.concurrency.Future;
import com.xlythe.textmanager.text.util.Utils;
import com.xlythe.view.camera.Image;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import androidx.recyclerview.widget.RecyclerView;

public class ThreadAdapter extends SelectableAdapter<Thread, ThreadAdapter.ViewHolder> implements StickyRecyclerHeadersAdapter<ThreadAdapter.SectionViewHolder> {
    private static final Typeface TYPEFACE_NORMAL = Typeface.create("sans-serif-regular", Typeface.NORMAL);
    private static final Typeface TYPEFACE_BOLD = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final int CARD_STATE_ACTIVE_COLOR = Color.rgb(229, 244, 243);
    private static final int CARD_STATE_COLOR = Color.WHITE;
    private static final int CACHE_SIZE = 50;

    private static final long ONE_MINUTE = 60 * 1000;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    private static final long ONE_DAY = 24 * ONE_HOUR;
    private static final long ONE_WEEK = 7 * ONE_DAY;
    private static final long ONE_MONTH = 4 * ONE_WEEK;

    private static final int TYPE_TEXT = 0;
    private static final int TYPE_ATTACHMENT = 1;

    private static final SparseIntArray LAYOUT_MAP = new SparseIntArray();

    static {
        LAYOUT_MAP.put(TYPE_TEXT, R.layout.list_item);
        LAYOUT_MAP.put(TYPE_ATTACHMENT, R.layout.list_item_attachment);
    }

    private final Context mContext;
    private Thread.ThreadCursor mCursor;
    private OnClickListener mClickListener;
    private final LruCache<Integer, Thread> mThreadLruCache = new LruCache<>(CACHE_SIZE);

    public ThreadAdapter(Context context, Thread.ThreadCursor cursor) {
        mClickListener = (OnClickListener) context;
        mContext = context;
        mCursor = cursor;
    }

    public void destroy() {
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
    }

    public void swapCursor(Thread.ThreadCursor cursor) {
        if (!mCursor.isClosed()) {
            mCursor.close();
        }

        mCursor = cursor;
        mThreadLruCache.evictAll();
        notifyDataSetChanged();
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {
        private Thread mThread;
        private Context mContext;

        public ViewHolder(View view) {
            super(view);
        }

        public void setThread(Context context, Thread thread, boolean isSelected, boolean selectMode) {
            mThread = thread;
            mContext = context;
        }

        public Thread getThread() {
            return mThread;
        }

        public Context getContext() {
            return mContext;
        }
    }

    public static class ThreadViewHolder extends ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {
        public final TextView title;
        public final TextView unread;
        public final TextView message;
        public final TextView date;
        public final RoundedImageView attachment;
        public final ImageView videoLabel;
        public final ViewGroup card;
        public final ImageView profile;
        private OnClickListener mListener;

        public ThreadViewHolder(View view, OnClickListener listener) {
            super(view);
            title = view.findViewById(R.id.sender);
            unread = view.findViewById(R.id.unread);
            message = view.findViewById(R.id.message);
            date = view.findViewById(R.id.date);
            attachment = view.findViewById(R.id.attachment);
            videoLabel = view.findViewById(R.id.video_label);
            card = view.findViewById(R.id.card);
            profile = view.findViewById(R.id.profile_image);

            mListener = listener;

            profile.setOnClickListener(this);
            if (attachment != null) {
                attachment.setOnClickListener(this);
            }
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void setThread(Context context, Thread thread, boolean isSelected, boolean selectMode) {
            super.setThread(context, thread, isSelected, selectMode);
            createView(isSelected, selectMode);
        }

        public void createView(boolean isSelected, final boolean selectMode) {
            String body = "";
            String time = "";
            int unreadCount = 0;
            int color = getContext().getResources().getColor(R.color.colorPrimary);

            final Text latest = getThread().getLatestMessage();
            final TextManager manager = TextManager.getInstance(getContext());

            Handler mainHandler = new Handler(Looper.getMainLooper());

            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    manager.getMembersExceptMe(latest).get(contacts -> {
                        String address = Utils.join(", ", contacts, Contact::getDisplayName);
                        title.setText(address);
                        profile.setImageDrawable(new ProfileDrawable(getContext(), contacts));
                        if (selectMode) {
                            profile.setImageResource(android.R.color.transparent);
                            profile.setBackgroundResource(R.drawable.selector);
                        } else {
                            profile.setBackgroundResource(android.R.color.transparent);
                        }
                    });
                }
            };
            mainHandler.post(myRunnable);

            if (latest != null) {
                body = latest.getBody();
                time = DateFormatter.getFormattedDate(latest);
                unreadCount = manager.getUnreadCount(getThread());
                color = ColorUtils.getColor(getThread().getIdAsLong());
            }
            if (message != null) {
                message.setText(body);
            }
            date.setText(time);

            if (attachment != null && latest.getAttachment() != null) {
                if (latest.getAttachment().getType() == Attachment.Type.VIDEO) {
                    videoLabel.setVisibility(View.VISIBLE);
                } else {
                    videoLabel.setVisibility(View.GONE);
                }
                Image.with(getContext())
                        .load(latest.getAttachment().getUri())
                        .into(attachment);
            }

            if (unreadCount > 0) {
                unread.setVisibility(View.VISIBLE);
                unread.setText(getContext().getString(R.string.thread_unread_messages, unreadCount));
                unread.setTextColor(color);
                unread.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                unread.getBackground().setAlpha(25);
                title.setTextColor(color);
                title.setTypeface(TYPEFACE_BOLD);
                if (message != null) {
                    message.setTypeface(TYPEFACE_BOLD);
                }
                date.setTypeface(TYPEFACE_BOLD);
            } else {
                unread.setVisibility(View.GONE);
                title.setTextColor(getContext().getResources().getColor(R.color.headerText));
                title.setTypeface(TYPEFACE_NORMAL);
                if (message != null) {
                    message.setTypeface(TYPEFACE_NORMAL);
                }
                date.setTypeface(TYPEFACE_NORMAL);
            }

            if (selectMode) {
                profile.setImageResource(android.R.color.transparent);
                profile.setBackgroundResource(R.drawable.selector);
            } else {
                profile.setBackgroundResource(android.R.color.transparent);
            }

            profile.setActivated(isSelected);
            if (isSelected) {
                card.setBackgroundColor(CARD_STATE_ACTIVE_COLOR);
            } else {
                card.setBackgroundColor(CARD_STATE_COLOR);
            }
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                if (v.equals(profile)) {
                    mListener.onProfileClicked(getThread());
                } else if (v.equals(attachment)){
                    mListener.onAttachmentClicked(getThread());
                } else {
                    mListener.onItemClicked(getThread());
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            return mListener != null && mListener.onItemLongClicked(getThread());
        }
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(LAYOUT_MAP.get(viewType), parent, false);
        return new ThreadViewHolder(layout, mClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        boolean isSelected = isSelected(getThread(position));
        boolean selectMode = selectMode();

        holder.setThread(mContext, getThread(position), isSelected, selectMode);
    }

    @Override
    public long getItemId(int position) {
        return getThread(position).getIdAsLong();
    }

    @Override
    public int getItemViewType(int position) {
        Text text = getThread(position).getLatestMessage();
        if (text != null && text.getAttachment() != null) {
            return TYPE_ATTACHMENT;
        }
        return TYPE_TEXT;
    }

    public Thread getThread(int position) {
        Thread thread = mThreadLruCache.get(position);
        if (thread == null) {
            mCursor.moveToPosition(position);
            thread = mCursor.getThread();
            mThreadLruCache.put(position, thread);
        }
        return thread;
    }

    public Set<Thread> getThreads(Collection<Integer> positions) {
        Set<Thread> set = new HashSet<>(positions.size());
        for (Integer position : positions) {
            set.add(getThread(position));
        }
        return set;
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public static class SectionViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;

        public SectionViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.section_text);
            Configuration config = view.getResources().getConfiguration();
            if(config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                title.setLayoutParams(params);
            } else {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                title.setLayoutParams(params);
            }
        }
    }

    public long getHeaderId(int position) {
        Thread thread = getThread(position);
        long date = thread.getLatestMessage().getTimestamp();
        long time = System.currentTimeMillis() - date;
        if (time < ONE_DAY) {
            return 1;
        } else if (time < 2 * ONE_DAY) {
            return 2;
        } else if (time < ONE_WEEK) {
            return 3;
        } else if (time < ONE_MONTH) {
            return 4;
        }
        // eg 022015
        SimpleDateFormat formatter = new SimpleDateFormat("MMyyyy");
        return Integer.parseInt(formatter.format(date)) << 3;
    }

    @Override
    public SectionViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.section, parent, false);
        return new SectionViewHolder(view);
    }

    public void onBindHeaderViewHolder(SectionViewHolder holder, int position) {
        String title = "";

        Thread thread = getThread(position);
        long date = thread.getLatestMessage().getTimestamp();
        long time = System.currentTimeMillis() - date;
        if (time < ONE_DAY) {
            title = mContext.getString(R.string.thread_title_today);
        } else if (time < 2 * ONE_DAY) {
            title = mContext.getString(R.string.thread_title_yesterday);
        } else if (time < ONE_WEEK) {
            title = mContext.getString(R.string.thread_title_week);
        } else if (time < ONE_MONTH) {
            title = mContext.getString(R.string.thread_title_month);
        } else {
            SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy");
            title = formatter.format(date);
        }

        holder.title.setText(title);
    }

    public interface OnClickListener {
        void onProfileClicked(Thread thread);
        void onAttachmentClicked(Thread thread);
        void onItemClicked(Thread thread);
        boolean onItemLongClicked(Thread thread);
    }
}
