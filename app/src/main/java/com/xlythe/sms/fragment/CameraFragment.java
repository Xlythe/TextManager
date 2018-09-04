package com.xlythe.sms.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xlythe.sms.R;
import com.xlythe.textmanager.text.ImageAttachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.VideoAttachment;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CameraFragment extends com.xlythe.fragment.camera.CameraFragment {
    public static final String ARG_MESSAGE = "message";

    private static final int VIDEO_MAX_DURATION = 10 * 1000;

    private Text mText;

    public static CameraFragment newInstance(Text text) {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSAGE, text);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onImageCaptured(File file) {
        TextManager.getInstance(getContext()).send(new ImageAttachment(Uri.fromFile(file))).to(mText);
    }

    @Override
    public void onVideoCaptured(File file) {
        TextManager.getInstance(getContext()).send(new VideoAttachment(Uri.fromFile(file))).to(mText);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mText = getArguments().getParcelable(ARG_MESSAGE);
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setMaxVideoDuration(VIDEO_MAX_DURATION);
    }
}
