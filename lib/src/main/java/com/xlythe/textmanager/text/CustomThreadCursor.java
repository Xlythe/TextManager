package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * A custom thread cursor.
 * Created by Niko on 5/23/15.
 */
public class CustomThreadCursor extends CursorWrapper {
    public CustomThreadCursor(Cursor c) {
        super(c);
    }

    /**
     * Returns the TextThread a the cursor position.
     * @return The TextThread at the cursor position
     */
    public TextThread getThread(){
        return new TextThread(this);
    }
}
