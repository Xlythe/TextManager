package com.xlythe.textmanager.text.pdu;

import static org.junit.Assert.*;

import com.xlythe.textmanager.text.util.CharacterSets;
import com.xlythe.textmanager.text.util.EncodedStringValue;
import com.xlythe.textmanager.text.util.Utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class MmsWireFormatTest {
    @Test
    public void singlePartRetrieveKeepsMimeCharsetAndOriginalBytes() {
        byte[] wire = Utils.hexToBytes("8C848D928501118403838184636166E9");
        RetrieveConf message = (RetrieveConf) new PduParser(wire, true).parse();
        PduPart part = message.getBody().getPart(0);
        assertEquals("text/plain", new String(part.getContentType(), StandardCharsets.US_ASCII));
        assertEquals(CharacterSets.ISO_8859_1, part.getCharset());
        assertEquals("café", new EncodedStringValue(part.getCharset(), part.getData()).getString());
    }

    @Test
    public void binaryAndFoldedMimeTransferEncodingsDecodeOnTheWire() throws Exception {
        assertArrayEquals(new byte[] {0, (byte) 255}, part("base64", "AP8=\r\n"));
        assertArrayEquals(
                "café".getBytes(StandardCharsets.UTF_8),
                part("quoted-printable", "ca=\r\nf=C3=A9"));
        assertArrayEquals("plain".getBytes(StandardCharsets.US_ASCII), part("binary", "plain"));
        assertNull(part("quoted-printable", "bad=QZ"));
        assertNull(part("base64", "AA=A"));
    }

    private byte[] part(String encoding, String value) throws Exception {
        byte[] header =
                ("Content-Transfer-Encoding\0" + encoding + "\0")
                        .getBytes(StandardCharsets.US_ASCII);
        byte[] data = value.getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        wire.write(1);
        wire.write(header.length + 1);
        wire.write(data.length);
        wire.write(0x83);
        wire.write(header);
        wire.write(data);
        PduBody parts =
                new PduParser(new byte[0], true)
                        .parseParts(new ByteArrayInputStream(wire.toByteArray()));
        return parts == null ? null : parts.getPart(0).getData();
    }

    @Test
    public void controlMessagesKeepTransactionsAndRecipientAddresses() {
        byte[] tx = {'t', '1'};
        AcknowledgeInd ack = new AcknowledgeInd(18, tx);
        ack.setReportAllowed(PduHeaders.VALUE_NO);
        AcknowledgeInd ackResult = (AcknowledgeInd) roundTrip(ack);
        assertArrayEquals(tx, ackResult.getTransactionId());
        assertEquals(PduHeaders.VALUE_NO, ackResult.getReportAllowed());
        NotifyRespInd notify = new NotifyRespInd(18, tx, PduHeaders.STATUS_RETRIEVED);
        NotifyRespInd notifyResult = (NotifyRespInd) roundTrip(notify);
        assertEquals(PduHeaders.STATUS_RETRIEVED, notifyResult.getStatus());
        assertArrayEquals(tx, notifyResult.getTransactionId());
        ReadRecInd read =
                new ReadRecInd(
                        new EncodedStringValue("+12025550100"),
                        tx,
                        18,
                        PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ,
                        new EncodedStringValue[] {new EncodedStringValue("person@example.com")});
        read.setDate(42);
        ReadRecInd decoded = (ReadRecInd) roundTrip(read);
        assertEquals(42, decoded.getDate());
        assertArrayEquals(tx, decoded.getMessageId());
        assertEquals("person@example.com", decoded.getTo()[0].getString());
        assertEquals("+12025550100", decoded.getFrom().getString());
        assertEquals(PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ, decoded.getReadStatus());
    }

    private GenericPdu roundTrip(GenericPdu pdu) {
        byte[] bytes = new PduComposer(RuntimeEnvironment.getApplication(), pdu).make();
        assertNotNull(bytes);
        return new PduParser(bytes, true).parse();
    }
}
