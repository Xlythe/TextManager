package com.xlythe.sms.fragment;

import android.app.Activity;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ScrollView;

import com.xlythe.sms.MessageActivity;
import com.xlythe.sms.R;
import com.xlythe.sms.ThreadActivity;
import com.xlythe.sms.adapter.CursorImageAdapter;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.ImageAttachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.VideoAttachment;

import java.io.IOException;

public class ScreenSlidePageFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_screen_slide_page, container, false);

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

        final Cursor c = cursorLoader.loadInBackground();

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(new CursorImageAdapter(getActivity(), c));

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                c.moveToPosition(position);
                String mediaId = c.getString(c.getColumnIndex(MediaStore.Files.FileColumns._ID));
                String data = c.getString(c.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                int type = c.getInt(c.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
                Uri content = MediaStore.Files.getContentUri("external");
                Attachment attachment;
                Uri uri;
                try {
                    switch (type) {
                        case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
                            // TODO: must be a bitmap for sending
                            // TODO: I probably need to add a to bitmap from uri in image attachment...
                            uri = Uri.withAppendedPath(content, mediaId);
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
                            attachment = new ImageAttachment(bitmap);
                            break;
                        default:
                            uri = Uri.parse(data);
                            attachment = new VideoAttachment(uri);
                            break;
                    }
                    TextManager.getInstance(getContext()).send(new Text.Builder(getContext())
                                    .recipient("2163138473")
                                    .attach(attachment)
                                    .build()
                    );
                } catch (IOException ioe) {
                    Log.d("photo fragment", "failed to find image: " + mediaId);
                }
            }
        });

        return rootView;
    }
}