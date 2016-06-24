package com.xlythe.textmanager.text;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Implements(TextManager.class)
public class ShadowTextManager {
    @RealObject
    private TextManager mRealTextManager;

    private Contact mSelf;

    private Map<String, Contact> mContacts = new HashMap<>();

    @Implementation
    public Contact getSelf() {
        if (mSelf != null) {
            return mSelf;
        }

        return mRealTextManager.getSelf();
    }

//    private String sanitizeNumber(String phoneNumber) {
//        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
//        try {
//            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNumber, Locale.getDefault().getCountry());
//            return Long.toString(numberProto.getNationalNumber());
//        } catch (NumberParseException e) {
//            e.printStackTrace();
//        }
//        return phoneNumber;
//    }

    @Implementation
    public Contact lookupContact(String phoneNumber) {
        String sanitizedNumber = phoneNumber;
        if (sanitizedNumber == null) {
            return Contact.UNKNOWN;
        }
        return mContacts.get(sanitizedNumber);
    }

    public void setFakeContact(Contact contact) {
        String sanitizedNumber = contact.getNumber();
        mContacts.put(sanitizedNumber, contact);
    }

    public void setSelf(Contact self) {
        mSelf = self;
    }

    public void setSelf(String self) {
        mSelf = new Contact(self);
    }
}
