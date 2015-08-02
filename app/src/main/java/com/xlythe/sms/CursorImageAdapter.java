package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;

/**
 * Created by Niko on 8/2/15.
 */
public class CursorImageAdapter extends CursorAdapter{

    public CursorImageAdapter(Context context, Cursor c) {
        super(context, c);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // TODO Auto-generated method stub
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        Picasso.with(context).load(new File(path)).into((SquareImageView) view);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        SquareImageView iView = new SquareImageView(context);
        iView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return iView;
    }
}
