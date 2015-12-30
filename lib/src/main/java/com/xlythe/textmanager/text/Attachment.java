package com.xlythe.textmanager.text;

import android.net.Uri;

/**
 * Created by Niko on 12/29/15.
 */
public class Attachment {
    enum Type {IMAGE, VIDEO, VOICE}

    Type mType;

    Type getType(){
        return mType;
    }

    Uri getUri(){
        return null;
    }
}