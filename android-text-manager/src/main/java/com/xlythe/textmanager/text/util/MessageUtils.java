package com.xlythe.textmanager.text.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.util.Log;

import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.ImageAttachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.VideoAttachment;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class MessageUtils {
    private static final String TAG = MessageUtils.class.getSimpleName();
    private static final String SMS_BODY = "sms_body";
    private static final String ADDRESS = "address";

    @Nullable
    public static Text parse(Context context, Intent intent) {
        if (intent == null) {
            return null;
        }

        String[] recipients = getRecipients(intent);
        String message = getBody(intent);
        Attachment attachment = getAttachment(intent);

        if (recipients == null) {
            Log.w(TAG, "Parsing intent, but found no recipients");
            return null;
        }

        if (TextUtils.isEmpty(message) && attachment == null) {
            Log.w(TAG, "Parsing intent, but found no message");
            return null;
        }

        return new Text.Builder()
                .message(message)
                .addRecipients(context, recipients)
                .attach(attachment)
                .build();
    }

    public static String[] getRecipients(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            String recipients = uri.getSchemeSpecificPart();
            final int pos = recipients.indexOf('?');
            if (pos != -1) {
                recipients = recipients.substring(0, pos);
            }
            recipients = replaceUnicodeDigits(recipients).replace(',', ';');

            if (!recipients.isEmpty()) {
                return TextUtils.split(recipients, ";");
            }
        }

        if (intent.hasExtra(Intent.EXTRA_PHONE_NUMBER)) {
            return intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER).split(";");
        }

        final boolean haveAddress = !TextUtils.isEmpty(intent.getStringExtra(ADDRESS));
        final boolean haveEmail = !TextUtils.isEmpty(intent.getStringExtra(Intent.EXTRA_EMAIL));

        if (haveAddress) {
            return new String[] { intent.getStringExtra(ADDRESS) };
        }

        if (haveEmail) {
            return new String[] { intent.getStringExtra(Intent.EXTRA_EMAIL) };
        }

        return null;
    }

    // This function was lifted from Telephony.PhoneNumberUtils because it was @hide
    /**
     * Replace arabic/unicode digits with decimal digits.
     * @param number
     *            the number to be normalized.
     * @return the replaced number.
     */
    private static String replaceUnicodeDigits(final String number) {
        final StringBuilder normalizedDigits = new StringBuilder(number.length());
        for (final char c : number.toCharArray()) {
            final int digit = Character.digit(c, 10);
            if (digit != -1) {
                normalizedDigits.append(digit);
            } else {
                normalizedDigits.append(c);
            }
        }
        return normalizedDigits.toString();
    }

    public static String getBody(Intent intent) {
        // Try grabbing the body from the intent
        String message = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(message)) {
            return message;
        }

        // Maybe they used a remote input?
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            final CharSequence extra = remoteInput.getCharSequence(Intent.EXTRA_TEXT);
            if (extra != null) {
                message = extra.toString();
            }
        }
        if (!TextUtils.isEmpty(message)) {
            return message;
        }

        // Ok, lets see if its part of the uri
        Uri uri = intent.getData();
        if (uri == null) {
            return null;
        }
        String urlStr = uri.getSchemeSpecificPart();
        if (urlStr.contains("?")) {
            urlStr = urlStr.substring(urlStr.indexOf('?') + 1);
            final String[] params = urlStr.split("&");
            for (final String p : params) {
                if (p.startsWith("body=")) {
                    try {
                        message = URLDecoder.decode(p.substring(5), "UTF-8");
                    } catch (final UnsupportedEncodingException e) {
                        // Invalid URL, ignore
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(message)) {
            return message;
        }

        // Fine, lets see if they used SMS_BODY
        message = intent.getStringExtra(SMS_BODY);
        if (!TextUtils.isEmpty(message)) {
            return message;
        }

        // I give up. I tried.
        return null;
    }

    public static Attachment getAttachment(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String type = intent.getType();

        if (uri == null) {
            uri = intent.getData();
        }

        if (uri == null || type == null) {
            return null;
        }

        if (type.startsWith("image")) {
            return new ImageAttachment(uri);
        }

        if (type.startsWith("video")) {
            return new VideoAttachment(uri);
        }

        return null;
    }
}
