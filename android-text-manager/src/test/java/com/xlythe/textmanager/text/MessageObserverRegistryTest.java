package com.xlythe.textmanager.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.xlythe.textmanager.MessageObserver;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageObserverRegistryTest {
    @Test
    public void registrationCanChangeDuringNotification() throws Exception {
        MessageObserverRegistry registry = new MessageObserverRegistry();
        CountDownLatch notificationStarted = new CountDownLatch(1);
        CountDownLatch continueNotification = new CountDownLatch(1);
        AtomicInteger blockingCalls = new AtomicInteger();
        AtomicInteger removedCalls = new AtomicInteger();
        AtomicInteger addedCalls = new AtomicInteger();
        MessageObserver removed = removedCalls::incrementAndGet;
        MessageObserver added = addedCalls::incrementAndGet;
        registry.register(() -> {
            blockingCalls.incrementAndGet();
            notificationStarted.countDown();
            try {
                continueNotification.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                java.lang.Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        });
        registry.register(removed);

        java.lang.Thread notifier = new java.lang.Thread(registry::notifyDataChanged);
        notifier.start();
        assertTrue(notificationStarted.await(5, TimeUnit.SECONDS));
        registry.unregister(removed);
        registry.register(added);
        continueNotification.countDown();
        notifier.join(5000);

        assertFalse(notifier.isAlive());
        assertEquals(1, blockingCalls.get());
        assertEquals(1, removedCalls.get());
        assertEquals(0, addedCalls.get());

        registry.notifyDataChanged();
        assertEquals(2, blockingCalls.get());
        assertEquals(1, removedCalls.get());
        assertEquals(1, addedCalls.get());
    }

    @Test(expected = NullPointerException.class)
    public void nullObserversAreRejected() {
        new MessageObserverRegistry().register(null);
    }
}