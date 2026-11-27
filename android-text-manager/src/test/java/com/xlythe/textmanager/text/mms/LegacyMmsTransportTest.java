package com.xlythe.textmanager.text.mms;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class LegacyMmsTransportTest {
    private static final String MATCH = "<apn mcc='310' mnc='260' apn='mobile' type='default,mms' " +
            "mmsc='http://carrier.test/mms' mmsproxy='192.0.2.7' mmsport='8080'/>";
    private static LegacyMmsTransport.Carrier carrier(String rows) throws IOException {
        return LegacyMmsTransport.fromXml(new ByteArrayInputStream(("<apns>" + rows + "</apns>")
                .getBytes(StandardCharsets.UTF_8)), "310260", "mobile");
    }

    @Test public void selectsOnlyAnUnambiguousCarrierAndActiveMmsApn() throws Exception {
        LegacyMmsTransport.Carrier apn = carrier(MATCH.replace("mobile", "other")
                + MATCH.replace("mnc='260'", "mnc='001'") + MATCH);
        assertEquals("http://carrier.test/mms", apn.endpoint(null).toString());
        assertEquals("192.0.2.7", apn.proxy);
        assertEquals(8080, apn.port);
        assertThrows(IOException.class, () -> carrier(MATCH.replace("default,mms", "default")));
        assertThrows(IOException.class, () -> carrier(MATCH + MATCH.replace("carrier.test", "other.test")));
    }

    @Test public void directDownloadsStayOnTheConfiguredCarrierOrigin() throws Exception {
        LegacyMmsTransport.Carrier apn = new LegacyMmsTransport.Carrier("http://carrier.test/mms", "", "");
        assertEquals("http://carrier.test/42", apn.endpoint("http://carrier.test/42").toString());
        assertThrows(IOException.class, () -> apn.endpoint("http://other.test/42"));
        assertThrows(IOException.class, () -> apn.endpoint("http://carrier.test:99/42"));
        assertThrows(IOException.class, () -> apn.endpoint("file:///message"));
    }

    @Test public void exchangePostsExactPduAndGetsResponseWithoutFollowingRedirects() throws Exception {
        byte[] pdu = {12, 34};
        try (MockWebServer mmsc = new MockWebServer()) {
            mmsc.enqueue(new MockResponse().setBody(new Buffer().write(pdu)));
            mmsc.enqueue(new MockResponse().setBody(new Buffer().write(pdu)));
            mmsc.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/other"));
            mmsc.start();
            URL url = mmsc.url("/mms").url();
            assertArrayEquals(pdu, LegacyMmsTransport.exchange(url, Proxy.NO_PROXY, pdu));
            RecordedRequest post = mmsc.takeRequest();
            assertEquals("POST", post.getMethod());
            assertEquals("application/vnd.wap.mms-message", post.getHeader("Content-Type"));
            assertArrayEquals(pdu, post.getBody().readByteArray());
            assertArrayEquals(pdu, LegacyMmsTransport.exchange(url, Proxy.NO_PROXY, null));
            assertEquals("GET", mmsc.takeRequest().getMethod());
            assertThrows(IOException.class, () -> LegacyMmsTransport.exchange(url, Proxy.NO_PROXY, pdu));
            assertEquals(3, mmsc.getRequestCount());
        }
    }
}
