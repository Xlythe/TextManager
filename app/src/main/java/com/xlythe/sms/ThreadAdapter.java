package com.xlythe.sms;

import android.content.Context;
import android.graphics.PorterDuff;
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
        super(context, R.layout.list_item_threads, values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_threads, parent, false);
        }

        TextThread mt = getItem(position);

        ImageView user = (ImageView) convertView.findViewById(R.id.user);

        user.getDrawable().setColorFilter(mt.getColor(), PorterDuff.Mode.SRC_IN);

        TextView number = (TextView) convertView.findViewById(R.id.number);
        number.setText(mt.getAddress());

        TextView message = (TextView) convertView.findViewById(R.id.message);
        message.setText(mt.getBody());

        TextView date = (TextView) convertView.findViewById(R.id.date);
        String formatDate = mt.getDate();
        date.setText((new Date(Long.parseLong(formatDate))).toString());

        return convertView;
    }
}
