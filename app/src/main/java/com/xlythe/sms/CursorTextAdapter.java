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
        // Here's where things get a little messy. There are four different layouts.
        // User: The messages that you sent.
        // User Recent: Messages that you sent in quick succession (appear close together, date displays once).
        // Recipient: The messages that you received.
        // Recipient Recent: Messages that you received in quick succession (appear close together, date displays once).

        // Get both base layouts (User and Recipient)
        RelativeLayout userLayout = (RelativeLayout) view.findViewById(R.id.you);
        RelativeLayout recipientLayout = (RelativeLayout) view.findViewById(R.id.them);

        // Used to convert dp to pixels
        final float scale = context.getResources().getDisplayMetrics().density;

        // * Previous meaning older message
        // * Next meaning most recently received

        // Get the date of the current, previous and next message.
        long dateCurrent = Long.parseLong(mCursor.getDate());
        long datePrevious = 0;
        long dateNext = 0;

        // Get the sender of the current, previous and next message. (returns true if you)
        boolean userCurrent = mCursor.sentByUser();
        boolean userPrevious = mCursor.sentByUser();
        boolean userNext = !mCursor.sentByUser();

        // Time gap between the current and previous, and the next and current messages.
        long splitCP;
        long splitNC;

        // Check if previous message exists, then get the date and sender.
        if(!mCursor.isFirst()) {
            mCursor.moveToPrevious();
            datePrevious = Long.parseLong(mCursor.getDate());
            userPrevious = mCursor.sentByUser();
            mCursor.moveToNext();
        }

        // Check if next message exists, then get the date and sender.
        if(!mCursor.isLast()) {
            mCursor.moveToNext();
            dateNext = Long.parseLong(mCursor.getDate());
            userNext = mCursor.sentByUser();
            mCursor.moveToPrevious();
        }

        // Calculate time gap.
        splitCP = dateCurrent-datePrevious;
        splitNC = dateNext-dateCurrent;

        // User Recent
        // Check if time gap between the current and previous message is less than 1 minute
        // they are both the same sender, and if the sender is you.
        // (This means an older message exists in the cluster)
        if(splitCP<=60000 && userCurrent==userPrevious && userCurrent){
            // Hide and show respective layouts.
            userLayout.setVisibility(View.VISIBLE);
            recipientLayout.setVisibility(View.GONE);

            // Set the message bubble background.
            userLayout.getChildAt(0).setBackgroundResource(R.drawable.recent_you);

            // Hide the user icon because there is already one showing on your first message in the cluster.
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon);
            icon.setVisibility(View.INVISIBLE);

            // Set the message body.
            TextView message = (TextView) view.findViewById(R.id.message);
            message.setText(mCursor.getBody());

            // Set the date and show it because this might be the last message in the cluster.
            TextView date = (TextView) view.findViewById(R.id.date);
            date.setVisibility(View.VISIBLE);
            date.setText(mCursor.getFormattedDate());

            // Update bottom margins to regular size because this might be the last message in the cluster.
            int pixel =  (int)(4 * scale + 0.5f);
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
            pixel =  (int)(16 * scale + 0.5f);
            ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = pixel;

            // Check if time gap between the next and current message is less than 1 minute
            // and that the sender is still you. (This means a newer message exists in the cluster)
            if(splitNC<=60000 && userNext){
                // Hide the date because it is not the most recent in the cluster.
                date.setVisibility(View.GONE);

                // Update margins so bubbles in cluster are closer together.
                pixel =  (int)(12 * scale + 0.5f);
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
                pixel =  (int)(2.5 * scale + 0.5f);
                ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = pixel;
            }
        }

        // Recipient Recent
        // Check if time gap between the current and previous message is less than 1 minute
        // they are both the same sender, and if the sender is not you.
        // (This means an older message exists in the cluster)
        else if(splitCP<=60000 && userCurrent==userPrevious && !userCurrent){
            // Hide and show respective layouts.
            recipientLayout.setVisibility(View.VISIBLE);
            userLayout.setVisibility(View.GONE);

            // Set the message bubble background and color it.
            recipientLayout.getChildAt(0).setBackgroundResource(R.drawable.recent_other);
            recipientLayout.getChildAt(0).getBackground().setColorFilter(ColorUtils.getColor(mCursor.getThreadId()), PorterDuff.Mode.SRC_IN);

            // Hide the user icon because there is already one showing on your first message in the cluster.
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon2);
            icon.setVisibility(View.INVISIBLE);

            // Set the message body.
            TextView message = (TextView) view.findViewById(R.id.message2);
            message.setText(mCursor.getBody());

            // Set the date and show it because this might be the last message in the cluster.
            TextView date = (TextView) view.findViewById(R.id.date2);
            date.setVisibility(View.VISIBLE);
            date.setText(mCursor.getFormattedDate());

            // Update bottom margins to regular size because this might be the last message in the cluster.
            int pixel =  (int)(4 * scale + 0.5f);
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
            pixel =  (int)(16 * scale + 0.5f);
            ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = pixel;

            // Check if time gap between the next and current message is less than 1 minute
            // and that the sender is still not you. (This means a newer message exists in the cluster)
            if(splitNC<=60000 && !userNext){
                // Hide the date because it is not the most recent in the cluster.
                date.setVisibility(View.GONE);

                // Update margins so bubbles in cluster are closer together.
                pixel =  (int)(12 * scale + 0.5f);
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
                pixel =  (int)(2.5 * scale + 0.5f);
                ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = pixel;
            }
        }

        // User
        // Check if the sender is you.
        // (This means it is the first message of any possible cluster)
        else if(userCurrent) {
            // Hide and show respective layouts.
            userLayout.setVisibility(View.VISIBLE);
            recipientLayout.setVisibility(View.GONE);

            // Set the message bubble background.
            userLayout.getChildAt(0).setBackgroundResource(R.drawable.you);

            // Display the user icon.
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon);
            icon.setVisibility(View.VISIBLE);
            ImageView user = (ImageView) view.findViewById(R.id.user);
            user.setColorFilter(0xff757575);

            // Set the message body.
            TextView message = (TextView) view.findViewById(R.id.message);
            message.setText(mCursor.getBody());

            // Set the date.
            TextView date = (TextView) view.findViewById(R.id.date);
            date.setVisibility(View.VISIBLE);
            date.setText(mCursor.getFormattedDate());

            // Update bottom margins to regular size because a cluster may never form.
            int pixel =  (int)(4 * scale + 0.5f);
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
            pixel =  (int)(16 * scale + 0.5f);
            ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = pixel;

            // Check if time gap between the next and current message is less than 1 minute
            // and that the sender is still you. (This means a newer message exists and a cluster has formed)
            if(splitNC<=60000 && userNext){
                // Hide the date because it is not the most recent in the cluster.
                date.setVisibility(View.GONE);

                // Update margins so bubbles in cluster are closer together.
                pixel =  (int)(12 * scale + 0.5f);
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
                pixel =  (int)(2.5 * scale + 0.5f);
                ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = pixel;
            }
        }

        // Recipient
        // Check if the sender is not you.
        // (This means it is the first message of any possible cluster)
        else {
            // Hide and show respective layouts.
            recipientLayout.setVisibility(View.VISIBLE);
            userLayout.setVisibility(View.GONE);

            // Set the message bubble background and color it.
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon2);
            icon.setVisibility(View.VISIBLE);
            recipientLayout.getChildAt(0).setBackgroundResource(R.drawable.other);
            recipientLayout.getChildAt(0).getBackground().setColorFilter(ColorUtils.getColor(mCursor.getThreadId()), PorterDuff.Mode.SRC_IN);

            // Display the user icon.
            ImageView user = (ImageView) view.findViewById(R.id.user2);
            user.setColorFilter(ColorUtils.getColor(mCursor.getThreadId()));

            // Set the message body.
            TextView message = (TextView) view.findViewById(R.id.message2);
            message.setText(mCursor.getBody());

            // Set the date.
            TextView date = (TextView) view.findViewById(R.id.date2);
            date.setVisibility(View.VISIBLE);
            date.setText(mCursor.getFormattedDate());

            // Update bottom margins to regular size because a cluster may never form.
            int pixel =  (int)(4 * scale + 0.5f);
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
            pixel =  (int)(16 * scale + 0.5f);
            ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = pixel;

            // Check if time gap between the next and current message is less than 1 minute
            // and that the sender is still not you. (This means a newer message exists and a cluster has formed)
            if(splitNC<=60000 && !userNext){
                // Hide the date because it is not the most recent in the cluster.
                date.setVisibility(View.GONE);

                // Update margins so bubbles in cluster are closer together.
                pixel =  (int)(12 * scale + 0.5f);
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = pixel;
                pixel =  (int)(2.5 * scale + 0.5f);
                ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = pixel;
            }
        }

        // Get rid of any bottom margins if its the newest message.
        if(mCursor.isLast()) {
            ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = 0;
            ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = 0;
        }
    }
}
