package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.textmanager.text.CustomManagerCursor;

import java.util.Date;

/**
 * Created by Niko on 5/24/15.
 */
public class CursorThreadAdapter extends CursorAdapter {

    private CustomManagerCursor mCursor;

    public CursorThreadAdapter(Context context, CustomManagerCursor c) {
        super(context, c);
        mCursor = c;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.list_item_threads, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView user = (ImageView) view.findViewById(R.id.user);
        user.setColorFilter(mCursor.getColor());

        TextView number = (TextView) view.findViewById(R.id.number);
        number.setText(mCursor.getAddress());

        TextView message = (TextView) view.findViewById(R.id.message);
        message.setText(mCursor.getBody());

        TextView date = (TextView) view.findViewById(R.id.date);
        date.setText(mCursor.getFormattedDate());
    }
}