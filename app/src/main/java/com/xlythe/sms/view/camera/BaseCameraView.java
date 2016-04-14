package com.xlythe.sms.view.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.TextureView;
import android.view.WindowManager;

import java.io.File;

public abstract class BaseCameraView extends TextureView {
    static final String TAG = BaseCameraView.class.getSimpleName();
    static final boolean DEBUG = true;

    private enum Status {
        OPEN, CLOSED, AWAITING_TEXTURE
    }

    /**
     * {@link SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.v(TAG, "Surface Texture now available.");
            synchronized (BaseCameraView.this) {
                if (getStatus() == Status.AWAITING_TEXTURE) {
                    setStatus(Status.OPEN);
                    onOpen();
                }
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            synchronized (BaseCameraView.this) {
                if (getStatus() == Status.OPEN) {
                    Log.w(TAG, "Surface destroyed but was not closed.");
                    close();
                }
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {}
    };

    private Status mStatus = Status.CLOSED;
    private OnImageCapturedListener mOnImageCapturedListener;
    private OnVideoCapturedListener mOnVideoCapturedListener;

    public BaseCameraView(Context context) {
        this(context, null);
    }

    public BaseCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSurfaceTextureListener(mSurfaceTextureListener);
    }

    public synchronized Status getStatus() {
        return mStatus;
    }

    private synchronized void setStatus(Status status) {
        if (mStatus == status) {
            return;
        }

        if (DEBUG) {
            Log.v(TAG, "Camera state set to " + status.name());
        }
        mStatus = status;
    }

    /*
     * Opens the camera and starts displaying a preview. You are in charge of checking if the
     * phone has PackageManager.FEATURE_CAMERA_ANY and, if you are targeting Android M+, that
     * the phone has the following permissions:
     *       Manifest.permission.CAMERA
     *       Manifest.permission.RECORD_AUDIO
     *       Manifest.permission.WRITE_EXTERNAL_STORAGE
     */
    public synchronized void open() {
        if (isAvailable()) {
            setStatus(Status.OPEN);
            onOpen();
        } else {
            setStatus(Status.AWAITING_TEXTURE);
        }
    }

    /*
     * Closes the camera.
     */
    public synchronized void close() {
        setStatus(Status.CLOSED);
        onClose();
    }

    protected int getDisplayRotation() {
        Display display;
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            display = getDisplay();
        } else {
            display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        }
        return display.getRotation();
    }

    public abstract void takePicture(File file);

    public abstract void startRecording(File file);

    public abstract void stopRecording();

    public abstract boolean isRecording();

    public abstract void toggleCamera();

    public abstract boolean hasFrontFacingCamera();

    public abstract boolean isUsingFrontFacingCamera();

    protected void onOpen() {}

    protected void onClose() {}

    public void setOnImageCapturedListener(OnImageCapturedListener l) {
        mOnImageCapturedListener = l;
    }

    protected OnImageCapturedListener getOnImageCapturedListener() {
        return mOnImageCapturedListener;
    }

    public void setOnVideoCapturedListener(OnVideoCapturedListener l) {
        mOnVideoCapturedListener = l;
    }

    protected OnVideoCapturedListener getOnVideoCapturedListener() {
        return mOnVideoCapturedListener;
    }

    public interface OnImageCapturedListener {
        void onImageCaptured(File file);
    }

    public interface OnVideoCapturedListener {
        void onVideoCaptured(File file);
    }
}
