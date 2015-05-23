package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.xlythe.textmanager.text.CustomCursor;

import java.util.Date;

/**
 * Created by Niko on 5/23/15.
 */

public class CursorTextAdapter extends CursorAdapter{

    private CustomCursor mCursor;

    public CursorTextAdapter(Context context, CustomCursor c) {
        super(context, c);
        mCursor = c;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.list_item_conversation, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView message = (TextView) view.findViewById(R.id.message);
        message.setText(mCursor.getBody());

        TextView date = (TextView) view.findViewById(R.id.date);
        String formatDate = mCursor.getDate();
        date.setText((new Date(Long.parseLong(formatDate))).toString());

        if(mCursor.getPerson()==null) {
            view.setBackgroundResource(R.drawable.you);
            message.setTextColor(0xff323232);
            date.setTextColor(0xa2000000);

        }
        else {
            view.setBackgroundResource(R.drawable.other);
            view.getBackground().setColorFilter(0xffff5722, PorterDuff.Mode.SRC_IN);
            message.setTextColor(0xffffffff);
            date.setTextColor(0xa2ffffff);
        }
    }
}
