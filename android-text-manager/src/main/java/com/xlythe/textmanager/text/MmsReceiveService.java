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
import com.xlythe.textmanager.text.mms.LegacyMmsTransport;
import com.xlythe.textmanager.text.pdu.GenericPdu;
import com.xlythe.textmanager.text.pdu.NotificationInd;
import com.xlythe.textmanager.text.pdu.NotifyRespInd;
import com.xlythe.textmanager.text.pdu.AcknowledgeInd;
import com.xlythe.textmanager.text.pdu.PduComposer;
import com.xlythe.textmanager.text.pdu.DeliveryInd;
import com.xlythe.textmanager.text.pdu.ReadOrigInd;
import com.xlythe.textmanager.text.pdu.ReadRecInd;
import com.xlythe.textmanager.text.pdu.PduHeaders;
import com.xlythe.textmanager.text.util.EncodedStringValue;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.RetrieveConf;

import java.io.IOException;
import java.nio.charset.Charset;

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
  private static final String INPUT_PUSH_URI = "push_uri";
  private static final String INPUT_READ_PDU = "read_pdu";
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
    enqueuePush(context, new Data.Builder().putInt(INPUT_SUBSCRIPTION_ID, getSubscriptionId(intent)), pushData);
  }

  static void schedule(Context context, Text text) {
    Uri messageUri = Uri.withAppendedPath(Telephony.Mms.Inbox.CONTENT_URI, text.getId());
    enqueue(context, new Data.Builder()
        .putString(INPUT_MESSAGE_URI, messageUri.toString())
        .putInt(INPUT_SUBSCRIPTION_ID, text.getSubscriptionId())
        .build());
  }

  static void scheduleReadReport(Context context, Text message, boolean deletedUnread) {
    if (!message.isIncoming() || !message.isMms() || message.getIdAsLong() < 0)
      throw new IllegalArgumentException("Expected a stored incoming MMS");
    TextManager.MmsDetails details = TextManager.getInstance(context).getMmsDetails(message);
    if (details == null || !details.readReportRequested || TextUtils.isEmpty(details.messageId)) return;
    Uri addresses = Uri.parse("content://mms/" + message.getId() + "/addr");
    String[] columns = {Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.CHARSET};
    try (Cursor rows = context.getContentResolver().query(addresses, columns, Telephony.Mms.Addr.TYPE + "=?",
        new String[]{Integer.toString(PduHeaders.FROM)}, null)) {
      if (rows == null || !rows.moveToFirst() || TextUtils.isEmpty(rows.getString(0))) return;
      EncodedStringValue to = new EncodedStringValue(rows.getInt(1), PduPersister.getBytes(rows.getString(0)));
      int status = deletedUnread ? PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ : PduHeaders.READ_STATUS_READ;
      ReadRecInd report = new ReadRecInd(new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR),
          PduPersister.getBytes(details.messageId), PduHeaders.CURRENT_MMS_VERSION, status, new EncodedStringValue[]{to});
      report.setDate(System.currentTimeMillis() / 1000L);
      byte[] bytes = new PduComposer(context, report).make();
      if (bytes != null) enqueue(context, new Data.Builder().putByteArray(INPUT_READ_PDU, bytes)
          .putInt(INPUT_SUBSCRIPTION_ID, message.getSubscriptionId()).build());
    }
  }

  private static void enqueuePush(Context context, Data.Builder input, byte[] bytes) {
    Data data;
    try {
      data = input.putByteArray(INPUT_NOTIFICATION_PDU, bytes).build();
    } catch (IllegalStateException oversized) {
      try {
        Uri file = MmsPduStore.write(context, bytes);
        data = input.putByteArray(INPUT_NOTIFICATION_PDU, null).putString(INPUT_PUSH_URI, file.toString()).build();
      } catch (IOException e) {
        Log.e(TAG, "Unable to queue the MMS push", e);
        return;
      }
    }
    enqueue(context, data);
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
    byte[] read = getInputData().getByteArray(INPUT_READ_PDU);
    if (read != null) {
      return TelephonyPermissions.canSendSms(getApplicationContext())
          && sendResponse(getApplicationContext(), read, getInputData().getInt(INPUT_SUBSCRIPTION_ID, -1))
          ? Result.success() : Result.failure();
    }
    int resultCode = getInputData().getInt(INPUT_RESULT_CODE, NO_RESULT);
    Uri pushFile = parseUri(getInputData().getString(INPUT_PUSH_URI));
    try {
      byte[] push = pushFile == null ? getInputData().getByteArray(INPUT_NOTIFICATION_PDU)
          : MmsPduStore.read(getApplicationContext(), pushFile);
      return resultCode == NO_RESULT ? dispatchDownload(push) : persistDownload(resultCode, push);
    } catch (IOException e) {
      Log.e(TAG, "Unable to read queued MMS push", e);
      return Result.failure();
    } finally {
      if (pushFile != null) MmsPduStore.delete(getApplicationContext(), pushFile);
    }
  }

  private Result dispatchDownload(byte[] notificationPdu) {
    Context context = getApplicationContext();
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
      GenericPdu push = parsePush(notificationPdu);
      if (broadcastReport(context, push, subscriptionId)) return Result.success();
      NotificationInd notification = push instanceof NotificationInd ? (NotificationInd) push : null;
      if (notification == null || notification.getContentLocation() == null) {
        return Result.failure();
      }
      location = new String(notification.getContentLocation(), Charset.forName("ISO-8859-1"));
    } else {
      location = getContentLocation(context, messageUri);
    }
    if (TextUtils.isEmpty(location)) {
      markAsFailed(context, notificationPdu, messageUri);
      return Result.failure();
    }

    Uri rawPduUri = null;
    try {
      if (Build.VERSION.SDK_INT < 21) {
        rawPduUri = MmsPduStore.write(context, LegacyMmsTransport.transfer(context, location, null));
        return persistDownload(Activity.RESULT_OK, rawPduUri, notificationPdu, messageUri, subscriptionId);
      }
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

  private Result persistDownload(int resultCode, byte[] notificationPdu) {
    Uri rawPduUri = parseUri(getInputData().getString(INPUT_RAW_PDU_URI));
    Uri messageUri = parseUri(getInputData().getString(INPUT_MESSAGE_URI));
    int subscriptionId = getInputData().getInt(
        INPUT_SUBSCRIPTION_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);

    return persistDownload(resultCode, rawPduUri, notificationPdu, messageUri, subscriptionId);
  }

  private Result persistDownload(int resultCode, Uri rawPduUri, byte[] notificationPdu, Uri messageUri, int subscriptionId) {
    Context context = getApplicationContext();
    try {
      if (resultCode != Activity.RESULT_OK || rawPduUri == null) {
        Log.e(TAG, "MMS download failed with result " + resultCode);
        markAsFailed(context, notificationPdu, messageUri);
        return Result.failure();
      }

      GenericPdu parsed = new PduParser(MmsPduStore.read(context, rawPduUri), true).parse();
      if (!(parsed instanceof RetrieveConf) || (((RetrieveConf) parsed).getRetrieveStatus() != 0
          && ((RetrieveConf) parsed).getRetrieveStatus() != PduHeaders.RETRIEVE_STATUS_OK)) {
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
      if (Build.VERSION.SDK_INT >= 22 && subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
        values.put(Telephony.Mms.SUBSCRIPTION_ID, subscriptionId);
      }
      context.getContentResolver().update(persisted, values, null, null);

      NotificationInd notice = parseNotification(notificationPdu);
      GenericPdu response = null;
      if (notice != null) {
        response = new NotifyRespInd(notice.getMmsVersion(), notice.getTransactionId(), PduHeaders.STATUS_RETRIEVED);
      } else if (((RetrieveConf) parsed).getTransactionId() != null) {
        response = new AcknowledgeInd(parsed.getMmsVersion(), ((RetrieveConf) parsed).getTransactionId());
      }
      if (response != null) sendResponse(context, new PduComposer(context, response).make(), subscriptionId);
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

  private static boolean sendResponse(Context context, byte[] bytes, int subscription) {
    Uri file = null;
    try {
      if (bytes == null) throw new IOException("Unable to compose retrieval response");
      if (Build.VERSION.SDK_INT < 21) {
        LegacyMmsTransport.sendResponse(context, bytes);
        return true;
      }
      file = MmsPduStore.write(context, bytes);
      Intent sent = new Intent(context, ResponseSentReceiver.class).setData(file);
      PendingIntent callback = PendingIntent.getBroadcast(context, 0, sent,
          PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
      SendService.resolveSmsManager(subscription).sendMultimediaMessage(context, file, null, null, callback);
      return true;
    } catch (RuntimeException | IOException e) {
      if (file != null) MmsPduStore.delete(context, file);
      Log.w(TAG, "Could not dispatch MMS control message", e);
      return false;
    }
  }

  public static final class ResponseSentReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
      if (intent.getData() != null) MmsPduStore.delete(context, intent.getData());
      if (getResultCode() != Activity.RESULT_OK) Log.w(TAG, "Carrier rejected MMS retrieval response");
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

  private static boolean broadcastReport(Context context, GenericPdu pdu, int subscriptionId) {
    byte[] id;
    int status;
    long date;
    EncodedStringValue[] parties;
    if (pdu instanceof DeliveryInd) {
      DeliveryInd delivery = (DeliveryInd) pdu;
      id = delivery.getMessageId();
      status = delivery.getStatus();
      date = delivery.getDate();
      parties = delivery.getTo();
    } else if (pdu instanceof ReadOrigInd) {
      ReadOrigInd read = (ReadOrigInd) pdu;
      id = read.getMessageId();
      status = read.getReadStatus();
      date = read.getDate();
      parties = new EncodedStringValue[]{read.getFrom()};
    } else {
      return false;
    }
    String[] addresses = new String[parties.length];
    for (int i = 0; i < parties.length; i++) addresses[i] = parties[i].getString();
    context.sendBroadcast(new Intent(TextReceiver.ACTION_MMS_REPORT).setPackage(context.getPackageName())
        .putExtra("message_id", new String(id, Charset.forName("ISO-8859-1")))
        .putExtra("message_type", pdu.getMessageType()).putExtra("status", status)
        .putExtra("addresses", addresses).putExtra("date_seconds", date)
        .putExtra(INPUT_SUBSCRIPTION_ID, subscriptionId));
    return true;
  }

  private static NotificationInd parseNotification(byte[] data) {
    GenericPdu pdu = parsePush(data);
    return pdu instanceof NotificationInd ? (NotificationInd) pdu : null;
  }

  private static GenericPdu parsePush(byte[] data) {
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
    return parsed;
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
        NotificationInd parsed = parseNotification(notificationPdu);
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

  static int getSubscriptionId(Intent intent) {
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
      Data.Builder data = new Data.Builder()
          .putString(INPUT_RAW_PDU_URI, rawPduUri.toString())
          .putString(INPUT_MESSAGE_URI, intent.getStringExtra(INPUT_MESSAGE_URI))
          .putInt(INPUT_SUBSCRIPTION_ID, intent.getIntExtra(
              INPUT_SUBSCRIPTION_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID))
          .putInt(INPUT_RESULT_CODE, getResultCode());
      enqueuePush(context, data, intent.getByteArrayExtra(INPUT_NOTIFICATION_PDU));
    }
  }
}
