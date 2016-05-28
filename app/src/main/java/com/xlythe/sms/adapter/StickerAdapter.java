package com.xlythe.sms.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xlythe.sms.R;

public class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.ViewHolder> {
    private static final String TAG = StickerAdapter.class.getSimpleName();

    private static final int[] STICKERS = {
            R.raw.stickers_affection,
            R.raw.stickers_approval,
            R.raw.stickers_confused,
            R.raw.stickers_crying,
            R.raw.stickers_depressed,
            R.raw.stickers_elated,
            R.raw.stickers_embarrassed,
            R.raw.stickers_excited,
            R.raw.stickers_failure,
            R.raw.stickers_killme,
            R.raw.stickers_lazyass,
            R.raw.stickers_love,
            R.raw.stickers_nicksface,
            R.raw.stickers_noproblem,
            R.raw.stickers_pat,
            R.raw.stickers_pets,
            R.raw.stickers_pout,
            R.raw.stickers_praiseme,
            R.raw.stickers_puppydog,
            R.raw.stickers_seansface,
            R.raw.stickers_shocked,
            R.raw.stickers_shy,
            R.raw.stickers_stressed,
            R.raw.stickers_suspicious,
            R.raw.stickers_thinking,
            R.raw.stickers_unmotivated
    };

    private static final int[] THUMBNAILS = {
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
        holder.setImage(mContext, THUMBNAILS[position], STICKERS[position]);
    }

    @Override
    public int getItemCount() {
        return STICKERS.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private StickerAdapter.OnItemClickListener mClickListener;
        private ImageView mImage;
        private int mStickerId;
        private Context mContext;

        public ViewHolder(View view, StickerAdapter.OnItemClickListener listener) {
            super(view);
            mClickListener = listener;
            mImage = (ImageView) view.findViewById(R.id.image);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        void setImage(Context context, int resIdThumbnail, int resIdSticker) {
            mImage.setImageResource(resIdThumbnail);
            mStickerId = resIdSticker;
            mContext = context;
        }

        @Override
        public void onClick(View v) {
            mClickListener.onItemClick(Uri.parse("android.resource://" + mContext.getPackageName() + "/" + mStickerId));
        }

        @Override
        public boolean onLongClick(View v) {
            mClickListener.onItemLongClick(mContext.getResources().getDrawable(mStickerId));
            return true;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Uri uri);
        void onItemLongClick(Drawable drawable);
    }
}
