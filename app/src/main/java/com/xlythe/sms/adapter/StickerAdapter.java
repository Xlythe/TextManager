package com.xlythe.sms.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xlythe.sms.R;
import com.xlythe.sms.pojo.Sticker;

public class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.ViewHolder> {
    private static final Sticker[] STICKERS = {
            new Sticker(R.drawable.thumb_affection, R.raw.stickers_affection),
            new Sticker(R.drawable.thumb_approval, R.raw.stickers_approval),
            new Sticker(R.drawable.thumb_confused, R.raw.stickers_confused),
            new Sticker(R.drawable.thumb_crying, R.raw.stickers_crying),
            new Sticker(R.drawable.thumb_depressed, R.raw.stickers_depressed),
            new Sticker(R.drawable.thumb_elated, R.raw.stickers_elated),
            new Sticker(R.drawable.thumb_embarrassed, R.raw.stickers_embarrassed),
            new Sticker(R.drawable.thumb_excited, R.raw.stickers_excited),
            new Sticker(R.drawable.thumb_failure, R.raw.stickers_failure),
            new Sticker(R.drawable.thumb_killme, R.raw.stickers_killme),
            new Sticker(R.drawable.thumb_lazyass, R.raw.stickers_lazyass),
            new Sticker(R.drawable.thumb_love, R.raw.stickers_love),
            new Sticker(R.drawable.thumb_nicksface, R.raw.stickers_nicksface),
            new Sticker(R.drawable.thumb_noproblem, R.raw.stickers_noproblem),
            new Sticker(R.drawable.thumb_pat, R.raw.stickers_pat),
            new Sticker(R.drawable.thumb_pets, R.raw.stickers_pets),
            new Sticker(R.drawable.thumb_pout, R.raw.stickers_pout),
            new Sticker(R.drawable.thumb_praiseme, R.raw.stickers_praiseme),
            new Sticker(R.drawable.thumb_puppydog, R.raw.stickers_puppydog),
            new Sticker(R.drawable.thumb_seansface, R.raw.stickers_seansface),
            new Sticker(R.drawable.thumb_shocked, R.raw.stickers_shocked),
            new Sticker(R.drawable.thumb_shy, R.raw.stickers_shy),
            new Sticker(R.drawable.thumb_stressed, R.raw.stickers_stressed),
            new Sticker(R.drawable.thumb_suspicious, R.raw.stickers_suspicious),
            new Sticker(R.drawable.thumb_thinking, R.raw.stickers_thinking),
            new Sticker(R.drawable.thumb_unmotivated, R.raw.stickers_unmotivated)
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
        holder.setSticker(STICKERS[position]);
    }

    @Override
    public int getItemCount() {
        return STICKERS.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final Context mContext;
        private final StickerAdapter.OnItemClickListener mClickListener;
        private final ImageView mImage;

        private Sticker mSticker;

        public ViewHolder(View view, StickerAdapter.OnItemClickListener listener) {
            super(view);
            mContext = view.getContext();
            mClickListener = listener;
            mImage = (ImageView) view.findViewById(R.id.image);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        void setSticker(Sticker sticker) {
            mSticker = sticker;
            mImage.setImageResource(sticker.getThumbnail());
        }

        @Override
        public void onClick(View v) {
            mClickListener.onItemClick(mSticker);
        }

        @Override
        public boolean onLongClick(View v) {
            mClickListener.onItemLongClick(mSticker);
            return true;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Sticker sticker);
        void onItemLongClick(Sticker sticker);
    }
}
