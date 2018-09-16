package com.xlythe.sms.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.xlythe.sms.R;
import com.xlythe.view.camera.Image;

import java.io.File;

import androidx.recyclerview.widget.RecyclerView;

public class AttachmentAdapter extends SelectableAdapter<Integer, AttachmentAdapter.ViewHolder> {
    private static final String TAG = AttachmentAdapter.class.getSimpleName();

    private AttachmentAdapter.OnItemClickListener mClickListener;
    private Context mContext;
    private Cursor mCursor;
    private int mColor;

    public AttachmentAdapter(Context context, Cursor cursor, int color, AttachmentAdapter.OnItemClickListener listener) {
        mContext = context;
        mCursor = cursor;
        mColor = color;
        mClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private OnItemClickListener mListener;
        private ImageView mImage;
        private ImageView mButton;
        private ImageView mButtonShape;
        private Context mContext;

        public ViewHolder(View view, OnItemClickListener listener, Context context) {
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
            mListener.onItemClick(getAdapterPosition(), (ImageView) v.findViewById(R.id.button));
        }
    }

    @Override
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

    public interface OnItemClickListener {
        void onItemClick(int position, ImageView button);
    }
}
