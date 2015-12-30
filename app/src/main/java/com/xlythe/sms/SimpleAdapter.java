package com.xlythe.sms;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xlythe.textmanager.text.Thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Niko on 12/22/15.
 */
public class SimpleAdapter extends SelectableAdapter<RecyclerView.ViewHolder>{
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final Context mContext;
    private List mData;
    private final int CARD_STATE_ACTIVE_COLOR = Color.rgb(229, 244, 243);
    private final int CARD_STATE_COLOR = Color.WHITE;
    private SimpleViewHolder.ClickListener mClickListener;

    public SimpleAdapter(Context context, List<Thread> data, ArrayList<Section> headers) {
        mClickListener = (SimpleViewHolder.ClickListener) context;
        mContext = context;
        mData = data;
        for(int i=0; i<headers.size();i++) {
            mData.add(headers.get(i).mPosition, headers.get(i));
        }
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

    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_ITEM) {
            final View view = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
            return new SimpleViewHolder(view, mClickListener);
        } else if (viewType == TYPE_HEADER) {
            final View view = LayoutInflater.from(mContext).inflate(R.layout.section, parent, false);
            return new SimpleViewHolderHeader(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof SimpleViewHolder) {
            Thread data = (Thread) mData.get(position);
            SimpleViewHolder simpleHolder = (SimpleViewHolder) holder;
            simpleHolder.message.setText("body");
            simpleHolder.date.setText("date");
            simpleHolder.profile.setBackground(mContext.getDrawable(R.drawable.selector));

            int unread=0; //TODO: getUnread()
            int color=mContext.getColor(R.color.colorPrimary); //TODO: color
            Bitmap drawable=null; //TODO: drawable

            if (unread > 0) {
                simpleHolder.title.setText("address");
                simpleHolder.unread.setVisibility(View.VISIBLE);
                simpleHolder.unread.setText(unread + " new");
                simpleHolder.unread.setTextColor(color);
                simpleHolder.unread.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                simpleHolder.unread.getBackground().setAlpha(25);
                simpleHolder.title.setTextColor(color);
                simpleHolder.title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                simpleHolder.message.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                simpleHolder.date.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            } else {
                simpleHolder.title.setText("address");
                simpleHolder.unread.setVisibility(View.GONE);
                simpleHolder.title.setTextColor(mContext.getColor(R.color.headerText));
                simpleHolder.title.setTypeface(Typeface.create("sans-serif-regular", Typeface.NORMAL));
                simpleHolder.message.setTypeface(Typeface.create("sans-serif-regular", Typeface.NORMAL));
                simpleHolder.date.setTypeface(Typeface.create("sans-serif-regular", Typeface.NORMAL));
            }

            boolean isSelected = isSelected(position);

            if (selectMode()) {
                simpleHolder.profile.setImageResource(android.R.color.transparent);
            } else {
                if ("address"!=null) {
                    ProfileDrawable border = new ProfileDrawable(mContext,
                            "address".charAt(0),
                            color,
                            drawable);
                    simpleHolder.profile.setImageDrawable(border);
                }
            }

            simpleHolder.profile.setActivated(isSelected);
            if (isSelected) {
                simpleHolder.card.setCardBackgroundColor(CARD_STATE_ACTIVE_COLOR);
            } else {
                simpleHolder.card.setCardBackgroundColor(CARD_STATE_COLOR);
            }
        } else if (holder instanceof SimpleViewHolderHeader) {
            Section data = (Section) mData.get(position);
            SimpleViewHolderHeader header = (SimpleViewHolderHeader) holder;
            header.title.setText(data.mTitle);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mData.get(position) instanceof Section) {
            return TYPE_HEADER;
        }

        return TYPE_ITEM;
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

    public static class SimpleViewHolderHeader extends RecyclerView.ViewHolder {
        public final TextView title;
        public SimpleViewHolderHeader(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.section_text);
        }
    }
}
