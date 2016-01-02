package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Patterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fakes the Telephony constants
 */
public class Mock {
    private Mock() {}

    public static final class Telephony {
        private Telephony() {}

        public static final class Sms extends TextBasedSmsColumns implements BaseColumns {
            private Sms() {}

            public static Cursor query(ContentResolver cr, String[] projection) {
                return cr.query(CONTENT_URI, projection, null, null, DEFAULT_SORT_ORDER);
            }

            public static Cursor query(ContentResolver cr, String[] projection, String where, String orderBy) {
                return cr.query(CONTENT_URI, projection, where, null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
            }

            public static final Uri CONTENT_URI;
            public static final String DEFAULT_SORT_ORDER;

            static {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    CONTENT_URI = android.provider.Telephony.Sms.CONTENT_URI;
                    DEFAULT_SORT_ORDER = android.provider.Telephony.Sms.DEFAULT_SORT_ORDER;
                } else {
                    CONTENT_URI = Uri.parse("content://sms");
                    DEFAULT_SORT_ORDER = "date DESC";
                }
            }

            public static boolean isOutgoingFolder(int messageType) {
                return  (messageType == MESSAGE_TYPE_FAILED)
                        || (messageType == MESSAGE_TYPE_OUTBOX)
                        || (messageType == MESSAGE_TYPE_SENT)
                        || (messageType == MESSAGE_TYPE_QUEUED);
            }

            public static final class Inbox extends TextBasedSmsColumns implements BaseColumns {
                private Inbox() {}

                public static final Uri CONTENT_URI;
                public static final String DEFAULT_SORT_ORDER;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Sms.Inbox.CONTENT_URI;
                        DEFAULT_SORT_ORDER = android.provider.Telephony.Sms.Inbox.DEFAULT_SORT_ORDER;
                    } else {
                        CONTENT_URI = Uri.parse("content://sms/inbox");
                        DEFAULT_SORT_ORDER = "date DESC";
                    }
                }
            }

            public static final class Sent extends TextBasedSmsColumns implements BaseColumns {
                private Sent() {}

