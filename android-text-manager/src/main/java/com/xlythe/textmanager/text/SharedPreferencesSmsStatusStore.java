package com.xlythe.textmanager.text;

import android.content.Context;
import android.content.SharedPreferences;

import com.xlythe.textmanager.text.sms.SmsStatusTracker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class SharedPreferencesSmsStatusStore implements SmsStatusTracker.Store {
    private static final String PREFERENCES = "text_manager_sms_status";
    private static final String PART_COUNT = ".part_count";
    private static final String FAILED = ".failed";
    private static final String SENT_PARTS = ".sent_parts";
    private static final String DELIVERED_PARTS = ".delivered_parts";

    private final SharedPreferences preferences;

    SharedPreferencesSmsStatusStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override
    public SmsStatusTracker.State read(String messageId) {
        String prefix = keyPrefix(messageId);
        if (!preferences.contains(prefix + PART_COUNT)) {
            return null;
        }
        return new SmsStatusTracker.State(
                preferences.getInt(prefix + PART_COUNT, 0),
                preferences.getBoolean(prefix + FAILED, false),
                parseParts(preferences.getStringSet(prefix + SENT_PARTS, Collections.emptySet())),
                parseParts(
                        preferences.getStringSet(
                                prefix + DELIVERED_PARTS, Collections.emptySet())));
    }

    @Override
    public void write(String messageId, SmsStatusTracker.State state) {
        String prefix = keyPrefix(messageId);
        boolean committed =
                preferences
                        .edit()
                        .putInt(prefix + PART_COUNT, state.getPartCount())
                        .putBoolean(prefix + FAILED, state.hasFailed())
                        .putStringSet(prefix + SENT_PARTS, formatParts(state.getSentParts()))
                        .putStringSet(
                                prefix + DELIVERED_PARTS,
                                formatParts(state.getDeliveredParts()))
                        .commit();
        if (!committed) {
            throw new IllegalStateException("Unable to persist SMS callback state");
        }
    }

    private static String keyPrefix(String messageId) {
        return "message." + messageId;
    }

    private static Set<Integer> parseParts(Set<String> values) {
        Set<Integer> parts = new HashSet<>();
        for (String value : values) {
            try {
                parts.add(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                // Ignore corrupt state and continue tracking valid callbacks.
            }
        }
        return parts;
    }

    private static Set<String> formatParts(Set<Integer> parts) {
        Set<String> values = new HashSet<>();
        for (Integer part : parts) {
            values.add(Integer.toString(part));
        }
        return values;
    }
}
