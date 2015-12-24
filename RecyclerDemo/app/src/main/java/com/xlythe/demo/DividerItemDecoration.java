package com.xlythe.demo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private float px;

    public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

    public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;


    private int mOrientation;

    public DividerItemDecoration(Context context, int orientation, int size) {
        Resources r = context.getResources();
        px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, r.getDisplayMetrics());
        setOrientation(orientation);
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
            throw new IllegalArgumentException("invalid orientation");
        }
        mOrientation = orientation;

    }

    @Override
    public void onDraw(Canvas c, final RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        Log.d("Divider", "onDraw");
        if (mOrientation == VERTICAL_LIST) {
            drawVertical(parent);
        }
    }


    public void drawVertical(RecyclerView parent) {

        final int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                final View childTop = parent.getChildAt(i-1);
                final View childBottom = parent.getChildAt(i+1);
                if (childTop instanceof LinearLayout) {
                    final View card = ((LinearLayout) childTop).getChildAt(0);
                    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                    params.setMargins(0, 0, 0, (int)px);
                    card.setLayoutParams(params);
                }
                if (childBottom instanceof LinearLayout) {
                    final View card = ((LinearLayout) childBottom).getChildAt(0);
                    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                    params.setMargins(0, (int)px, 0, 0);
                    card.setLayoutParams(params);
                }
            }
            else {
                final View childTop = parent.getChildAt(i-1);
                final View childBottom = parent.getChildAt(i+1);
                if (childTop instanceof LinearLayout) {
                    final View card = ((LinearLayout) child).getChildAt(0);
                    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                    int bottom = params.bottomMargin;
                    params.setMargins(0, 0, 0, bottom);
                    card.setLayoutParams(params);
                }
                if (childBottom instanceof LinearLayout) {
                    final View card = ((LinearLayout) child).getChildAt(0);
                    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                    int top = params.topMargin;
                    params.setMargins(0, top, 0, 0);
                    card.setLayoutParams(params);
                }
            }
        }
    }
}