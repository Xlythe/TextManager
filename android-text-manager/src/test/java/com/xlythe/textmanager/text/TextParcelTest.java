package com.xlythe.textmanager.text;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class TextParcelTest {
    @Test
    public void preparedPayloadRoundTrips() throws Exception {
        Text original =
                new Text.DebugBuilder()
                        .setSender("+15551234567")
                        .message("hello")
                        .build();
        setPreparedPayload(original, new byte[] {1, 2, 3, 4});

        Text restored = Text.fromBytes(original.toBytes());

        assertArrayEquals(new byte[] {1, 2, 3, 4}, getPreparedPayload(restored));
    }

    @Test
    public void writingDoesNotMutateMissingPayload() throws Exception {
        Text text = new Text.DebugBuilder().setSender("+15551234567").message("hello").build();

        text.toBytes();

        assertNull(getPreparedPayload(text));
    }

    private static void setPreparedPayload(Text text, byte[] payload) throws Exception {
        payloadField().set(text, payload);
    }

    private static byte[] getPreparedPayload(Text text) throws Exception {
        return (byte[]) payloadField().get(text);
    }

    private static Field payloadField() throws Exception {
        Field field = Text.class.getDeclaredField("mBytesToSend");
        field.setAccessible(true);
        return field;
    }
}
