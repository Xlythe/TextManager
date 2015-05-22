package com.xlythe.sms;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.textmanager.text.TextThread;

import java.util.Date;
import java.util.List;

/**
 * Created by Niko on 5/21/15.
 */
public class ThreadAdapter extends ArrayAdapter<TextThread> {

    public ThreadAdapter(Context context, List<TextThread> values) {
        super(context, R.layout.list_item_conversations, values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_conversations, parent, false);
        }

        TextThread mt = getItem(position);

        ImageView user = (ImageView) convertView.findViewById(R.id.user);

        user.getDrawable().setColorFilter(getColor(mt.getThreadId()), PorterDuff.Mode.SRC_IN);

        TextView number = (TextView) convertView.findViewById(R.id.number);
        number.setText(mt.getAddress());

        TextView message = (TextView) convertView.findViewById(R.id.message);
        message.setText(mt.getBody());

        TextView date = (TextView) convertView.findViewById(R.id.date);
        String formatDate = mt.getDate();
        date.setText((new Date(Long.parseLong(formatDate))).toString());

        return convertView;
    }

    public int getColor(String threadId){
        int num = Integer.parseInt(threadId)%12;
        switch (num){
            case 0:
                return 0xffdb4437;
            case 1:
                return 0xffe91e63;
            case 2:
                return 0xff9c27b0;
            case 3:
                return 0xff3f51b5;
            case 4:
                return 0xff039be5;
            case 5:
                return 0xff4285f4;
            case 6:
                return 0xff0097a7;
            case 7:
                return 0xff009688;
            case 8:
                return 0xff0f9d58;
            case 9:
                return 0xff689f38;
            case 10:
                return 0xffef6c00;
            case 11:
                return 0xffff5722;
            default:
                return 0xff757575;
        }
    }
}
