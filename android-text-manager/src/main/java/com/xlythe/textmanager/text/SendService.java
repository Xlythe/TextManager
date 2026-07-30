package com.xlythe.textmanager.text;

import android.Manifest;
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
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.sms.SmsCallbackFactory;
import com.xlythe.textmanager.text.sms.SmsGateway;
import com.xlythe.textmanager.text.sms.SmsSendCoordinator;
import com.xlythe.textmanager.text.sms.SmsSendRequest;
import com.xlythe.textmanager.text.sms.SmsStatusTracker;
import com.xlythe.textmanager.text.mms.MmsPduStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
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

    static void schedule(Context context, Text text) {
        Data inputData = new Data.Builder()
                .putByteArray(EXTRA_TEXT, text.toBytes())
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SendService.class)
                .setInputData(inputData)
                .addTag(WORK_TAG)
                .build();
        WorkManager.getInstance(context.getApplicationContext()).enqueue(request);
    }

    public SendService(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    @NonNull
    @SuppressLint("InlinedApi")
    @WorkerThread
    @Override
    public Result doWork() {
        byte[] serializedText = getInputData().getByteArray(EXTRA_TEXT);
        if (serializedText == null) {
            Log.e(TAG, "Send work is missing its text payload");
            return Result.failure();
        }

        final Text text;
        try {
            text = Text.fromBytes(serializedText);
        } catch (RuntimeException e) {
            Log.e(TAG, "Send work contains an invalid text payload", e);
            return Result.failure();
        }

        Context context = getApplicationContext();
        if (text.isMms() && !TelephonyPermissions.canSendMms(context)) {
            Log.e(TAG, "MMS send permissions are unavailable");
            return Result.failure();
        }
        if (!text.isMms() && !TelephonyPermissions.canSendSms(context)) {
            Log.e(TAG, "SMS send permission is unavailable");
            return Result.failure();
        }

        if (text.isMms()) {
            Uri messageUri = storeMMS(text);
            if (messageUri == null) {
                return Result.failure();
            }
            Uri pduUri = null;
            try {
                pduUri = MmsPduStore.write(context, text.getByteData(context));
                PendingIntent sentIntent =
                        newMmsSentPendingIntent(context, messageUri, pduUri, text.getId());
                sendMMS(text, pduUri, sentIntent);
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "Failed to dispatch MMS through the platform service", e);
                if (pduUri != null) {
                    MmsPduStore.delete(context, pduUri);
                }
                markMmsFailed(messageUri);
                return Result.failure();
            }
        } else {
            sendSMS(text);
        }
        return Result.success();
    }

    private Uri storeSMS(Text text, String address) {
        ContentValues values = new ContentValues();
        Uri uri = Mock.Telephony.Sms.Sent.CONTENT_URI;
        values.put(Mock.Telephony.Sms.ADDRESS, address);
        values.put(Mock.Telephony.Sms.BODY, text.getBody());
        values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
        String clause = Mock.Telephony.Sms._ID + " = ?";
        String[] selectionArgs = new String[] {text.getId()};

        int rowsUpdated = getApplicationContext().getContentResolver().update(uri, values, clause, selectionArgs);
        if (rowsUpdated == 0) {
            return getApplicationContext().getContentResolver().insert(uri, values);
        }
        return Uri.withAppendedPath(uri, text.getId());
    }

    private void sendSMS(Text text) {
        Set<Contact> contacts = TextManager.getInstance(getApplicationContext()).getMembers(text).get();
        if (contacts.size() != 1) {
            Log.e(TAG, "SMS requires exactly one recipient; found " + contacts.size());
            return;
        }

        Contact contact = contacts.iterator().next();
        if (contact.equals(Contact.UNKNOWN)) {
            Log.e(TAG, "SMS recipient could not be resolved");
            return;
        }

        final SmsSendRequest request;
        try {
            request = new SmsSendRequest(text.getId(), contact.getNumber(), text.getBody());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid outgoing SMS", e);
            return;
        }

        Uri uri = null;
        try {
            uri = storeSMS(text, request.getRecipient());
            if (uri == null) {
                Log.e(TAG, "Failed to persist outgoing SMS " + request.getMessageId());
                return;
            }
            SmsSendRequest persistedRequest =
                    new SmsSendRequest(
                            uri.getLastPathSegment(),
                            request.getRecipient(),
                            request.getBody());
            dispatchSMS(persistedRequest, uri, text.getSubscriptionId());
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to send SMS " + request.getMessageId(), e);
            if (uri != null) {
                markSmsFailed(uri);
            }
        }
    }

    private void dispatchSMS(SmsSendRequest request, Uri uri, int subscriptionId) {
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
                                partCount);
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
        new SmsSendCoordinator<>(gateway, callbackFactory).send(request);
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
    private Uri storeMMS(Text text) {
        PduPersister p = PduPersister.getPduPersister(getApplicationContext());

        Uri uri;
        String id = text.getId();
        if (Long.parseLong(id) != -1) {
            uri = Uri.withAppendedPath(Mock.Telephony.Mms.Sent.CONTENT_URI, id);
        } else {
            uri = Mock.Telephony.Mms.Sent.CONTENT_URI;
        }
        try {
            uri = p.persist(text.getSendRequest(getApplicationContext()), uri, true, true, null);
        } catch (MmsException e) {
            Log.e(TAG, "persisting pdu failed", e);
            return null;
        }

        ContentValues values = new ContentValues();
        values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
        getApplicationContext().getContentResolver().update(uri, values, null, null);

        return uri;
    }

    /** Dispatches the raw PDU to Android's telephony-managed MMS transport. */
    private void sendMMS(Text text, Uri pduUri, PendingIntent sentMmsPendingIntent) {
        SmsManager smsManager = resolveSmsManager(text.getSubscriptionId());
        smsManager.sendMultimediaMessage(
                getApplicationContext(), pduUri, null, null, sentMmsPendingIntent);
    }

    private void markMmsFailed(Uri uri) {
        ContentValues values = new ContentValues();
        values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
        try {
            getApplicationContext().getContentResolver().update(uri, values, null, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to persist MMS failure for " + uri, e);
        }
    }
    /**
     * Pending Intents to notify when an MMS or SMS has been sent or failed to send
     */
    @SuppressLint("InlinedApi")
    private static PendingIntent newSmsSentPendingIntent(
            Context context, Uri uri, String id, int partIndex, int partCount) {
        Intent intent = new Intent(context, SmsSentReceiver.class);
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
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
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
            Context context, Uri messageUri, Uri pduUri, String id) {
        Intent intent = new Intent(context, MmsSentReceiver.class);
        intent.setAction(ACTION_MMS_SENT);
        intent.setData(messageUri);
        intent.putExtra(EXTRA_MMS_PDU_URI, pduUri);
        return PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
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

        final SmsStatusTracker.Status status;
        try {
            status =
                    new SmsStatusTracker(new SharedPreferencesSmsStatusStore(context))
                            .record(messageId, phase, partIndex, partCount, successful);
        } catch (RuntimeException e) {
            Log.e(TAG, "Invalid SMS callback metadata", e);
            return;
        }
        if (status == SmsStatusTracker.Status.PENDING) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(
                Mock.Telephony.Sms.Sent.STATUS,
                status == SmsStatusTracker.Status.COMPLETE
                        ? Mock.Telephony.Sms.Sent.STATUS_COMPLETE
                        : Mock.Telephony.Sms.Sent.STATUS_FAILED);
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
            boolean successful = getResultCode() == Activity.RESULT_OK;
            Log.d(TAG, successful ? "SMS part delivered" : "SMS part not delivered");
            recordSmsResult(context, intent, SmsStatusTracker.Phase.DELIVERED, successful);
        }
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
            ContentValues values = new ContentValues();
            if (getResultCode() == Activity.RESULT_OK) {
                Log.d(TAG, "MMS sent");
                values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_COMPLETE);
            } else {
                Log.d(TAG, "MMS failed to send with result " + getResultCode());
                values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
            }
            try {
                applicationContext.getContentResolver().update(messageUri, values, null, null);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to persist MMS callback status for " + messageUri, e);
            }
        }
    }
}
