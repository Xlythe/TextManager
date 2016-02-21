package com.xlythe.sms.view;

import android.content.Context;
import android.util.AttributeSet;

import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.CameraHostProvider;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;

import java.io.File;
import java.io.IOException;

public class LegacyCameraView extends com.commonsware.cwac.camera.CameraView implements ICameraView {

    public LegacyCameraView(Context context) {
        this(context, null);
    }

    public LegacyCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LegacyCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
        getHost().getVideoPath().delete();
        getHost().getPhotoPath().delete();
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
            getHost().mVideoListener.onVideoCaptured(getHost().getVideoPath());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPictureListener(PictureListener listener) {
        getHost().setPictureListener(listener);
    }

    @Override
    public void setVideoListener(VideoListener listener) {
        getHost().setVideoListener(listener);
    }

    @Override
    public Host getHost() {
        return (Host) super.getHost();
    }

    public interface HostProvider extends CameraHostProvider {
        Host getCameraHost();
    }

    public static final class Host extends SimpleCameraHost {
        private ICameraView.PictureListener mPictureListener;
        private ICameraView.VideoListener mVideoListener;

        private Context mContext;

        public Host(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public void saveImage(PictureTransaction xact, byte[] image) {
            super.saveImage(xact, image);
            if (mPictureListener != null) {
                mPictureListener.onImageCaptured(getPhotoPath());
            }
        }

        public void setPictureListener(ICameraView.PictureListener listener) {
            mPictureListener = listener;
        }

        public void setVideoListener(ICameraView.VideoListener listener) {
            mVideoListener = listener;
        }

        public Context getContext() {
            return mContext;
        }

        @Override
        public File getPhotoPath() {
            return new File(getContext().getCacheDir(), "temp.png");
        }

        @Override
        public File getVideoPath() {
            return new File(getContext().getCacheDir(), "temp.mp4");
        }
    }
}
