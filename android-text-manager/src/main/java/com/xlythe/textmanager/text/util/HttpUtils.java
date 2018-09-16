package com.xlythe.textmanager.text.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.HttpRequest;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.conn.params.ConnRouteParams;
import cz.msebera.android.httpclient.params.HttpConnectionParams;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.params.HttpProtocolParams;

public class HttpUtils {
    private static final String DEFAULT_HTTP_KEY_X_WAP_PROFILE = "x-wap-profile";
    private static final String DEFAULT_USER_AGENT = "Android-Mms/2.0";

    /**
     * Whether to hide MMS functionality from the user (i.e. SMS only).
     */
    private static final int MAX_MESSAGE_SIZE = 300 * 1024;            // default to 300k max size
    private static final String USER_AGENT = DEFAULT_USER_AGENT;
    private static final String UA_PROF_TAG_NAME = DEFAULT_HTTP_KEY_X_WAP_PROFILE;
    private static final int HTTP_SOCKET_TIMEOUT = 60*1000;            // default to 1 min

    public static final int HTTP_POST_METHOD = 1;
    public static final int HTTP_GET_METHOD = 2;

    // This is the value to use for the "Accept-Language" header.
    // Once it becomes possible for the user to change the locale
    // setting, this should no longer be static.  We should call
    // getHttpAcceptLanguage instead.
    private static final String HDR_VALUE_ACCEPT_LANGUAGE;

    static {
        HDR_VALUE_ACCEPT_LANGUAGE = getCurrentAcceptLanguage(Locale.getDefault());
    }

    // Definition for necessary HTTP headers.
    private static final String HDR_KEY_ACCEPT = "Accept";
    private static final String HDR_KEY_ACCEPT_LANGUAGE = "Accept-Language";

    private static final String HDR_VALUE_ACCEPT =
            "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic";

    private HttpUtils() {
        // To forbidden instantiate this class.
    }

