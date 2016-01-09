package com.xlythe.textmanager.text;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import junit.framework.TestCase;

public class ParcelableTest extends TestCase {
    private Parcel createParcel(Parcelable original) {
        // Write the data.
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, original.describeContents());

        // After you're done with writing, you need to reset the parcel for reading.
        parcel.setDataPosition(0);

        return parcel;
    }

    public void testImageAttachment() {
        // Set up the Parcelable object.
        Parcelable original = new ImageAttachment(Uri.EMPTY);

        // Write the data.
        Parcel parcel = createParcel(original);

        // Verify that the received data is correct.
        assertEquals("ImageAttachment", original, ImageAttachment.CREATOR.createFromParcel(parcel));
    }

    public void testVideoAttachment() {
        // Set up the Parcelable object.
        Parcelable original = new VideoAttachment(Uri.EMPTY);

        // Write the data.
        Parcel parcel = createParcel(original);

        // Verify that the received data is correct.
        assertEquals("VideoAttachment", original, VideoAttachment.CREATOR.createFromParcel(parcel));
    }

    public void testVoiceAttachment() {
        // Set up the Parcelable object.
        Parcelable original = new VoiceAttachment(Uri.EMPTY);

        // Write the data.
        Parcel parcel = createParcel(original);

        // Verify that the received data is correct.
        assertEquals("VoiceAttachment", original, VoiceAttachment.CREATOR.createFromParcel(parcel));
    }

    public void testContact() {
        // Set up the Parcelable object.
        Parcelable original = new Contact("123-456-7890");

        // Write the data.
        Parcel parcel = createParcel(original);

        // Verify that the received data is correct.
        assertEquals("Contact", original, Contact.CREATOR.createFromParcel(parcel));
    }

    public void testText() {
        // Set up the Parcelable object.
        Parcelable original = new Text.Builder().build();

        // Write the data.
        Parcel parcel = createParcel(original);

        // Verify that the received data is correct.
        assertEquals("Text", original, Text.CREATOR.createFromParcel(parcel));
    }

    public void testThread() {
        // Set up the Parcelable object.
        Parcelable original = new Thread(1, 100, 50, null);

        // Write the data.
        Parcel parcel = createParcel(original);

        // Verify that the received data is correct.
        assertEquals("Thread", original, Thread.CREATOR.createFromParcel(parcel));
    }
}