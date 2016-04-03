package com.xlythe.sms.util;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;

import com.xlythe.sms.R;

public class ActionBarUtils {
    public static void grayUpArrow(AppCompatActivity activity) {
        final Drawable upArrow = activity.getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(activity.getResources().getColor(R.color.icon_color), PorterDuff.Mode.SRC_ATOP);
        activity.getSupportActionBar().setHomeAsUpIndicator(upArrow);
    }

    public static void whiteUpArrow(AppCompatActivity activity) {
        final Drawable upArrow = activity.getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        activity.getSupportActionBar().setHomeAsUpIndicator(upArrow);
    }
}
