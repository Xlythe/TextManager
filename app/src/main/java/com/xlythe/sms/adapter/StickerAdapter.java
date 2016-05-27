package com.xlythe.sms.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xlythe.sms.R;

public class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.ViewHolder> {
    private static final String TAG = StickerAdapter.class.getSimpleName();

    private static final int[] STICKERS = {
            R.drawable.thumb_affection,
            R.drawable.thumb_approval,
            R.drawable.thumb_confused,
            R.drawable.thumb_crying,
            R.drawable.thumb_depressed,
            R.drawable.thumb_elated,
            R.drawable.thumb_embarrassed,
            R.drawable.thumb_excited,
            R.drawable.thumb_failure,
            R.drawable.thumb_killme,
            R.drawable.thumb_lazyass,
            R.drawable.thumb_love,
            R.drawable.thumb_nicksface,
            R.drawable.thumb_noproblem,
            R.drawable.thumb_pat,
            R.drawable.thumb_pets,
            R.drawable.thumb_pout,
            R.drawable.thumb_praiseme,
            R.drawable.thumb_puppydog,
            R.drawable.thumb_seansface,
            R.drawable.thumb_shocked,
            R.drawable.thumb_shy,
            R.drawable.thumb_stressed,
            R.drawable.thumb_suspicious,
            R.drawable.thumb_thinking,
            R.drawable.thumb_unmotivated
    };

    private StickerAdapter.OnItemClickListener mClickListener;
    private Context mContext;

    public StickerAdapter(Context context, OnItemClickListener listener) {
        mContext = context;
        mClickListener = listener;
    }

    @Override
    public StickerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.grid_item_sticker, parent, false);
        return new ViewHolder(layout, mClickListener);
    }

    @Override
    public void onBindViewHolder(StickerAdapter.ViewHolder holder, final int position) {
        holder.setImage(STICKERS[position]);
    }

    @Override
    public int getItemCount() {
        return STICKERS.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private StickerAdapter.OnItemClickListener mClickListener;
        private ImageView mImage;

        public ViewHolder(View view, StickerAdapter.OnItemClickListener listener) {
            super(view);
            mClickListener = listener;
            mImage = (ImageView) view.findViewById(R.id.image);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        void setImage(int resId) {
            mImage.setImageResource(resId);
        }

        @Override
        public void onClick(View v) {
            mClickListener.onItemClick(mImage.getDrawable());
        }

        @Override
        public boolean onLongClick(View v) {
            mClickListener.onItemLongClick(mImage.getDrawable());
            return true;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Drawable drawable);
        void onItemLongClick(Drawable drawable);
    }
}
