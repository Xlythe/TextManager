package com.xlythe.sms.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xlythe.sms.R;
import com.xlythe.sms.adapter.AttachmentAdapter;
import com.xlythe.sms.decoration.GalleryItemDecoration;
import com.xlythe.textmanager.text.Attachment;
import com.xlythe.textmanager.text.ImageAttachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.VideoAttachment;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.xlythe.sms.util.PermissionUtils.hasPermissions;

public class GalleryFragment extends Fragment implements AttachmentAdapter.OnItemClickListener {
    public static final String ARG_COLOR = "color";
    public static final String ARG_MESSAGE = "message";

    private static final String[] REQUIRED_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= 33) {
            REQUIRED_PERMISSIONS = new String[] {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
            };
        } else if (Build.VERSION.SDK_INT >= 16) {
            REQUIRED_PERMISSIONS = new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            };
        } else {
            REQUIRED_PERMISSIONS = new String[0];
        }
    }

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 2;

    public static GalleryFragment newInstance(Text text, int color) {
        GalleryFragment fragment = new GalleryFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSAGE, text);
        args.putInt(ARG_COLOR, color);
        fragment.setArguments(args);
        return fragment;
    }

    @ColorInt private int mColor;
    private Text mText;
    private Cursor mCursor;
    private AttachmentAdapter mAdapter;
    private ViewGroup mContainer;
    private RecyclerView mAttachments;
    private View mPermissionPrompt;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            if (hasPermissions(getContext(), REQUIRED_PERMISSIONS)) {
                showGallery();
            } else {
                showPermissionPrompt();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_gallery, container, false);

        mColor = getArguments().getInt(ARG_COLOR);
        mText = getArguments().getParcelable(ARG_MESSAGE);

        mAttachments = (RecyclerView) rootView.findViewById(R.id.attachments);
        mAttachments.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mAttachments.addItemDecoration(new GalleryItemDecoration(getResources().getDrawable(R.drawable.divider_attach)));
        mAttachments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState > 0) {
                    mAdapter.clearSelection();
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        mPermissionPrompt = rootView.findViewById(R.id.layout_permissions);
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
        if (hasPermissions(getContext(), REQUIRED_PERMISSIONS)) {
            showGallery();
        } else {
            showPermissionPrompt();
        }
    }

    private void showGallery() {
        mAttachments.setVisibility(View.VISIBLE);
        mPermissionPrompt.setVisibility(View.GONE);

        mCursor = createCursor();
        mAdapter = new AttachmentAdapter(getContext(), mCursor, mColor, this);
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
    public void onItemClick(final int position, ImageView button) {
        if (mAdapter.isSelected(position)) {
            mAdapter.toggleSelection(position);
        } else {
            mAdapter.clearSelection();
            mAdapter.toggleSelection(position);
        }
        button.setOnClickListener(v -> {
            TextManager.getInstance(getContext()).send(buildAttachment(position)).to(mText);
            mContainer.setVisibility(View.GONE);
        });
    }

    @SuppressLint("Range")
    private Attachment buildAttachment(int position) {
        mCursor.moveToPosition(position);
        String mediaId = mCursor.getString(mCursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
        String data = mCursor.getString(mCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
        int type = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE));
        Uri content = MediaStore.Files.getContentUri("external");
        Attachment attachment;
        Uri uri;
        if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
            uri = Uri.withAppendedPath(content, mediaId);
            attachment = new ImageAttachment(uri);
        } else {
            uri = Uri.parse(data);
            attachment = new VideoAttachment(uri);
        }
        return attachment;
    }
}