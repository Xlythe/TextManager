package com.xlythe.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Niko on 12/22/15.
 */
public class SimpleAdapter extends SelectableAdapter<SimpleAdapter.SimpleViewHolder>{

    private final Context mContext;
    private List<Thread> mData;
    private final int CARD_STATE_ACTIVE_COLOR = Color.rgb(232, 240, 254);
    private final int CARD_STATE_COLOR = Color.WHITE;
    private SimpleViewHolder.ClickListener mClickListener;

    public SimpleAdapter(Context context, ArrayList<Thread> data) {
        mClickListener = (SimpleViewHolder.ClickListener) context;
        mContext = context;
        mData = data;
    }

    public void add(Thread s,int position) {
        position = position == -1 ? getItemCount()  : position;
        mData.add(position, s);
        notifyItemInserted(position);
    }

    public void removeItem(int position) {
        mData.remove(position);
        notifyItemRemoved(position);
    }

    public void removeItems(List<Integer> positions) {
        // Reverse-sort the list
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
        clearSelection();
        invalidate();
    }

    void invalidate(){
        for (int i=0; i<mData.size(); i++) {
            notifyItemChanged(i);
        }
    }

    private void removeRange(int positionStart, int itemCount) {
        for (int i = 0; i < itemCount; ++i) {
            mData.remove(positionStart);
        }
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
        return new SimpleViewHolder(view, mClickListener);
    }

    @Override
    public void onBindViewHolder(final SimpleViewHolder holder, final int position) {
        holder.message.setText(mData.get(position).mMessagesPeek);
        holder.date.setText(mData.get(position).mTimeStamp);
        holder.profile.setBackground(mContext.getDrawable(R.drawable.selector));

        if (mData.get(position).mUnreadCount>0){
            holder.title.setText(mData.get(position).mSender);
            holder.unread.setVisibility(View.VISIBLE);
            holder.unread.setText(mData.get(position).mUnreadCount + " new");
            holder.unread.setTextColor(mData.get(position).mColor);
            holder.unread.getBackground().setColorFilter(mData.get(position).mColor, PorterDuff.Mode.SRC_IN);
            holder.unread.getBackground().setAlpha(25);
            holder.title.setTextColor(mData.get(position).mColor);
            holder.title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            holder.message.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            holder.date.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        } else {
            holder.title.setText(mData.get(position).mSender);
            holder.unread.setVisibility(View.GONE);
            holder.title.setTextColor(mContext.getColor(R.color.headerText));
            holder.title.setTypeface(Typeface.create("sans-serif-regular", Typeface.NORMAL));
            holder.message.setTypeface(Typeface.create("sans-serif-regular", Typeface.NORMAL));
            holder.date.setTypeface(Typeface.create("sans-serif-regular", Typeface.NORMAL));
        }

        boolean isSelected = isSelected(position);

        if (selectMode()) {
            holder.profile.setImageResource(android.R.color.transparent);
        } else {
            ProfileDrawable border = new ProfileDrawable(mContext,
                    mData.get(position).mSender.charAt(0),
                    mData.get(position).mColor,
                    mData.get(position).mDrawable);
            holder.profile.setImageDrawable(border);
        }

        holder.profile.setActivated(isSelected);
        if (isSelected) {
            holder.card.setCardBackgroundColor(CARD_STATE_ACTIVE_COLOR);
        } else {
            holder.card.setCardBackgroundColor(CARD_STATE_COLOR);
        }

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {
        public final TextView title;
        public final TextView unread;
        public final TextView message;
        public final TextView date;
        public final CardView card;
        public final de.hdodenhof.circleimageview.CircleImageView profile;
        private ClickListener mListener;

        public SimpleViewHolder(View view, ClickListener listener) {
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
}
