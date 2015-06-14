package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.textmanager.text.*;
import com.xlythe.textmanager.text.Thread;

/**
 * Created by Niko on 5/24/15.
 */
public class CursorThreadAdapter extends CursorAdapter {

    private ThreadCursor mCursor;

    public CursorThreadAdapter(Context context, ThreadCursor c) {
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
        // Get thread from cursor.
        Thread thread = mCursor.getThread();
        view.setTag(thread);

        // Get name and photo from contacts
        TextManager manager = TextManager.getInstance(context);
        Contact sender = manager.getSender(thread);
        String name = sender.getDisplayName();
        Uri photo = sender.getPhotoUri();

        // Color user icons
        ImageView user = (ImageView) view.findViewById(R.id.user);
        ImageView userImage = (ImageView) view.findViewById(R.id.profile_image);
        ImageView userIcon = (ImageView) view.findViewById(R.id.user_icon);
        TextView text = (TextView) view.findViewById(R.id.text);
        if(photo!=null){
            userImage.setImageURI(photo);
            userImage.setVisibility(View.VISIBLE);
            userIcon.setVisibility(View.GONE);
            user.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
        }
        else {
            user.setColorFilter(ColorUtils.getColor(thread.getThreadId()));
            userImage.setVisibility(View.GONE);
            userIcon.setVisibility(View.VISIBLE);
            user.setVisibility(View.VISIBLE);
            text.setVisibility(View.GONE);
            if (sender.hasName()){
                text.setText(Character.toString(name.charAt(0)));
                text.setVisibility(View.VISIBLE);
                userIcon.setVisibility(View.GONE);
            }
        }

        // Add numbers to the list.
        TextView number = (TextView) view.findViewById(R.id.number);
        number.setText(name);

        // Add message bodies to the list.
        TextView message = (TextView) view.findViewById(R.id.message);
        message.setText(thread.getBody());

        // Add a formatted dates to the list.
        TextView date = (TextView) view.findViewById(R.id.date);
        date.setText(DateFormatter.getFormattedDate(thread));
    }
}