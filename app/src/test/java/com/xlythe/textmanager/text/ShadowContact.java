package com.xlythe.textmanager.text;

import android.content.Context;
import android.net.Uri;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import java.util.List;

@Implements(Contact.class)
public class ShadowContact {
    public static Contact getInstance(String number) {
        return new Contact(number);
    }

    public static Contact getInstance(String number, String displayName) {
        return new Contact(number, displayName);
    }
}
