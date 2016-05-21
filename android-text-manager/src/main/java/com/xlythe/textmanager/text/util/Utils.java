package com.xlythe.textmanager.text.util;

import android.os.Parcelable;

import java.util.Collection;

public class Utils {
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private Utils() {}

    public static boolean equals(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    public static int hashCode(Object a) {
        if (a == null) {
            return 0;
        } else {
            return a.hashCode();
        }
    }

    public static int describeContents(Parcelable a) {
        if (a == null) {
            return 0;
        } else {
            return a.describeContents();
        }
    }

    public static <T> String join(char separator, Collection<T> collection, Rule<T> rule) {
        return join(Character.toString(separator), collection, rule);
    }

    public static <T> String join(String separator, Collection<T> collection, Rule<T> rule) {
        StringBuilder builder = new StringBuilder();
        for (T obj : collection) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(rule.toString(obj));
        }
        return builder.toString();
    }

    public interface Rule<T> {
        String toString(T t);
    }
}
