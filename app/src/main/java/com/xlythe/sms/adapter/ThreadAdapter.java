package com.xlythe.sms.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.sms.R;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.sms.util.DateFormatter;
import com.xlythe.textmanager.text.*;
import com.xlythe.textmanager.text.Thread;

import java.util.List;

/**
 * Created by Niko on 6/18/15.
 */
public class ThreadAdapter extends ArrayAdapter {
    private Context mContext;
    private int mLayoutResourceId;
    private List<Thread> mThreads;

    public ThreadAdapter(Context context, int layoutResourceId, List<Thread> threads) {
        super(context, layoutResourceId, threads);
        mLayoutResourceId = layoutResourceId;
        mContext = context;
        mThreads = threads;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = inflater.inflate(mLayoutResourceId, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        Thread thread = mThreads.get(position);

        // Get name and photo from contacts
        TextManager manager = TextManager.getInstance(mContext);
        Contact sender = null;//manager.getSender(thread);
        String name = sender.getDisplayName();
        Uri photo = sender.getPhotoUri();

        // Color user icons
        if (photo != null) {
            holder.userImage.setImageURI(photo);
            holder.userImage.setVisibility(View.VISIBLE);
            holder.userIcon.setVisibility(View.GONE);
            holder.user.setVisibility(View.GONE);
            holder.text.setVisibility(View.GONE);
        } else {
            holder.user.setColorFilter(ColorUtils.getColor(Long.parseLong(thread.getId())));
            holder.userImage.setVisibility(View.GONE);
            holder.userIcon.setVisibility(View.VISIBLE);
            holder.user.setVisibility(View.VISIBLE);
            holder.text.setVisibility(View.GONE);
            if (sender.hasName()) {
                holder.text.setText(Character.toString(name.charAt(0)));
                holder.text.setVisibility(View.VISIBLE);
                holder.userIcon.setVisibility(View.GONE);
            }
        }

        // Add numbers to the list.
        holder.number.setText(name);

        // Add message bodies to the list.
        holder.message.setText("Body");

        // Add a formatted dates to the list.
        holder.date.setText(DateFormatter.getFormattedDate(thread));

        return view;
    }

    static class ViewHolder {
        ImageView user;
        ImageView userImage;
        ImageView userIcon;
        TextView text;
        TextView number;
        TextView message;
        TextView date;

        public ViewHolder(View view) {
            user = (ImageView) view.findViewById(R.id.user);
            userImage = (ImageView) view.findViewById(R.id.profile_image);
            userIcon = (ImageView) view.findViewById(R.id.user_icon);
            text = (TextView) view.findViewById(R.id.text);
            number = (TextView) view.findViewById(R.id.number);
            message = (TextView) view.findViewById(R.id.message);
            date = (TextView) view.findViewById(R.id.date);
        }
    }
}
