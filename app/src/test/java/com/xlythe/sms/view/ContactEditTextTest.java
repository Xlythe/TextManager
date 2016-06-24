package com.xlythe.sms.view;

import android.content.Context;
import android.util.Log;

import com.xlythe.sms.BuildConfig;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.ShadowContact;
import com.xlythe.textmanager.text.ShadowTextManager;
import com.xlythe.textmanager.text.TextManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk=23, shadows={ShadowTextManager.class, ShadowContact.class}, constants = BuildConfig.class)
public class ContactEditTextTest {
    private static final String ME = "111-111-1111";
    private static final String ALICE_NUMBER = "222-222-2222";
    private static final String BOB_NUMBER = "333-333-3333";
    private static final String BROKEN_NUMBER = "+1 216-313-8473";

    private static final String ALICE_NAME = "Alice Lazname";
    private static final String BOB_NAME = "Bob Burger";
    private static final String BROKEN_NAME = "Me Normalized";

    private Context mContext;
    private ContactEditText mEditText;

    @Before
    public void setup() {
        // Print out everything from logcat to the console instead
        ShadowLog.stream = System.out;

        // Setup variables
        mContext = RuntimeEnvironment.application;
        mEditText = new ContactEditText(mContext);

        // Update the owner's contact in TextManager
        ShadowTextManager shadowTextManager = (ShadowTextManager) ShadowExtractor.extract(TextManager.getInstance(mContext));
        shadowTextManager.setSelf(ME);
    }

    @Test
    public void oneNumber() {
        Contact contact = ShadowContact.getInstance(ALICE_NUMBER);
        mEditText.insert(contact);
        assertEquals(contact, mEditText.getContacts().get(0));
    }

    @Test
    public void multipleNumbers() {
        List<Contact> contacts = new ArrayList<>();
        contacts.add(ShadowContact.getInstance(ALICE_NUMBER));
        contacts.add(ShadowContact.getInstance(BOB_NUMBER));
        mEditText.setContacts(contacts);
        for (int i = 0; i < contacts.size(); i++) {
            assertEquals(contacts.get(i), mEditText.getContacts().get(i));
        }
    }

    @Test
    public void oneNumberWithName() {
        Contact contact = ShadowContact.getInstance(ALICE_NUMBER, ALICE_NAME);
        mEditText.insert(contact);
        assertEquals(contact, mEditText.getContacts().get(0));
    }

    @Test
    public void MultipleNumbersWithNames() {
        List<Contact> contacts = new ArrayList<>();
        contacts.add(ShadowContact.getInstance(ALICE_NUMBER, ALICE_NAME));
        contacts.add(ShadowContact.getInstance(BOB_NUMBER, BOB_NAME));
        mEditText.setContacts(contacts);
        for (int i = 0; i < contacts.size(); i++) {
            assertEquals(contacts.get(i), mEditText.getContacts().get(i));
        }
    }

    @Test
    public void brokenCase() {
        List<Contact> contacts = new ArrayList<>();
        contacts.add(ShadowContact.getInstance(BROKEN_NUMBER, BROKEN_NAME));
        contacts.add(ShadowContact.getInstance(BOB_NUMBER, BOB_NAME));
        mEditText.setContacts(contacts);
        for (int i = 0; i < contacts.size(); i++) {
            assertEquals(contacts.get(i), mEditText.getContacts().get(i));
        }
    }

    @Test
    public void brokenCaseInsert() {
        Contact contact = ShadowContact.getInstance(BROKEN_NUMBER, BROKEN_NAME);
        mEditText.insert(contact);
        mEditText.append("hi");
        assertEquals(contact, mEditText.getContacts().get(0));
        assertEquals("hi", mEditText.getPendingText());
    }

//    @Test
//    public void brokenCaseInsertPending() {
//        mEditText.append(BROKEN_NAME);
//        mEditText.insertPendingText();
//        assertEquals(BROKEN_NAME, mEditText.getPendingText());
//    }
}
