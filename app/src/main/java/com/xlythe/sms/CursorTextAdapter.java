package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

        final float scale = context.getResources().getDisplayMetrics().density;

        long date1 = Long.parseLong(mCursor.getDate());
        boolean user1 = mCursor.sentByUser();
        long date2 = 0;
        boolean user2 = mCursor.sentByUser();
        long date3 = 0;
        boolean user3 = !mCursor.sentByUser();
        long split;
        long split2;

        if(!mCursor.isFirst()) {
            mCursor.moveToPrevious();
            date2 = Long.parseLong(mCursor.getDate());
            user2 = mCursor.sentByUser();
            mCursor.moveToNext();
        }
        if(!mCursor.isLast()) {
            mCursor.moveToNext();
            date3 = Long.parseLong(mCursor.getDate());
            user3 = mCursor.sentByUser();
            mCursor.moveToPrevious();
        }

        split = date1-date2;
        split2 = date3-date1;

        // User recent
        if(split<=60000 && user1==user2 && user1){
            rl.setVisibility(View.VISIBLE);
            rl2.setVisibility(View.GONE);
            rl.getChildAt(0).setBackgroundResource(R.drawable.recent_you);
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon);
            icon.setVisibility(View.INVISIBLE);
            TextView message = (TextView) view.findViewById(R.id.message);
            message.setText(mCursor.getBody());
            TextView date = (TextView) view.findViewById(R.id.date);
            date.setVisibility(View.VISIBLE);
            date.setText(mCursor.getFormattedDate());

            int pixel =  (int)(4 * scale + 0.5f);
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
            pixel =  (int)(16 * scale + 0.5f);
            ((RelativeLayout.LayoutParams)rl.getChildAt(0).getLayoutParams()).bottomMargin = pixel;

            if(split2<=60000 && user3){
                date.setVisibility(View.GONE);
                pixel =  (int)(12 * scale + 0.5f);
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
                pixel =  (int)(2.5 * scale + 0.5f);
                ((RelativeLayout.LayoutParams)rl.getChildAt(0).getLayoutParams()).bottomMargin = pixel;
            }
        }
        // Recipient recent
        else if(split<=60000 && user1==user2 && !user1){
            rl2.setVisibility(View.VISIBLE);
            rl.setVisibility(View.GONE);
            rl2.getChildAt(0).setBackgroundResource(R.drawable.recent_other);
            rl2.getChildAt(0).getBackground().setColorFilter(ColorUtils.getColor(mCursor.getThreadId()), PorterDuff.Mode.SRC_IN);
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon2);
            icon.setVisibility(View.INVISIBLE);
            TextView message = (TextView) view.findViewById(R.id.message2);
            message.setText(mCursor.getBody());
            TextView date = (TextView) view.findViewById(R.id.date2);
            date.setVisibility(View.VISIBLE);
            date.setText(mCursor.getFormattedDate());

            int pixel =  (int)(4 * scale + 0.5f);
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
            pixel =  (int)(16 * scale + 0.5f);
            ((RelativeLayout.LayoutParams)rl2.getChildAt(0).getLayoutParams()).bottomMargin = pixel;

            if(split2<=60000 && !user3){
                date.setVisibility(View.GONE);
                pixel =  (int)(12 * scale + 0.5f);
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
                pixel =  (int)(2.5 * scale + 0.5f);
                ((RelativeLayout.LayoutParams)rl2.getChildAt(0).getLayoutParams()).bottomMargin = pixel;
            }
        }
        // User
        else if(user1) {
            rl.setVisibility(View.VISIBLE);
            rl2.setVisibility(View.GONE);
            rl.getChildAt(0).setBackgroundResource(R.drawable.you);
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon);
            icon.setVisibility(View.VISIBLE);
            ImageView user = (ImageView) view.findViewById(R.id.user);
            user.setColorFilter(0xff757575);
            TextView message = (TextView) view.findViewById(R.id.message);
            message.setText(mCursor.getBody());
            TextView date = (TextView) view.findViewById(R.id.date);
            date.setVisibility(View.VISIBLE);
            date.setText(mCursor.getFormattedDate());

            int pixel =  (int)(4 * scale + 0.5f);
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
            pixel =  (int)(16 * scale + 0.5f);
            ((RelativeLayout.LayoutParams)rl.getChildAt(0).getLayoutParams()).bottomMargin = pixel;

            if(split2<=60000 && user3){
                date.setVisibility(View.GONE);
                pixel =  (int)(12 * scale + 0.5f);
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
                pixel =  (int)(2.5 * scale + 0.5f);
                ((RelativeLayout.LayoutParams)rl.getChildAt(0).getLayoutParams()).bottomMargin = pixel;
            }
        }
        // Recipient
        else {
            rl2.setVisibility(View.VISIBLE);
            rl.setVisibility(View.GONE);
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon2);
            icon.setVisibility(View.VISIBLE);
            rl2.getChildAt(0).setBackgroundResource(R.drawable.other);
            rl2.getChildAt(0).getBackground().setColorFilter(ColorUtils.getColor(mCursor.getThreadId()), PorterDuff.Mode.SRC_IN);
            ImageView user = (ImageView) view.findViewById(R.id.user2);
            user.setColorFilter(ColorUtils.getColor(mCursor.getThreadId()));
            TextView message = (TextView) view.findViewById(R.id.message2);
            message.setText(mCursor.getBody());
            TextView date = (TextView) view.findViewById(R.id.date2);
            date.setVisibility(View.VISIBLE);
            date.setText(mCursor.getFormattedDate());

            int pixel =  (int)(4 * scale + 0.5f);
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
            pixel =  (int)(16 * scale + 0.5f);
            ((RelativeLayout.LayoutParams)rl2.getChildAt(0).getLayoutParams()).bottomMargin = pixel;

            if(split2<=60000 && !user3){
                date.setVisibility(View.GONE);
                pixel =  (int)(12 * scale + 0.5f);
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
                pixel =  (int)(2.5 * scale + 0.5f);
                ((RelativeLayout.LayoutParams)rl2.getChildAt(0).getLayoutParams()).bottomMargin = pixel;
            }
        }
        if(mCursor.isLast()) {
            ((RelativeLayout.LayoutParams)rl2.getChildAt(0).getLayoutParams()).bottomMargin = 0;
            ((RelativeLayout.LayoutParams)rl.getChildAt(0).getLayoutParams()).bottomMargin = 0;
        }
    }
}
