package com.xlythe.textmanager.text.mms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/** Carrier HTTP transport for API 14–20. These routing APIs were public on those releases. */
public final class LegacyMmsTransport {
    private static final int MMS_NETWORK = ConnectivityManager.TYPE_MOBILE_MMS;
    private static final int NETWORK_TIMEOUT_MS = 30_000;

    private LegacyMmsTransport() {}

    // The legacy connectivity service tracks enableMMS by process, not by individual request.
    @SuppressLint("MissingPermission") // Optional for clients. Checked before using the route.
    public static byte[] transfer(Context context, String location, byte[] outgoing) throws IOException {
        return transfer(context, location, outgoing, true);
    }

    public static void sendResponse(Context context, byte[] response) throws IOException {
        transfer(context, null, response, false);
    }

    @SuppressLint("MissingPermission")
    private static synchronized byte[] transfer(Context context, String location, byte[] outgoing, boolean readBody) throws IOException {
        if (outgoing != null && outgoing.length > MmsPduStore.MAX_PDU_BYTES) {
            throw new IOException("Oversized outgoing MMS");
        }
        if (context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            throw new IOException("Network state permission is required");
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) throw new IOException("No connectivity service");
        int status = (Integer) networkFeature(cm, "startUsingNetworkFeature");
        if (status != 0 && status != 1) throw new IOException("Carrier MMS network unavailable");
        try {
            long deadline = SystemClock.elapsedRealtime() + NETWORK_TIMEOUT_MS;
            NetworkInfo network;
            while ((network = cm.getNetworkInfo(MMS_NETWORK)) == null || !network.isConnected()) {
                if (SystemClock.elapsedRealtime() >= deadline) throw new IOException("MMS network timed out");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted waiting for MMS network", e);
                }
            }
            Carrier carrier = carrier(context, network.getExtraInfo());
            URL endpoint = carrier.endpoint(location);
            String nextHop = carrier.proxy == null ? endpoint.getHost() : carrier.proxy;
            InetAddress[] addresses = InetAddress.getAllByName(nextHop);
            for (InetAddress address : addresses) {
                if (!(address instanceof Inet4Address)) throw new IOException("Unsupported legacy MMS route");
                byte[] ip = address.getAddress();
                int route = (ip[0] & 255) | ((ip[1] & 255) << 8) | ((ip[2] & 255) << 16) | ((ip[3] & 255) << 24);
                if (!requestRoute(cm, route)) throw new IOException("Carrier MMS route unavailable");
            }
            Proxy proxy = carrier.proxy == null ? Proxy.NO_PROXY
                    : new Proxy(Proxy.Type.HTTP, new InetSocketAddress(addresses[0], carrier.port));
            return exchange(endpoint, proxy, outgoing, readBody);
        } finally {
            try {
                networkFeature(cm, "stopUsingNetworkFeature");
            } catch (IOException releaseFailure) {
                Log.w("TextManager", "Could not release MMS network", releaseFailure);
            }
        }
    }

    private static Object networkFeature(ConnectivityManager cm, String method) throws IOException {
        try {
            return ConnectivityManager.class.getMethod(method, int.class, String.class)
                    .invoke(cm, ConnectivityManager.TYPE_MOBILE, "enableMMS");
        } catch (Exception e) {
            throw new IOException("Legacy MMS network feature unavailable", e);
        }
    }

    private static boolean requestRoute(ConnectivityManager cm, int address) throws IOException {
        try {
            return (Boolean) ConnectivityManager.class.getMethod("requestRouteToHost", int.class, int.class)
                    .invoke(cm, MMS_NETWORK, address);
        } catch (Exception e) {
            throw new IOException("Legacy MMS routing unavailable", e);
        }
    }

    static byte[] exchange(URL endpoint, Proxy proxy, byte[] outgoing) throws IOException {
        return exchange(endpoint, proxy, outgoing, true);
    }

    private static byte[] exchange(URL endpoint, Proxy proxy, byte[] outgoing, boolean readBody) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection(proxy);
        try {
            connection.setConnectTimeout(NETWORK_TIMEOUT_MS);
            connection.setReadTimeout(NETWORK_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/vnd.wap.mms-message");
            if (outgoing != null) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(outgoing.length);
                connection.setRequestProperty("Content-Type", "application/vnd.wap.mms-message");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(outgoing);
                }
            }
            int status = connection.getResponseCode();
            if (!readBody && status >= 200 && status < 300) return new byte[0];
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("MMSC returned HTTP " + status);
            try (InputStream input = connection.getInputStream()) {
                return MmsPduStore.readBounded(input, outgoing == null ? MmsPduStore.MAX_PDU_BYTES : 64 * 1024);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static Carrier carrier(Context context, String apn) throws IOException {
        TelephonyManager phone = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String operator = phone == null ? null : phone.getSimOperator();
        if (operator == null || !operator.matches("[0-9]{5,6}") || apn == null || apn.isEmpty()) {
            throw new IOException("Missing SIM operator or active MMS APN");
        }
        Carrier selected = null;
        try (Cursor c = context.getContentResolver().query(Uri.parse("content://telephony/carriers"),
                new String[]{"type", "mmsc", "mmsproxy", "mmsport"}, "numeric=? AND apn=?",
                new String[]{operator, apn}, null)) {
            while (c != null && c.moveToNext()) {
                selected = select(selected, c.getString(0), c.getString(1), c.getString(2), c.getString(3));
            }
        } catch (SecurityException denied) {
            // Starting with Jelly Bean MR1, ordinary apps generally cannot read the APN provider.
        }
        if (selected != null) return selected;
        File config = new File(Environment.getRootDirectory(), "etc/apns-conf.xml");
        try (InputStream stream = new FileInputStream(config)) {
            return fromXml(stream, operator, apn);
        }
    }

    static Carrier fromXml(InputStream input, String operator, String apn) throws IOException {
        Carrier selected = null;
        try {
            XmlPullParser xml = Xml.newPullParser();
            xml.setInput(input, null);
            for (int event = xml.next(); event != XmlPullParser.END_DOCUMENT; event = xml.next()) {
                if (event != XmlPullParser.START_TAG || !"apn".equals(xml.getName())) continue;
                String numeric = xml.getAttributeValue(null, "mcc") + xml.getAttributeValue(null, "mnc");
                if (!operator.equals(numeric) || !apn.equalsIgnoreCase(xml.getAttributeValue(null, "apn"))) continue;
                selected = select(selected, xml.getAttributeValue(null, "type"), xml.getAttributeValue(null, "mmsc"),
                        xml.getAttributeValue(null, "mmsproxy"), xml.getAttributeValue(null, "mmsport"));
            }
        } catch (XmlPullParserException e) {
            throw new IOException("Invalid device carrier configuration", e);
        }
        if (selected == null) throw new IOException("No matching carrier MMS configuration");
        return selected;
    }

    private static Carrier select(Carrier previous, String types, String mmsc, String proxy, String port) throws IOException {
        if (mmsc == null || mmsc.isEmpty() || "null".equalsIgnoreCase(mmsc)) return previous;
        if (types != null && !types.trim().isEmpty()) {
            List<String> supported = Arrays.asList(types.trim().split("\\s*,\\s*"));
            if (!supported.contains("mms") && !supported.contains("*")) return previous;
        }
        Carrier found = new Carrier(mmsc, proxy, port);
        if (previous != null && !(previous.mmsc.toExternalForm().equals(found.mmsc.toExternalForm())
                && (previous.proxy == null ? found.proxy == null : previous.proxy.equals(found.proxy))
                && previous.port == found.port)) throw new IOException("Ambiguous carrier MMS configuration");
        return found;
    }

    static final class Carrier {
        final URL mmsc;
        final String proxy;
        final int port;

        Carrier(String mmsc, String proxy, String port) throws IOException {
            this.mmsc = httpUrl(mmsc);
            this.proxy = proxy == null || proxy.trim().isEmpty() ? null : proxy.trim();
            try {
                this.port = port == null || port.isEmpty() ? 80 : Integer.parseInt(port);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid MMS proxy port", e);
            }
            if (this.port <= 0 || this.port > 65535) throw new IOException("Invalid MMS proxy port");
        }

        URL endpoint(String location) throws IOException {
            if (location == null) return mmsc;
            URL download = httpUrl(location);
            if (proxy == null && !(mmsc.getHost().equalsIgnoreCase(download.getHost())
                    && mmsc.getProtocol().equals(download.getProtocol())
                    && effectivePort(mmsc) == effectivePort(download))) {
                throw new IOException("Download is outside the configured MMSC");
            }
            return download;
        }

        private static int effectivePort(URL url) { return url.getPort() < 0 ? url.getDefaultPort() : url.getPort(); }

        private static URL httpUrl(String value) throws IOException {
            URL url = new URL(value);
            if (!("http".equals(url.getProtocol()) || "https".equals(url.getProtocol()))
                    || url.getHost().isEmpty() || url.getUserInfo() != null || url.getRef() != null) {
                throw new IOException("Invalid carrier MMS URL");
            }
            return url;
        }
    }
}
