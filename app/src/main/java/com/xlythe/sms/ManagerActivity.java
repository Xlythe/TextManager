package com.xlythe.sms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.xlythe.textmanager.text.ApnDefaults;
import com.xlythe.textmanager.text.CharacterSets;
import com.xlythe.textmanager.text.ContentType;
import com.xlythe.textmanager.text.EncodedStringValue;
import com.xlythe.textmanager.text.HttpUtils;
import com.xlythe.textmanager.text.PduBody;
import com.xlythe.textmanager.text.PduComposer;
import com.xlythe.textmanager.text.PduPart;
import com.xlythe.textmanager.text.SendReq;
import com.xlythe.textmanager.text.smil.SmilHelper;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.smil.SmilXmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;

public class ManagerActivity extends Activity {
    private ThreadAdapter mThreadAdapter;
    private ImageButton mCompose;
    private ListView mListView;
    private TextManager mManager;

    // TODO: DELETE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public static byte[] getBytes(Context context, String[] recipients, MMSPart[] parts, String subject) {
        final SendReq sendRequest = new SendReq();
        // create send request addresses
        for (int i = 0; i < recipients.length; i++) {
            final EncodedStringValue[] phoneNumbers = EncodedStringValue.extract(recipients[i]);
            Log.d("send", recipients[i] + "");
            if (phoneNumbers != null && phoneNumbers.length > 0) {
                sendRequest.addTo(phoneNumbers[0]);
            }
        }
        if (subject != null) {
            sendRequest.setSubject(new EncodedStringValue(subject));
        }
        sendRequest.setDate(Calendar.getInstance().getTimeInMillis() / 1000L);
        try {
            sendRequest.setFrom(new EncodedStringValue("2163138473"));
        } catch (Exception e) {
            Log.d("bad number","what the fuck");
        }
        final PduBody pduBody = new PduBody();
        // assign parts to the pdu body which contains sending data
        long size = 0;
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                MMSPart part = parts[i];
                if (part != null) {
                    try {
                        PduPart partPdu = new PduPart();
                        partPdu.setName(part.Name.getBytes());
                        partPdu.setContentType(part.MimeType.getBytes());
                        if (part.MimeType.startsWith("text")) {
                            partPdu.setCharset(CharacterSets.UTF_8);
                        }
                        partPdu.setData(part.Data);
                        pduBody.addPart(partPdu);
                        size += (part.Name.getBytes().length + part.MimeType.getBytes().length + part.Data.length);
                    } catch (Exception e) {
                        Log.d("bad part","what the fuck");
                    }
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
        Log.d("send", "setting message size to " + size + " bytes");
        sendRequest.setMessageSize(size);
        // create byte array which will actually be sent
        final PduComposer composer = new PduComposer(context, sendRequest);
        final byte[] bytesToSend;
        bytesToSend = composer.make();
        return bytesToSend;
    }

    public class MMSPart {
        public String Name = "";
        public String MimeType = "";
        public byte[] Data;
        public Uri Path;
    }
    // TODO: DELETE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        mManager = TextManager.getInstance(getBaseContext());
        mCompose = (ImageButton) findViewById(R.id.compose);
        mListView = (ListView) findViewById(R.id.listView);

        // Start a new Message.
        mCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent i = new Intent(getBaseContext(), ComposeActivity.class);
//                startActivity(i);
                new Thread(new Runnable() {
                    public void run() {
                        String text = "FUCKING WORKS BITCH - no images yet haha";
                        String address = "4132828427";
                        String subject = "im pro";
                        address = address.trim();

                        ArrayList<MMSPart> data = new ArrayList<>();

                        MMSPart part = new MMSPart();
                        part.Name = "text";
                        part.MimeType = "text/plain";
                        part.Data = text.getBytes();
                        data.add(part);

                        byte[] pdu = getBytes(getBaseContext(), address.split(" "), data.toArray(new MMSPart[data.size()]), subject);

                        Log.d("bytes", new String(pdu, StandardCharsets.UTF_8));

                        try {
                            ApnDefaults.ApnParameters apnParameters = ApnDefaults.getApnParameters(getBaseContext());
                            //Utils.ensureRouteToHost(context, apns.MMSCenterUrl, apns.MMSProxy);
                            HttpUtils.httpConnection(
                                    getBaseContext(), 4444L,
                                    apnParameters.getMmscUrl(), pdu, HttpUtils.HTTP_POST_METHOD,
                                    apnParameters.isProxySet(),
                                    apnParameters.getProxyAddress(),
                                    apnParameters.getProxyPort());
                        } catch (IOException ioe) {
                            Log.d("in","what the fuck");
                        }
                    }
                }).start();
            }
        });

        // Start Thread Activity.
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                Intent i = new Intent(getBaseContext(), ThreadActivity.class);
                i.putExtra(ThreadActivity.EXTRA_THREAD_ID, ((com.xlythe.textmanager.text.Thread) av.getItemAtPosition(position)).getThreadId());
                i.putExtra(ThreadActivity.EXTRA_ADDRESS, mManager.getSender((com.xlythe.textmanager.text.Thread) av.getItemAtPosition(position)).getDisplayName());
                i.putExtra(ThreadActivity.EXTRA_NUMBER, ((com.xlythe.textmanager.text.Thread) av.getItemAtPosition(position)).getAddress());
                startActivity(i);
            }
        });

        // Populate Adapter with list of threads.
        mThreadAdapter = new ThreadAdapter(getBaseContext(), R.layout.list_item_threads, mManager.getThreads());
        mListView.setAdapter(mThreadAdapter);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}