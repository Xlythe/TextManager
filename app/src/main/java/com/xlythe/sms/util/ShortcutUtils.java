package com.xlythe.sms.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.app.Person;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.xlythe.sms.MessageActivity;
import com.xlythe.sms.drawable.ProfileDrawable;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.Thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ShortcutUtils {
  private ShortcutUtils() {}

  @SuppressLint("InlinedApi")
  @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS})
  @RequiresApi(25)
  public static ShortcutInfoCompat createShortcutInfo(Context context, Thread thread) {
    TextManager textManager = TextManager.getInstance(context);

    Set<Contact> contacts = textManager.getMembersExceptMe(thread.getLatestMessage()).get();

    // A shortcut requires a label.
    StringBuilder label = new StringBuilder();
    for (Contact contact : contacts) {
      if (label.length() != 0) {
        label.append(", ");
      }
      label.append(contact.getDisplayName());
    }

    return new ShortcutInfoCompat.Builder(context, thread.getId())
            .setShortLabel(thread.getLatestMessage().getBody())
            .setIcon(getIcon(context, contacts))
            .setIntent(createShortcutIntent(context, thread.getId()))
            .setLongLived(true)
            .setPersons(getPeople(contacts))
            .build();
  }

  @RequiresApi(24)
  private static Person[] getPeople(Set<Contact> contacts) {
    List<Person> people = new ArrayList<>();
    for (Contact contact : contacts) {
      people.add(contact.asPersonCompat());
    }
    Collections.sort(people, Comparator.comparing(p -> p.getName() != null ? p.getName().toString() : ""));
    return people.toArray(new Person[0]);
  }

  @RequiresApi(23)
  private static IconCompat getIcon(Context context, Set<Contact> contacts) {
    ProfileDrawable drawable = new ProfileDrawable(context, contacts);

    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.draw(canvas);

    return IconCompat.createWithBitmap(bitmap);
  }

  private static Intent createShortcutIntent(Context context, String threadId) {
    Intent intent = new Intent(context, MessageActivity.class);
    intent.setAction(Intent.ACTION_VIEW);
    intent.putExtra(MessageActivity.EXTRA_THREAD_ID, threadId);
    return intent;
  }
}
