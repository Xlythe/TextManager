package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;

import com.xlythe.textmanager.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Niko on 5/23/15.
 */
public class CustomTextCursor extends CursorWrapper {
    public CustomTextCursor(Cursor c) {
        super(c);
    }

    public Text getText(){
        return new Text(this);
    }
}
