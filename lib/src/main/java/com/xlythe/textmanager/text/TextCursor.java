package com.xlythe.textmanager.text;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * A custom text cursor.
 * Created by Niko on 5/23/15.
 */
public class TextCursor extends CursorWrapper {
    public TextCursor(Cursor c) {
        super(c);
    }

    /**
     * Returns the Text a the cursor position.
     * @return The Text at the cursor position
     */
    public Text getText(){
        return new Text(this);
    }
}