    /**
     * Execute an MMS HTTP request, either a POST (sending) or a GET (downloading)
     *
     * @param url The request URL, for sending it is usually the MMSC, and for downloading
     *                  it is the message URL
     * @param pdu For POST (sending) only, the PDU to send
     * @param method HTTP method, POST for sending and GET for downloading
     * @param isProxySet Is there a proxy for the MMSC
     * @param proxyHost The proxy host
     * @param proxyPort The proxy port
     * @return The HTTP response body
     */
    public static byte[] httpConnection(Context context, long token,
                                           String url, byte[] pdu, int method, boolean isProxySet,
                                           String proxyHost, int proxyPort) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("URL must not be null.");
        }
        Log.v("HttpUtils", "httpConnection: params list");
        Log.v("HttpUtils", "\ttoken\t\t= " + token);
        Log.v("HttpUtils", "\turl\t\t= " + url);
        Log.v("HttpUtils", "\tmethod\t\t= "
                + ((method == HTTP_POST_METHOD) ? "POST"
                : ((method == HTTP_GET_METHOD) ? "GET" : "UNKNOWN")));
        Log.v("HttpUtils", "\tisProxySet\t= " + isProxySet);
        Log.v("HttpUtils", "\tproxyHost\t= " + proxyHost);
        Log.v("HttpUtils", "\tproxyPort\t= " + proxyPort);

        String mUaProfUrl = null;
        String mHttpParams = null;
        String mHttpParamsLine1Key = null;

        AndroidHttpClient client = null;

        try {
            // Make sure to use a proxy which supports CONNECT.
            URI hostUrl = new URI(url);
            HttpHost target = new HttpHost(hostUrl.getHost(), hostUrl.getPort(), HttpHost.DEFAULT_SCHEME_NAME);

            client = createHttpClient(context);
            HttpRequest req = null;
            switch(method) {
                case HTTP_POST_METHOD:
                    ProgressCallbackEntity entity = new ProgressCallbackEntity(context, token, pdu);
                    // Set request content type.
                    entity.setContentType("application/vnd.wap.mms-message");

                    HttpPost post = new HttpPost(url);
                    post.setEntity(entity);
                    req = post;
                    break;
                case HTTP_GET_METHOD:
                    req = new HttpGet(url);
                    break;
                default:
                    Log.e("HttpUtils", "Unknown HTTP method: " + method + ". Must be one of POST[" + HTTP_POST_METHOD
                            + "] or GET[" + HTTP_GET_METHOD + "].");
                    return null;
            }

            // Set route parameters for the request.
            HttpParams params = client.getParams();
            if (isProxySet) {
                ConnRouteParams.setDefaultProxy(
                        params, new HttpHost(proxyHost, proxyPort));
            }
            req.setParams(params);

            // Set necessary HTTP headers for MMS transmission.
            req.addHeader(HDR_KEY_ACCEPT, HDR_VALUE_ACCEPT);
            {
                String xWapProfileTagName = UA_PROF_TAG_NAME;
                String xWapProfileUrl = mUaProfUrl;

                if (xWapProfileUrl != null) {
                    Log.d("HttpUtils", "[HttpUtils] httpConn: xWapProfUrl=" + xWapProfileUrl);
                    req.addHeader(xWapProfileTagName, xWapProfileUrl);
                }
            }

            // Extra http parameters. Split by '|' to get a list of value pairs.
            // Separate each pair by the first occurrence of ':' to obtain a name and
            // value. Replace the occurrence of the string returned by
            // MmsConfig.getHttpParamsLine1Key() with the users telephone number inside
            // the value.
            String extraHttpParams = mHttpParams;

            if (extraHttpParams != null) {
                String line1Number = ((TelephonyManager)context
                        .getSystemService(Context.TELEPHONY_SERVICE))
                        .getLine1Number();
                String line1Key = mHttpParamsLine1Key;
                String paramList[] = extraHttpParams.split("\\|");

                for (String paramPair : paramList) {
                    String splitPair[] = paramPair.split(":", 2);

                    if (splitPair.length == 2) {
                        String name = splitPair[0].trim();
                        String value = splitPair[1].trim();

                        if (line1Key != null) {
                            value = value.replace(line1Key, line1Number);
                        }
                        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                            req.addHeader(name, value);
                        }
                    }
                }
            }
            req.addHeader(HDR_KEY_ACCEPT_LANGUAGE, HDR_VALUE_ACCEPT_LANGUAGE);

            HttpResponse response = client.execute(target, req);
            StatusLine status = response.getStatusLine();
            Log.d("HttpUtils",status+"");
            if (status.getStatusCode() == 302) {
                for (int i = 0; i<response.getAllHeaders().length; i++) {
                    Log.d("httputils", response.getAllHeaders()[i].toString());
                }
            }

            // HTTP 200 is success.
            if (status.getStatusCode() != 200 && status.getStatusCode() != 201) {
                throw new IOException(String.format(Locale.ENGLISH, "HTTP error[%d]: %s", status.getStatusCode(), status.getReasonPhrase()));
            }

            HttpEntity entity = response.getEntity();
            byte[] body = null;
            if (entity != null) {
                try {
                    if (entity.getContentLength() > 0) {
                        body = new byte[(int) entity.getContentLength()];
                        DataInputStream dis = new DataInputStream(entity.getContent());
                        try {
                            dis.readFully(body);
                        } finally {
                            try {
                                dis.close();
                            } catch (IOException e) {
                                Log.e("HttpUtils", "Error closing input stream: " + e.getMessage());
                            }
                        }
                    }
                    if (entity.isChunked()) {
                        Log.v("HttpUtils", "httpConnection: transfer encoding is chunked");
                        int bytesTobeRead = MAX_MESSAGE_SIZE;
                        byte[] tempBody = new byte[bytesTobeRead];
                        DataInputStream dis = new DataInputStream(entity.getContent());
                        try {
                            int bytesRead = 0;
                            int offset = 0;
                            boolean readError = false;
                            do {
                                try {
                                    bytesRead = dis.read(tempBody, offset, bytesTobeRead);
                                } catch (IOException e) {
                                    readError = true;
                                    Log.e("HttpUtils", "httpConnection: error reading input stream"
                                            + e.getMessage());
                                    break;
                                }
                                if (bytesRead > 0) {
                                    bytesTobeRead -= bytesRead;
                                    offset += bytesRead;
                                }
                            } while (bytesRead >= 0 && bytesTobeRead > 0);
                            if (bytesRead == -1 && offset > 0 && !readError) {
                                // offset is same as total number of bytes read
                                // bytesRead will be -1 if the data was read till the eof
                                body = new byte[offset];
                                System.arraycopy(tempBody, 0, body, 0, offset);
                                Log.v("HttpUtils", "httpConnection: Chunked response length ["
                                        + Integer.toString(offset) + "]");
                            } else {
                                Log.e("HttpUtils", "httpConnection: Response entity too large or empty");
                            }
                        } finally {
                            try {
                                dis.close();
                            } catch (IOException e) {
                                Log.e("HttpUtils", "Error closing input stream: " + e.getMessage());
                            }
                        }
                    }
                } finally {
                    entity.consumeContent();
                }
            }
            return body;
        } catch (Exception e) {
            handleHttpConnectionException(e, url);
        }
        finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }

    private static void handleHttpConnectionException(Exception exception, String url)
            throws IOException {
        // Inner exception should be logged to make life easier.
        Log.e("HttpUtils", "Url: " + url + "\n" + exception.getMessage(), exception);
        throw new IOException(exception);
    }

    private static AndroidHttpClient createHttpClient(Context context) {
        String userAgent = USER_AGENT;
        AndroidHttpClient client = AndroidHttpClient.newInstance(userAgent, context);
        HttpParams params = client.getParams();
        HttpProtocolParams.setContentCharset(params, "UTF-8");

        // set the socket timeout
        int soTimeout = HTTP_SOCKET_TIMEOUT;

        Log.d("HttpUtils", "[HttpUtils] createHttpClient w/ socket timeout " + soTimeout + " ms, "
                + ", UA=" + userAgent);
        HttpConnectionParams.setSoTimeout(params, soTimeout);
        return client;
    }

    private static final String ACCEPT_LANG_FOR_US_LOCALE = "en-US";

    /**
     * Return the Accept-Language header.  Use the current locale plus
     * US if we are in a different locale than US.
     * This code copied from the browser's WebSettings.java
     * @return Current AcceptLanguage String.
     */
    private static String getCurrentAcceptLanguage(Locale locale) {
        StringBuilder buffer = new StringBuilder();
        addLocaleToHttpAcceptLanguage(buffer, locale);

        if (!Locale.US.equals(locale)) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append(ACCEPT_LANG_FOR_US_LOCALE);
        }

        return buffer.toString();
    }

    /**
     * Convert obsolete language codes, including Hebrew/Indonesian/Yiddish,
     * to new standard.
     */
    private static String convertObsoleteLanguageCodeToNew(String langCode) {
        if (langCode == null) {
            return null;
        }
        if ("iw".equals(langCode)) {
            // Hebrew
            return "he";
        } else if ("in".equals(langCode)) {
            // Indonesian
            return "id";
        } else if ("ji".equals(langCode)) {
            // Yiddish
            return "yi";
        }
        return langCode;
    }

    private static void addLocaleToHttpAcceptLanguage(StringBuilder builder, Locale locale) {
        String language = convertObsoleteLanguageCodeToNew(locale.getLanguage());
        if (language != null) {
            builder.append(language);
            String country = locale.getCountry();
            if (country != null) {
                builder.append("-");
                builder.append(country);
            }
        }
    }
}
