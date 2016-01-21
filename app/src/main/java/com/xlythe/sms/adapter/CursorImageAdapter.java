package com.xlythe.sms.adapter;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.squareup.picasso.Picasso;
import com.xlythe.sms.R;
import com.xlythe.sms.view.SquareImageView;

import java.io.File;

/**
 * Created by Niko on 8/2/15.
 */
public class CursorImageAdapter extends CursorAdapter{

    public CursorImageAdapter(Context context, Cursor c) {
        super(context, c);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
        Glide.with(context)
                .load(new File(path))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .dontAnimate()
                .placeholder(R.color.loading)
                .into((SquareImageView) view);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        SquareImageView iView = new SquareImageView(context);
        iView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return iView;
    }


}
