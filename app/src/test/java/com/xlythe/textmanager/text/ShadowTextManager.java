package com.xlythe.textmanager.text;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

@Implements(TextManager.class)
public class ShadowTextManager {
    @RealObject
    private TextManager mRealTextManager;

    private Contact mSelf;

    @Implementation
    public Contact getSelf() {
        if (mSelf != null) {
            return mSelf;
        }

        return mRealTextManager.getSelf();
    }

    public void setSelf(Contact self) {
        mSelf = self;
    }

    public void setSelf(String self) {
        mSelf = new Contact(self);
    }
}
