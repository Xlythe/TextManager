package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * A custom thread cursor.
 * Created by Niko on 5/23/15.
 */
public class ThreadCursor extends CursorWrapper {
    public ThreadCursor(Cursor c) {
        super(c);
    }

    /**
     * Returns the Thread a the cursor position.
     * @return The Thread at the cursor position
     */
    public Thread getThread(){
        return new Thread(this);
    }
}
