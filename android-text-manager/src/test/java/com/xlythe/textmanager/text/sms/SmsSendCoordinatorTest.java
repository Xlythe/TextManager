package com.xlythe.textmanager.text.sms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SmsSendCoordinatorTest {
    @Test
    public void requestExposesDispatchData() {
        SmsSendRequest request = new SmsSendRequest("42", "+15551234567", "hello");

        assertEquals("42", request.getMessageId());
        assertEquals("+15551234567", request.getRecipient());
        assertEquals("hello", request.getBody());
    }

    @Test
    public void sendsSinglePartWithSentAndDeliveryCallbacks() {
        FakeGateway gateway = new FakeGateway(Collections.singletonList("hello"));
        SmsSendCoordinator<String> coordinator =
                new SmsSendCoordinator<>(gateway, new FakeCallbackFactory());

        coordinator.send(new SmsSendRequest("42", "+15551234567", "hello"));

        assertEquals("+15551234567", gateway.recipient);
        assertEquals("hello", gateway.body);
        assertEquals("sent:42:0/1", gateway.sentCallback);
        assertEquals("delivered:42:0/1", gateway.deliveredCallback);
        assertNull(gateway.parts);
    }

    @Test
    public void preservesLegacySingleCallbackForMultipartDispatch() {
        FakeGateway gateway = new FakeGateway(Arrays.asList("one", "two"));
        SmsSendCoordinator<String> coordinator =
                new SmsSendCoordinator<>(gateway, new FakeCallbackFactory());

        coordinator.send(new SmsSendRequest("7", "+15557654321", "onetwo"));

        assertEquals("+15557654321", gateway.recipient);
        assertEquals(Arrays.asList("one", "two"), gateway.parts);
        assertEquals(Collections.singletonList("sent:7:0/2"), gateway.sentCallbacks);
        assertEquals(
                Collections.singletonList("delivered:7:0/2"), gateway.deliveredCallbacks);
        assertNull(gateway.body);
    }

    private static final class FakeCallbackFactory implements SmsCallbackFactory<String> {
        @Override
        public String createSent(SmsSendRequest request, int partIndex, int partCount) {
            return "sent:" + request.getMessageId() + ":" + partIndex + "/" + partCount;
        }

        @Override
        public String createDelivered(SmsSendRequest request, int partIndex, int partCount) {
            return "delivered:" + request.getMessageId() + ":" + partIndex + "/" + partCount;
        }
    }

    private static final class FakeGateway implements SmsGateway<String> {
        private final List<String> dividedParts;
        private String recipient;
        private String body;
        private String sentCallback;
        private String deliveredCallback;
        private List<String> parts;
        private List<String> sentCallbacks;
        private List<String> deliveredCallbacks;

        FakeGateway(List<String> dividedParts) {
            this.dividedParts = dividedParts;
        }

        @Override
        public List<String> divideMessage(String body) {
            return dividedParts;
        }

        @Override
        public void sendText(
                String recipient,
                String body,
                String sentCallback,
                String deliveredCallback) {
            this.recipient = recipient;
            this.body = body;
            this.sentCallback = sentCallback;
            this.deliveredCallback = deliveredCallback;
        }

        @Override
        public void sendMultipartText(
                String recipient,
                List<String> parts,
                List<String> sentCallbacks,
                List<String> deliveredCallbacks) {
            this.recipient = recipient;
            this.parts = new ArrayList<>(parts);
            this.sentCallbacks = new ArrayList<>(sentCallbacks);
            this.deliveredCallbacks = new ArrayList<>(deliveredCallbacks);
        }
    }
}
