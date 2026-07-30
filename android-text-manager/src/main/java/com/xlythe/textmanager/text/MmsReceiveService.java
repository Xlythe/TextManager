package com.xlythe.textmanager.text;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.xlythe.textmanager.text.Mock.Telephony;
import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.mms.MmsPduStore;
import com.xlythe.textmanager.text.pdu.GenericPdu;
import com.xlythe.textmanager.text.pdu.NotificationInd;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.RetrieveConf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Coordinates platform-managed MMS downloads and persists their results. */
public class MmsReceiveService extends Worker {
  private static final String TAG = MmsReceiveService.class.getSimpleName();
  private static final String EXTRA_DATA = "data";
  private static final String LEGACY_EXTRA_SUBSCRIPTION = "subscription";
  private static final String INPUT_NOTIFICATION_PDU = "notification_pdu";
  private static final String INPUT_MESSAGE_URI = "message_uri";
  private static final String INPUT_SUBSCRIPTION_ID = "subscription_id";
  private static final String INPUT_RAW_PDU_URI = "raw_pdu_uri";
  private static final String INPUT_RESULT_CODE = "result_code";
  private static final int NO_RESULT = Integer.MIN_VALUE;
  static final String WORK_TAG = "com.xlythe.textmanager.text.MMS_DOWNLOAD";

  public MmsReceiveService(@NonNull Context context, @NonNull WorkerParameters parameters) {
    super(context, parameters);
  }

  static void schedule(Context context, Intent intent) {
    byte[] pushData = intent.getByteArrayExtra(EXTRA_DATA);
    if (pushData == null || pushData.length == 0) {
      Log.e(TAG, "Ignoring MMS notification without push data");
      return;
    }
    enqueue(context, new Data.Builder()
        .putByteArray(INPUT_NOTIFICATION_PDU, pushData)
        .putInt(INPUT_SUBSCRIPTION_ID, getSubscriptionId(intent))
        .build());
  }

  static void schedule(Context context, Text text) {
    Uri messageUri = Uri.withAppendedPath(Telephony.Mms.Inbox.CONTENT_URI, text.getId());
    enqueue(context, new Data.Builder()
        .putString(INPUT_MESSAGE_URI, messageUri.toString())
        .putInt(INPUT_SUBSCRIPTION_ID, text.getSubscriptionId())
        .build());
  }

