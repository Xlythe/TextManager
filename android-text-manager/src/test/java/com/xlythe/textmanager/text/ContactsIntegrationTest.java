package com.xlythe.textmanager.text;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.SimPhonebookContract;
import android.provider.SimPhonebookContract.ElementaryFiles;
import android.provider.SimPhonebookContract.SimRecords;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class ContactsIntegrationTest {
    private final Context context = RuntimeEnvironment.getApplication();
    private final TextManager manager = TextManager.getInstance(context);
    private final Directory contacts = new Directory();
    private final Directory sim = new Directory();
    private static final String[] CONTACT_COLUMNS = {
        "_id", "lookup", "number", "display_name", "photo_uri", "photo_thumb_uri"
    };

    @Before
    public void setUp() {
        ((ShadowApplication) Shadow.extract(RuntimeEnvironment.getApplication()))
                .grantPermissions(Manifest.permission.READ_CONTACTS);
        register(contacts, ContactsContract.AUTHORITY);
        register(sim, SimPhonebookContract.AUTHORITY);
    }

    private void register(ContentProvider provider, String authority) {
        ProviderInfo info = new ProviderInfo();
        info.authority = authority;
        provider.attachInfo(context, info);
        ShadowContentResolver.registerProviderInternal(authority, provider);
    }

    @Test
    public void internationalAndNationalLookupsUseNumberingPlans() {
        assertEquals("+442079460018", PhoneNumbers.normalize("020 7946 0018", "gb"));
        assertEquals("+442079460018", PhoneNumbers.normalize("011 44 20 7946 0018", "US"));
        assertEquals("+390212345678", PhoneNumbers.normalize("02 12345678", "IT"));
        assertEquals("+14155550101", PhoneNumbers.normalize("４１５５５５０１０１", "US"));
        for (String address :
                new String[] {"911", "*123#", "BANK2", "alice@example.com", "5550123"}) {
            assertEquals(address, PhoneNumbers.normalize(address, "US"));
        }
        assertEquals("London", PhoneNumbers.describe("+44 20 7946 0018", "US", Locale.UK));
        assertEquals("", PhoneNumbers.describe("*123#", "GB", Locale.US));
    }

    @Test
    public void sharedCallingCodesAndGeographicAreaCodesUseOfflineMetadata() {
        assertTrue(PhoneNumbers.regionsForCallingCode(1).containsAll(Arrays.asList("US", "CA")));
        assertTrue(PhoneNumbers.regionsForCallingCode(44).contains("GB"));
        assertTrue(PhoneNumbers.regionsForCallingCode(999).isEmpty());
        assertEquals("20", PhoneNumbers.areaCode("020 7946 0018", "GB"));
        assertEquals("", PhoneNumbers.areaCode("911", "US"));
    }

    @Test
    public void listAllSimContactsVisitsOnlyPersonalDialingFiles() {
        assertEquals(1, manager.getSimContacts().size());
        assertEquals(SimRecords.getContentUri(7, ElementaryFiles.EF_ADN), sim.uri);
    }

    @Test
    public void exactContactNameResolvesToTheSelectedNumberAndEmailUsesItsOwnDirectory() {
        Contact name = manager.lookupContact("Ada", 3);
        assertEquals(contacts.number, name.getMobileNumber(context).get());
        Contact email = manager.lookupContact("Ada2@example.com", 3);
        assertEquals("Ada", email.getDisplayName());
        assertEquals("Ada2@example.com", email.getNumber());
        assertEquals("12", email.getId());
        assertTrue(new Text.Builder().addRecipient(email).message("hello").build().isMms());
    }

    @Test
    public void directoryLookupPreservesDialedAddressAndSeesRenames() {
        String address = "+44 20 7946 0018";
        contacts.number = "+44 20 7946 0018";
        Contact found = manager.lookupContact(address, 3);
        assertEquals(address, found.getNumber());
        assertEquals("Ada", found.getDisplayName());
        assertEquals("+442079460018", contacts.uri.getLastPathSegment());
        contacts.name = "Grace";
        assertEquals("Grace", manager.lookupContact(address, 3).getDisplayName());
        contacts.number = "+1 202 555 0100";
        assertFalse(manager.lookupContact(address, 3).hasName());
    }

    @Test
    public void simLookupIsScopedToSubscriptionAndPersonalRecords() {
        String address = "+1 650 555 0100";
        Contact found = manager.lookupContact(address, 7);
        assertEquals("SIM entry", found.getDisplayName());
        assertEquals(address, found.getNumber());
        assertEquals(SimRecords.getItemUri(7, ElementaryFiles.EF_ADN, 9), found.getSimRecordUri());
        assertEquals(found.getSimRecordUri().toString(), found.asPersonCompat().getUri());
        assertEquals(SimRecords.getContentUri(7, ElementaryFiles.EF_ADN), sim.uri);
        assertTrue(manager.getSimContacts(8).isEmpty());
        assertTrue(manager.getSimContacts(-1).isEmpty());
    }

    @Test
    public void revokedContactsGrantStopsPhoneAndSimLookup() {
        assertEquals(1, manager.getSimContacts(7).size());
        ((ShadowApplication) Shadow.extract(RuntimeEnvironment.getApplication()))
                .denyPermissions(Manifest.permission.READ_CONTACTS);
        assertTrue(manager.getSimContacts(7).isEmpty());
        Contact fallback = manager.lookupContact("+16505550100", 7);
        assertFalse(fallback.hasName());
        assertEquals("tel", Uri.parse(fallback.asPerson().getUri()).getScheme());
        try (Contact.ContactCursor rows = manager.getContactCursor()) {
            assertEquals(0, rows.getCount());
        }
        assertTrue(fallback.getEmails(context).isEmpty());
    }

    @Test
    public void contactsCursorUsesSelectedNumberAndHandlesMissingColumns() {
        MatrixCursor c = new MatrixCursor(new String[] {"_id", "number", "display_name"});
        c.addRow(new Object[] {12, "02012345678", null});
        c.moveToFirst();
        Contact selected = new Contact(c);
        assertEquals("02012345678", selected.getMobileNumber(context).get());
        assertEquals("12", selected.getId());
        assertNull(selected.getPhotoUri());
        assertNull(selected.getThumbnailUri());
    }

    @Test
    public void phoneAndEmailListsRespectProviderPreferenceAndDeduplicate() {
        MatrixCursor c = new MatrixCursor(new String[] {"_id", "display_name"});
        c.addRow(new Object[] {12, "Ada"});
        c.moveToFirst();
        Contact selected = new Contact(c);
        assertEquals("5550100", selected.getMobileNumber(context).get());
        assertEquals(Arrays.asList("5550100", "5550101"), selected.getNumbers(context));
        assertEquals(Collections.singletonList("a@example.com"), selected.getEmails(context));
        assertTrue(contacts.order.startsWith("is_super_primary DESC"));
        try (Contact.ContactCursor rows =
                manager.getContactCursor("A/B", Contact.Sort.FrequentlyContacted)) {
            assertEquals("A/B", contacts.uri.getLastPathSegment());
            assertTrue(contacts.order.endsWith("DESC"));
        }
    }

    @Test
    @Config(sdk = 28)
    public void oldAndroidCanReadContactsWithoutSimProvider() {
        assertTrue(manager.getSimContacts(7).isEmpty());
        assertNull(sim.uri);
    }

    static final class Directory extends ContentProvider {
        String number = "+441234567890", name = "Ada", order;
        Uri uri;

        public boolean onCreate() {
            return true;
        }

        public Cursor query(
                Uri uri, String[] projection, String selection, String[] args, String order) {
            this.uri = uri;
            this.order = order;
            if (SimPhonebookContract.AUTHORITY.equals(uri.getAuthority())) {
                MatrixCursor rows = new MatrixCursor(projection);
                if (ElementaryFiles.CONTENT_URI.equals(uri)) {
                    rows.addRow(new Object[] {7, ElementaryFiles.EF_ADN});
                    rows.addRow(new Object[] {8, ElementaryFiles.EF_FDN});
                    return rows;
                }
                if (SimRecords.getContentUri(7, ElementaryFiles.EF_ADN).equals(uri)) {
                    rows.addRow(new Object[] {9, "SIM entry", "+16505550100"});
                    rows.addRow(new Object[] {10, "", ""});
                }
                return rows;
            }
            if (uri.toString()
                    .startsWith(
                            ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI.toString())) {
                MatrixCursor rows =
                        new MatrixCursor(
                                new String[] {
                                    "_id", "contact_id", "data1", "display_name", "lookup"
                                });
                rows.addRow(new Object[] {26, 12, "ada2@example.com", name, "key"});
                return rows;
            }
            if (projection != null && projection.length == 1) {
                MatrixCursor rows = new MatrixCursor(projection);
                String first =
                        uri.equals(ContactsContract.CommonDataKinds.Email.CONTENT_URI)
                                ? "a@example.com"
                                : "5550100";
                rows.addRow(new Object[] {first});
                rows.addRow(new Object[] {first});
                if (first.startsWith("5")) rows.addRow(new Object[] {"5550101"});
                return rows;
            }
            MatrixCursor rows = new MatrixCursor(CONTACT_COLUMNS);
            rows.addRow(new Object[] {12, "key", number, name, null, null});
            return rows;
        }

        public String getType(Uri u) {
            return null;
        }

        public Uri insert(Uri u, ContentValues v) {
            throw new UnsupportedOperationException();
        }

        public int update(Uri u, ContentValues v, String s, String[] a) {
            throw new UnsupportedOperationException();
        }

        public int delete(Uri u, String s, String[] a) {
            throw new UnsupportedOperationException();
        }
    }
}
