package com.xlythe.sms.view;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.AttributeSet;
import android.util.Log;

import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.CameraHostProvider;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;

import java.io.IOException;

public class LegacyCameraView extends com.commonsware.cwac.camera.CameraView implements ICameraView {

    public LegacyCameraView(Context context) {
        this(context, null);
    }

    public LegacyCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LegacyCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(new HostContextWrapper(context), attrs, defStyle);
    }

    @Override
    public void open() {
        super.onResume();
    }

    @Override
    public void close() {
        if (isRecording()) {
            stopRecording();
        }
        super.onPause();
    }

    @Override
    public void takePicture() {
        super.takePicture(false, true);
    }

    @Override
    public void startRecording() {
        try {
            super.record();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopRecording() {
        try {
            super.stopRecording();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPictureListener(PictureListener listener) {
        ((HostContextWrapper) getContext()).getCameraHost().setPictureListener(listener);
    }

    @Override
    public void setVideoListener(VideoListener listener) {
        ((HostContextWrapper) getContext()).getCameraHost().setVideoListener(listener);
    }

    private static final class Host extends SimpleCameraHost {
        private PictureListener mPictureListener;
        private VideoListener mVideoListener;

        Host(Context context) {
            super(context);
        }



        @Override
        public void saveImage(PictureTransaction xact, byte[] image) {
            super.saveImage(xact, image);
            if (mPictureListener != null) {
                mPictureListener.onImageCaptured(getPhotoPath());
            }
        }

        public void setPictureListener(PictureListener listener) {
            mPictureListener = listener;
        }

        public void setVideoListener(VideoListener listener) {
            mVideoListener = listener;
        }
    }

    private static final class HostContextWrapper extends ContextWrapper implements CameraHostProvider {
        private final Host mHost;
        HostContextWrapper(Context context) {
            super(context);
            mHost = new Host(this);
        }

        @Override
        public Host getCameraHost() {
            return mHost;
        }
    }
}
