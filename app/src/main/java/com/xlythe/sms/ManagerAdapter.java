package com.xlythe.sms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.xlythe.textmanager.MessageThread;
import com.xlythe.textmanager.text.TextThread;

import java.util.Date;
import java.util.List;

/**
 * Created by Niko on 5/21/15.
 */
public class ManagerAdapter extends ArrayAdapter<MessageThread> {
    private final Context context;
    private final List<MessageThread> values;

    public ManagerAdapter(Context context, List<MessageThread> values) {
        super(context, R.layout.list_item_conversations, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_item_conversations, parent, false);

        MessageThread mt = values.get(position);
            TextView number = (TextView) rowView.findViewById(R.id.number);
            number.setText(mt.getAddress());

            TextView message = (TextView) rowView.findViewById(R.id.message);
            message.setText(mt.getBody());

            TextView date = (TextView) rowView.findViewById(R.id.date);
            String formatDate = mt.getDate();
            date.setText((new Date(Long.parseLong(formatDate))).toString());
        return rowView;
    }
}
