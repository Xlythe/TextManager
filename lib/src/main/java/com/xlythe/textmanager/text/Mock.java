package com.xlythe.textmanager.text;

import android.provider.BaseColumns;

/**
 * Created by Niko on 12/30/15.
 */
public class Mock {
    public static class Telephony{
        public static class MmsSms{
            public static String TYPE_DISCRIMINATOR_COLUMN;
            public static String _ID;
            public static String THREAD_ID;
            public static String DATE;
            public static String DATE_SENT;
            public static String ADDRESS;
            public static String BODY;
            public static String TYPE;
            public static String SUBJECT;
            public static String MESSAGE_BOX;
            public static String CONTENT_CONVERSATIONS_URI;
            public static String DEFAULT_SORT_ORDER;
            public static String CONTENT_URI;
            public static String STATUS;
            public static int STATUS_COMPLETE;
            public static int STATUS_FAILED;
            public static int STATUS_PENDING;
            static {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    TYPE_DISCRIMINATOR_COLUMN = android.provider.Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN;
                    _ID = BaseColumns._ID;
                    THREAD_ID = android.provider.Telephony.Sms.Conversations.THREAD_ID;
                    DATE = android.provider.Telephony.Sms.DATE;
                    DATE_SENT = android.provider.Telephony.Sms.DATE_SENT;
                    ADDRESS = android.provider.Telephony.Sms.ADDRESS;
                    BODY = android.provider.Telephony.Sms.BODY;
                    TYPE = android.provider.Telephony.Sms.TYPE;
                    SUBJECT = android.provider.Telephony.Mms.SUBJECT;
                    MESSAGE_BOX = android.provider.Telephony.Mms.MESSAGE_BOX;
                    CONTENT_CONVERSATIONS_URI = String.valueOf(android.provider.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI);
                    DEFAULT_SORT_ORDER = android.provider.Telephony.Sms.DEFAULT_SORT_ORDER;
                    CONTENT_URI = String.valueOf(android.provider.Telephony.Sms.Sent.CONTENT_URI);
                    STATUS = android.provider.Telephony.Sms.Sent.STATUS;
                    STATUS_COMPLETE = android.provider.Telephony.Sms.Sent.STATUS_COMPLETE;
                    STATUS_FAILED = android.provider.Telephony.Sms.Sent.STATUS_FAILED;
                    STATUS_PENDING = android.provider.Telephony.Sms.Sent.STATUS_PENDING;
                } else {
                    TYPE_DISCRIMINATOR_COLUMN = "type_discriminator_column";
                    _ID = "_id";
                    THREAD_ID = "thread_id";
                    DATE = "date";
                    DATE_SENT = "date_sent";
                    ADDRESS = "address";
                    BODY = "body";
                    TYPE = "type";
                    SUBJECT = "subject";
                    MESSAGE_BOX = "message_box";
                    CONTENT_CONVERSATIONS_URI = "content://mms-sms/conversations/";
                    DEFAULT_SORT_ORDER = "date DESC";
                    CONTENT_URI = "content://sms/sent";
                    STATUS = "status";
                    STATUS_COMPLETE = 0;
                    STATUS_FAILED = 64;
                    STATUS_PENDING = 32;
                }
            }
        }
    }
}
