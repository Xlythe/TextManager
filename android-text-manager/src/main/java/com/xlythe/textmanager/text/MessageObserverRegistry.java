package com.xlythe.textmanager.text;

import com.xlythe.textmanager.MessageObserver;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

final class MessageObserverRegistry {
    private final Set<MessageObserver> observers = new CopyOnWriteArraySet<>();

    void register(MessageObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    void unregister(MessageObserver observer) {
        observers.remove(Objects.requireNonNull(observer, "observer"));
    }

    void notifyDataChanged() {
        for (MessageObserver observer : observers) {
            observer.notifyDataChanged();
        }
    }
}