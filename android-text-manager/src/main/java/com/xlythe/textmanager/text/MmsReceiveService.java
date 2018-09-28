package com.xlythe.textmanager.text;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.xlythe.textmanager.text.Mock.Telephony;
import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.GenericPdu;
import com.xlythe.textmanager.text.pdu.NotificationInd;
import com.xlythe.textmanager.text.pdu.PduParser;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.RetrieveConf;

import androidx.annotation.WorkerThread;

public class MmsReceiveService extends IntentService {
  private static final String TAG = MmsReceiveService.class.getSimpleName();
  private static final String EXTRA_TEXT = "text";

  private static final String TAG_MMS = "sms:mms";
  private static final long TIMEOUT_MMS = 5000;

  static void schedule(Context context, Intent intent) {
    intent.setComponent(new ComponentName(context, SendService.class));
    context.startService(intent);
  }

  public MmsReceiveService() {
    super("MmsReceiveService");
  }

  @WorkerThread
  @Override
  protected void onHandleIntent(Intent intent) {
    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_MMS);

    try {
      wakelock.acquire(TIMEOUT_MMS);

      byte[] pushData = intent.getByteArrayExtra("data");

      final PduParser parser = new PduParser(pushData, true);
      final GenericPdu pdu = parser.parse();
      NotificationInd notif = (NotificationInd) pdu;
      if (pdu == null) {
        Log.e(TAG, "Invalid PUSH data");
        return;
      }

      byte[] location = notif.getContentLocation();
      final String loc = new String (location);

      if (!Network.forceDataConnection(this)) {
        markAsFailed(pdu);
        return;
      }

      byte[] data = Receive.receive(this, loc);
      if (data == null) {
        markAsFailed(pdu);
        return;
      }

      RetrieveConf retrieveConf = (RetrieveConf) new PduParser(data, true).parse();
      PduPersister persister = PduPersister.getPduPersister(this);
      Uri msgUri;
      try {
        msgUri = persister.persist(retrieveConf, Mock.Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
      } catch (MmsException e) {
        markAsFailed(pdu);
        return;
      }

      // Use local time instead of PDU time
      ContentValues values = new ContentValues(1);
      values.put(Mock.Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
      getContentResolver().update(msgUri, values, null, null);

      Cursor textCursor = getContentResolver().query(msgUri, null, null, null, null);
      if (textCursor == null) {
        markAsFailed(pdu);
        return;
      }
      textCursor.moveToFirst();

      final String[] mmsProjection = new String[]{
              BaseColumns._ID,
              Mock.Telephony.Mms.Part.CONTENT_TYPE,
              Mock.Telephony.Mms.Part.TEXT,
              Mock.Telephony.Mms.Part._DATA,
              Mock.Telephony.Mms.Part.MSG_ID
      };
      Uri mmsUri = Uri.withAppendedPath(Mock.Telephony.Mms.CONTENT_URI, "/part");
      Cursor mmsCursor = getContentResolver().query(mmsUri, mmsProjection, null, null, null);
      if (mmsCursor == null) {
        textCursor.close();
        markAsFailed(pdu);
        return;
      }

      mmsCursor.moveToFirst();

      Text text = new Text(textCursor, mmsCursor);
      textCursor.close();
      mmsCursor.close();
      broadcastMmsDownloaded(text);
    } finally {
      wakelock.release();
    }
  }

  private void markAsFailed(GenericPdu pdu) {
    try {
      PduPersister p = PduPersister.getPduPersister(this);
      Uri uri = p.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
      ContentValues values = new ContentValues(2);
      values.put(Telephony.Mms.STATUS, Telephony.Sms.Sent.STATUS_FAILED);
      values.put(Telephony.Mms.DATE, System.currentTimeMillis());
      getContentResolver().update(uri, values, null, null);
    } catch (MmsException e) {
      Log.e(TAG, "Persisting pdu failed", e);
    }
  }

  private void broadcastMmsDownloaded(Text text) {
    Intent intent = new Intent(TextReceiver.ACTION_TEXT_RECEIVED);
    intent.setPackage(getPackageName());
    intent.putExtra(EXTRA_TEXT, text);
    sendBroadcast(intent);
  }
}
