package com.xlythe.textmanager.text.util;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * A simple lru cache with reversible lookup. Both key and value must be unique.
 * */
public class SimpleLruCache<K, V> {
    private final int mSize;
    private final LinkedList<V> mStack = new LinkedList<>();
    private final HashMap<K, V> mMap;
    private final HashMap<V, K> mReversedMap;

    public SimpleLruCache(int size) {
        mSize = size;
        mMap = new HashMap<>(mSize);
        mReversedMap = new HashMap<>(mSize);
    }

    public void add(K key, V val) {
        // Check for dups
        if (mStack.contains(val)) {
            return;
        }

        // If the stack has grown too large, start trimming it
        if (mStack.size() == mSize) {
            V oldValue = mStack.removeFirst();
            K oldKey = mReversedMap.remove(oldValue);
            mMap.remove(oldKey);
        }

        // Add the new object to the cache
        mStack.add(val);
        mMap.put(key, val);
        mReversedMap.put(val, key);
    }

    public V get(K key) {
        V val = mMap.get(key);

        // Move this object to the back of the stack
        if (val != null) {
            mStack.remove(val);
            mStack.add(val);
        }

        return val;
    }
}
