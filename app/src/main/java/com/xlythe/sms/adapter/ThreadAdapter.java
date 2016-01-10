package com.xlythe.sms.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;
import com.xlythe.sms.ProfileDrawable;
import com.xlythe.sms.R;
import com.xlythe.sms.Section;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.sms.util.DateFormatter;
import com.xlythe.textmanager.text.Thread;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ThreadAdapter extends SelectableAdapter<ThreadAdapter.ThreadViewHolder> implements StickyRecyclerHeadersAdapter<ThreadAdapter.SectionViewHolder> {
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

    private final Context mContext;
    private Thread.ThreadCursor mCursor;
    private ThreadViewHolder.ClickListener mClickListener;
    private final LruCache<Integer, Thread> mThreadLruCache = new LruCache<>(CACHE_SIZE);

    public ThreadAdapter(Context context, Thread.ThreadCursor cursor) {
        mClickListener = (ThreadViewHolder.ClickListener) context;
        mContext = context;
        mCursor = cursor;

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

    private void invalidateDataSet() {
        mThreadLruCache.evictAll();
        notifyDataSetChanged();
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void add(Thread s, int position) {
        position = position == -1 ? getItemCount()  : position;
        // TODO: FIX
        //mData.add(position, s);
        notifyItemInserted(position);
    }

    public void removeItem(int position) {
        // TODO: FIX
        //mData.remove(position);
        notifyItemRemoved(position);
    }

    public void removeItems(List<Integer> positions) {
        // TODO: FIX
        // Reverse-sort the list
        if (positions!=null) {
            Collections.sort(positions, new Comparator<Integer>() {
                @Override
                public int compare(Integer lhs, Integer rhs) {
                    return rhs - lhs;
                }
            });

            // Split the list in ranges
            while (!positions.isEmpty()) {
                if (positions.size() == 1) {
                    removeItem(positions.get(0));
                    positions.remove(0);
                } else {
                    int count = 1;
                    while (positions.size() > count && positions.get(count).equals(positions.get(count - 1) - 1)) {
                        ++count;
                    }

                    if (count == 1) {
                        removeItem(positions.get(0));
                    } else {
                        removeRange(positions.get(count - 1), count);
                    }

                    for (int i = 0; i < count; ++i) {
                        positions.remove(0);
                    }
                }
            }
        }
        clearSelection();
        invalidate();
    }

    void invalidate(){
        for (int i=0; i < mCursor.getCount(); i++) {
            notifyItemChanged(i);
        }
    }

    private void removeRange(int positionStart, int itemCount) {
        for (int i = 0; i < itemCount; ++i) {
            // TODO: FIX
            //mData.remove(positionStart);
        }
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    public ThreadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
        return new ThreadViewHolder(view, mClickListener);
    }

    @Override
    public void onBindViewHolder(final ThreadViewHolder holder, final int position) {
        Thread data = getThread(position);
        String body = "";
        String time = "";
        String address = "";
        Uri uri = null;
        int unread = 0;
        int color = mContext.getResources().getColor(R.color.colorPrimary);

        if (data.getLatestMessage()!=null) {
            body = data.getLatestMessage().getBody();
            time = DateFormatter.getFormattedDate(data.getLatestMessage());
            address = data.getLatestMessage().getSender().getDisplayName()+"";
            uri = data.getLatestMessage().getSender().getPhotoUri();
            unread = data.getUnreadCount();
            color = ColorUtils.getColor(Long.parseLong(data.getId()));
        }
        holder.message.setText(body);
        holder.date.setText(time);
        holder.profile.setBackgroundResource(R.drawable.selector);

        if (unread > 0) {
            holder.title.setText(address);
            holder.unread.setVisibility(View.VISIBLE);
            holder.unread.setText(unread + " new");
            holder.unread.setTextColor(color);
            holder.unread.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            holder.unread.getBackground().setAlpha(25);
            holder.title.setTextColor(color);
            holder.title.setTypeface(TYPEFACE_BOLD);
            holder.message.setTypeface(TYPEFACE_BOLD);
            holder.date.setTypeface(TYPEFACE_BOLD);
        } else {
            holder.title.setText(address);
            holder.unread.setVisibility(View.GONE);
            holder.title.setTextColor(mContext.getResources().getColor(R.color.headerText));
            holder.title.setTypeface(TYPEFACE_NORMAL);
            holder.message.setTypeface(TYPEFACE_NORMAL);
            holder.date.setTypeface(TYPEFACE_NORMAL);
        }

        boolean isSelected = isSelected(position);

        if (selectMode()) {
            holder.profile.setImageResource(android.R.color.transparent);
        } else {
            if (!address.equals("")) {
                ProfileDrawable border = new ProfileDrawable(mContext,
                        address.charAt(0),
                        color,
                        uri);
                holder.profile.setImageDrawable(border);
            }
        }

        holder.profile.setActivated(isSelected);
        if (isSelected) {
            holder.card.setCardBackgroundColor(CARD_STATE_ACTIVE_COLOR);
        } else {
            holder.card.setCardBackgroundColor(CARD_STATE_COLOR);
        }
    }

    private Thread getThread(int position) {
        Thread thread = mThreadLruCache.get(position);
        if (thread == null) {
            mCursor.moveToPosition(position);
            thread = mCursor.getThread();
            mThreadLruCache.put(position, thread);
        }
        return thread;
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public static class ThreadViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {
        public final TextView title;
        public final TextView unread;
        public final TextView message;
        public final TextView date;
        public final CardView card;
        public final de.hdodenhof.circleimageview.CircleImageView profile;
        private ClickListener mListener;

        public ThreadViewHolder(View view, ClickListener listener) {
            super(view);
            title = (TextView) view.findViewById(R.id.sender);
            unread = (TextView) view.findViewById(R.id.unread);
            message = (TextView) view.findViewById(R.id.message);
            date = (TextView) view.findViewById(R.id.date);
            card = (CardView) view.findViewById(R.id.card);
            profile = (de.hdodenhof.circleimageview.CircleImageView) view.findViewById(R.id.profile_image);

            mListener = listener;

            profile.setOnClickListener(this);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                if (v instanceof de.hdodenhof.circleimageview.CircleImageView) {
                    mListener.onProfileClicked(getAdapterPosition());
                } else {
                    mListener.onItemClicked(getAdapterPosition());
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mListener != null) {
                return mListener.onItemLongClicked(getAdapterPosition());
            }

            return false;
        }

        public interface ClickListener {
            void onProfileClicked(int position);
            void onItemClicked(int position);
            boolean onItemLongClicked(int position);
        }
    }

    public static class SectionViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;

        public SectionViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.section_text);
        }
    }

    @Override
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
        return Integer.parseInt(formatter.format(date)) << 4;
    }

    @Override
    public SectionViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.section, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(SectionViewHolder holder, int position) {
        String title = "";

        Thread thread = getThread(position);
        long date = thread.getLatestMessage().getTimestamp();
        long time = System.currentTimeMillis() - date;
        if (time < ONE_DAY) {
            title = "Today";
        } else if (time < 2 * ONE_DAY) {
            title = "Yesterday";
        } else if (time < ONE_WEEK) {
            title = "This week";
        } else if (time < ONE_MONTH) {
            title = "This month";
        } else {
            SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy");
            title = formatter.format(date);
        }

        holder.title.setText(title);
    }
}
