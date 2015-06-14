package com.xlythe.sms;

import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.Thread;

import java.text.SimpleDateFormat;

public class DateFormatter {
    private static final long ONE_MINUTE = 60 * 1000;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    private static final long ONE_DAY = 24 * ONE_HOUR;
    private static final long ONE_WEEK = 7 * ONE_DAY;
    private static final long ONE_MONTH = 4 * ONE_WEEK;

    public static String getFormattedDate(Thread thread) {
        long date = thread.getDate();
        long time = System.currentTimeMillis() - date;
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");

        if (time < ONE_MINUTE){
            // Just now
            return "Just now";
        } else if (time < ONE_HOUR){
            // 1 min, 2 mins
            long minutes = time / ONE_MINUTE;
            if (minutes == 1)
                return minutes + " min";
            else
                return minutes + " mins";
        } else if (time < ONE_DAY) {
            // 1 hour
            long hours = time / ONE_HOUR;
            if (hours == 1)
                return hours + " hour";
            else
                return hours + " hours";
        } else if (f.format(date).equals(f.format(System.currentTimeMillis()))) {
            // 3:09 PM
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
            return formatter.format(date);
        } else if (time < ONE_WEEK) {
            // Mon
            SimpleDateFormat formatter = new SimpleDateFormat("EEE");
            return formatter.format(date);
        } else if (time < ONE_MONTH) {
            // Apr 15
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d");
            return formatter.format(date);
        } else {
            // 4/15/14
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
            return formatter.format(date);
        }
    }

    public static String getFormattedDate(Text text) {
        long date = text.getDate();
        long time = System.currentTimeMillis() - date;
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");

        if (time < ONE_MINUTE) {
            // Now
            return "Now";
        } else if (time < ONE_HOUR) {
            // 1 min, 2 mins
            long minutes = time / ONE_MINUTE;
            if (minutes == 1)
                return minutes + " min";
            else
                return minutes + " mins";
        } else if (time < ONE_DAY) {
            // 1 hour
            long hours = time / ONE_HOUR;
            if (hours == 1)
                return hours + " hour";
            else
                return hours + " hours";
        } else if (f.format(date).equals(f.format(System.currentTimeMillis()))) {
            // 3:09 PM
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
            return formatter.format(date);
        } else if (time < ONE_WEEK) {
            //Mon 3:09PM
            SimpleDateFormat formatter = new SimpleDateFormat("EEE h:mma");
            return formatter.format(date);
        } else if (time < ONE_MONTH) {
            // Apr 15, 3:09PM
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, h:mma");
            return formatter.format(date);
        } else {
            // 4/15/14 3:09PM
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy h:mma");
            return formatter.format(date);
        }
    }
}
