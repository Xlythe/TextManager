package com.xlythe.textmanager.text;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import java.util.HashMap;
import java.util.Map;

@Implements(TextManager.class)
public class ShadowTextManager {
    @RealObject
    private TextManager mRealTextManager;

    private Contact mSelf;

    private Map<String, Contact> mContactsByNumber = new HashMap<>();
    private Map<String, Contact> mContactsByName = new HashMap<>();

    @Implementation
    public Contact getSelf() {
        if (mSelf != null) {
            return mSelf;
        }

        return mRealTextManager.getSelf();
    }

    private String sanitize(String number) {
        return number.replaceAll("[^\\d+]", "");
    }

    @Implementation
    public Contact lookupContact(String arg) {
        if (mContactsByNumber.get(sanitize(arg)) != null) {
            return mContactsByNumber.get(sanitize(arg));
        }
        if (mContactsByName.get(arg) != null) {
            return mContactsByName.get(arg);
        }
        return ShadowContact.getInstance(arg);
    }

    public void addContact(Contact contact) {
        mContactsByNumber.put(sanitize(contact.getNumber()), contact);
        mContactsByName.put(contact.getDisplayName(), contact);
    }

    public void setSelf(Contact self) {
        mSelf = self;
    }

    public void setSelf(String self) {
        mSelf = new Contact(self);
    }
}
