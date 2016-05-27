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
            R.mipmap.stickers_affection,
            R.mipmap.stickers_approval,
            R.mipmap.stickers_confused,
            R.mipmap.stickers_crying,
            R.mipmap.stickers_depressed,
            R.mipmap.stickers_elated,
            R.mipmap.stickers_embarrassed,
            R.mipmap.stickers_excited,
            R.mipmap.stickers_failure,
            R.mipmap.stickers_killme,
            R.mipmap.stickers_lazyass,
            R.mipmap.stickers_love,
            R.mipmap.stickers_nicksface,
            R.mipmap.stickers_noproblem,
            R.mipmap.stickers_pat,
            R.mipmap.stickers_pets,
            R.mipmap.stickers_pout,
            R.mipmap.stickers_praiseme,
            R.mipmap.stickers_puppydog,
            R.mipmap.stickers_seansface,
            R.mipmap.stickers_shocked,
            R.mipmap.stickers_shy,
            R.mipmap.stickers_stressed,
            R.mipmap.stickers_suspicious,
            R.mipmap.stickers_thinking,
            R.mipmap.stickers_unmotivated
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
