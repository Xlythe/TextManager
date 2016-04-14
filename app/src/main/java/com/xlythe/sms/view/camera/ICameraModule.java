package com.xlythe.sms.view.camera;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.TextureView;

import java.io.File;

public abstract class ICameraModule {
    static final String TAG = "CameraModule";
    static final boolean DEBUG = true;

    private final CameraView mView;

    ICameraModule(CameraView view) {
        mView = view;
    }

    public int getWidth() {
        return mView.getWidth();
    }

    public int getHeight() {
        return mView.getHeight();
    }

    public int getDisplayRotation() {
        return mView.getDisplayRotation();
    }

    public SurfaceTexture getSurfaceTexture() {
        return mView.getSurfaceTexture();
    }

    public Matrix getTransform(Matrix matrix) {
        return mView.getTransform(matrix);
    }

    public void setTransform(Matrix matrix) {
        mView.setTransform(matrix);
    }

    /*
     * Opens the camera and starts displaying a preview. You are in charge of checking if the
     * phone has PackageManager.FEATURE_CAMERA_ANY and, if you are targeting Android M+, that
     * the phone has the following permissions:
     *       Manifest.permission.CAMERA
     *       Manifest.permission.RECORD_AUDIO
     *       Manifest.permission.WRITE_EXTERNAL_STORAGE
     */
    public abstract void open();

    /*
     * Closes the camera.
     */
    public abstract void close();

    public abstract void takePicture(File file);

    public abstract void startRecording(File file);

    public abstract void stopRecording();

    public abstract boolean isRecording();

    public abstract void toggleCamera();

    public abstract boolean hasFrontFacingCamera();

    public abstract boolean isUsingFrontFacingCamera();

    public void setOnImageCapturedListener(OnImageCapturedListener l) {

    }

    public OnImageCapturedListener getOnImageCapturedListener() {
        return null;
    }

    public void setOnVideoCapturedListener(OnVideoCapturedListener l) {

    }

    public OnVideoCapturedListener getOnVideoCapturedListener() {
        return null;
    }

    interface OnImageCapturedListener {
        void onImageCaptured(File file);
    }

    interface OnVideoCapturedListener {
        void onVideoCaptured(File file);
    }
}
