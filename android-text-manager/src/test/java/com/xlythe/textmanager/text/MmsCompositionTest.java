package com.xlythe.textmanager.text;

import android.content.Context;
import android.net.Uri;

import com.xlythe.textmanager.text.pdu.PduBody;
import com.xlythe.textmanager.text.pdu.PduHeaders;
import com.xlythe.textmanager.text.pdu.SendReq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class MmsCompositionTest {
    private final Context context = RuntimeEnvironment.getApplication();

    @Test public void providerVideoAndCaptionRemainSeparateWithoutALineNumber() {
        Uri uri = Uri.parse("content://documents/document/video-42");
        ShadowContentResolver resolver = Shadow.extract(context.getContentResolver());
        byte[] video = {1, 2, 3};
        boolean[] closed = {false};
        resolver.registerInputStream(uri, new ByteArrayInputStream(video) {
            @Override public void close() { closed[0] = true; }
        });
        Text text = new Text.Builder().addRecipient(new Contact("+15555550100"))
                .attach(new VideoAttachment(uri)).message("caption").build();
        SendReq request = text.getSendRequest(context);
        PduBody parts = request.getBody();
        assertEquals(3, parts.getPartsNum());
        assertArrayEquals(video, parts.getPart(1).getData());
        assertArrayEquals("caption".getBytes(StandardCharsets.UTF_8), parts.getPart(2).getData());
        assertTrue(closed[0]);
        assertEquals(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR, request.getFrom().getString());
    }

    @Test public void fileAndSchemalessAttachmentsStillReadTheSameBytes() throws Exception {
        File file = new File(context.getCacheDir(), "clip.mp4");
        byte[] bytes = {5, 6};
        Files.write(file.toPath(), bytes);
        assertArrayEquals(bytes, new VideoAttachment(Uri.fromFile(file)).getBytes(context).get());
        assertArrayEquals(bytes, new VideoAttachment(Uri.parse(file.getPath())).getBytes(context).get());
    }
}
