package com.xlythe.sms.view;

import android.content.Context;

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
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 *
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk=23, shadows={ShadowTextManager.class, ShadowContact.class}, constants = BuildConfig.class)
public class ContactEditTextTest {
    private static final String ME = "111-111-1111";
    private static final Contact ALICE = ShadowContact.getInstance("222-222-2222", "Alice");
    private static final Contact BOB = ShadowContact.getInstance("333-333-3333", "Bob Bobberson");
    private static final Contact CHARLIE = ShadowContact.getInstance("+1 234-567-8901", "Charlie");
    private static final Contact NAMELESS = ShadowContact.getInstance("444444444");

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
        shadowTextManager.addContact(ALICE);
        shadowTextManager.addContact(BOB);
        shadowTextManager.addContact(CHARLIE);
        shadowTextManager.addContact(NAMELESS);
    }

    @Test
    public void oneNumber() {
        mEditText.insert(ALICE);
        assertEquals(1, mEditText.getContacts().size());
        assertEquals(ALICE, mEditText.getContacts().get(0));
    }

    @Test
    public void multipleNumbers() {
        List<Contact> contacts = Arrays.asList(ALICE, BOB);
        mEditText.setContacts(contacts);
        assertEquals(contacts.size(), mEditText.getContacts().size());
        for (int i = 0; i < contacts.size(); i++) {
            assertEquals(contacts.get(i), mEditText.getContacts().get(i));
        }
    }

    @Test
    public void pendingText() {
        mEditText.insert(ALICE);
        mEditText.append("Hello World");
        assertEquals(ALICE, mEditText.getContacts().get(0));
        assertEquals("Hello World", mEditText.getPendingText());
    }

    @Test
    public void insertOnSemiColon() {
        mEditText.append(ALICE.getDisplayName() + ";");
        assertEquals(ALICE, mEditText.getContacts().get(0));
    }

    @Test
    public void namelessPendingText() {
        mEditText.append(NAMELESS.getDisplayName() + ";");
        mEditText.append("Hello World");
        assertEquals(NAMELESS, mEditText.getContacts().get(0));
        assertEquals("Hello World", mEditText.getPendingText());
    }

    @Test
    public void insertPending() {
        mEditText.append(ALICE.getDisplayName());
        mEditText.insertPendingText();
        assertEquals(ALICE, mEditText.getContacts().get(0));
    }
}
