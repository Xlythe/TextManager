package com.xlythe.textmanager.text;

/**
 * Created by Niko on 12/30/15.
 */
public class Mock {
    public static class Telephony {
        public static class Sms {
            public static final String DATE;
            public static final String DATE_SENT;
            public static final String ADDRESS;
            public static final String BODY;
            public static final String TYPE;
            public static final String DEFAULT_SORT_ORDER;

            static {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    DATE = android.provider.Telephony.Sms.DATE;
                    DATE_SENT = android.provider.Telephony.Sms.DATE_SENT;
                    ADDRESS = android.provider.Telephony.Sms.ADDRESS;
                    BODY = android.provider.Telephony.Sms.BODY;
                    TYPE = android.provider.Telephony.Sms.TYPE;
                    DEFAULT_SORT_ORDER = android.provider.Telephony.Sms.DEFAULT_SORT_ORDER;
                } else {
                    DATE = "date";
                    DATE_SENT = "date_sent";
                    ADDRESS = "address";
                    BODY = "body";
                    TYPE = "type";
                    DEFAULT_SORT_ORDER = "date DESC";
                }
            }

            public static class Sent {
                public static final String CONTENT_URI;
                public static final String STATUS;
                public static final int STATUS_COMPLETE;
                public static final int STATUS_FAILED;
                public static final int STATUS_PENDING;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = String.valueOf(android.provider.Telephony.Sms.Sent.CONTENT_URI);
                        STATUS = android.provider.Telephony.Sms.Sent.STATUS;
                        STATUS_COMPLETE = android.provider.Telephony.Sms.Sent.STATUS_COMPLETE;
                        STATUS_FAILED = android.provider.Telephony.Sms.Sent.STATUS_FAILED;
                        STATUS_PENDING = android.provider.Telephony.Sms.Sent.STATUS_PENDING;
                    } else {
                        CONTENT_URI = "content://sms/sent";
                        STATUS = "status";
                        STATUS_COMPLETE = 0;
                        STATUS_FAILED = 64;
                        STATUS_PENDING = 32;
                    }
                }

            }

            public static class Conversations {
                public static final String THREAD_ID;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        THREAD_ID = android.provider.Telephony.Sms.Conversations.THREAD_ID;
                    } else {
                        THREAD_ID = "thread_id";
                    }
                }
            }
        }

        public static class Mms {
            public static final String SUBJECT;
            public static final String MESSAGE_BOX;

            static {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    SUBJECT = android.provider.Telephony.Mms.SUBJECT;
                    MESSAGE_BOX = android.provider.Telephony.Mms.MESSAGE_BOX;
                } else {
                    SUBJECT = "subject";
                    MESSAGE_BOX = "message_box";
                }
            }

            public static class Part {
                public static final String CONTENT_TYPE;
                public static final String TEXT;
                public static final String _DATA;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_TYPE = android.provider.Telephony.Mms.Part.CONTENT_TYPE;
                        TEXT = android.provider.Telephony.Mms.Part.TEXT;
                        _DATA = android.provider.Telephony.Mms.Part._DATA;
                    } else {
                        CONTENT_TYPE = "ct";
                        TEXT = "text";
                        _DATA = "_data";
                    }
                }
            }
        }

        public static class MmsSms {
            public static final String TYPE_DISCRIMINATOR_COLUMN;
            public static final String CONTENT_CONVERSATIONS_URI;

            static {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    TYPE_DISCRIMINATOR_COLUMN = android.provider.Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN;
                    CONTENT_CONVERSATIONS_URI = String.valueOf(android.provider.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI);
                } else {
                    TYPE_DISCRIMINATOR_COLUMN = "type_discriminator_column";
                    CONTENT_CONVERSATIONS_URI = "content://mms-sms/conversations/";
                }
            }
        }
    }
}
