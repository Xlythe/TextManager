package com.xlythe.sms.fragment;

import android.Manifest;
import android.content.CursorLoader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xlythe.sms.DividerItemDecoration;
import com.xlythe.sms.R;
import com.xlythe.sms.adapter.AttachmentAdapter;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.ImageAttachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.VideoAttachment;

public class GalleryFragment extends Fragment implements AttachmentAdapter.ViewHolder.ClickListener {
    public static final String ARG_COLOR = "color";
    public static final String ARG_MESSAGE = "message";

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 2;

    private int mColor;
    private Cursor mCursor;
    private Text mText;
    private AttachmentAdapter mAdapter;
    private ViewGroup mContainer;
    private RecyclerView mAttachments;
    private View mPermissionPrompt;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            if (hasPermissions(REQUIRED_PERMISSIONS)) {
                showGallery();
            } else {
                showPermissionPrompt();
            }
        }
    }

    /**
     * Returns true if all given permissions are available
     * */
    protected boolean hasPermissions(String... permissions) {
        boolean ok = true;
        for (String permission : permissions) {
            ok = ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED;
            if (!ok) break;
        }
        return ok;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_gallery, container, false);

        mColor = getArguments().getInt(ARG_COLOR);
        mText = getArguments().getParcelable(ARG_MESSAGE);

        mAttachments = (RecyclerView) rootView.findViewById(R.id.attachments);
        mAttachments.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mAttachments.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.divider_attach)));
        mAttachments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState > 0) {
                    mAdapter.clearSelection();
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        mPermissionPrompt = rootView.findViewById(R.id.permission_error);
        mPermissionPrompt.findViewById(R.id.request_permissions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasPermissions(REQUIRED_PERMISSIONS)) {
            showGallery();
        } else {
            showPermissionPrompt();
        }
    }

    private void showGallery() {
        mAttachments.setVisibility(View.VISIBLE);
        mPermissionPrompt.setVisibility(View.GONE);

        mCursor = createCursor();
        mAdapter = new AttachmentAdapter(this, getContext(), mCursor, mColor);
        mAttachments.setAdapter(mAdapter);
    }

    private void showPermissionPrompt() {
        mAttachments.setVisibility(View.GONE);
        mPermissionPrompt.setVisibility(View.VISIBLE);
    }

    private Cursor createCursor() {
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

        return cursorLoader.loadInBackground();
    }

    @Override
    public void onItemClicked(final int position, ImageView button) {
        if (mAdapter.isSelected(position)) {
            mAdapter.toggleSelection(position);
        } else {
            mAdapter.clearSelection();
            mAdapter.toggleSelection(position);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextManager.getInstance(getContext()).send(new Text.Builder(getContext())
                                .recipient(mText.getMembersExceptMe(getContext()))
                                .attach(buildAttachment(position))
                                .build()
                );
                mContainer.setVisibility(View.GONE);
            }
        });
    }

    private Attachment buildAttachment(int position) {
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
        return attachment;
    }
}