package com.xlythe.textmanager.text;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.SimPhonebookContract.ElementaryFiles;
import android.provider.SimPhonebookContract.SimRecords;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/** Read personal dialing records. Fixed and service dialing records are separate SIM files. */
@RequiresApi(31)
final class SimContacts {
    private SimContacts() {}

    static List<Integer> subscriptions(ContentResolver resolver) {
        List<Integer> result = new ArrayList<>();
        String[] projection = {ElementaryFiles.SUBSCRIPTION_ID, ElementaryFiles.EF_TYPE};
        try (Cursor rows =
                resolver.query(ElementaryFiles.CONTENT_URI, projection, null, null, null)) {
            while (rows != null && rows.moveToNext()) {
                if (rows.getInt(1) == ElementaryFiles.EF_ADN) result.add(rows.getInt(0));
            }
        } catch (SecurityException
                | IllegalArgumentException
                | IllegalStateException
                | UnsupportedOperationException ignored) {
            // Some devices have no SIM phonebook provider.
        }
        return result;
    }

    static List<Contact> read(ContentResolver resolver, int subscription) {
        List<Contact> contacts = new ArrayList<>();
        String[] columns = {SimRecords.RECORD_NUMBER, SimRecords.NAME, SimRecords.PHONE_NUMBER};
        try (Cursor rows =
                resolver.query(
                        SimRecords.getContentUri(subscription, ElementaryFiles.EF_ADN),
                        columns,
                        null,
                        null,
                        null)) {
            while (rows != null && rows.moveToNext()) {
                String number = rows.getString(2);
                if (number != null && !number.isEmpty()) {
                    contacts.add(
                            new Contact(
                                    number,
                                    rows.getString(1),
                                    SimRecords.getItemUri(
                                            subscription, ElementaryFiles.EF_ADN, rows.getInt(0))));
                }
            }
        } catch (SecurityException
                | IllegalArgumentException
                | IllegalStateException
                | UnsupportedOperationException ignored) {
            // The grant or subscription may disappear between selection and query.
        }
        return contacts;
    }
}
