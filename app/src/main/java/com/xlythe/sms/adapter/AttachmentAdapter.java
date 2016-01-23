package com.xlythe.sms.adapter;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.support.v4.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.xlythe.sms.R;
import com.xlythe.sms.util.ColorUtils;

import java.io.File;

/**
 * Created by Niko on 1/21/16.
 */
public class AttachmentAdapter extends SelectableAdapter<AttachmentAdapter.ViewHolder> {
    private static final String TAG = AttachmentAdapter.class.getSimpleName();

    private ViewHolder.ClickListener mClickListener;
    private Context mContext;
    private Cursor mCursor;
    private int mColor;

    public AttachmentAdapter(Fragment fragment, Context context, Cursor cursor, int color) {
        mClickListener = (ViewHolder.ClickListener) fragment;
        mContext = context;
        mCursor = cursor;
        mColor = color;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ClickListener mListener;
        private ImageView mImage;
        private ImageView mButton;
        private ImageView mButtonShape;
        private Context mContext;

        public ViewHolder(View view, ClickListener listener, Context context) {
            super(view);
            mContext = context;
            mListener = listener;
            mImage = (ImageView) view.findViewById(R.id.image);
            mButton = (ImageView) view.findViewById(R.id.button);
            mButtonShape = (ImageView) view.findViewById(R.id.button_shape);
            view.setOnClickListener(this);
        }

        public void setCursor(Cursor cursor, boolean isSelected, boolean selectMode, int color){
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            Log.d(TAG, "Color: " + color);
            Glide.with(mContext)
                    .load(new File(path))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontAnimate()
                    .placeholder(R.color.loading)
                    .into(mImage);

            mButton.setScaleX(0);
            mButton.setScaleY(0);
            mButtonShape.setScaleX(0);
            mButtonShape.setScaleY(0);
            if (selectMode && isSelected) {
                Log.d(TAG, "selected");
                mImage.animate().alpha(1f).setDuration(100).setInterpolator(new DecelerateInterpolator()).start();
                mButton.setVisibility(View.VISIBLE);
                mButtonShape.setVisibility(View.VISIBLE);
                mButton.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                mButton.animate().scaleX(1).scaleY(1).setDuration(100).start();
                mButtonShape.animate().scaleX(1).scaleY(1).setDuration(100).start();
                mImage.animate().scaleX(1.3f).scaleY(1.3f).setDuration(300).setInterpolator(new OvershootInterpolator(2)).start();
            } else {
                Log.d(TAG, "not selected");
                if (selectMode) {
                    mImage.animate().alpha(0.4f).setDuration(100).setInterpolator(new DecelerateInterpolator()).start();
                } else {
                    mImage.animate().alpha(1f).setDuration(100).setInterpolator(new DecelerateInterpolator()).start();
                }
                mButton.setVisibility(View.GONE);
                mButtonShape.setVisibility(View.GONE);
                mButton.clearColorFilter();
                mImage.animate().scaleX(1).scaleY(1).setDuration(100).setInterpolator(new AccelerateInterpolator()).start();
            }
        }

        @Override
        public void onClick(View v) {
            mListener.onItemClicked(getAdapterPosition(), (ImageView) v.findViewById(R.id.button));
        }

        public interface ClickListener {
            void onItemClicked(int position, ImageView button);
        }
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.attach_icon, parent, false);
        return new ViewHolder(layout, mClickListener, mContext);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        boolean isSelected = isSelected(position);
        boolean selectMode = selectMode();
        Log.d(TAG, "Selected: " + isSelected);
        Log.d(TAG, "Select mode: " + selectMode);

        mCursor.moveToPosition(position);
        holder.setCursor(mCursor, isSelected, selectMode, mColor);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }
}