  private static void enqueue(Context context, Data data) {
    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MmsReceiveService.class)
        .setInputData(data)
        .addTag(WORK_TAG)
        .build();
    WorkManager.getInstance(context.getApplicationContext()).enqueue(request);
  }

  @NonNull
  @Override
  public Result doWork() {
    int resultCode = getInputData().getInt(INPUT_RESULT_CODE, NO_RESULT);
    return resultCode == NO_RESULT ? dispatchDownload() : persistDownload(resultCode);
  }

  private Result dispatchDownload() {
    Context context = getApplicationContext();
    byte[] notificationPdu = getInputData().getByteArray(INPUT_NOTIFICATION_PDU);
    String messageUriValue = getInputData().getString(INPUT_MESSAGE_URI);
    Uri messageUri = messageUriValue == null ? null : Uri.parse(messageUriValue);
    int subscriptionId = getInputData().getInt(
        INPUT_SUBSCRIPTION_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

    if (!TelephonyPermissions.canReceiveMms(context)) {
      Log.e(TAG, "MMS receive permission is unavailable");
      markAsFailed(context, notificationPdu, messageUri);
      return Result.failure();
    }

    final String location;
    if (notificationPdu != null) {
      NotificationInd notification = parseNotification(notificationPdu);
      if (notification == null || notification.getContentLocation() == null) {
        markAsFailed(context, notificationPdu, null);
        return Result.failure();
      }
      location = new String(notification.getContentLocation(), StandardCharsets.ISO_8859_1);
    } else {
      location = getContentLocation(context, messageUri);
    }
    if (TextUtils.isEmpty(location)) {
      markAsFailed(context, notificationPdu, messageUri);
      return Result.failure();
    }

    Uri rawPduUri = null;
    try {
      rawPduUri = MmsPduStore.create(context);
      PendingIntent callback = newDownloadedPendingIntent(
          context, rawPduUri, notificationPdu, messageUri, subscriptionId);
      SmsManager smsManager = SendService.resolveSmsManager(subscriptionId);
      smsManager.downloadMultimediaMessage(context, location, rawPduUri, null, callback);
      return Result.success();
    } catch (IOException | RuntimeException e) {
      Log.e(TAG, "Unable to dispatch MMS download", e);
      if (rawPduUri != null) {
        MmsPduStore.delete(context, rawPduUri);
      }
      markAsFailed(context, notificationPdu, messageUri);
      return Result.failure();
    }
  }

  private Result persistDownload(int resultCode) {
    Context context = getApplicationContext();
    Uri rawPduUri = parseUri(getInputData().getString(INPUT_RAW_PDU_URI));
    byte[] notificationPdu = getInputData().getByteArray(INPUT_NOTIFICATION_PDU);
    Uri messageUri = parseUri(getInputData().getString(INPUT_MESSAGE_URI));
    int subscriptionId = getInputData().getInt(
        INPUT_SUBSCRIPTION_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

    try {
      if (resultCode != Activity.RESULT_OK || rawPduUri == null) {
        Log.e(TAG, "MMS download failed with result " + resultCode);
        markAsFailed(context, notificationPdu, messageUri);
        return Result.failure();
      }

      GenericPdu parsed = new PduParser(MmsPduStore.read(context, rawPduUri), true).parse();
      if (!(parsed instanceof RetrieveConf)) {
        Log.e(TAG, "Downloaded MMS did not contain a retrieve confirmation");
        markAsFailed(context, notificationPdu, messageUri);
        return Result.failure();
      }

      Uri destination = messageUri == null ? Telephony.Mms.Inbox.CONTENT_URI : messageUri;
      Uri persisted = PduPersister.getPduPersister(context)
          .persist((RetrieveConf) parsed, destination, true, true, null);
      ContentValues values = new ContentValues();
      values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
      values.put(Telephony.Mms.STATUS, Telephony.Sms.Sent.STATUS_COMPLETE);
      if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
        values.put(Telephony.Mms.SUBSCRIPTION_ID, subscriptionId);
      }
      context.getContentResolver().update(persisted, values, null, null);

      Text text = loadText(context, persisted);
      if (text != null) {
        Intent received = new Intent(TextReceiver.ACTION_TEXT_RECEIVED)
            .setPackage(context.getPackageName())
            .putExtra(TextReceiver.EXTRA_TEXT, text);
        context.sendBroadcast(received);
      }
      return Result.success();
    } catch (IOException | MmsException | RuntimeException e) {
      Log.e(TAG, "Unable to persist downloaded MMS", e);
      markAsFailed(context, notificationPdu, messageUri);
      return Result.failure();
    } finally {
      if (rawPduUri != null) {
        MmsPduStore.delete(context, rawPduUri);
      }
    }
  }

  private static PendingIntent newDownloadedPendingIntent(
      Context context,
      Uri rawPduUri,
      byte[] notificationPdu,
      Uri messageUri,
      int subscriptionId) {
    Intent intent = new Intent(context, MmsDownloadedReceiver.class)
        .setData(rawPduUri)
        .putExtra(INPUT_NOTIFICATION_PDU, notificationPdu)
        .putExtra(INPUT_MESSAGE_URI, messageUri == null ? null : messageUri.toString())
        .putExtra(INPUT_SUBSCRIPTION_ID, subscriptionId);
    return PendingIntent.getBroadcast(
        context,
        rawPduUri.hashCode(),
        intent,
        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
  }

  private static NotificationInd parseNotification(byte[] data) {
    if (data == null || data.length == 0) {
      return null;
    }
    GenericPdu parsed;
    try {
      parsed = new PduParser(data, true).parse();
    } catch (RuntimeException e) {
      Log.e(TAG, "Invalid MMS notification PDU", e);
      return null;
    }
    return parsed instanceof NotificationInd ? (NotificationInd) parsed : null;
  }

  private static String getContentLocation(Context context, Uri uri) {
    if (uri == null) {
      return null;
    }
    try (Cursor cursor = context.getContentResolver().query(
        uri, new String[] {Telephony.Mms.CONTENT_LOCATION}, null, null, null)) {
      return cursor != null && cursor.moveToFirst() ? cursor.getString(0) : null;
    } catch (RuntimeException e) {
      Log.e(TAG, "Unable to read MMS content location for " + uri, e);
      return null;
    }
  }

  private static Text loadText(Context context, Uri messageUri) {
    String[] partProjection = new String[] {
        BaseColumns._ID,
        Telephony.Mms.Part.CONTENT_TYPE,
        Telephony.Mms.Part.TEXT,
        Telephony.Mms.Part._DATA,
        Telephony.Mms.Part.MSG_ID
    };
    Uri partsUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part");
    String messageId = messageUri.getLastPathSegment();
    try (Cursor message = context.getContentResolver().query(messageUri, null, null, null, null);
         Cursor parts = context.getContentResolver().query(
             partsUri,
             partProjection,
             Telephony.Mms.Part.MSG_ID + " = ?",
             new String[] {messageId},
             null)) {
      if (message == null || parts == null || !message.moveToFirst()) {
        return null;
      }
      return new Text(message, parts);
    } catch (RuntimeException e) {
      Log.e(TAG, "Unable to load downloaded MMS " + messageUri, e);
      return null;
    }
  }

  private static void markAsFailed(Context context, byte[] notificationPdu, Uri messageUri) {
    try {
      Uri uri = messageUri;
      if (uri == null && notificationPdu != null) {
        GenericPdu parsed = new PduParser(notificationPdu, true).parse();
        if (parsed != null) {
          uri = PduPersister.getPduPersister(context)
              .persist(parsed, Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
        }
      }
      if (uri != null) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Mms.STATUS, Telephony.Sms.Sent.STATUS_FAILED);
        values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
        context.getContentResolver().update(uri, values, null, null);
      }
    } catch (MmsException | RuntimeException e) {
      Log.e(TAG, "Unable to persist MMS failure", e);
    }
  }

  private static int getSubscriptionId(Intent intent) {
    int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    if (Build.VERSION.SDK_INT >= 28) {
      subscriptionId = intent.getIntExtra(TelephonyManager.EXTRA_SUBSCRIPTION_ID, subscriptionId);
    }
    if (Build.VERSION.SDK_INT >= 26) {
      subscriptionId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, subscriptionId);
    }
    return intent.getIntExtra(LEGACY_EXTRA_SUBSCRIPTION, subscriptionId);
  }

  private static Uri parseUri(String value) {
    return value == null ? null : Uri.parse(value);
  }

  public static final class MmsDownloadedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      Uri rawPduUri = intent.getData();
      if (rawPduUri == null) {
        Log.e(TAG, "MMS download callback is missing its raw PDU URI");
        return;
      }
      Data data = new Data.Builder()
          .putString(INPUT_RAW_PDU_URI, rawPduUri.toString())
          .putByteArray(INPUT_NOTIFICATION_PDU, intent.getByteArrayExtra(INPUT_NOTIFICATION_PDU))
          .putString(INPUT_MESSAGE_URI, intent.getStringExtra(INPUT_MESSAGE_URI))
          .putInt(INPUT_SUBSCRIPTION_ID, intent.getIntExtra(
              INPUT_SUBSCRIPTION_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID))
          .putInt(INPUT_RESULT_CODE, getResultCode())
          .build();
      enqueue(context, data);
    }
  }
}
