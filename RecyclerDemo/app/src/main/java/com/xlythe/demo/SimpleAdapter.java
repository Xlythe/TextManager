package com.xlythe.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
    private List<String> mData;
    private MultiSelector mMultiSelector;
    private final int CARD_STATE_ACTIVE_COLOR = Color.rgb(232, 240, 254);
    private final int CARD_STATE_COLOR = Color.WHITE;

    public void add(String s,int position) {
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
        public final CardView card;
        public final de.hdodenhof.circleimageview.CircleImageView profile;

        public SimpleViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.sender);
            card = (CardView) view.findViewById(R.id.card);
            profile = (de.hdodenhof.circleimageview.CircleImageView) view.findViewById(R.id.profile_image);
        }
    }

    public SimpleAdapter(Context context, String[] data) {
        mMultiSelector = new MultiSelector();
        mContext = context;
        if (data != null)
            mData = new ArrayList<String>(Arrays.asList(data));
        else mData = new ArrayList<String>();
    }

    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SimpleViewHolder holder, final int position) {
            holder.title.setText(mData.get(position));
            holder.profile.setBackground(mContext.getDrawable(R.drawable.selector));

        if (mMultiSelector.selectMode(mData.size())) {
            Log.d("Simple Adapter", "onBind, select mode: " + position);
            holder.profile.setImageResource(android.R.color.transparent);
        } else {
            Log.d("Simple Adapter", "onBind, normal mode: " + position);
            ProfileDrawable border = new ProfileDrawable(mContext);
            holder.profile.setImageDrawable(border);
        }
        holder.profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean selected = mMultiSelector.selectMode(mData.size());
                Log.d("Simple Adapter", "toggle");
                holder.profile.setActivated(!holder.profile.isActivated());
                if (holder.profile.isActivated()) {
                    holder.card.setCardBackgroundColor(CARD_STATE_ACTIVE_COLOR);
                } else {
                    holder.card.setCardBackgroundColor(CARD_STATE_COLOR);
                }

                mMultiSelector.setItemChecked(position, holder.profile.isActivated());
                if (selected != mMultiSelector.selectMode(mData.size())) {
                    notifyDataSetChanged();
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

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class MultiSelector {
        private SparseBooleanArray mSelectedPositions = new SparseBooleanArray();

        private boolean selectMode(int size){
            for (int i = 0; i<size; i++) {
                if (mSelectedPositions.get(i)){
                    return true;
                }
            }
            return false;
        }

        private void setItemChecked(int position, boolean isChecked) {
            mSelectedPositions.put(position, isChecked);
        }

        private boolean isItemChecked(int position) {
            return mSelectedPositions.get(position);
        }
    }
}
