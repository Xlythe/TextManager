package com.xlythe.sms.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;

import com.xlythe.sms.R;

public class PreviewDialog extends Dialog {
    private final int mResId;

    public PreviewDialog(Context context, int resId) {
        super(context, R.style.PreviewDialog);
        mResId = resId;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        setContentView(R.layout.dialog_sticker_preview);
        ImageView imageView = (ImageView) findViewById(R.id.image);
        imageView.setImageResource(mResId);
    }
}
