package com.xlythe.sms.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

import com.xlythe.sms.drawable.ExtendedProfileDrawable;
import com.xlythe.textmanager.text.Contact;
import com.xlythe.textmanager.text.TextManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ContactEditText extends EditText {
    private static final String TAG = ContactEditText.class.getSimpleName();

    private final PendingText mPendingText = new PendingText();

    public ContactEditText(Context context) {
        super(context);
        init();
    }

    public ContactEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ContactEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        addTextChangedListener(new TextWatcher() {
            private final ArrayList<ImageSpan> mEmoticonsToRemove = new ArrayList<>();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Check if text is being removed.
                if (count > 0) {
                    int end = start + count;
                    Editable message = getEditableText();
                    ImageSpan[] list = message.getSpans(start, end, ImageSpan.class);

                    for (ImageSpan span : list) {
                        // Get only the emoticons that are inside of the changed region.
                        int spanStart = message.getSpanStart(span);
                        int spanEnd = message.getSpanEnd(span);
                        if ((spanStart < end) && (spanEnd > start)) {
                            // Add to remove list
                            mEmoticonsToRemove.add(span);
                        }
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                findPendingText(s);
            }

            private void findPendingText(CharSequence s) {
                // Grab all the spans currently in the EditText. They do not overlap, so we can sort by span start.
                final Editable message = getEditableText();
                ImageSpan[] spans = message.getSpans(0, message.length(), ImageSpan.class);
                Arrays.sort(spans, new Comparator<ImageSpan>() {
                    @Override
                    public int compare(ImageSpan lhs, ImageSpan rhs) {
                        return message.getSpanStart(lhs) - message.getSpanStart(rhs);
                    }
                });

                // Find the block of text (if any) that does not have a span yet. This is our pending text.
                int start = 0;
                int end = s.length();
                for (ImageSpan span : spans) {
                    int spanStart = message.getSpanStart(span);
                    int spanEnd = message.getSpanEnd(span);

                    // This span is beyond our starting position, meaning that there is a gap
                    if (spanStart > start) {
                        end = spanStart;
                        break;
                    }

                    start = spanEnd;
                }

                // The block of text is...
                mPendingText.start = start;
                mPendingText.end = end;
                mPendingText.text = s.subSequence(start, end).toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Commit the emoticons to be removed.
                Editable message = getEditableText();
                for (ImageSpan span : mEmoticonsToRemove) {
                    int start = message.getSpanStart(span);
                    int end = message.getSpanEnd(span);

                    // Remove the span
                    message.removeSpan(span);

                    // Remove the remaining emoticon text.
                    if (start != end) {
                        message.delete(start, end);
                    }
                }
                mEmoticonsToRemove.clear();

                // Break up the text at ;
                int indexOfBreakpoint = mPendingText.text.indexOf(';');
                if (indexOfBreakpoint != -1) {
                    mPendingText.text = mPendingText.text.substring(0, indexOfBreakpoint);
                    mPendingText.end = mPendingText.start + mPendingText.text.length() + 1;
                    insertPendingText();
                }
            }
        });
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        // This gets called in the super constructor, before we're set up. Ignore it in that case.
        if (mPendingText == null) return;

        if (selStart != selEnd) {
            // User has selected a large amount of text
            insertPendingText();
        } else if (selStart < mPendingText.start || selStart > mPendingText.end) {
            // User has moved out of the pending text
            insertPendingText();
        }
    }

    /**
     * Inserts the given Contact into the EditText
     */
    public void insert(Contact contact) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        // Create a drawable to represent this Contact
        Drawable drawable = new ExtendedProfileDrawable(getContext(), contact);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

        // Create the String that the real value is of
        String stringRepresentation = contact.getDisplayName() + ";";
        builder.append(stringRepresentation);

        // Map the drawable to the string
        builder.setSpan(
                new ImageSpan(drawable),
                builder.length() - stringRepresentation.length(),
                builder.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Grab the pending text info and then reset it
        int start = mPendingText.start;
        int end = mPendingText.end;
        mPendingText.reset();

        // Update the text
        getText().replace(start, end, builder);
    }

    /**
     * Inserts whatever pending text is left into the EditText
     */
    public void insertPendingText() {
        if (!TextUtils.isEmpty(mPendingText.text)) {
            insert(TextManager.getInstance(getContext()).lookupContact(mPendingText.text));
        }
    }

    public String getPendingText() {
        return mPendingText.text;
    }

    public ArrayList<Contact> getContacts() {
        final Editable message = getEditableText();
        ImageSpan[] spans = message.getSpans(0, message.length(), ImageSpan.class);
        Arrays.sort(spans, new Comparator<ImageSpan>() {
            @Override
            public int compare(ImageSpan lhs, ImageSpan rhs) {
                return message.getSpanStart(lhs) - message.getSpanStart(rhs);
            }
        });

        ArrayList<Contact> contacts = new ArrayList<>(spans.length);
        for (ImageSpan span : spans) {
            ExtendedProfileDrawable drawable = (ExtendedProfileDrawable) span.getDrawable();
            contacts.add(drawable.getContact());
        }
        return contacts;
    }

    public void setContacts(Collection<Contact> contacts) {
        setText(null);
        for (Contact contact : contacts) {
            insert(contact);
        }
    }

    /**
     * Holds the user's state (cursor position, text)
     */
    private static final class PendingText {
        int start;
        int end;
        String text = "";

        void reset() {
            start = 0;
            end = 0;
            text = "";
        }
    }
}
