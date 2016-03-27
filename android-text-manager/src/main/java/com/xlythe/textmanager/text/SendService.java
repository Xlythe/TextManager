package com.xlythe.textmanager.text;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.xlythe.textmanager.text.exception.MmsException;
import com.xlythe.textmanager.text.pdu.PduBody;
import com.xlythe.textmanager.text.pdu.PduComposer;
import com.xlythe.textmanager.text.pdu.PduPart;
import com.xlythe.textmanager.text.pdu.PduPersister;
import com.xlythe.textmanager.text.pdu.SendReq;
import com.xlythe.textmanager.text.smil.SmilHelper;
import com.xlythe.textmanager.text.smil.SmilXmlSerializer;
import com.xlythe.textmanager.text.util.ApnDefaults;
import com.xlythe.textmanager.text.util.CharacterSets;
import com.xlythe.textmanager.text.util.ContentType;
import com.xlythe.textmanager.text.util.EncodedStringValue;
import com.xlythe.textmanager.text.util.HttpUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// TODO: Mark message as failed when service is killed
public class SendService extends IntentService {
    private static final String TAG = SendService.class.getSimpleName();
    private static final String URI_EXTRA = "uri_extra";
    private static final String PREAMBLE = "com.xlythe.textmanager.text.";
    private static final String SMS_SENT = PREAMBLE + "SMS_SENT";
    private static final String SMS_DELIVERED = PREAMBLE + "SMS_DELIVERED";
    private static final String MMS_SENT = PREAMBLE + "MMS_SENT";
    public static final String TEXT_EXTRA = "text_extra";

    public SendService() {
        super("SendService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Text text = intent.getParcelableExtra(TEXT_EXTRA);
        send(this, text);
    }

    private static void send(Context context, final Text text) {
        String address = "";

        if (!text.isMms()) {
            SmsManager sms = SmsManager.getDefault();
            address = text.getMembers().iterator().next().getNumber();
            ContentValues values = new ContentValues();
            Uri uri = Mock.Telephony.Sms.Sent.CONTENT_URI;
            values.put(Mock.Telephony.Sms.ADDRESS, address);
            values.put(Mock.Telephony.Sms.BODY, text.getBody());
            values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
            uri = context.getContentResolver().insert(uri, values);
            sms.sendTextMessage(address, null, text.getBody(), newSmsSentPendingIntent(context, uri), newSmsDeliveredPendingIntent(context));
        } else {
            Attachment attachment = text.getAttachment();
            for (Contact member : text.getMembers()) {
                if (!address.isEmpty()) {
                    address += ";";
                }
                address += member.getNumber();
            }
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                sendMediaMessage(context, address, " ", text.getBody(), Arrays.asList(new Attachment[]{attachment}), newMmsSentPendingIntent(context));
            }
        }
    }

