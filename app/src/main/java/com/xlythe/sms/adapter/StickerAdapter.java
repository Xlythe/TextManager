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
            R.drawable.sticker_affection,
            R.drawable.sticker_approval,
            R.drawable.sticker_confused,
            R.drawable.sticker_crying,
            R.drawable.sticker_depressed,
            R.drawable.sticker_elated,
            R.drawable.sticker_embarrassed,
            R.drawable.sticker_excited,
            R.drawable.sticker_failure,
            R.drawable.sticker_kill_me,
            R.drawable.sticker_lazyass,
            R.drawable.sticker_love,
            R.drawable.sticker_nicks_true_face,
            R.drawable.sticker_no_problem,
            R.drawable.sticker_pat,
            R.drawable.sticker_pets,
            R.drawable.sticker_pout,
            R.drawable.sticker_praise_me,
            R.drawable.sticker_puppydog_eyes,
            R.drawable.sticker_seans_true_face,
            R.drawable.sticker_shocked,
            R.drawable.sticker_shy,
            R.drawable.sticker_stressed,
            R.drawable.sticker_suspiscious,
            R.drawable.sticker_thinking,
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

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private StickerAdapter.OnItemClickListener mClickListener;
        private ImageView mImage;

        public ViewHolder(View view, StickerAdapter.OnItemClickListener listener) {
            super(view);
            mClickListener = listener;
            mImage = (ImageView) view.findViewById(R.id.image);
            view.setOnClickListener(this);
        }

        void setImage(int resId) {
            mImage.setImageResource(resId);
        }

        @Override
        public void onClick(View v) {
            mClickListener.onItemClick(mImage.getDrawable());
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Drawable drawable);
    }
}
