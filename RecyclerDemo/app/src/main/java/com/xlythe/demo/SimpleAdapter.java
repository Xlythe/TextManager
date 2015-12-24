package com.xlythe.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Niko on 12/22/15.
 */
public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.SimpleViewHolder> {

    private final Context mContext;
    private List<Thread> mData;
    private MultiSelector mMultiSelector;
    private final int CARD_STATE_ACTIVE_COLOR = Color.rgb(232, 240, 254);
    private final int CARD_STATE_COLOR = Color.WHITE;

    public void add(Thread s,int position) {
        position = position == -1 ? getItemCount()  : position;
        mData.add(position,s);
        notifyItemInserted(position);
    }

    public void remove(int position){
        if (position < getItemCount()  ) {
            mData.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final TextView unread;
        public final TextView message;
        public final TextView date;
        public final CardView card;
        public final de.hdodenhof.circleimageview.CircleImageView profile;

        public SimpleViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.sender);
            unread = (TextView) view.findViewById(R.id.unread);
            message = (TextView) view.findViewById(R.id.message);
            date = (TextView) view.findViewById(R.id.date);
            card = (CardView) view.findViewById(R.id.card);
            profile = (de.hdodenhof.circleimageview.CircleImageView) view.findViewById(R.id.profile_image);
        }
    }

    RecyclerView mRecyclerView;

    public SimpleAdapter(Context context, ArrayList<Thread> data, RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //invalidate();
            }
        });
        mMultiSelector = new MultiSelector();
        mContext = context;
        mData = data;
    }

    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SimpleViewHolder holder, final int position) {
        holder.message.setText(mData.get(position).mMessagesPeek);
        holder.date.setText(mData.get(position).mTimeStamp);

        if (mData.get(position).mUnreadCount>0){
            holder.title.setText(mData.get(position).mSender);
            holder.unread.setVisibility(View.VISIBLE);
            holder.unread.setText(mData.get(position).mUnreadCount+" new");
            holder.title.setTextColor(mContext.getColor(R.color.icon));
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

        holder.profile.setBackground(mContext.getDrawable(R.drawable.selector));

        if (mMultiSelector.selectMode()) {
            holder.profile.setImageResource(android.R.color.transparent);
        } else {
            ProfileDrawable border = new ProfileDrawable(mContext,
                    mData.get(position).mSender.charAt(0),
                    mData.get(position).mColor,
                    mData.get(position).mDrawable);
            holder.profile.setImageDrawable(border);
        }
        holder.profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean selected = mMultiSelector.selectMode();
                holder.profile.setActivated(!holder.profile.isActivated());
                if (mMultiSelector.selectMode()) {
                    holder.profile.setImageResource(android.R.color.transparent);
                } else {
                    ProfileDrawable border = new ProfileDrawable(mContext,
                            mData.get(position).mSender.charAt(0),
                            mData.get(position).mColor,
                            mData.get(position).mDrawable);
                    holder.profile.setImageDrawable(border);
                }
                if (holder.profile.isActivated()) {
                    holder.card.setCardBackgroundColor(CARD_STATE_ACTIVE_COLOR);
                } else {
                    holder.card.setCardBackgroundColor(CARD_STATE_COLOR);
                }
                mMultiSelector.setItemChecked(position, holder.profile.isActivated());
                if (selected != mMultiSelector.selectMode()) {
                    notifyDataSetChanged();
                    //invalidate();
                }
            }
        });

        boolean isSelected = mMultiSelector.isItemChecked(position);
        holder.profile.setActivated(isSelected);
        if (isSelected) {
            holder.card.setCardBackgroundColor(CARD_STATE_ACTIVE_COLOR);
        } else {
            holder.card.setCardBackgroundColor(CARD_STATE_COLOR);
        }

    }

    //TODO: Factor in the section count.
    private void invalidate() {
        for(int i=0; i<mData.size()+3; i++) {
            Log.d("Simple Adapter", "pos: "+i);
            if (mRecyclerView.findViewHolderForAdapterPosition(i) instanceof SimpleViewHolder) {
                SimpleViewHolder holder = (SimpleViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
                if (mMultiSelector.selectMode()) {
                    Log.d("Simple Adapter", "simple holder: "+i);
                    holder.profile.setImageResource(android.R.color.transparent);
                } else {
                    //ProfileDrawable border = new ProfileDrawable(mContext, mData.get(i).mSender.charAt(0));
                    //holder.profile.setImageDrawable(border);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class MultiSelector {
        private SparseBooleanArray mSelectedPositions = new SparseBooleanArray();

        private boolean selectMode(){
            return mSelectedPositions.size()>0;
        }

        private void setItemChecked(int position, boolean isChecked) {
            if (isChecked) {
                mSelectedPositions.put(position, true);
            } else {
                mSelectedPositions.delete(position);
            }
        }

        private boolean isItemChecked(int position) {
            return mSelectedPositions.get(position);
        }
    }
}
