package com.xlythe.textmanager.text.sms;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Thread-safe, idempotent aggregation of multipart SMS transport callbacks. */
public final class SmsStatusTracker {
    private static final Object LOCK = new Object();

    private final Store store;

    public SmsStatusTracker(Store store) {
        this.store = store;
    }

    public void reset(String messageId, int partCount) {
        validate(messageId, Phase.SENT, 0, partCount);
        synchronized (LOCK) {
            store.write(messageId, new State(partCount));
        }
    }

    public Status record(
            String messageId,
            Phase phase,
            int partIndex,
            int partCount,
            boolean successful) {
        validate(messageId, phase, partIndex, partCount);
        synchronized (LOCK) {
            State state = store.read(messageId);
            if (state == null || state.partCount != partCount) {
                state = new State(partCount);
            } else {
                state = state.copy();
            }

            if (successful) {
                if (phase == Phase.SENT) {
                    state.sentParts.add(partIndex);
                } else {
                    state.deliveredParts.add(partIndex);
                }
            } else {
                state.failed = true;
            }
            store.write(messageId, state);

            if (state.failed) {
                return Status.FAILED;
            }
            if (state.sentParts.size() == partCount
                    && state.deliveredParts.size() == partCount) {
                return Status.COMPLETE;
            }
            return Status.PENDING;
        }
    }

    private static void validate(
            String messageId, Phase phase, int partIndex, int partCount) {
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        if (phase == null) {
            throw new IllegalArgumentException("phase must not be null");
        }
        if (partCount <= 0) {
            throw new IllegalArgumentException("partCount must be positive");
        }
        if (partIndex < 0 || partIndex >= partCount) {
            throw new IllegalArgumentException("partIndex is outside the message");
        }
    }

    public enum Phase {
        SENT,
        DELIVERED
    }

    public enum Status {
        PENDING,
        COMPLETE,
        FAILED
    }

    public interface Store {
        State read(String messageId);

        void write(String messageId, State state);
    }

    /** Persistable callback state. Collections are defensively copied. */
    public static final class State {
        private final int partCount;
        private boolean failed;
        private final Set<Integer> sentParts;
        private final Set<Integer> deliveredParts;

        public State(int partCount) {
            this(partCount, false, Collections.emptySet(), Collections.emptySet());
        }

        public State(
                int partCount,
                boolean failed,
                Set<Integer> sentParts,
                Set<Integer> deliveredParts) {
            this.partCount = partCount;
            this.failed = failed;
            this.sentParts = new HashSet<>(sentParts);
            this.deliveredParts = new HashSet<>(deliveredParts);
        }

        public int getPartCount() {
            return partCount;
        }

        public boolean hasFailed() {
            return failed;
        }

        public Set<Integer> getSentParts() {
            return Collections.unmodifiableSet(new HashSet<>(sentParts));
        }

        public Set<Integer> getDeliveredParts() {
            return Collections.unmodifiableSet(new HashSet<>(deliveredParts));
        }

        private State copy() {
            return new State(partCount, failed, sentParts, deliveredParts);
        }
    }
}
