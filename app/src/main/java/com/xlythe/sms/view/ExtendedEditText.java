package com.xlythe.sms.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class ExtendedEditText extends EditText {

    private OnDismissKeyboardListener mListener;

    public ExtendedEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public ExtendedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public ExtendedEditText(Context context) {
        super(context);

    }

    public void setOnDismissKeyboardListener(OnDismissKeyboardListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            mListener.onDismissed();
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public interface OnDismissKeyboardListener {
        void onDismissed();
    }
}
