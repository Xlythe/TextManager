package com.xlythe.textmanager.text.sms;

import static com.xlythe.textmanager.text.sms.SmsStatusTracker.Phase.DELIVERED;
import static com.xlythe.textmanager.text.sms.SmsStatusTracker.Phase.SENT;
import static com.xlythe.textmanager.text.sms.SmsStatusTracker.Status.COMPLETE;
import static com.xlythe.textmanager.text.sms.SmsStatusTracker.Status.FAILED;
import static com.xlythe.textmanager.text.sms.SmsStatusTracker.Status.PENDING;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SmsStatusTrackerTest {
    @Test
    public void completesOnlyAfterSentAndDeliverySucceed() {
        SmsStatusTracker tracker = new SmsStatusTracker(new FakeStore());

        assertEquals(PENDING, tracker.record("1", SENT, 0, 1, true));
        assertEquals(COMPLETE, tracker.record("1", DELIVERED, 0, 1, true));
    }

    @Test
    public void multipartCallbacksAreOrderIndependentAndIdempotent() {
        FakeStore store = new FakeStore();
        SmsStatusTracker tracker = new SmsStatusTracker(store);

        assertEquals(PENDING, tracker.record("2", DELIVERED, 1, 2, true));
        assertEquals(PENDING, tracker.record("2", SENT, 0, 2, true));
        assertEquals(PENDING, tracker.record("2", SENT, 0, 2, true));
        assertEquals(PENDING, tracker.record("2", DELIVERED, 0, 2, true));
        assertEquals(COMPLETE, tracker.record("2", SENT, 1, 2, true));
        assertEquals(COMPLETE, tracker.record("2", DELIVERED, 1, 2, true));
    }

    @Test
    public void sendWithoutDeliveryReportsFinishesWhenEveryPartIsSent() {
        SmsStatusTracker tracker = new SmsStatusTracker(new FakeStore());
        assertEquals(PENDING, tracker.record("4", SENT, 1, 2, true, false));
        assertEquals(COMPLETE, tracker.record("4", SENT, 0, 2, true, false));
    }

    @Test
    public void anyFailureRemainsTerminal() {
        FakeStore store = new FakeStore();
        SmsStatusTracker firstTracker = new SmsStatusTracker(store);

        assertEquals(FAILED, firstTracker.record("3", SENT, 1, 2, false));

        SmsStatusTracker restoredTracker = new SmsStatusTracker(store);
        assertEquals(FAILED, restoredTracker.record("3", SENT, 0, 2, true));
        assertEquals(FAILED, restoredTracker.record("3", DELIVERED, 0, 2, true));
        assertEquals(FAILED, restoredTracker.record("3", DELIVERED, 1, 2, true));
    }

    @Test
    public void resetClearsPreviousCallbacks() {
        FakeStore store = new FakeStore();
        SmsStatusTracker tracker = new SmsStatusTracker(store);

        assertEquals(PENDING, tracker.record("5", SENT, 0, 1, true));
        tracker.reset("5", 1);
        assertEquals(PENDING, tracker.record("5", DELIVERED, 0, 1, true));
    }

    @Test
    public void differentPartCountStartsFreshState() {
        FakeStore store = new FakeStore();
        SmsStatusTracker tracker = new SmsStatusTracker(store);

        assertEquals(PENDING, tracker.record("4", SENT, 0, 1, true));
        assertEquals(PENDING, tracker.record("4", DELIVERED, 0, 2, true));
    }

    @Test
    public void rejectsMalformedCallbackMetadata() {
        assertInvalid(null, SENT, 0, 1, "messageId must not be blank");
        assertInvalid("1", null, 0, 1, "phase must not be null");
        assertInvalid("1", SENT, 0, 0, "partCount must be positive");
        assertInvalid("1", SENT, -1, 1, "partIndex is outside the message");
        assertInvalid("1", SENT, 1, 1, "partIndex is outside the message");
    }

    private static void assertInvalid(
            String messageId,
            SmsStatusTracker.Phase phase,
            int partIndex,
            int partCount,
            String expectedMessage) {
        try {
            new SmsStatusTracker(new FakeStore())
                    .record(messageId, phase, partIndex, partCount, true);
        } catch (IllegalArgumentException expected) {
            assertEquals(expectedMessage, expected.getMessage());
            return;
        }
        throw new AssertionError("Expected an IllegalArgumentException");
    }

    private static final class FakeStore implements SmsStatusTracker.Store {
        private final Map<String, SmsStatusTracker.State> states = new HashMap<>();

        @Override
        public SmsStatusTracker.State read(String messageId) {
            return states.get(messageId);
        }

        @Override
        public void write(String messageId, SmsStatusTracker.State state) {
            states.put(
                    messageId,
                    new SmsStatusTracker.State(
                            state.getPartCount(),
                            state.hasFailed(),
                            state.getSentParts(),
                            state.getDeliveredParts()));
        }
    }
}
