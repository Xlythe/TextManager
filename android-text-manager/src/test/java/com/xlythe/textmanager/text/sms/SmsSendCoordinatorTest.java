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
    public void rejectsMissingMessageId() {
        assertInvalidRequest(null, "+15551234567", "hello", "messageId must not be blank");
    }

    @Test
    public void rejectsMissingRecipient() {
        assertInvalidRequest("42", "  ", "hello", "recipient must not be blank");
    }

    @Test
    public void rejectsMissingBody() {
        assertInvalidRequest("42", "+15551234567", "", "body must not be blank");
    }

    @Test
    public void rejectsEmptySegmentationResult() {
        FakeGateway gateway = new FakeGateway(Collections.emptyList());
        SmsSendCoordinator<String> coordinator =
                new SmsSendCoordinator<>(gateway, new FakeCallbackFactory());

        try {
            coordinator.send(new SmsSendRequest("42", "+15551234567", "hello"));
        } catch (IllegalStateException expected) {
            assertEquals("SMS gateway returned no message parts", expected.getMessage());
            return;
        }
        throw new AssertionError("Expected an IllegalStateException");
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
    public void createsDistinctCallbacksForEveryMultipartSegment() {
        FakeGateway gateway = new FakeGateway(Arrays.asList("one", "two"));
        SmsSendCoordinator<String> coordinator =
                new SmsSendCoordinator<>(gateway, new FakeCallbackFactory());

        coordinator.send(new SmsSendRequest("7", "+15557654321", "onetwo"));

        assertEquals("+15557654321", gateway.recipient);
        assertEquals(Arrays.asList("one", "two"), gateway.parts);
        assertEquals(Arrays.asList("sent:7:0/2", "sent:7:1/2"), gateway.sentCallbacks);
        assertEquals(
                Arrays.asList("delivered:7:0/2", "delivered:7:1/2"),
                gateway.deliveredCallbacks);
        assertNull(gateway.body);
    }

    @Test
    public void sendsSeparateSegmentsWithDistinctSentCallbacksAndOptionalDeliveryReports() {
        FakeGateway gateway = new FakeGateway(Arrays.asList("one", "two"));
        new SmsSendCoordinator<>(gateway, new FakeCallbackFactory())
                .send(new SmsSendRequest("7", "+15557654321", "onetwo"), true, false);
        assertEquals(Arrays.asList("one", "two"), gateway.singleBodies);
        assertEquals(Arrays.asList("sent:7:0/2", "sent:7:1/2"), gateway.singleSentCallbacks);
        assertNull(gateway.deliveredCallback);
        assertNull(gateway.parts);
    }

    @Test
    public void nonDefaultAppCanSendMultipartWithoutProviderCallbacks() {
        FakeGateway gateway = new FakeGateway(Arrays.asList("one", "two"));
        new SmsSendCoordinator<>(gateway, null)
                .send(new SmsSendRequest("7", "+15557654321", "onetwo"));
        assertEquals(Arrays.asList("one", "two"), gateway.parts);
        assertEquals(Arrays.asList(null, null), gateway.sentCallbacks);
        assertEquals(Arrays.asList(null, null), gateway.deliveredCallbacks);
    }

    private static void assertInvalidRequest(
            String messageId, String recipient, String body, String expectedMessage) {
        try {
            new SmsSendRequest(messageId, recipient, body);
        } catch (IllegalArgumentException expected) {
            assertEquals(expectedMessage, expected.getMessage());
            return;
        }
        throw new AssertionError("Expected an IllegalArgumentException");
    }

    private static final class FakeCallbackFactory implements SmsCallbackFactory<String> {
        @Override
        public void initialize(SmsSendRequest request, int partCount) {}
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
        final List<String> singleBodies = new ArrayList<>();
        final List<String> singleSentCallbacks = new ArrayList<>();
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
            singleBodies.add(body);
            singleSentCallbacks.add(sentCallback);
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
