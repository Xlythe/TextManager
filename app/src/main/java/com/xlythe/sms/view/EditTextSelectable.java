package com.xlythe.sms.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import java.util.HashSet;
import java.util.Set;

public class EditTextSelectable extends EditText {
    private final Set<OnSelectionChangedListener> mListeners = new HashSet<>();

    public EditTextSelectable(Context context) {
        super(context);
    }

    public EditTextSelectable(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextSelectable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void addOnSelectionChangedListener(OnSelectionChangedListener l) {
        mListeners.add(l);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        for (OnSelectionChangedListener l : mListeners) {
            l.onSelectionChanged(selStart, selEnd);
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selStart, int selEnd);
    }
}