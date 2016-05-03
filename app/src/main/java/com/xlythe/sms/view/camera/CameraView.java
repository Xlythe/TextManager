package com.xlythe.sms.view.camera;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import java.io.File;

public class CameraView extends BaseCameraView {

    private final ICameraModule mCameraModule;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        if (android.os.Build.VERSION.SDK_INT >= 21) {
//            mCameraModule = new Camera2Module(this);
//        } else {
            mCameraModule = new LegacyCameraModule(this);
//        }
    }

    @Override
    protected void onOpen() {
        mCameraModule.open();
    }

    @Override
    public void onClose() {
        mCameraModule.close();
    }

    @Override
    public void takePicture(File file) {
        mCameraModule.takePicture(file);
    }

    @Override
    public void startRecording(File file) {
        mCameraModule.startRecording(file);
    }

    @Override
    public void stopRecording() {
        mCameraModule.stopRecording();
    }

    @Override
    public boolean isRecording() {
        return mCameraModule.isRecording();
    }

    @Override
    public boolean hasFrontFacingCamera() {
        return mCameraModule.hasFrontFacingCamera();
    }

    @Override
    public boolean isUsingFrontFacingCamera() {
        return mCameraModule.isUsingFrontFacingCamera();
    }

    @Override
    public void toggleCamera() {
        mCameraModule.toggleCamera();
    }

    @Override
    public void focus(Rect focus, Rect metering) {
        mCameraModule.focus(focus, metering);
    }

    @Override
    public void setOnImageCapturedListener(OnImageCapturedListener l) {
        super.setOnImageCapturedListener(l);
        mCameraModule.setOnImageCapturedListener(l);
    }

    @Override
    public void setOnVideoCapturedListener(OnVideoCapturedListener l) {
        super.setOnVideoCapturedListener(l);
        mCameraModule.setOnVideoCapturedListener(l);
    }
}
