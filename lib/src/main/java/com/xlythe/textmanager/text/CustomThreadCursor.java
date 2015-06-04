package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;

import java.text.SimpleDateFormat;

/**
 * Created by Niko on 5/23/15.
 */
public class CustomThreadCursor extends CursorWrapper {
    public CustomThreadCursor(Cursor c) {
        super(c);
    }

    public TextThread getThread(){
        return new TextThread(this);
    }
}
