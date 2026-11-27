package com.xlythe.textmanager.text.smil;

import static org.junit.Assert.*;

import com.xlythe.textmanager.text.pdu.PduBody;
import com.xlythe.textmanager.text.pdu.PduPart;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class MmsPresentationTest {
    @Test
    public void fullPartialAndMetricSlideClocksAgree() {
        assertEquals(3723500, TimeImpl.parseClockValue("01:02:03.5"), 0);
        assertEquals(123500, TimeImpl.parseClockValue("02:03.5"), 0);
        assertEquals(250, TimeImpl.parseClockValue("250ms"), 0);
        assertEquals(1500, TimeImpl.parseClockValue("1.5s"), 0);
        assertEquals(1500, TimeImpl.parseClockValue("1.5"), 0);
        assertEquals(90000, TimeImpl.parseClockValue("1.5min"), 0);
        assertEquals(5400000, TimeImpl.parseClockValue("1.5h"), 0);
        assertThrows(IllegalArgumentException.class, () -> TimeImpl.parseClockValue("00:61"));
    }

    @Test
    public void slidesReferenceTheRightPartsAndEscapeAttributeValuesOnce() throws Exception {
        PduBody parts = new PduBody();
        String[] mime = {"image/jpeg", "text/plain", "audio/amr", "text/plain", "video/mp4"};
        for (int i = 0; i < mime.length; i++) {
            PduPart part = new PduPart();
            part.setContentType(mime[i].getBytes(StandardCharsets.US_ASCII));
            part.setContentLocation(("a&b" + i).getBytes(StandardCharsets.US_ASCII));
            parts.addPart(part);
        }
        SMILDocument model = SmilHelper.createSmilDocument(parts);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(model, bytes);
        Document document =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(3, document.getElementsByTagName("par").getLength());
        assertEquals(
                "8000ms",
                ((Element) document.getElementsByTagName("par").item(0)).getAttribute("dur"));
        assertEquals(
                "a&b0",
                ((Element) document.getElementsByTagName("img").item(0)).getAttribute("src"));
        assertEquals(
                "a&b2",
                ((Element) document.getElementsByTagName("audio").item(0)).getAttribute("src"));
        assertEquals(
                "a&b4",
                ((Element) document.getElementsByTagName("video").item(0)).getAttribute("src"));
    }

    @Test
    public void layoutsResolvePercentAndPixelRegionsAgainstTheRoot() {
        SMILDocument model = SmilHelper.createSmilDocument(new PduBody());
        LayoutManager.init(RuntimeEnvironment.getApplication());
        SMILLayoutElement layout = model.getLayout();
        SMILRootLayoutElement root = layout.getRootLayout();
        root.setWidth(320);
        root.setHeight(240);
        SMILRegionElement region = (SMILRegionElement) model.createElement("region");
        layout.appendChild(region);
        region.setId("caption");
        region.setTitle("Caption");
        region.setFit("meet");
        region.setBackgroundColor("#ffffff");
        region.setZIndex(2);
        region.setAttribute("width", "50%");
        region.setAttribute("height", "25%");
        region.setLeft(10);
        region.setTop(30);
        assertEquals(160, region.getWidth());
        assertEquals(60, region.getHeight());
        assertEquals(10, region.getLeft());
        assertEquals(30, region.getTop());
        assertEquals("caption", region.getId());
        assertEquals("Caption", region.getTitle());
        assertEquals("meet", region.getFit());
        assertEquals("#ffffff", region.getBackgroundColor());
        assertEquals(2, region.getZIndex());
    }
}
