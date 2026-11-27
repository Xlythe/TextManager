package com.xlythe.textmanager.text.pdu;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class PduParserTest {
    private static ByteArrayInputStream stream(int... bytes) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) result[i] = (byte) bytes[i];
        return new ByteArrayInputStream(result);
    }

    @Test public void interleavedMessagesRetainTheirOwnRootPart() {
        PduParser imageRoot = new PduParser(new byte[0], true);
        PduParser textRoot = new PduParser(new byte[0], true);
        // Content-Type: multipart/related, type=image/jpeg or text/plain.
        imageRoot.parseHeaders(stream(0x84, 3, 0xb3, 0x83, 0x9e));
        textRoot.parseHeaders(stream(0x84, 3, 0xb3, 0x83, 0x83));
        int[] body = {2, 1, 1, 0x83, 't', 1, 1, 0x9e, 'j'};
        assertArrayEquals(new byte[]{'j'}, imageRoot.parseParts(stream(body)).getPart(0).getData());
        assertArrayEquals(new byte[]{'t'}, textRoot.parseParts(stream(body)).getPart(0).getData());
    }

    @Test public void incompletePartOrEmptyAlternativeFailsTheParse() {
        PduParser parser = new PduParser(new byte[0], true);
        assertNull(parser.parseParts(stream(1, 1, 2, 0x83, 'x')));
        assertNull(parser.parseParts(stream(1, 1, 1, 0xa6, 0)));
        assertNull(parser.parseParts(stream(0x80)));
        byte[] emptyAlternative = {(byte) 0x8c, (byte) 0x84, (byte) 0x8d, (byte) 0x92,
                (byte) 0x85, 1, 17, (byte) 0x84, (byte) 0xa6, 0};
        assertNull(new PduParser(emptyAlternative, true).parse());
        assertEquals(-1, PduParser.skipWapValue(stream(1), 2));
    }

    @Test public void normalRetrieveConfirmationRetainsDateAndBody() {
        byte[] wire = {(byte)0x8c, (byte)0x84, (byte)0x8d, (byte)0x92,
                (byte)0x85, 1, 17, (byte)0x84, (byte)0xa3, 1, 1, 2, (byte)0x83, 'o', 'k'};
        RetrieveConf received = (RetrieveConf) new PduParser(wire, true).parse();
        assertEquals(17, received.getDate());
        assertArrayEquals(new byte[]{'o','k'}, received.getBody().getPart(0).getData());
    }
}
