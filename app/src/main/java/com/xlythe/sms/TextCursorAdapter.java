package com.xlythe.sms;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;

/**
 * Created by Niko on 8/16/15.
 */
public class TextCursorAdapter extends CursorAdapter {

    // Duration between considering a text to be part of the same message, or split into different messages
    private static final long SPLIT_DURATION = 60 * 1000;

    public TextCursorAdapter(Context context, Cursor c) {
        super(context, c);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // TODO Auto-generated method stub
        TelephonyManager tmanager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceNumber = tmanager.getLine1Number();
        Text text = new Text(context,cursor, deviceNumber);

        // Here's where things get a little messy. There are four different layouts.
        // User: The messages that you sent.
        // User Recent: Messages that you sent in quick succession (appear close together, date displays once).
        // Recipient: The messages that you received.
        // Recipient Recent: Messages that you received in quick succession (appear close together, date displays once).

        // Get name and photo from contacts.
        TextManager manager = TextManager.getInstance(context);
        Contact sender = manager.getSender(text);
        String name = sender.getDisplayName();
        Uri photo = sender.getPhotoUri();

        // Get both base layouts (User and Recipient)
        RelativeLayout userLayout = (RelativeLayout) view.findViewById(R.id.you);
        RelativeLayout recipientLayout = (RelativeLayout) view.findViewById(R.id.them);

        // * Previous meaning older message
        // * Next meaning most recently received

        // Get the date of the current, previous and next message.
        long dateCurrent = text.getDate();
        long datePrevious = 0;
        long dateNext = 0;

        // Get the sender of the current, previous and next message. (returns true if you)
        boolean userCurrent = !text.getIncoming();
        boolean userPrevious = !text.getIncoming();
        boolean userNext = text.getIncoming();

        // Time gap between the current and previous, and the next and current messages.
        long splitCP;
        long splitNC;

        // Check if previous message exists, then get the date and sender.
        if(!cursor.isFirst()) {
            cursor.moveToPrevious();
            Text nextText = new Text(context,cursor, deviceNumber);
            cursor.moveToNext();
            datePrevious = nextText.getDate();
            userPrevious = !nextText.getIncoming();
        }

        // Check if next message exists, then get the date and sender.
        if(!cursor.isLast()) {
            cursor.moveToNext();
            Text nextText = new Text(context,cursor, deviceNumber);
            cursor.moveToPrevious();
            dateNext = nextText.getDate();
            userNext = !nextText.getIncoming();
        }

        // Calculate time gap.
        splitCP = dateCurrent-datePrevious;
        splitNC = dateNext-dateCurrent;

        // User Recent
        // Check if time gap between the current and previous message is less than 1 minute
        // they are both the same sender, and if the sender is you.
        // (This means an older message exists in the cluster)
        if(splitCP<=SPLIT_DURATION && userCurrent==userPrevious && userCurrent){
            // Hide and show respective layouts.
            userLayout.setVisibility(View.VISIBLE);
            recipientLayout.setVisibility(View.GONE);

            // Set the message bubble background.
            userLayout.getChildAt(0).setBackgroundResource(R.drawable.recent_you);

            // Hide the user icon because there is already one showing on your first message in the cluster.
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon);
            icon.setVisibility(View.INVISIBLE);

            // Set the mms data
            ImageView pic = (ImageView) view.findViewById(R.id.pic);
            pic.setImageURI(text.getAttachment());

            // Set the message body.
            TextView message = (TextView) view.findViewById(R.id.message);
            message.setText(text.getBody());

            // Set the date and show it because this might be the last message in the cluster.
            TextView date = (TextView) view.findViewById(R.id.date);
            date.setVisibility(View.VISIBLE);
            date.setText(DateFormatter.getFormattedDate(text));

            // Update bottom margins to regular size because this might be the last message in the cluster.
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.message_margin_bottom);
            ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.bubble_margin_bottom);

            // Check if time gap between the next and current message is less than 1 minute
            // and that the sender is still you. (This means a newer message exists in the cluster)
            if(splitNC<=SPLIT_DURATION && userNext){
                // Hide the date because it is not the most recent in the cluster.
                date.setVisibility(View.GONE);

                // Update margins so bubbles in cluster are closer together.
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.message_margin_bottom_compact);
                ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.bubble_margin_bottom_compact);
            }
        }

        // Recipient Recent
        // Check if time gap between the current and previous message is less than 1 minute
        // they are both the same sender, and if the sender is not you.
        // (This means an older message exists in the cluster)
        else if(splitCP<=SPLIT_DURATION && userCurrent==userPrevious && !userCurrent){
            // Hide and show respective layouts.
            recipientLayout.setVisibility(View.VISIBLE);
            userLayout.setVisibility(View.GONE);

            // Set the message bubble background and color it.
            recipientLayout.getChildAt(0).setBackgroundResource(R.drawable.recent_other);
            recipientLayout.getChildAt(0).getBackground().setColorFilter(ColorUtils.getColor(text.getThreadId()), PorterDuff.Mode.SRC_IN);

            // Hide the user icon because there is already one showing on your first message in the cluster.
            RelativeLayout icon = (RelativeLayout) view.findViewById(R.id.icon2);
            icon.setVisibility(View.INVISIBLE);

            // Set the mms data
            ImageView pic = (ImageView) view.findViewById(R.id.pic2);
            pic.setImageURI(text.getAttachment());

            // Set the message body.
            TextView message = (TextView) view.findViewById(R.id.message2);
            message.setText(text.getBody());

            // Set the date and show it because this might be the last message in the cluster.
            TextView date = (TextView) view.findViewById(R.id.date2);
            date.setVisibility(View.VISIBLE);
            date.setText(DateFormatter.getFormattedDate(text));

            // Update bottom margins to regular size because this might be the last message in the cluster.
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.message_margin_bottom);
            ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.bubble_margin_bottom);

            // Check if time gap between the next and current message is less than 1 minute
            // and that the sender is still not you. (This means a newer message exists in the cluster)
            if(splitNC<=SPLIT_DURATION && !userNext){
                // Hide the date because it is not the most recent in the cluster.
                date.setVisibility(View.GONE);

                // Update margins so bubbles in cluster are closer together.
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.message_margin_bottom_compact);
                ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.bubble_margin_bottom_compact);
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
            ImageView user = (ImageView) view.findViewById(R.id.user_background);
            TextView userIcon = (TextView) view.findViewById(R.id.user_overlay);
            user.setColorFilter(0xff757575);

            // Set the mms data
            ImageView pic = (ImageView) view.findViewById(R.id.pic);
            pic.setImageURI(text.getAttachment());

            // Set the message body.
            TextView message = (TextView) view.findViewById(R.id.message);
            message.setText(text.getBody());

            // Set the date.
            TextView date = (TextView) view.findViewById(R.id.date);
            date.setVisibility(View.VISIBLE);
            date.setText(DateFormatter.getFormattedDate(text));

            // Update bottom margins to regular size because a cluster may never form.
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.message_margin_bottom);
            ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.bubble_margin_bottom);

            // Check if time gap between the next and current message is less than 1 minute
            // and that the sender is still you. (This means a newer message exists and a cluster has formed)
            if(splitNC<=SPLIT_DURATION && userNext){
                // Hide the date because it is not the most recent in the cluster.
                date.setVisibility(View.GONE);

                // Update margins so bubbles in cluster are closer together.
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.message_margin_bottom_compact);
                ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.bubble_margin_bottom_compact);
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
            recipientLayout.getChildAt(0).getBackground().setColorFilter(ColorUtils.getColor(text.getThreadId()), PorterDuff.Mode.SRC_IN);

            // Display the user icon.
            ImageView userImage = (ImageView) view.findViewById(R.id.user_background2);
            TextView textLetter = (TextView) view.findViewById(R.id.user_overlay2);
            if(photo!=null){
                userImage.setImageURI(photo);
                textLetter.setBackground(null);
            }
            else {
                userImage.setColorFilter(ColorUtils.getColor(text.getThreadId()));
                textLetter.setText("");
                if (sender.hasName()){
                    textLetter.setText(Character.toString(name.charAt(0)));
                    textLetter.setBackground(null);
                }
            }

            // Set the mms data
            ImageView pic = (ImageView) view.findViewById(R.id.pic2);
            pic.setImageURI(text.getAttachment());

            // Set the message body.
            TextView message = (TextView) view.findViewById(R.id.message2);
            message.setText(text.getBody());

            // Set the date.
            TextView date = (TextView) view.findViewById(R.id.date2);
            date.setVisibility(View.VISIBLE);
            date.setText(DateFormatter.getFormattedDate(text));

            // Update bottom margins to regular size because a cluster may never form.
            ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.message_margin_bottom);
            ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.bubble_margin_bottom);

            // Check if time gap between the next and current message is less than 1 minute
            // and that the sender is still not you. (This means a newer message exists and a cluster has formed)
            if(splitNC<=SPLIT_DURATION && !userNext){
                // Hide the date because it is not the most recent in the cluster.
                date.setVisibility(View.GONE);

                // Update margins so bubbles in cluster are closer together.
                ((LinearLayout.LayoutParams)message.getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.message_margin_bottom_compact);
                ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.bubble_margin_bottom_compact);
            }
        }

        // Get rid of any bottom margins if its the newest message.
        if(cursor.isLast()) {
            ((RelativeLayout.LayoutParams)recipientLayout.getChildAt(0).getLayoutParams()).bottomMargin = 0;
            ((RelativeLayout.LayoutParams)userLayout.getChildAt(0).getLayoutParams()).bottomMargin = 0;
        }

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_texts, parent, false);
    }
}