    private static PendingIntent newSmsSentPendingIntent(Context context, Uri uri) {
        Intent intent = new Intent(SMS_SENT);
        intent.putExtra(URI_EXTRA, uri);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static PendingIntent newSmsDeliveredPendingIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(SMS_DELIVERED), 0);
    }

    private static PendingIntent newMmsSentPendingIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(MMS_SENT), 0);
    }

    @TargetApi(21)
    public static void sendMediaMessage(final Context context,
                                        final String address,
                                        final String subject,
                                        final String body,
                                        final List<Attachment> attachments,
                                        final PendingIntent sentMmsPendingIntent) {

        // Store the pending message in the database
        Set set = storeData(context, address, subject, body, attachments);

        // Collect the data we're going to send to the server
        final byte[] pdu = set.data;
        final Uri uri = set.messageUri;

        ContentValues values = new ContentValues();
        values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_PENDING);
        context.getContentResolver().update(uri, values, null, null);

        // Request a data connection
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        // Use a countdownlatch because this may never return, and we want to mark the MMS
        // as failed in that case.
        final CountDownLatch latch = new CountDownLatch(1);
        boolean success = false;
        Log.d(TAG, "Network callback");
        new java.lang.Thread(new Runnable() {
            public void run() {
                connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        latch.countDown();
                        ConnectivityManager.setProcessDefaultNetwork(network);
                        sendData(context, pdu, sentMmsPendingIntent, uri);
                        connectivityManager.unregisterNetworkCallback(this);
                    }
                });
            }
        }).start();
        try {
            success = latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!success) {
            Log.d(TAG, "Mark Failed");
            values = new ContentValues();
            values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
            context.getContentResolver().update(uri, values, null, null);
        }
    }

    public static Set storeData(final Context context,
                                final String address,
                                final String subject,
                                final String body,
                                final List<Attachment> attachments){
        ArrayList<MMSPart> data = new ArrayList<>();

        int i = 0;
        MMSPart part;
        for(Attachment attachment: attachments){
            Attachment.Type type = attachment.getType();
            Uri uri = attachment.getUri();
            switch(type) {
                case IMAGE:
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                        byte[] imageBytes = bitmapToByteArray(bitmap);
                        part = new MMSPart();
                        part.MimeType = "image/jpeg";
                        part.Name = "image" + i;
                        part.Data = imageBytes;
                        data.add(part);
                    } catch (IOException e) {
                        Log.e(TAG, "File not found", e);
                    }
                    break;
                case VIDEO:
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        FileInputStream fis = new FileInputStream(new File(uri.getPath()));

                        byte[] buf = new byte[1024];
                        int n;
                        while (-1 != (n = fis.read(buf)))
                            baos.write(buf, 0, n);

                        byte[] videoBytes = baos.toByteArray();

                        part = new MMSPart();
                        part.MimeType = "video/mpeg";
                        part.Name = "video" + i;
                        part.Data = videoBytes;
                        data.add(part);
                    } catch (FileNotFoundException e){
                        throw new RuntimeException("Uri doesn't exist", e);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    break;
                case VOICE:
                    //TODO: Voice support
                    break;
            }
            i++;
        }

        if (body != null && !body.isEmpty()) {
            // add text to the end of the part and send
            part = new MMSPart();
            part.Name = "text";
            part.MimeType = "text/plain";
            part.Data = body.getBytes();
            data.add(part);
        }

        return getBytes(context, address.split(" "), data.toArray(new MMSPart[data.size()]), subject);
    }

    public static void sendData(Context context, byte[] pdu, PendingIntent sentMmsPendingIntent, Uri uri) {
        try {
            ApnDefaults.ApnParameters apnParameters = ApnDefaults.getApnParameters(context);
            HttpUtils.httpConnection(
                    context, 4444L,
                    apnParameters.getMmscUrl(),
                    pdu,
                    HttpUtils.HTTP_POST_METHOD,
                    apnParameters.isProxySet(),
                    apnParameters.getProxyAddress(),
                    apnParameters.getProxyPort());
            notify(sentMmsPendingIntent, context, Activity.RESULT_OK, uri);
        } catch(IOException e){
            Log.e(TAG, "Failed to connect to the MMS server", e);
            notify(sentMmsPendingIntent, context, Activity.RESULT_CANCELED, uri);
        }
    }

    private static void notify(PendingIntent pendingIntent, Context context, int result, Uri uri) {
        try {
            Intent intent = new Intent();
            intent.putExtra(URI_EXTRA, uri.toString());
            pendingIntent.send(context, result, intent);
        } catch (PendingIntent.CanceledException ex) {
            Log.e(TAG, "Failed to notified mms sent", ex);
        }
    }

    public static Set getBytes(Context context, String[] recipients, MMSPart[] parts, String subject) {
        final SendReq sendRequest = new SendReq();
        // create send request addresses
        for (int i = 0; i < recipients.length; i++) {
            final EncodedStringValue[] phoneNumbers = EncodedStringValue.extract(recipients[i]);
            Log.d(TAG, recipients[i]);
            if (phoneNumbers != null && phoneNumbers.length > 0) {
                sendRequest.addTo(phoneNumbers[0]);
            }
        }
        if (subject != null) {
            sendRequest.setSubject(new EncodedStringValue(subject));
        }
        sendRequest.setDate(Calendar.getInstance().getTimeInMillis() / 1000L);
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        sendRequest.setFrom(new EncodedStringValue(manager.getLine1Number()));
        final PduBody pduBody = new PduBody();
        // assign parts to the pdu body which contains sending data
        long size = 0;
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                MMSPart part = parts[i];
                if (part != null) {
                    PduPart partPdu = new PduPart();
                    partPdu.setName(part.Name.getBytes());
                    partPdu.setContentType(part.MimeType.getBytes());
                    if (part.MimeType.startsWith("text")) {
                        partPdu.setCharset(CharacterSets.UTF_8);
                    }
                    partPdu.setData(part.Data);
                    pduBody.addPart(partPdu);
                    size += (part.Name.getBytes().length + part.MimeType.getBytes().length + part.Data.length);
                }
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(SmilHelper.createSmilDocument(pduBody), out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(ContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        pduBody.addPart(0, smilPart);
        sendRequest.setBody(pduBody);
        Log.d(TAG, "setting message size to " + size + " bytes");
        sendRequest.setMessageSize(size);

        PduPersister p = PduPersister.getPduPersister(context);
        Uri uri;
        try {
            uri = p.persist(sendRequest, Mock.Telephony.Mms.Sent.CONTENT_URI, true, true, null);
        } catch (MmsException e) {
            Log.e(TAG, "persisting pdu failed", e);
            uri = null;
        }
        // create byte array which will actually be sent
        final PduComposer composer = new PduComposer(context, sendRequest);
        final byte[] bytesToSend;
        bytesToSend = composer.make();
        return new Set(bytesToSend, uri);
    }

    public static class Set {
        byte [] data;
        Uri messageUri;

        Set(byte [] data, Uri messageUri) {
            this.data = data;
            this.messageUri = messageUri;
        }
    }

    public static byte[] bitmapToByteArray(Bitmap image) {
        if (image == null) {
            Log.v(TAG, "image is null, returning byte array of size 0");
            return new byte[0];
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }

    public static class MMSPart {
        public String Name = "";
        public String MimeType = "";
        public byte[] Data;
    }

    public static final class SmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String uri = intent.getStringExtra(URI_EXTRA);
            ContentValues values = new ContentValues();
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_COMPLETE);
                    break;
                default:
                    values.put(Mock.Telephony.Sms.Sent.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
                    break;
            }
            context.getContentResolver().update(Uri.parse(uri), values, null, null);
        }
    }

    public static final class SmsDeliveredReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Log.d(TAG, "SMS delivered");
                    break;
                case Activity.RESULT_CANCELED:
                    Log.d(TAG, "SMS not delivered");
                    break;
            }
        }
    }

    public static final class MmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String uri = intent.getStringExtra(URI_EXTRA);
            ContentValues values = new ContentValues();
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_COMPLETE);
                    break;
                default:
                    values.put(Mock.Telephony.Mms.STATUS, Mock.Telephony.Sms.Sent.STATUS_FAILED);
                    break;
            }
            context.getContentResolver().update(Uri.parse(uri), values, null, null);
        }
    }
}