                public static final Uri CONTENT_URI;
                public static final String DEFAULT_SORT_ORDER;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Sms.Sent.CONTENT_URI;
                        DEFAULT_SORT_ORDER = android.provider.Telephony.Sms.Sent.DEFAULT_SORT_ORDER;
                    } else {
                        CONTENT_URI = Uri.parse("content://sms/sent");
                        DEFAULT_SORT_ORDER = "date DESC";
                    }
                }
            }

            public static final class Draft extends TextBasedSmsColumns implements BaseColumns {
                private Draft() {}

                public static final Uri CONTENT_URI;
                public static final String DEFAULT_SORT_ORDER;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Sms.Draft.CONTENT_URI;
                        DEFAULT_SORT_ORDER = android.provider.Telephony.Sms.Draft.DEFAULT_SORT_ORDER;
                    } else {
                        CONTENT_URI = Uri.parse("content://sms/draft");
                        DEFAULT_SORT_ORDER = "date DESC";
                    }
                }
            }

            public static final class Outbox extends TextBasedSmsColumns implements BaseColumns {
                private Outbox() {}

                public static final Uri CONTENT_URI;
                public static final String DEFAULT_SORT_ORDER;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Sms.Outbox.CONTENT_URI;
                        DEFAULT_SORT_ORDER = android.provider.Telephony.Sms.Outbox.DEFAULT_SORT_ORDER;
                    } else {
                        CONTENT_URI = Uri.parse("content://sms/outbox");
                        DEFAULT_SORT_ORDER = "date DESC";
                    }
                }
            }

            public static final class Conversations extends TextBasedSmsColumns implements BaseColumns {
                private Conversations() {}

                public static final Uri CONTENT_URI;
                public static final String DEFAULT_SORT_ORDER;
                public static final String SNIPPET;
                public static final String MESSAGE_COUNT;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Sms.Conversations.CONTENT_URI;
                        DEFAULT_SORT_ORDER = android.provider.Telephony.Sms.Conversations.DEFAULT_SORT_ORDER;
                        SNIPPET = android.provider.Telephony.Sms.Conversations.SNIPPET;
                        MESSAGE_COUNT = android.provider.Telephony.Sms.Conversations.MESSAGE_COUNT;
                    } else {
                        CONTENT_URI = Uri.parse("content://sms/conversations");
                        DEFAULT_SORT_ORDER = "date DESC";
                        SNIPPET = "snippet";
                        MESSAGE_COUNT = "msg_count";
                    }
                }
            }

            public static final class Intents {
                private Intents() {}

                public static final int RESULT_SMS_HANDLED;
                public static final int RESULT_SMS_GENERIC_ERROR;
                public static final int RESULT_SMS_OUT_OF_MEMORY;
                public static final int RESULT_SMS_UNSUPPORTED;
                public static final int RESULT_SMS_DUPLICATED;

                public static final String ACTION_CHANGE_DEFAULT;
                public static final String EXTRA_PACKAGE_NAME;
                public static final String SMS_DELIVER_ACTION;
                public static final String SMS_RECEIVED_ACTION;
                public static final String DATA_SMS_RECEIVED_ACTION;
                public static final String WAP_PUSH_DELIVER_ACTION;
                public static final String WAP_PUSH_RECEIVED_ACTION;
                public static final String SMS_CB_RECEIVED_ACTION;
                public static final String SMS_EMERGENCY_CB_RECEIVED_ACTION;
                public static final String SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED_ACTION;
                public static final String SIM_FULL_ACTION;
                public static final String SMS_REJECTED_ACTION;
                public static final String MMS_DOWNLOADED_ACTION = "android.provider.Telephony.MMS_DOWNLOADED"; // hidden

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        RESULT_SMS_HANDLED = android.provider.Telephony.Sms.Intents.RESULT_SMS_HANDLED;
                        RESULT_SMS_GENERIC_ERROR = android.provider.Telephony.Sms.Intents.RESULT_SMS_GENERIC_ERROR;
                        RESULT_SMS_OUT_OF_MEMORY = android.provider.Telephony.Sms.Intents.RESULT_SMS_OUT_OF_MEMORY;
                        RESULT_SMS_UNSUPPORTED = android.provider.Telephony.Sms.Intents.RESULT_SMS_UNSUPPORTED;
                        RESULT_SMS_DUPLICATED = android.provider.Telephony.Sms.Intents.RESULT_SMS_DUPLICATED;

                        ACTION_CHANGE_DEFAULT = android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT;
                        EXTRA_PACKAGE_NAME = android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME;
                        SMS_DELIVER_ACTION = android.provider.Telephony.Sms.Intents.SMS_DELIVER_ACTION;
                        SMS_RECEIVED_ACTION = android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
                        DATA_SMS_RECEIVED_ACTION = android.provider.Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION;
                        WAP_PUSH_DELIVER_ACTION = android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;
                        WAP_PUSH_RECEIVED_ACTION = android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;
                        SMS_CB_RECEIVED_ACTION = android.provider.Telephony.Sms.Intents.SMS_CB_RECEIVED_ACTION;
                        SMS_EMERGENCY_CB_RECEIVED_ACTION = android.provider.Telephony.Sms.Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION;
                        SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED_ACTION = android.provider.Telephony.Sms.Intents.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED_ACTION;
                        SIM_FULL_ACTION = android.provider.Telephony.Sms.Intents.SIM_FULL_ACTION;
                        SMS_REJECTED_ACTION = android.provider.Telephony.Sms.Intents.SMS_REJECTED_ACTION;
                    } else {
                        RESULT_SMS_HANDLED = 1;
                        RESULT_SMS_GENERIC_ERROR = 2;
                        RESULT_SMS_OUT_OF_MEMORY = 3;
                        RESULT_SMS_UNSUPPORTED = 4;
                        RESULT_SMS_DUPLICATED = 5;

                        ACTION_CHANGE_DEFAULT = "android.provider.Telephony.ACTION_CHANGE_DEFAULT";
                        EXTRA_PACKAGE_NAME = "package";
                        SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER";
                        SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
                        DATA_SMS_RECEIVED_ACTION = "android.intent.action.DATA_SMS_RECEIVED";
                        WAP_PUSH_DELIVER_ACTION = "android.provider.Telephony.WAP_PUSH_DELIVER";
                        WAP_PUSH_RECEIVED_ACTION = "android.provider.Telephony.WAP_PUSH_RECEIVED";
                        SMS_CB_RECEIVED_ACTION = "android.provider.Telephony.SMS_CB_RECEIVED";
                        SMS_EMERGENCY_CB_RECEIVED_ACTION = "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED";
                        SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED_ACTION = "android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED";
                        SIM_FULL_ACTION = "android.provider.Telephony.SIM_FULL";
                        SMS_REJECTED_ACTION = "android.provider.Telephony.SMS_REJECTED";
                    }
                }
            }
        }

        public static class BaseMmsColumns implements BaseColumns {
            private BaseMmsColumns() {}

            public static final int MESSAGE_BOX_ALL;
            public static final int MESSAGE_BOX_INBOX;
            public static final int MESSAGE_BOX_SENT;
            public static final int MESSAGE_BOX_DRAFTS;
            public static final int MESSAGE_BOX_OUTBOX;
            public static final int MESSAGE_BOX_FAILED;

            public static final String THREAD_ID;
            public static final String DATE;
            public static final String DATE_SENT;
            public static final String MESSAGE_BOX;
            public static final String READ;
            public static final String SEEN;
            public static final String TEXT_ONLY;
            public static final String MESSAGE_ID;
            public static final String SUBJECT;
            public static final String SUBJECT_CHARSET;
            public static final String CONTENT_TYPE;
            public static final String CONTENT_LOCATION;
            public static final String EXPIRY;
            public static final String MESSAGE_CLASS;
            public static final String MESSAGE_TYPE;
            public static final String MMS_VERSION;
            public static final String MESSAGE_SIZE;
            public static final String PRIORITY;
            public static final String READ_REPORT;
            public static final String REPORT_ALLOWED;
            public static final String RESPONSE_STATUS;
            public static final String STATUS;
            public static final String TRANSACTION_ID;
            public static final String RETRIEVE_STATUS;
            public static final String RETRIEVE_TEXT;
            public static final String RETRIEVE_TEXT_CHARSET;
            public static final String READ_STATUS;
            public static final String CONTENT_CLASS;
            public static final String DELIVERY_REPORT;
            @Deprecated
            public static final String DELIVERY_TIME_TOKEN = "d_tm_tok";
            public static final String DELIVERY_TIME;
            public static final String RESPONSE_TEXT;
            @Deprecated
            public static final String SENDER_VISIBILITY = "s_vis";
            @Deprecated
            public static final String REPLY_CHARGING = "r_chg";
            @Deprecated
            public static final String REPLY_CHARGING_DEADLINE_TOKEN = "r_chg_dl_tok";
            @Deprecated
            public static final String REPLY_CHARGING_DEADLINE = "r_chg_dl";
            @Deprecated
            public static final String REPLY_CHARGING_ID = "r_chg_id";
            @Deprecated
            public static final String REPLY_CHARGING_SIZE = "r_chg_sz";
            @Deprecated
            public static final String PREVIOUSLY_SENT_BY = "p_s_by";
            @Deprecated
            public static final String PREVIOUSLY_SENT_DATE = "p_s_d";
            @Deprecated
            public static final String STORE = "store";
            @Deprecated
            public static final String MM_STATE = "mm_st";
            @Deprecated
            public static final String MM_FLAGS_TOKEN = "mm_flg_tok";
            @Deprecated
            public static final String MM_FLAGS = "mm_flg";
            @Deprecated
            public static final String STORE_STATUS = "store_st";
            @Deprecated
            public static final String STORE_STATUS_TEXT = "store_st_txt";
            @Deprecated
            public static final String STORED = "stored";
            @Deprecated
            public static final String TOTALS = "totals";
            @Deprecated
            public static final String MBOX_TOTALS = "mb_t";
            @Deprecated
            public static final String MBOX_TOTALS_TOKEN = "mb_t_tok";
            @Deprecated
            public static final String QUOTAS = "qt";
            @Deprecated
            public static final String MBOX_QUOTAS = "mb_qt";
            @Deprecated
            public static final String MBOX_QUOTAS_TOKEN = "mb_qt_tok";
            @Deprecated
            public static final String MESSAGE_COUNT = "m_cnt";
            @Deprecated
            public static final String START = "start";
            @Deprecated
            public static final String DISTRIBUTION_INDICATOR = "d_ind";
            @Deprecated
            public static final String ELEMENT_DESCRIPTOR = "e_des";
            @Deprecated
            public static final String LIMIT = "limit";
            @Deprecated
            public static final String RECOMMENDED_RETRIEVAL_MODE = "r_r_mod";
            @Deprecated
            public static final String RECOMMENDED_RETRIEVAL_MODE_TEXT = "r_r_mod_txt";
            @Deprecated
            public static final String STATUS_TEXT = "st_txt";
            @Deprecated
            public static final String APPLIC_ID = "apl_id";
            @Deprecated
            public static final String REPLY_APPLIC_ID = "r_apl_id";
            @Deprecated
            public static final String AUX_APPLIC_ID = "aux_apl_id";
            @Deprecated
            public static final String DRM_CONTENT = "drm_c";
            @Deprecated
            public static final String ADAPTATION_ALLOWED = "adp_a";
            @Deprecated
            public static final String REPLACE_ID = "repl_id";
            @Deprecated
            public static final String CANCEL_ID = "cl_id";
            @Deprecated
            public static final String CANCEL_STATUS = "cl_st";
            public static final String LOCKED;
            public static final String SUBSCRIPTION_ID;
            public static final String CREATOR;

            static {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    MESSAGE_BOX_ALL    = android.provider.Telephony.BaseMmsColumns.MESSAGE_BOX_ALL;
                    MESSAGE_BOX_INBOX  = android.provider.Telephony.BaseMmsColumns.MESSAGE_BOX_INBOX;
                    MESSAGE_BOX_SENT   = android.provider.Telephony.BaseMmsColumns.MESSAGE_BOX_SENT;
                    MESSAGE_BOX_DRAFTS = android.provider.Telephony.BaseMmsColumns.MESSAGE_BOX_DRAFTS;
                    MESSAGE_BOX_OUTBOX = android.provider.Telephony.BaseMmsColumns.MESSAGE_BOX_OUTBOX;
                    MESSAGE_BOX_FAILED = android.provider.Telephony.BaseMmsColumns.MESSAGE_BOX_FAILED;

                    THREAD_ID = android.provider.Telephony.BaseMmsColumns.THREAD_ID;
                    DATE = android.provider.Telephony.BaseMmsColumns.DATE;
                    DATE_SENT = android.provider.Telephony.BaseMmsColumns.DATE_SENT;
                    MESSAGE_BOX = android.provider.Telephony.BaseMmsColumns.MESSAGE_BOX;
                    READ = android.provider.Telephony.BaseMmsColumns.READ;
                    SEEN = android.provider.Telephony.BaseMmsColumns.SEEN;
                    TEXT_ONLY = android.provider.Telephony.BaseMmsColumns.TEXT_ONLY;
                    MESSAGE_ID = android.provider.Telephony.BaseMmsColumns.MESSAGE_ID;
                    SUBJECT = android.provider.Telephony.BaseMmsColumns.SUBJECT;
                    SUBJECT_CHARSET = android.provider.Telephony.BaseMmsColumns.SUBJECT_CHARSET;
                    CONTENT_TYPE = android.provider.Telephony.BaseMmsColumns.CONTENT_TYPE;
                    CONTENT_LOCATION = android.provider.Telephony.BaseMmsColumns.CONTENT_LOCATION;
                    EXPIRY = android.provider.Telephony.BaseMmsColumns.EXPIRY;
                    MESSAGE_CLASS = android.provider.Telephony.BaseMmsColumns.MESSAGE_CLASS;
                    MESSAGE_TYPE = android.provider.Telephony.BaseMmsColumns.MESSAGE_TYPE;
                    MMS_VERSION = android.provider.Telephony.BaseMmsColumns.MMS_VERSION;
                    MESSAGE_SIZE = android.provider.Telephony.BaseMmsColumns.MESSAGE_SIZE;
                    PRIORITY = android.provider.Telephony.BaseMmsColumns.PRIORITY;
                    READ_REPORT = android.provider.Telephony.BaseMmsColumns.READ_REPORT;
                    REPORT_ALLOWED = android.provider.Telephony.BaseMmsColumns.REPORT_ALLOWED;
                    RESPONSE_STATUS = android.provider.Telephony.BaseMmsColumns.RESPONSE_STATUS;
                    STATUS = android.provider.Telephony.BaseMmsColumns.STATUS;
                    TRANSACTION_ID = android.provider.Telephony.BaseMmsColumns.TRANSACTION_ID;
                    RETRIEVE_STATUS = android.provider.Telephony.BaseMmsColumns.RETRIEVE_STATUS;
                    RETRIEVE_TEXT = android.provider.Telephony.BaseMmsColumns.RETRIEVE_TEXT;
                    RETRIEVE_TEXT_CHARSET = android.provider.Telephony.BaseMmsColumns.RETRIEVE_TEXT_CHARSET;
                    READ_STATUS = android.provider.Telephony.BaseMmsColumns.READ_STATUS;
                    CONTENT_CLASS = android.provider.Telephony.BaseMmsColumns.CONTENT_CLASS;
                    DELIVERY_REPORT = android.provider.Telephony.BaseMmsColumns.DELIVERY_REPORT;
                    DELIVERY_TIME = android.provider.Telephony.BaseMmsColumns.DELIVERY_TIME;
                    RESPONSE_TEXT = android.provider.Telephony.BaseMmsColumns.RESPONSE_TEXT;
                    LOCKED = android.provider.Telephony.BaseMmsColumns.LOCKED;
                    SUBSCRIPTION_ID = android.provider.Telephony.BaseMmsColumns.SUBSCRIPTION_ID;
                    CREATOR = android.provider.Telephony.BaseMmsColumns.CREATOR;
                } else {
                    MESSAGE_BOX_ALL    = 0;
                    MESSAGE_BOX_INBOX  = 1;
                    MESSAGE_BOX_SENT   = 2;
                    MESSAGE_BOX_DRAFTS = 3;
                    MESSAGE_BOX_OUTBOX = 4;
                    MESSAGE_BOX_FAILED = 5;

                    THREAD_ID = "thread_id";
                    DATE = "date";
                    DATE_SENT = "date_sent";
                    MESSAGE_BOX = "msg_box";
                    READ = "read";
                    SEEN = "seen";
                    TEXT_ONLY = "text_only";
                    MESSAGE_ID = "m_id";
                    SUBJECT = "sub";
                    SUBJECT_CHARSET = "sub_cs";
                    CONTENT_TYPE = "ct_t";
                    CONTENT_LOCATION = "ct_l";
                    EXPIRY = "exp";
                    MESSAGE_CLASS = "m_cls";
                    MESSAGE_TYPE = "m_type";
                    MMS_VERSION = "v";
                    MESSAGE_SIZE = "m_size";
                    PRIORITY = "pri";
                    READ_REPORT = "rr";
                    REPORT_ALLOWED = "rpt_a";
                    RESPONSE_STATUS = "resp_st";
                    STATUS = "st";
                    TRANSACTION_ID = "tr_id";
                    RETRIEVE_STATUS = "retr_st";
                    RETRIEVE_TEXT = "retr_txt";
                    RETRIEVE_TEXT_CHARSET = "retr_txt_cs";
                    READ_STATUS = "read_status";
                    CONTENT_CLASS = "ct_cls";
                    DELIVERY_REPORT = "d_rpt";
                    DELIVERY_TIME = "d_tm";
                    RESPONSE_TEXT = "resp_txt";
                    LOCKED = "locked";
                    SUBSCRIPTION_ID = "sub_id";
                    CREATOR = "creator";
                }
            }
        }

        public static final class Mms extends BaseMmsColumns {
            private Mms() {}

            public static final Uri CONTENT_URI;
            public static final Uri REPORT_REQUEST_URI;
            public static final Uri REPORT_STATUS_URI;
            public static final String DEFAULT_SORT_ORDER;
            public static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*"); // hidden

            public static Cursor query(ContentResolver cr, String[] projection) {
                return cr.query(CONTENT_URI, projection, null, null, DEFAULT_SORT_ORDER);
            }

            public static Cursor query(ContentResolver cr, String[] projection, String where, String orderBy) {
                return cr.query(CONTENT_URI, projection, where, null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
            }

            public static String extractAddrSpec(String address) {
                Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

                if (match.matches()) {
                    return match.group(2);
                }
                return address;
            }

            public static boolean isEmailAddress(String address) {
                if (TextUtils.isEmpty(address)) {
                    return false;
                }

                String s = extractAddrSpec(address);
                Matcher match = Patterns.EMAIL_ADDRESS.matcher(s);
                return match.matches();
            }

            public static boolean isPhoneNumber(String number) {
                if (TextUtils.isEmpty(number)) {
                    return false;
                }

                Matcher match = Patterns.PHONE.matcher(number);
                return match.matches();
            }

            static {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    CONTENT_URI = android.provider.Telephony.Mms.CONTENT_URI;
                    REPORT_REQUEST_URI = android.provider.Telephony.Mms.REPORT_REQUEST_URI;
                    REPORT_STATUS_URI = android.provider.Telephony.Mms.REPORT_STATUS_URI;
                    DEFAULT_SORT_ORDER = android.provider.Telephony.Mms.DEFAULT_SORT_ORDER;
                } else {
                    CONTENT_URI = Uri.parse("content://mms");
                    REPORT_REQUEST_URI = Uri.withAppendedPath(CONTENT_URI, "report-request");
                    REPORT_STATUS_URI = Uri.withAppendedPath(CONTENT_URI, "report-status");
                    DEFAULT_SORT_ORDER = "date DESC";
                }
            }

            public static final class Inbox extends BaseMmsColumns {
                private Inbox() {}

                public static final Uri CONTENT_URI;
                public static final String DEFAULT_SORT_ORDER;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Mms.Inbox.CONTENT_URI;
                        DEFAULT_SORT_ORDER = android.provider.Telephony.Mms.Inbox.DEFAULT_SORT_ORDER;
                    } else {
                        CONTENT_URI = Uri.parse("content://mms/inbox");
                        DEFAULT_SORT_ORDER = "date DESC";
                    }
                }
            }

            public static final class Sent extends BaseMmsColumns {
                private Sent() {}

                public static final Uri CONTENT_URI;
                public static final String DEFAULT_SORT_ORDER;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Mms.Sent.CONTENT_URI;
                        DEFAULT_SORT_ORDER = android.provider.Telephony.Mms.Sent.DEFAULT_SORT_ORDER;
                    } else {
                        CONTENT_URI = Uri.parse("content://mms/sent");
                        DEFAULT_SORT_ORDER = "date DESC";
                    }
                }
            }

            public static final class Draft extends BaseMmsColumns {
                private Draft() {}

                public static final Uri CONTENT_URI;
                public static final String DEFAULT_SORT_ORDER;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Mms.Draft.CONTENT_URI;
                        DEFAULT_SORT_ORDER = android.provider.Telephony.Mms.Draft.DEFAULT_SORT_ORDER;
                    } else {
                        CONTENT_URI = Uri.parse("content://mms/drafts");
                        DEFAULT_SORT_ORDER = "date DESC";
                    }
                }
            }

            public static final class Outbox extends BaseMmsColumns {
                private Outbox() {}

                public static final Uri CONTENT_URI;
                public static final String DEFAULT_SORT_ORDER;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Mms.Outbox.CONTENT_URI;
                        DEFAULT_SORT_ORDER = android.provider.Telephony.Mms.Outbox.DEFAULT_SORT_ORDER;
                    } else {
                        CONTENT_URI = Uri.parse("content://mms/outbox");
                        DEFAULT_SORT_ORDER = "date DESC";
                    }
                }
            }

            public static final class Addr implements BaseColumns {
                private Addr() {}

                public static final String MSG_ID;
                public static final String CONTACT_ID;
                public static final String ADDRESS;
                public static final String TYPE;
                public static final String CHARSET;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        MSG_ID = android.provider.Telephony.Mms.Addr.MSG_ID;
                        CONTACT_ID = android.provider.Telephony.Mms.Addr.CONTACT_ID;
                        ADDRESS = android.provider.Telephony.Mms.Addr.ADDRESS;
                        TYPE = android.provider.Telephony.Mms.Addr.TYPE;
                        CHARSET = android.provider.Telephony.Mms.Addr.CHARSET;
                    } else {
                        MSG_ID = "msg_id";
                        CONTACT_ID = "contact_id";
                        ADDRESS = "address";
                        TYPE = "type";
                        CHARSET = "charset";
                    }
                }
            }

            public static final class Part implements BaseColumns {
                private Part() {}

                public static final String MSG_ID;
                public static final String SEQ;
                public static final String CONTENT_TYPE;
                public static final String NAME;
                public static final String CHARSET;
                public static final String FILENAME;
                public static final String CONTENT_DISPOSITION;
                public static final String CONTENT_ID;
                public static final String CONTENT_LOCATION;
                public static final String CT_START;
                public static final String CT_TYPE;
                public static final String _DATA;
                public static final String TEXT;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        MSG_ID = android.provider.Telephony.Mms.Part.MSG_ID;
                        SEQ = android.provider.Telephony.Mms.Part.SEQ;
                        CONTENT_TYPE = android.provider.Telephony.Mms.Part.CONTENT_TYPE;
                        NAME = android.provider.Telephony.Mms.Part.NAME;
                        CHARSET = android.provider.Telephony.Mms.Part.CHARSET;
                        FILENAME = android.provider.Telephony.Mms.Part.FILENAME;
                        CONTENT_DISPOSITION = android.provider.Telephony.Mms.Part.CONTENT_DISPOSITION;
                        CONTENT_ID = android.provider.Telephony.Mms.Part.CONTENT_ID;
                        CONTENT_LOCATION = android.provider.Telephony.Mms.Part.CONTENT_LOCATION;
                        CT_START = android.provider.Telephony.Mms.Part.CT_START;
                        CT_TYPE = android.provider.Telephony.Mms.Part.CT_TYPE;
                        _DATA = android.provider.Telephony.Mms.Part._DATA;
                        TEXT = android.provider.Telephony.Mms.Part.TEXT;
                    } else {
                        MSG_ID = "mid";
                        SEQ = "seq";
                        CONTENT_TYPE = "ct";
                        NAME = "name";
                        CHARSET = "chset";
                        FILENAME = "fn";
                        CONTENT_DISPOSITION = "cd";
                        CONTENT_ID = "cid";
                        CONTENT_LOCATION = "cl";
                        CT_START = "ctt_s";
                        CT_TYPE = "ctt_t";
                        _DATA = "_data";
                        TEXT = "text";
                    }
                }
            }

            public static final class Rate {
                private Rate() {}

                public static final Uri CONTENT_URI;
                public static final String SENT_TIME;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.Mms.Rate.CONTENT_URI;
                        SENT_TIME = android.provider.Telephony.Mms.Rate.SENT_TIME;
                    } else {
                        CONTENT_URI = Uri.withAppendedPath(Mms.CONTENT_URI, "rate");
                        SENT_TIME = "sent_time";
                    }
                }
            }

            public static final class Intents {
                private Intents() {}

                public static final String CONTENT_CHANGED_ACTION;
                public static final String DELETED_CONTENTS;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_CHANGED_ACTION = android.provider.Telephony.Mms.Intents.CONTENT_CHANGED_ACTION;
                        DELETED_CONTENTS = android.provider.Telephony.Mms.Intents.DELETED_CONTENTS;
                    } else {
                        CONTENT_CHANGED_ACTION = "android.intent.action.CONTENT_CHANGED";
                        DELETED_CONTENTS = "deleted_contents";
                    }
                }
            }
        }

        public static final class MmsSms implements BaseColumns {
            private MmsSms() {}

            public static final String TYPE_DISCRIMINATOR_COLUMN;
            public static final Uri CONTENT_URI;
            public static final Uri CONTENT_CONVERSATIONS_URI;
            public static final Uri CONTENT_FILTER_BYPHONE_URI;
            public static final Uri CONTENT_UNDELIVERED_URI;
            public static final Uri CONTENT_DRAFT_URI;
            public static final Uri CONTENT_LOCKED_URI;
            public static final Uri SEARCH_URI;

            public static final int SMS_PROTO;
            public static final int MMS_PROTO;

            public static final int NO_ERROR;
            public static final int ERR_TYPE_GENERIC;
            public static final int ERR_TYPE_SMS_PROTO_TRANSIENT;
            public static final int ERR_TYPE_MMS_PROTO_TRANSIENT;
            public static final int ERR_TYPE_TRANSPORT_FAILURE;
            public static final int ERR_TYPE_GENERIC_PERMANENT;
            public static final int ERR_TYPE_SMS_PROTO_PERMANENT;
            public static final int ERR_TYPE_MMS_PROTO_PERMANENT;

            static {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    TYPE_DISCRIMINATOR_COLUMN = android.provider.Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN;
                    CONTENT_URI = android.provider.Telephony.MmsSms.CONTENT_URI;
                    CONTENT_CONVERSATIONS_URI = android.provider.Telephony.MmsSms.CONTENT_CONVERSATIONS_URI;
                    CONTENT_FILTER_BYPHONE_URI = android.provider.Telephony.MmsSms.CONTENT_FILTER_BYPHONE_URI;
                    CONTENT_UNDELIVERED_URI = android.provider.Telephony.MmsSms.CONTENT_UNDELIVERED_URI;
                    CONTENT_DRAFT_URI = android.provider.Telephony.MmsSms.CONTENT_DRAFT_URI;
                    CONTENT_LOCKED_URI = android.provider.Telephony.MmsSms.CONTENT_LOCKED_URI;
                    SEARCH_URI = android.provider.Telephony.MmsSms.SEARCH_URI;

                    SMS_PROTO = android.provider.Telephony.MmsSms.SMS_PROTO;
                    MMS_PROTO = android.provider.Telephony.MmsSms.MMS_PROTO;

                    NO_ERROR                      = android.provider.Telephony.MmsSms.NO_ERROR;
                    ERR_TYPE_GENERIC              = android.provider.Telephony.MmsSms.ERR_TYPE_GENERIC;
                    ERR_TYPE_SMS_PROTO_TRANSIENT  = android.provider.Telephony.MmsSms.ERR_TYPE_SMS_PROTO_TRANSIENT;
                    ERR_TYPE_MMS_PROTO_TRANSIENT  = android.provider.Telephony.MmsSms.ERR_TYPE_MMS_PROTO_TRANSIENT;
                    ERR_TYPE_TRANSPORT_FAILURE    = android.provider.Telephony.MmsSms.ERR_TYPE_TRANSPORT_FAILURE;
                    ERR_TYPE_GENERIC_PERMANENT    = android.provider.Telephony.MmsSms.ERR_TYPE_GENERIC_PERMANENT;
                    ERR_TYPE_SMS_PROTO_PERMANENT  = android.provider.Telephony.MmsSms.ERR_TYPE_SMS_PROTO_PERMANENT;
                    ERR_TYPE_MMS_PROTO_PERMANENT  = android.provider.Telephony.MmsSms.ERR_TYPE_MMS_PROTO_PERMANENT;
                } else {
                    TYPE_DISCRIMINATOR_COLUMN = "transport_type";
                    CONTENT_URI = Uri.parse("content://mms-sms/");
                    CONTENT_CONVERSATIONS_URI = Uri.parse("content://mms-sms/conversations");
                    CONTENT_FILTER_BYPHONE_URI = Uri.parse("content://mms-sms/messages/byphone");
                    CONTENT_UNDELIVERED_URI = Uri.parse("content://mms-sms/undelivered");
                    CONTENT_DRAFT_URI = Uri.parse("content://mms-sms/draft");
                    CONTENT_LOCKED_URI = Uri.parse("content://mms-sms/locked");
                    SEARCH_URI = Uri.parse("content://mms-sms/search");

                    SMS_PROTO = 0;
                    MMS_PROTO = 1;

                    NO_ERROR                      = 0;
                    ERR_TYPE_GENERIC              = 1;
                    ERR_TYPE_SMS_PROTO_TRANSIENT  = 2;
                    ERR_TYPE_MMS_PROTO_TRANSIENT  = 3;
                    ERR_TYPE_TRANSPORT_FAILURE    = 4;
                    ERR_TYPE_GENERIC_PERMANENT    = 10;
                    ERR_TYPE_SMS_PROTO_PERMANENT  = 11;
                    ERR_TYPE_MMS_PROTO_PERMANENT  = 12;
                }
            }

            public static final class PendingMessages implements BaseColumns {
                private PendingMessages() {}

                public static final Uri CONTENT_URI;
                public static final String PROTO_TYPE;
                public static final String MSG_ID;
                public static final String MSG_TYPE;
                public static final String ERROR_TYPE;
                public static final String ERROR_CODE;
                public static final String RETRY_INDEX;
                public static final String DUE_TIME;
                public static final String LAST_TRY;
                public static final String SUBSCRIPTION_ID;

                static {
                    if (android.os.Build.VERSION.SDK_INT >= 19) {
                        CONTENT_URI = android.provider.Telephony.MmsSms.PendingMessages.CONTENT_URI;
                        PROTO_TYPE = android.provider.Telephony.MmsSms.PendingMessages.PROTO_TYPE;
                        MSG_ID = android.provider.Telephony.MmsSms.PendingMessages.MSG_ID;
                        MSG_TYPE = android.provider.Telephony.MmsSms.PendingMessages.MSG_TYPE;
                        ERROR_TYPE = android.provider.Telephony.MmsSms.PendingMessages.ERROR_TYPE;
                        ERROR_CODE = android.provider.Telephony.MmsSms.PendingMessages.ERROR_CODE;
                        RETRY_INDEX = android.provider.Telephony.MmsSms.PendingMessages.RETRY_INDEX;
                        DUE_TIME = android.provider.Telephony.MmsSms.PendingMessages.DUE_TIME;
                        LAST_TRY = android.provider.Telephony.MmsSms.PendingMessages.LAST_TRY;
                        SUBSCRIPTION_ID = android.provider.Telephony.MmsSms.PendingMessages.SUBSCRIPTION_ID;
                    } else {
                        CONTENT_URI = Uri.withAppendedPath(MmsSms.CONTENT_URI, "pending");
                        PROTO_TYPE = "proto_type";
                        MSG_ID = "msg_id";
                        MSG_TYPE = "msg_type";
                        ERROR_TYPE = "err_type";
                        ERROR_CODE = "err_code";
                        RETRY_INDEX = "retry_index";
                        DUE_TIME = "due_time";
                        LAST_TRY = "last_try";
                        SUBSCRIPTION_ID = "pending_sub_id";
                    }
                }
            }

            // This class is normally hidden
            public static final class WordsTable {
                private WordsTable() {}

                public static final String ID = "_id";
                public static final String SOURCE_ROW_ID = "source_id";
                public static final String TABLE_ID = "table_to_use";
                public static final String INDEXED_TEXT = "index_text";
            }
        }

        public static class TextBasedSmsColumns {
            private TextBasedSmsColumns() {}

            public static final int MESSAGE_TYPE_ALL;
            public static final int MESSAGE_TYPE_INBOX;
            public static final int MESSAGE_TYPE_SENT;
            public static final int MESSAGE_TYPE_DRAFT;
            public static final int MESSAGE_TYPE_OUTBOX;
            public static final int MESSAGE_TYPE_FAILED;
            public static final int MESSAGE_TYPE_QUEUED;

            public static final String TYPE;
            public static final String THREAD_ID;
            public static final String ADDRESS;
            public static final String DATE;
            public static final String DATE_SENT;
            public static final String READ;
            public static final String SEEN;
            public static final String STATUS;

            public static final int STATUS_NONE;
            public static final int STATUS_COMPLETE;
            public static final int STATUS_PENDING;
            public static final int STATUS_FAILED;

            public static final String SUBJECT;
            public static final String BODY;
            public static final String PERSON;
            public static final String PROTOCOL;
            public static final String REPLY_PATH_PRESENT;
            public static final String SERVICE_CENTER;
            public static final String LOCKED;
            public static final String SUBSCRIPTION_ID;
            public static final String MTU = "mtu"; // This constant is hidden
            public static final String ERROR_CODE;
            public static final String CREATOR;

            static {
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    MESSAGE_TYPE_ALL    = android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_ALL;
                    MESSAGE_TYPE_INBOX  = android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;
                    MESSAGE_TYPE_SENT   = android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
                    MESSAGE_TYPE_DRAFT  = android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT;
                    MESSAGE_TYPE_OUTBOX = android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;
                    MESSAGE_TYPE_FAILED = android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED;
                    MESSAGE_TYPE_QUEUED = android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED;

                    TYPE = android.provider.Telephony.TextBasedSmsColumns.TYPE;
                    THREAD_ID = android.provider.Telephony.TextBasedSmsColumns.THREAD_ID;
                    ADDRESS = android.provider.Telephony.TextBasedSmsColumns.ADDRESS;
                    DATE = android.provider.Telephony.TextBasedSmsColumns.DATE;
                    DATE_SENT = android.provider.Telephony.TextBasedSmsColumns.DATE_SENT;
                    READ = android.provider.Telephony.TextBasedSmsColumns.READ;
                    SEEN = android.provider.Telephony.TextBasedSmsColumns.SEEN;
                    STATUS = android.provider.Telephony.TextBasedSmsColumns.STATUS;

                    STATUS_NONE = android.provider.Telephony.TextBasedSmsColumns.STATUS_NONE;
                    STATUS_COMPLETE = android.provider.Telephony.TextBasedSmsColumns.STATUS_COMPLETE;
                    STATUS_PENDING = android.provider.Telephony.TextBasedSmsColumns.STATUS_PENDING;
                    STATUS_FAILED = android.provider.Telephony.TextBasedSmsColumns.STATUS_FAILED;

                    SUBJECT = android.provider.Telephony.TextBasedSmsColumns.SUBJECT;
                    BODY = android.provider.Telephony.TextBasedSmsColumns.BODY;
                    PERSON = android.provider.Telephony.TextBasedSmsColumns.PERSON;
                    PROTOCOL = android.provider.Telephony.TextBasedSmsColumns.PROTOCOL;
                    REPLY_PATH_PRESENT = android.provider.Telephony.TextBasedSmsColumns.REPLY_PATH_PRESENT;
                    SERVICE_CENTER = android.provider.Telephony.TextBasedSmsColumns.SERVICE_CENTER;
                    LOCKED = android.provider.Telephony.TextBasedSmsColumns.LOCKED;
                    SUBSCRIPTION_ID = android.provider.Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID;
                    ERROR_CODE = android.provider.Telephony.TextBasedSmsColumns.ERROR_CODE;
                    CREATOR = android.provider.Telephony.TextBasedSmsColumns.CREATOR;
                } else {
                    MESSAGE_TYPE_ALL    = 0;
                    MESSAGE_TYPE_INBOX  = 1;
                    MESSAGE_TYPE_SENT   = 2;
                    MESSAGE_TYPE_DRAFT  = 3;
                    MESSAGE_TYPE_OUTBOX = 4;
                    MESSAGE_TYPE_FAILED = 5;
                    MESSAGE_TYPE_QUEUED = 6;

                    TYPE = "type";
                    THREAD_ID = "thread_id";
                    ADDRESS = "address";
                    DATE = "date";
                    DATE_SENT = "date_sent";
                    READ = "read";
                    SEEN = "seen";
                    STATUS = "status";

                    STATUS_NONE = -1;
                    STATUS_COMPLETE = 0;
                    STATUS_PENDING = 32;
                    STATUS_FAILED = 64;

                    SUBJECT = "subject";
                    BODY = "body";
                    PERSON = "person";
                    PROTOCOL = "protocol";
                    REPLY_PATH_PRESENT = "reply_path_present";
                    SERVICE_CENTER = "service_center";
                    LOCKED = "locked";
                    SUBSCRIPTION_ID = "sub_id";
                    ERROR_CODE = "error_code";
                    CREATOR = "creator";
                }
            }
        }


    }
}
