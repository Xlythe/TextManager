package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xlythe.textmanager.text.CustomThreadCursor;

import java.util.Date;

/**
 * Created by Niko on 5/23/15.
 */

public class CursorTextAdapter extends CursorAdapter{

    private CustomThreadCursor mCursor;

    public CursorTextAdapter(Context context, CustomThreadCursor c) {
        super(context, c);
        mCursor = c;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.list_item_texts, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.you);
        RelativeLayout rl2 = (RelativeLayout) view.findViewById(R.id.them);

        if(mCursor.getPerson()==null) {
            rl.setVisibility(View.VISIBLE);
            rl2.setVisibility(View.GONE);
            ImageView user = (ImageView) view.findViewById(R.id.user);
            user.setColorFilter(0xff757575);
            TextView message = (TextView) view.findViewById(R.id.message);
            message.setText(mCursor.getBody());
            TextView date = (TextView) view.findViewById(R.id.date);
            String formatDate = mCursor.getDate();
            date.setText((new Date(Long.parseLong(formatDate))).toString());
        }
        else {
            rl2.setVisibility(View.VISIBLE);
            rl.setVisibility(View.GONE);
            rl2.getChildAt(0).getBackground().setColorFilter(mCursor.getColor(), PorterDuff.Mode.SRC_IN);
            ImageView user = (ImageView) view.findViewById(R.id.user2);
            user.setColorFilter(mCursor.getColor());
            TextView message = (TextView) view.findViewById(R.id.message2);
            message.setText(mCursor.getBody());
            TextView date = (TextView) view.findViewById(R.id.date2);
            String formatDate = mCursor.getDate();
            date.setText((new Date(Long.parseLong(formatDate))).toString());
        }
    }
}
