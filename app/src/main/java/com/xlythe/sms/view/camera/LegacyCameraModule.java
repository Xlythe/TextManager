package com.xlythe.sms.view.camera;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LegacyCameraModule extends ICameraModule {
    private static final int INVALID_CAMERA_ID = -1;

    private int mActiveCamera = INVALID_CAMERA_ID;
    private Camera mCamera;
    private Camera.Size mPreviewSize;

    private final Rect mFocusRect = new Rect();

    private MediaRecorder mVideoRecorder;
    private File mVideoFile;

    LegacyCameraModule(CameraView view) {
        super(view);
    }

    @Override
    public void open() {
        Log.d(TAG, "onOpen() activeCamera="+getActiveCamera());
        mCamera = Camera.open(getActiveCamera());

        try {
            mCamera.setPreviewTexture(getSurfaceTexture());

            Camera.Parameters parameters = mCamera.getParameters();
            int cameraOrientation = getRelativeCameraOrientation();
            mCamera.setDisplayOrientation(cameraOrientation);
            mPreviewSize = chooseOptimalPreviewSize(mCamera.getParameters().getSupportedPreviewSizes(), getWidth(), getHeight());
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(parameters);
            configureTransform(getWidth(), getHeight(), mPreviewSize.width, mPreviewSize.height, cameraOrientation);

            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        Log.d(TAG, "onClose() activeCamera="+getActiveCamera());
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void takePicture(File file) {
        mCamera.takePicture(null, null, new LegacyPictureListener(file, getRelativeCameraOrientation(), getOnImageCapturedListener()));
    }

    @Override
    public void startRecording(File file) {
        mVideoFile = file;
        mVideoRecorder = new MediaRecorder();

        mCamera.unlock();
        mVideoRecorder.setCamera(mCamera);

        mVideoRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mVideoRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mVideoRecorder.setProfile(cpHigh);
        mVideoRecorder.setOutputFile(file.getAbsolutePath());
        mVideoRecorder.setMaxDuration(30000); // 30 seconds
        mVideoRecorder.setMaxFileSize(10000000); // Approximately 10 megabytes
        mVideoRecorder.setOrientationHint(getRelativeCameraOrientation());
        mVideoRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                        Log.w(TAG, "Max duration for recording reached");
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        Log.w(TAG, "Max filesize for recording reached");
                        break;
                }
            }
        });

        try {
            mVideoRecorder.prepare();
            mVideoRecorder.start();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mVideoRecorder = null;
        }
    }

    @Override
    public void stopRecording() {
        if (mVideoRecorder != null) {
            mVideoRecorder.stop();
            mVideoRecorder = null;
        }
        if (getOnVideoCapturedListener() != null) {
            getOnVideoCapturedListener().onVideoCaptured(mVideoFile);
        }
    }

    @Override
    public boolean isRecording() {
        return mVideoRecorder != null;
    }

    @Override
    public void toggleCamera() {
        close();
        mActiveCamera = (mActiveCamera + 1) % Camera.getNumberOfCameras();
        open();
    }

    @Override
    public void focus(Rect focus, Rect metering) {
        Log.d(TAG, String.format("Focus: focus=%s, metering=%s", focus, metering));
        if (mCamera != null) {
            mCamera.cancelAutoFocus();

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            if (parameters.getMaxNumFocusAreas() > 0) {
                parameters.setFocusAreas(Arrays.asList(new Camera.Area(focus, 1000)));
            }

            if (parameters.getMaxNumMeteringAreas() > 0) {
                parameters.setMeteringAreas(Arrays.asList(new Camera.Area(metering, 1000)));
            }

            mCamera.setParameters(parameters);
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.d(TAG, "AutoFocus: " + success);
                }
            });
        }
    }

    @Override
    public boolean hasFrontFacingCamera() {
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUsingFrontFacingCamera() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(getActiveCamera(), info);
        return info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    @Override
    protected int getRelativeCameraOrientation() {
        return getRelativeImageOrientation(getDisplayRotation(), getSensorOrientation(), isUsingFrontFacingCamera(), true);
    }

    private int getSensorOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(getActiveCamera(), info);
        return info.orientation;
    }

    private int getActiveCamera() {
        if (mActiveCamera != INVALID_CAMERA_ID) {
            return mActiveCamera;
        }

        int numberOfCameras = Camera.getNumberOfCameras();
        if (numberOfCameras == 0) {
            return INVALID_CAMERA_ID;
        }

        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mActiveCamera = i;
                return mActiveCamera;
            }
        }

        mActiveCamera = 0;
        return mActiveCamera;
    }

    private static Camera.Size chooseOptimalPreviewSize(List<Camera.Size> choices, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, String.format("Initializing PreviewSurface with width=%s and height=%s", width, height));
        }
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        for (Camera.Size option : choices) {
            if (option.width >= width && option.height >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices.get(0);
        }
    }

    static class CompareSizesByArea implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height -
                    (long) rhs.width * rhs.height);
        }
    }
}
