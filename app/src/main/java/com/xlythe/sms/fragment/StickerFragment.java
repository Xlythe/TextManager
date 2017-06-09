package com.xlythe.sms.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xlythe.sms.R;
import com.xlythe.sms.adapter.StickerAdapter;
import com.xlythe.sms.pojo.Sticker;
import com.xlythe.sms.view.PreviewDialog;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.ImageAttachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;

public class StickerFragment extends Fragment {
    private static final String TAG = StickerFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String ARG_MESSAGE = "message";

    public static StickerFragment newInstance(Text text) {
        StickerFragment fragment = new StickerFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSAGE, text);
        fragment.setArguments(args);
        return fragment;
    }

    private Text mText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mText = getArguments().getParcelable(ARG_MESSAGE);

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_sticker, container, false);
        RecyclerView gridView = (RecyclerView) rootView.findViewById(R.id.content);
        gridView.setAdapter(new StickerAdapter(getContext(), new StickerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Sticker sticker) {
                TextManager.getInstance(getContext()).send(new ImageAttachment(sticker.getUri(getContext()), Attachment.Type.HIGH_RES)).to(mText);
            }

            @Override
            public void onItemLongClick(Sticker sticker) {
                new PreviewDialog(getContext(), sticker.getThumbnail()).show();
            }
        }));
        gridView.setLayoutManager(new GridAutofitLayoutManager(getContext()));
        return rootView;
    }

    private static class GridAutofitLayoutManager extends GridLayoutManager {
        private int mColumnWidth;
        private boolean mColumnWidthChanged = true;

        public GridAutofitLayoutManager(Context context) {
            // Initially set spanCount to 1, will be changed automatically later.
            super(context, 1);
        }

        private void setColumnWidth(int newColumnWidth) {
            log("setColumnWidth newColumnWidth=%s", newColumnWidth);
            if (newColumnWidth > 0 && newColumnWidth != mColumnWidth) {
                mColumnWidth = newColumnWidth;
                mColumnWidthChanged = true;
            }
        }

        private void measureChildren(RecyclerView.Recycler recycler) {
            if (getChildCount() == 0) {
                //Scrap measure one child
                View scrap = recycler.getViewForPosition(0);
                addView(scrap);
                measureChildWithMargins(scrap, 0, 0);

                /*
                 * We make some assumptions in this code based on every child
                 * view being the same size (i.e. a uniform grid). This allows
                 * us to compute the following values up front because they
                 * won't change.
                 */
                setColumnWidth(getDecoratedMeasuredWidth(scrap));

                detachAndScrapView(scrap, recycler);
            }

            if (mColumnWidthChanged && mColumnWidth > 0) {
                int totalSpace;
                if (getOrientation() == VERTICAL) {
                    totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
                } else {
                    totalSpace = getHeight() - getPaddingTop() - getPaddingBottom();
                }
                int spanCount = Math.max(1, totalSpace / mColumnWidth);
                log("totalSpace=%s, spanCount=%s", totalSpace, spanCount);
                setSpanCount(spanCount);
                mColumnWidthChanged = false;
            }
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            measureChildren(recycler);
            super.onLayoutChildren(recycler, state);
        }
    }

    private static void log(String msg, Object... args) {
        if (DEBUG) {
            Log.d(TAG, String.format(msg, args));
        }
    }
}
