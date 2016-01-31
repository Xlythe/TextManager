package com.xlythe.sms.fragment;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.xlythe.sms.DividerItemDecoration;
import com.xlythe.sms.R;
import com.xlythe.sms.adapter.AttachmentAdapter;
import com.xlythe.sms.util.ColorUtils;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.ImageAttachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.VideoAttachment;

import java.io.IOException;

public class ScreenSlidePageFragment extends Fragment implements AttachmentAdapter.ViewHolder.ClickListener {
    private static final String TAG = ScreenSlidePageFragment.class.getSimpleName();

    private int mColor;
    private Cursor mCursor;
    private String mRecipient;
    private AttachmentAdapter mAdapter;
    private ViewGroup mContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_screen_slide_page, container, false);

        mColor = getArguments().getInt("color");
        mRecipient = getArguments().getString("recipient");

        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
        };

        // Return only video and image metadata.
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        Uri queryUri = MediaStore.Files.getContentUri("external");

        CursorLoader cursorLoader = new CursorLoader(
                getContext(),
                queryUri,
                projection,
                selection,
                null,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        );

        mCursor = cursorLoader.loadInBackground();
        mAdapter = new AttachmentAdapter(this, getContext(), mCursor, mColor);

        RecyclerView attachments = (RecyclerView) rootView.findViewById(R.id.attachments);
        attachments.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        attachments.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.divider_attach)));
        attachments.setAdapter(mAdapter);

        attachments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState > 0) {
                    mAdapter.clearSelection();
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onItemClicked(final int position, ImageView button) {
        Log.d(TAG, "pressed");
        if (mAdapter.isSelected(position)) {
            mAdapter.toggleSelection(position);
        } else {
            mAdapter.clearSelection();
            mAdapter.toggleSelection(position);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "send");
                mCursor.moveToPosition(position);
                String mediaId = mCursor.getString(mCursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                String data = mCursor.getString(mCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                int type = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
                Uri content = MediaStore.Files.getContentUri("external");
                Attachment attachment;
                Uri uri;
                switch (type) {
                    case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
                        uri = Uri.withAppendedPath(content, mediaId);
                        attachment = new ImageAttachment(uri);
                        break;
                    default:
                        uri = Uri.parse(data);
                        attachment = new VideoAttachment(uri);
                        break;
                }
                TextManager.getInstance(getContext()).send(new Text.Builder(getContext())
                                .recipient(mRecipient)
                                .attach(attachment)
                                .build()
                );
                mContainer.setVisibility(View.GONE);
            }
        });
    }
}