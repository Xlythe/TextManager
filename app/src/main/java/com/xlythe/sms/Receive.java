package com.xlythe.sms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Niko on 5/19/15.
 */
public class Receive {

    // Email Address Pattern.
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-]{1,256}" + "\\@" + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" + "\\." + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+"
    );

    // Name Address Email Pattern.
    private static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

    /**
     * Store message in the content provider.
     * @param context Context
     * @param msgs Array of SMS Messages
     * @param error Error code
     */
    public static void storeMessage(Context context, SmsMessage[] msgs, int error) {
        SmsMessage sms = msgs[0];

        // Add everything but the message body
        ContentValues values = extractContentValues(sms);
        values.put(Telephony.Sms.ERROR_CODE, error);

        // Add the message body
        StringBuilder body = new StringBuilder();
        for(int i = 0; i < msgs.length; i++) {
            sms = msgs[i];
            if(sms.getDisplayMessageBody() != null) {
                body.append(sms.getDisplayMessageBody());
            }
        }
        values.put(Telephony.Sms.Inbox.BODY, replaceFormFeeds(body.toString()));

        // Make sure there is a thread id.
        Long threadId = values.getAsLong(Telephony.Sms.THREAD_ID);
        String address = values.getAsString(Telephony.Sms.ADDRESS);

        // If it doesn't exist, create a thread id.
        if(((threadId == null) || (threadId == 0)) && (address != null)) {
            threadId = getOrCreateThreadId(context, address);
            values.put(Telephony.Sms.THREAD_ID, threadId);
        }

        // Add to content provider
        context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
    }

    /**
     * Some providers send formfeeds in their messages. Convert those formfeeds to newlines.
     * @param s String of the message body
     * @return The message with newlines instead of formfeeds
     */
    private static String replaceFormFeeds(String s) {
        return s == null ? "" : s.replace('\f', '\n');
    }

    /**
     * Adds values except for message body.
     * @param sms SMS message
     * @return Content values
     */
    private static ContentValues extractContentValues(SmsMessage sms) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.Inbox.ADDRESS, sms.getDisplayOriginatingAddress());
        values.put(Telephony.Sms.Inbox.DATE, checkDate(sms));
        values.put(Telephony.Sms.Inbox.DATE_SENT, sms.getTimestampMillis());
        values.put(Telephony.Sms.Inbox.PROTOCOL, sms.getProtocolIdentifier());
        values.put(Telephony.Sms.Inbox.READ, 0);
        values.put(Telephony.Sms.Inbox.SEEN, 0);
        if(sms.getPseudoSubject().length() > 0) {
            values.put(Telephony.Sms.Inbox.SUBJECT, sms.getPseudoSubject());
        }
        values.put(Telephony.Sms.Inbox.REPLY_PATH_PRESENT, sms.isReplyPathPresent() ? 1 : 0);
        values.put(Telephony.Sms.Inbox.SERVICE_CENTER, sms.getServiceCenterAddress());
        return values;
    }

    /**
     * Check to make sure the date isn't fake
     * @param sms SMS message
     * @return date
     */
    private static long checkDate(SmsMessage sms){
        Calendar buildDate = new GregorianCalendar(2011, 8, 18); // 18 Sep 2011
        Calendar nowDate = new GregorianCalendar();
        long now = System.currentTimeMillis();
        nowDate.setTimeInMillis(now);
        if(nowDate.before(buildDate)) {
            now = sms.getTimestampMillis();
        }
        return now;
    }

    /**
     * Create thread id.
     * @param context Context
     * @param recip Recipient
     * @return Thread id
     */
    public static long getOrCreateThreadId(Context context, String recip) {
        Set<String> recipients = new HashSet<>();
        recipients.add(recip);
        Uri.Builder uriBuilder = Uri.parse("content://mms-sms/threadID").buildUpon();
        for (String recipient : recipients) {
            if (isEmailAddress(recipient)) {
                recipient = extractAddrSpec(recipient);
            }
            uriBuilder.appendQueryParameter("recipient", recipient);
        }

        Uri uri = uriBuilder.build();
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"_id"}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                } else {

                }
            } finally {
                cursor.close();
            }
        }

        Random random = new Random();
        return random.nextLong();
    }

    /**
     * Check if it is an email address
     * @param address Address
     * @return Boolean
     */
    private static boolean isEmailAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }
        String s = extractAddrSpec(address);
        Matcher match = EMAIL_ADDRESS_PATTERN.matcher(s);
        return match.matches();
    }

    /**
     * Get the name of the address
     * @param address Address
     * @return String
     */
    private static String extractAddrSpec(String address) {
        Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

        if (match.matches()) {
            return match.group(2);
        }
        return address;
    }
}