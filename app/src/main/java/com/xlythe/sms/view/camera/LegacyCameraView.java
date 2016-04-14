package com.xlythe.sms.view.camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LegacyCameraView extends BaseCameraView {
    private static final int INVALID_CAMERA_ID = -1;

    private int mActiveCamera = INVALID_CAMERA_ID;
    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;

    public LegacyCameraView(Context context) {
        this(context, null);
    }

    public LegacyCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LegacyCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight, int previewWidth, int previewHeight) {
        if (DEBUG) {
            Log.d(TAG, String.format("Configuring SurfaceView matrix: "
                            + "viewWidth=%s, viewHeight=%s, previewWidth=%s, previewHeight=%s",
                    viewWidth, viewHeight, previewWidth, previewHeight));
        }
        Matrix matrix = new Matrix();

        float ratio;
        if (previewHeight >= previewWidth) {
            ratio = (float) previewHeight / (float) previewWidth;
        } else {
            ratio = (float) previewWidth / (float) previewHeight;
        }

        matrix.postScale(1f, ratio);

        setTransform(matrix);
    }

    @Override
    protected void onOpen() {
        Log.d(TAG, "onOpen() activeCamera="+getActiveCamera());
        mCamera = Camera.open(getActiveCamera());

        try {
            mCamera.setPreviewTexture(getSurfaceTexture());

            Camera.Parameters parameters = mCamera.getParameters();
            if(getDisplayRotation() == Surface.ROTATION_0) {
                mCamera.setDisplayOrientation(90);
            } else if(getDisplayRotation() == Surface.ROTATION_270) {
                mCamera.setDisplayOrientation(180);
            }
            mPreviewSize = chooseOptimalPreviewSize(mCamera.getParameters().getSupportedPreviewSizes(), getWidth(), getHeight());
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(parameters);

            configureTransform(getWidth(), getHeight(), mPreviewSize.width, mPreviewSize.height);

            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();

            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onClose() {
        Log.d(TAG, "onClose() activeCamera="+getActiveCamera());
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void takePicture(File file) {
        // TODO callback for when the picture is finished saving
        mCamera.takePicture(null, null, new LegacyPictureListener(file));
    }

    @Override
    public void startRecording(File file) {

    }

    @Override
    public void stopRecording() {

    }

    @Override
    public boolean isRecording() {
        return false;
    }

    @Override
    public void toggleCamera() {
        mActiveCamera = (mActiveCamera + 1) % Camera.getNumberOfCameras();
        // Close the existing camera
        onClose();

        // And open the new one
        onOpen();
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
