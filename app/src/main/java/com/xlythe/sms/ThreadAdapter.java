package com.xlythe.sms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.xlythe.textmanager.Message;
import com.xlythe.textmanager.MessageThread;

import java.util.Date;
import java.util.List;

/**
 * Created by Niko on 5/21/15.
 */
public class ThreadAdapter extends ArrayAdapter<Message> {
    private final Context context;
    private final List<Message> values;

    public ThreadAdapter(Context context, List<Message> values) {
        super(context, R.layout.list_item_conversations, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_item_conversations, parent, false);

        Message msg = values.get(position);
        TextView number = (TextView) rowView.findViewById(R.id.number);
        number.setText(msg.getAddress());

        TextView message = (TextView) rowView.findViewById(R.id.message);
        message.setText(msg.getBody());

        TextView date = (TextView) rowView.findViewById(R.id.date);
        String formatDate = msg.getDate();
        date.setText((new Date(Long.parseLong(formatDate))).toString());
        return rowView;
    }
}
