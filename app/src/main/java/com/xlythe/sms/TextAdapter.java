package com.xlythe.sms;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xlythe.textmanager.text.Text;

import java.util.Date;
import java.util.List;

/**
 * Created by Niko on 5/21/15.
 */
public class TextAdapter extends ArrayAdapter<Text> {
    public TextAdapter(Context context, List<Text> values) {
        super(context, R.layout.list_item_conversation, values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_conversation, parent, false);
        }

        Text msg = getItem(position);

        if(msg.getPerson()==null) {
            convertView.setBackgroundResource(R.drawable.you);
        }
        else {
            convertView.setBackgroundResource(R.drawable.other);
            convertView.getBackground().setColorFilter(0xffff5722, PorterDuff.Mode.SRC_IN);
        }

        TextView number = (TextView) convertView.findViewById(R.id.number);
        number.setText(msg.getAddress());

        TextView message = (TextView) convertView.findViewById(R.id.message);
        message.setText(msg.getBody());

        TextView date = (TextView) convertView.findViewById(R.id.date);
        String formatDate = msg.getDate();
        date.setText((new Date(Long.parseLong(formatDate))).toString());

        return convertView;
    }
}
