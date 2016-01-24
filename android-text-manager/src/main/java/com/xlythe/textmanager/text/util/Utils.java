package com.xlythe.textmanager.text.util;

import android.os.Parcelable;

public class Utils {
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
}
