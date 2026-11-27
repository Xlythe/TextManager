package com.xlythe.textmanager.text;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.mms.LegacyMmsTransport;
import com.xlythe.textmanager.text.mms.MmsPduStore;
import com.xlythe.textmanager.text.pdu.GenericPdu;
import com.xlythe.textmanager.text.pdu.PduHeaders;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.SendConf;
import com.xlythe.textmanager.text.pdu.SendReq;
import com.xlythe.textmanager.text.sms.SmsCallbackFactory;
import com.xlythe.textmanager.text.sms.SmsGateway;
import com.xlythe.textmanager.text.sms.SmsSendCoordinator;
import com.xlythe.textmanager.text.sms.SmsSendRequest;
import com.xlythe.textmanager.text.sms.SmsStatusTracker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SendService extends Worker {
    private static final String TAG = SendService.class.getSimpleName();
    private static final String ACTION_SMS_SENT = "com.xlythe.textmanager.text.SMS_SENT";
    private static final String ACTION_SMS_DELIVERED = "com.xlythe.textmanager.text.SMS_DELIVERED";
    private static final String ACTION_MMS_SENT = "com.xlythe.textmanager.text.MMS_SENT";
    private static final String EXTRA_TEXT = "text";
    private static final String EXTRA_SMS_MESSAGE_ID = "sms_message_id";
    private static final String EXTRA_SMS_PART_INDEX = "sms_part_index";
    private static final String EXTRA_SMS_PART_COUNT = "sms_part_count";
    private static final String EXTRA_MMS_PDU_URI = "mms_pdu_uri";
    static final String WORK_TAG = "com.xlythe.textmanager.text.SEND";

    private static final String SEPARATE_PARTS = "separate_parts";
    private static final String DELIVERY_REPORTS = "delivery_reports";

    static void schedule(Context context, Text text) {
        schedule(context, text, false, true);
    }

    static void schedule(Context context, Text text, boolean separateParts, boolean deliveryReports) {
        schedule(context, text, separateParts, deliveryReports, null, false, false);
    }

    static void schedule(Context context, Text text, boolean separateParts, boolean deliveryReports,
            String subject, boolean mmsDeliveryReport, boolean mmsReadReport) {
        Data.Builder options = new Data.Builder()
                .putBoolean("mms_delivery_report", mmsDeliveryReport)
                .putBoolean("mms_read_report", mmsReadReport)
                .putBoolean(SEPARATE_PARTS, separateParts)
                .putBoolean(DELIVERY_REPORTS, deliveryReports);
        enqueue(context, options, text.toBytes(), subject);
    }

    static void scheduleMms(Context context, byte[] bytes, int subscriptionId) {
        enqueue(context, new Data.Builder().putBoolean("raw_mms", true)
                .putInt("subscription_id", subscriptionId), bytes, null);
    }

    private static void enqueue(Context context, Data.Builder options, byte[] bytes, String subject) {
        Context app = context.getApplicationContext();
        WorkManager manager = WorkManager.getInstance(app);
        QueuedSend.prune(app, manager);
        UUID id = UUID.randomUUID();
        Data data;
        data = null;
        if (bytes.length <= Data.MAX_DATA_BYTES) {
            try {
                data = new Data.Builder().putAll(options.build()).putByteArray(EXTRA_TEXT, bytes)
                        .putString("mms_subject", subject).build();
            } catch (IllegalStateException oversized) {
                // WorkManager includes serialized headers and options in its size limit.
            }
        }
        if (data == null) {
            try {
                QueuedSend.write(app, id, bytes, subject);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to queue outgoing message", e);
            }
            data = options.putBoolean("file_payload", true).build();
        }
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SendService.class)
                .setId(id).setInputData(data).addTag(WORK_TAG).build();
        try {
            manager.enqueue(request);
        } catch (RuntimeException failure) {
            if (data.getBoolean("file_payload", false)) QueuedSend.delete(app, id);
            throw failure;
        }
    }

    public SendService(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    @NonNull
    @SuppressLint("InlinedApi")
    @WorkerThread
    @Override
    public Result doWork() {
        if (!getInputData().getBoolean("file_payload", false)) {
            return send(getInputData().getByteArray(EXTRA_TEXT), getInputData().getString("mms_subject"));
        }
        try {
            QueuedSend payload = QueuedSend.read(getApplicationContext(), getId());
            return send(payload.text, payload.subject);
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Unable to read queued message", e);
            return Result.failure();
        } finally {
            QueuedSend.delete(getApplicationContext(), getId());
        }
    }

    @SuppressLint("MissingPermission")
    private Result send(byte[] serializedText, String subject) {
        if (serializedText == null) {
            Log.e(TAG, "Send work is missing its text payload");
            return Result.failure();
        }

        Context context = getApplicationContext();
        if (!TelephonyPermissions.canSendSms(context)) {
            Log.e(TAG, "SEND_SMS permission is unavailable");
            return Result.failure();
        }
        final Text text;
        try {
            if (getInputData().getBoolean("raw_mms", false)) {
                GenericPdu parsed = new PduParser(serializedText, true).parse();
                if (!(parsed instanceof SendReq)) return Result.failure();
                return dispatchMms((SendReq) parsed, serializedText, "-1", getInputData().getInt("subscription_id", -1));
            }
            text = Text.fromBytes(serializedText);
        } catch (RuntimeException e) {
            Log.e(TAG, "Invalid queued message", e);
            return Result.failure();
        }
        boolean deliveryReport = getInputData().getBoolean("mms_delivery_report", false);
        boolean readReport = getInputData().getBoolean("mms_read_report", false);
        if (text.isMms() || subject != null || deliveryReport || readReport) {
            try {
                SendReq request = text.getSendRequest(context, subject, deliveryReport, readReport);
                return dispatchMms(request, text.getByteData(), text.getId(), text.getSubscriptionId());
            } catch (RuntimeException failed) {
                Log.e(TAG, "Could not compose outgoing MMS", failed);
                return Result.failure();
            }
        }
        return sendSMS(text) ? Result.success() : Result.failure();
    }

    @SuppressLint("MissingPermission")
    private Result dispatchMms(SendReq request, byte[] data, String id, int subscription) {
        Context context = getApplicationContext();
        Uri messageUri = null;
        Uri file = null;
        try {
            if (data == null || data.length == 0 || data.length > MmsPduStore.MAX_PDU_BYTES) return Result.failure();
            messageUri = storeMMS(id, subscription, request);
            if (messageUri == null) return Result.failure();
            if (Build.VERSION.SDK_INT < 21) {
                SendConf confirmation = verifyConfirmation(request.getTransactionId(), LegacyMmsTransport.transfer(context, null, data));
                finishMms(context, messageUri, true, confirmation.getMessageId());
            } else {
                file = MmsPduStore.write(context, data);
                PendingIntent sent = newMmsSentPendingIntent(context, messageUri, file, request.getTransactionId());
                resolveSmsManager(subscription).sendMultimediaMessage(context, file, null, null, sent);
            }
            return Result.success();
        } catch (IOException | RuntimeException failed) {
            Log.e(TAG, "Failed to send MMS", failed);
            if (file != null) MmsPduStore.delete(context, file);
            if (messageUri != null) finishMms(context, messageUri, false, null);
            return Result.failure();
        }
    }

    private Uri storeSMS(Text text, String address) {
        ContentValues values = new ContentValues();
        Uri uri = Mock.Telephony.Sms.Sent.CONTENT_URI;
        if (Build.VERSION.SDK_INT >= 22 && text.getSubscriptionId() >= 0) {
            values.put(Mock.Telephony.Sms.SUBSCRIPTION_ID, text.getSubscriptionId());
        }
        values.put(Mock.Telephony.Sms.ADDRESS, address);
        values.put(Mock.Telephony.Sms.BODY, text.getBody());
        values.put(Mock.Telephony.Sms.DATE, text.getTimestamp());
        values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
        String clause = Mock.Telephony.Sms._ID + " = ?";
        String[] selectionArgs = new String[] {text.getId()};

        int rowsUpdated = getApplicationContext().getContentResolver().update(uri, values, clause, selectionArgs);
        if (rowsUpdated == 0) {
            return getApplicationContext().getContentResolver().insert(uri, values);
        }
        return Uri.withAppendedPath(uri, text.getId());
    }

    private boolean sendSMS(Text text) {
        Set<Contact> contacts = TextManager.getInstance(getApplicationContext()).getMembers(text).get();
        if (contacts.size() != 1) {
            Log.e(TAG, "SMS requires exactly one recipient; found " + contacts.size());
            return false;
        }

        Contact contact = contacts.iterator().next();
        if (contact.equals(Contact.UNKNOWN)) {
            Log.e(TAG, "SMS recipient could not be resolved");
            return false;
        }

        final SmsSendRequest request;
        try {
            request = new SmsSendRequest(text.getId(), contact.getMobileNumber(getApplicationContext()).get(), text.getBody());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid outgoing SMS", e);
            return false;
        }

        Uri uri = null;
        try {
            Context context = getApplicationContext();
            boolean canStore = Build.VERSION.SDK_INT < 19
                    ? TelephonyPermissions.has(context, "android.permission.WRITE_SMS")
                    : TextManager.getInstance(context).isDefaultSmsPackage();
            if (!canStore) {
                // Android persists sends from non-default apps. They cannot update the SMS provider.
                dispatchSMS(request, null, text.getSubscriptionId());
                return true;
            }
            uri = storeSMS(text, request.getRecipient());
            if (uri == null) {
                Log.e(TAG, "Failed to persist outgoing SMS " + request.getMessageId());
                return false;
            }
            SmsSendRequest persistedRequest =
                    new SmsSendRequest(
                            uri.getLastPathSegment(),
                            request.getRecipient(),
                            request.getBody());
            dispatchSMS(persistedRequest, uri, text.getSubscriptionId());
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to send SMS " + request.getMessageId(), e);
            if (uri != null) {
                markSmsFailed(uri);
            }
            return false;
        }
    }

    private void dispatchSMS(SmsSendRequest request, Uri uri, int subscriptionId) {
        boolean deliveryReports = getInputData().getBoolean(DELIVERY_REPORTS, true);
        SmsGateway<PendingIntent> gateway = new AndroidSmsGateway(resolveSmsManager(subscriptionId));
        SmsCallbackFactory<PendingIntent> callbackFactory =
                new SmsCallbackFactory<PendingIntent>() {
                    @Override
                    public void initialize(SmsSendRequest ignored, int partCount) {
                        new SmsStatusTracker(new SharedPreferencesSmsStatusStore(getApplicationContext()))
                                .reset(request.getMessageId(), partCount);
                    }

                    @Override
                    public PendingIntent createSent(
                            SmsSendRequest ignored, int partIndex, int partCount) {
                        return newSmsSentPendingIntent(
                                getApplicationContext(),
                                uri,
                                request.getMessageId(),
                                partIndex,
                                partCount, deliveryReports);
                    }

                    @Override
                    public PendingIntent createDelivered(
                            SmsSendRequest ignored, int partIndex, int partCount) {
                        return newSmsDeliveredPendingIntent(
                                getApplicationContext(),
                                uri,
                                request.getMessageId(),
                                partIndex,
                                partCount);
                    }
                };
        new SmsSendCoordinator<>(gateway, uri == null ? null : callbackFactory).send(request,
                getInputData().getBoolean(SEPARATE_PARTS, false),
                deliveryReports);
    }

    private void markSmsFailed(Uri uri) {
        ContentValues values = new ContentValues();
        values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
        try {
            getApplicationContext().getContentResolver().update(uri, values, null, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to persist SMS failure for " + uri, e);
        }
    }

    @SuppressWarnings("deprecation")
    static SmsManager resolveSmsManager(int subscriptionId) {
        if (subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return SmsManager.getDefault();
        }
        return SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
    }

    private static final class AndroidSmsGateway implements SmsGateway<PendingIntent> {
        private final SmsManager smsManager;

        AndroidSmsGateway(SmsManager smsManager) {
            this.smsManager = smsManager;
        }

        @Override
        public List<String> divideMessage(String body) {
            return smsManager.divideMessage(body);
        }

        @Override
        public void sendText(
                String recipient,
                String body,
                PendingIntent sentCallback,
                PendingIntent deliveredCallback) {
            smsManager.sendTextMessage(
                    recipient, null, body, sentCallback, deliveredCallback);
        }

        @Override
        public void sendMultipartText(
                String recipient,
                List<String> parts,
                List<PendingIntent> sentCallbacks,
                List<PendingIntent> deliveredCallbacks) {
            smsManager.sendMultipartTextMessage(
                    recipient,
                    null,
                    new ArrayList<>(parts),
                    new ArrayList<>(sentCallbacks),
                    new ArrayList<>(deliveredCallbacks));
        }
    }

    @SuppressLint("InlinedApi")
    @Nullable
    private Uri storeMMS(String id, int subscription, SendReq request) {
        PduPersister p = PduPersister.getPduPersister(getApplicationContext());

        Uri uri;
        if (Long.parseLong(id) != -1) {
            uri = Uri.withAppendedPath(Mock.Telephony.Mms.Sent.CONTENT_URI, id);
        } else {
            uri = Mock.Telephony.Mms.Sent.CONTENT_URI;
        }
        try {
            uri = p.persist(request, uri, true, true, null);
        } catch (MmsException e) {
            Log.e(TAG, "persisting pdu failed", e);
            return null;
        }

        ContentValues values = new ContentValues();
        if (Build.VERSION.SDK_INT >= 22 && subscription >= 0) {
            values.put(Mock.Telephony.Mms.SUBSCRIPTION_ID, subscription);
        }
        values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
        getApplicationContext().getContentResolver().update(uri, values, null, null);

        return uri;
    }

    /**
     * Pending Intents to notify when an MMS or SMS has been sent or failed to send
     */
    @SuppressLint("InlinedApi")
    private static PendingIntent newSmsSentPendingIntent(
            Context context, Uri uri, String id, int partIndex, int partCount, boolean deliveryReports) {
        Intent intent = new Intent(context, SmsSentReceiver.class);
        intent.putExtra(DELIVERY_REPORTS, deliveryReports);
        intent.setAction(ACTION_SMS_SENT);
        addSmsCallbackMetadata(intent, uri, id, partIndex, partCount);
        return PendingIntent.getBroadcast(
                context,
                callbackRequestCode(id, partIndex),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }

    @SuppressLint("InlinedApi")
    private static PendingIntent newSmsDeliveredPendingIntent(
            Context context, Uri uri, String id, int partIndex, int partCount) {
        Intent intent = new Intent(context, SmsDeliveredReceiver.class);
        intent.setAction(ACTION_SMS_DELIVERED);
        addSmsCallbackMetadata(intent, uri, id, partIndex, partCount);
        return PendingIntent.getBroadcast(
                context,
                callbackRequestCode(id, partIndex),
                intent,
                Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
    }

    private static void addSmsCallbackMetadata(
            Intent intent, Uri uri, String id, int partIndex, int partCount) {
        intent.setData(uri);
        intent.putExtra(EXTRA_SMS_MESSAGE_ID, id);
        intent.putExtra(EXTRA_SMS_PART_INDEX, partIndex);
        intent.putExtra(EXTRA_SMS_PART_COUNT, partCount);
    }

    private static int callbackRequestCode(String id, int partIndex) {
        return 31 * id.hashCode() + partIndex;
    }

    @SuppressLint("InlinedApi")
    private static PendingIntent newMmsSentPendingIntent(
            Context context, Uri messageUri, Uri pduUri, byte[] transactionId) {
        Intent intent = new Intent(context, MmsSentReceiver.class);
        intent.setAction(ACTION_MMS_SENT);
        intent.setData(messageUri);
        intent.putExtra(EXTRA_MMS_PDU_URI, pduUri);
        intent.putExtra("transaction_id", transactionId);
        return PendingIntent.getBroadcast(
                context,
                pduUri.hashCode(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0));
    }

    /**
     * Receivers to update content provided when an MMS or SMS has been sent or failed to send
     */
    private static void recordSmsResult(
            Context context,
            Intent intent,
            SmsStatusTracker.Phase phase,
            boolean successful) {
        Uri uri = intent.getData();
        String messageId = intent.getStringExtra(EXTRA_SMS_MESSAGE_ID);
        int partIndex = intent.getIntExtra(EXTRA_SMS_PART_INDEX, -1);
        int partCount = intent.getIntExtra(EXTRA_SMS_PART_COUNT, 0);
        if (uri == null) {
            Log.e(TAG, "SMS callback is missing its message URI");
            return;
        }

        boolean deliveryReports = intent.getBooleanExtra(DELIVERY_REPORTS, true);
        final SmsStatusTracker.Status status;
        try {
            status =
                    new SmsStatusTracker(new SharedPreferencesSmsStatusStore(context))
                            .record(messageId, phase, partIndex, partCount, successful, deliveryReports);
        } catch (RuntimeException e) {
            Log.e(TAG, "Invalid SMS callback metadata", e);
            return;
        }
        if (status == SmsStatusTracker.Status.PENDING) {
            return;
        }

        ContentValues values = new ContentValues();
        int providerStatus = Mock.Telephony.Sms.Sent.STATUS_FAILED;
        if (status == SmsStatusTracker.Status.COMPLETE) {
            providerStatus = deliveryReports ? Mock.Telephony.Sms.Sent.STATUS_COMPLETE : Mock.Telephony.Sms.Sent.STATUS_NONE;
        }
        values.put(Mock.Telephony.Sms.Sent.STATUS, providerStatus);
        try {
            context.getApplicationContext().getContentResolver().update(uri, values, null, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to persist SMS callback status for " + uri, e);
        }
    }

    public static final class SmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean successful = getResultCode() == Activity.RESULT_OK;
            Log.d(TAG, successful ? "SMS part sent" : "SMS part failed to send");
            recordSmsResult(context, intent, SmsStatusTracker.Phase.SENT, successful);
        }
    }

    public static final class SmsDeliveredReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getResultCode() != Activity.RESULT_OK) {
                recordSmsResult(context, intent, SmsStatusTracker.Phase.DELIVERED, false);
                return;
            }
            byte[] pdu = intent.getByteArrayExtra("pdu");
            if (pdu == null) return;
            String format = intent.getStringExtra("format");
            SmsMessage report = Build.VERSION.SDK_INT >= 23 && format != null
                    ? SmsMessage.createFromPdu(pdu, format) : SmsMessage.createFromPdu(pdu);
            if (report == null) return;
            boolean cdma = "3gpp2".equals(format) || (report.getStatus() >>> 16) != 0;
            if (!cdma && !report.isStatusReportMessage()) return;
            SmsStatusTracker.Status status = deliveryStatus(report.getStatus(), cdma);
            if (status != SmsStatusTracker.Status.PENDING) {
                recordSmsResult(context, intent, SmsStatusTracker.Phase.DELIVERED, status == SmsStatusTracker.Status.COMPLETE);
            }
        }
    }

    static SmsStatusTracker.Status deliveryStatus(int status, boolean cdma) {
        if (cdma) {
            int errorClass = (status >>> 24) & 3;
            if (errorClass == 3) return SmsStatusTracker.Status.FAILED;
            if (errorClass == 0 && ((status >>> 16) & 255) == 2) return SmsStatusTracker.Status.COMPLETE;
            return SmsStatusTracker.Status.PENDING;
        }
        if (status < 0x20) return SmsStatusTracker.Status.COMPLETE;
        if (status < 0x40) return SmsStatusTracker.Status.PENDING;
        return SmsStatusTracker.Status.FAILED;
    }

    public static final class MmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Context applicationContext = context.getApplicationContext();
            Uri pduUri = intent.getParcelableExtra(EXTRA_MMS_PDU_URI);
            if (pduUri != null) {
                MmsPduStore.delete(applicationContext, pduUri);
            }

            Uri messageUri = intent.getData();
            if (messageUri == null) {
                Log.e(TAG, "MMS callback is missing its message URI");
                return;
            }
            boolean sent = getResultCode() == Activity.RESULT_OK;
            byte[] id = null;
            byte[] response = intent.getByteArrayExtra(SmsManager.EXTRA_MMS_DATA);
            if (sent && response != null) {
                try {
                    id = verifyConfirmation(intent.getByteArrayExtra("transaction_id"), response).getMessageId();
                } catch (IOException | RuntimeException invalid) {
                    sent = false;
                }
            }
            finishMms(applicationContext, messageUri, sent, id);
        }
    }

    static SendConf verifyConfirmation(byte[] transaction, byte[] bytes) throws IOException {
        GenericPdu response = new PduParser(bytes, true).parse();
        if (!(response instanceof SendConf)) throw new IOException("MMSC did not return a send confirmation");
        SendConf confirmation = (SendConf) response;
        if (confirmation.getResponseStatus() != PduHeaders.RESPONSE_STATUS_OK
                || !Arrays.equals(transaction, confirmation.getTransactionId())) {
            throw new IOException("MMSC rejected the message or returned a different transaction");
        }
        return confirmation;
    }

    private static void finishMms(Context context, Uri messageUri, boolean success, byte[] id) {
        ContentValues values = new ContentValues();
        if (id != null) values.put(Mock.Telephony.Mms.MESSAGE_ID, new String(id, Charset.forName("ISO-8859-1")));
        values.put(Mock.Telephony.Mms.STATUS, success ? Mock.Telephony.Sms.Sent.STATUS_COMPLETE : Mock.Telephony.Sms.Sent.STATUS_FAILED);
        try {
            context.getContentResolver().update(messageUri, values, null, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to persist MMS callback status", e);
        }
    }
}
