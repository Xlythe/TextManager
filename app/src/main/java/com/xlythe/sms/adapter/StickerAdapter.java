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
            R.drawable.stickers_affection,
            R.drawable.stickers_approval,
            R.drawable.stickers_confused,
            R.drawable.stickers_crying,
            R.drawable.stickers_depressed,
            R.drawable.stickers_elated,
            R.drawable.stickers_embarrassed,
            R.drawable.stickers_excited,
            R.drawable.stickers_failure,
            R.drawable.stickers_killme,
            R.drawable.stickers_lazyass,
            R.drawable.stickers_love,
            R.drawable.stickers_nicksface,
            R.drawable.stickers_noproblem,
            R.drawable.stickers_pat,
            R.drawable.stickers_pets,
            R.drawable.stickers_pout,
            R.drawable.stickers_praiseme,
            R.drawable.stickers_puppydog,
            R.drawable.stickers_seansface,
            R.drawable.stickers_shocked,
            R.drawable.stickers_shy,
            R.drawable.stickers_stressed,
            R.drawable.stickers_suspicious,
            R.drawable.stickers_thinking,
            R.drawable.stickers_unmotivated
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
