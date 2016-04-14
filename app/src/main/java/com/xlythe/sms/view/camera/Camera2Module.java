package com.xlythe.sms.view.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@TargetApi(21)
public class Camera2Module extends ICameraModule {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private enum State {
        PREVIEW, WAITING_LOCK, WAITING_PRECAPTURE, WAITING_NON_PRECAPTURE, PICTURE_TAKEN;
    }

    private final CameraManager mCameraManager;
    private String mActiveCamera;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private State mState = State.PREVIEW;
    private CaptureRequest mPreviewRequest;
    private PreviewSurface mPreviewSurface = new PreviewSurface(this);
    private PhotoSurface mPhotoSurface = new PhotoSurface(this);
    private VideoSurface mVideoSurface = new VideoSurface(this);
    private CameraSurface[] mCameraSurfaces = new CameraSurface[] {
            mPreviewSurface,
            mPhotoSurface,
            mVideoSurface
    };
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // The camera has opened. Start the preview now.
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.w(TAG, "Camera disconnected");
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.e(TAG, "Camera crashed: " + error);
            cameraDevice.close();
            mCameraDevice = null;
        }
    };
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            // TODO
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }
    };

    Camera2Module(CameraView view) {
        super(view);
        mCameraManager = (CameraManager) view.getContext().getSystemService(Context.CAMERA_SERVICE);
    }

    private void setState(State state) {
        Log.d(TAG, "Setting State: " + state.name());
        mState = state;
    }

    @SuppressWarnings({"MissingPermission"})
    @Override
    public void open() {
        startBackgroundThread();

        try {
            mActiveCamera = getActiveCamera();
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mActiveCamera);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            for (CameraSurface surface : mCameraSurfaces) {
                surface.initialize(map);
            }

            configureTransform(getWidth(), getHeight(), mPreviewSurface.getWidth(), mPreviewSurface.getHeight());
            mCameraManager.openCamera(mActiveCamera, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        for (CameraSurface surface : mCameraSurfaces) {
            surface.close();
        }

        stopBackgroundThread();
    }

    @Override
    public void takePicture(File file) {
        // TODO
    }

    @Override
    public void startRecording(File file) {
        // TODO
    }

    @Override
    public void stopRecording() {
        mVideoSurface.stopRecording();
    }

    @Override
    public boolean isRecording() {
        return mVideoSurface.mIsRecordingVideo;
    }

    private String getActiveCamera() throws CameraAccessException {
        return mActiveCamera == null ? getDefaultCamera() : mActiveCamera;
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread == null) {
            return;
        }
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight, int previewWidth, int previewHeight) {
        if (DEBUG) {
            Log.d(TAG, String.format("Configuring SurfaceView matrix: "
                    + "viewWidth=%s, viewHeight=%s, previewWidth=%s, previewHeight=%s",
                    viewWidth, viewHeight, previewWidth, previewHeight));
        }
        int rotation = getDisplayRotation();

// TODO
        boolean iCanHazPhone = true;
        if (iCanHazPhone) {
            int temp = previewWidth;
            previewWidth = previewHeight;
            previewHeight = temp;
        }

        double aspectRatio = (double) previewHeight / (double) previewWidth;
        int newWidth, newHeight;

        if (getHeight() > (int) (viewWidth * aspectRatio)) {
            newWidth = (int)(viewHeight / aspectRatio);
            newHeight = viewHeight;
        } else {
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        }

        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;

        Matrix txform = new Matrix();

        getTransform(txform);

        float xscale = (float) newWidth / (float) viewWidth;
        float yscale = (float) newHeight / (float) viewHeight;

        txform.setScale(xscale, yscale);

        switch(rotation) {
            case Surface.ROTATION_90:
                txform.postRotate(270, newWidth / 2, newHeight / 2);
                break;

            case Surface.ROTATION_270:
                txform.postRotate(90, newWidth / 2, newHeight / 2);
                break;
        }

        txform.postTranslate(xoff, yoff);

        setTransform(txform);
    }

    //http://stackoverflow.com/questions/31839021/how-do-i-determine-the-default-orientation-of-camera-preview-frames
    // Legacy Camera.CameraInfo.orientation, compensateForMirroring = true
    // Camera2 CameraCharacteristics#get(CameraCharacteristics.SENSOR_ORIENTATION), compensateForMirroring = false
    static int getRelativeImageOrientation(int displayRotation, int sensorOrientation,
                                           boolean isFrontFacing, boolean compensateForMirroring) {
        int result;
        if (isFrontFacing) {
            result = (sensorOrientation + displayRotation) % 360;
            if (compensateForMirroring) {
                result = (360 - result) % 360;
            }
        } else {
            result = (sensorOrientation - displayRotation + 360) % 360;
        }
        return result;
    }

    private void startPreview() {
        try {
            SurfaceTexture texture = getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSurface.getWidth(), mPreviewSurface.getHeight());

            final CaptureRequest.Builder previewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            List<Surface> surfaces = new ArrayList<>();

            // The preview is, well, the preview. This surface draws straight to the CameraView
            surfaces.add(mPreviewSurface.getSurface());
            previewBuilder.addTarget(mPreviewSurface.getSurface());

            // The video surface is for recording. When you call startRecording, it starts capturing
            // the bytes given to it.
//            surfaces.add(mVideoSurface.getSurface());
//            previewBuilder.addTarget(mVideoSurface.getSurface());

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // When the session is ready, we start displaying the preview.
                    mCaptureSession = cameraCaptureSession;
                    try {
                        // Auto focus should be continuous for camera preview.
                        previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                        // Finally, we start displaying the camera preview.
                        mPreviewRequest = previewBuilder.build();
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException | IllegalStateException e) {
                        // Crashes on rotation. However, it does restore itself.
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "Configure failed");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IllegalStateException | IllegalArgumentException e) {
            // Crashes if the Camera is interacted with while still loading
            e.printStackTrace();
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private String getDefaultCamera() throws CameraAccessException {
        for (String cameraId : mCameraManager.getCameraIdList()) {
            if (isBackFacing(cameraId)) {
                return cameraId;
            }
        }
        return mCameraManager.getCameraIdList()[0];
    }

    @Override
    public boolean hasFrontFacingCamera() {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                boolean frontFacing = isFrontFacing(cameraId);
                if (frontFacing) return true;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isUsingFrontFacingCamera() {
        try {
            return isFrontFacing(getActiveCamera());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isFrontFacing(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        return facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT;
    }

    private boolean isBackFacing(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        return facing != null && facing == CameraCharacteristics.LENS_FACING_BACK;
    }

    @Override
    public void toggleCamera() {
        int position = 0;

        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                if (cameraId.equals(mActiveCamera)) {
                    break;
                }
                position++;
            }
            close();
            mActiveCamera = mCameraManager.getCameraIdList()[(position + 1) % mCameraManager.getCameraIdList().length];
            open();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static class ImageSaver implements Runnable {
        private final Image mImage;
        private final File mFile;

        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static abstract class CameraSurface {
        final Camera2Module mCameraView;

        Size mSize;
        private boolean mInitialized = false;

        CameraSurface(Camera2Module cameraView) {
            mCameraView = cameraView;
        }

        abstract void initialize(StreamConfigurationMap map);

        void initialize(Size size) {
            mSize = size;
            mInitialized = true;
        }

        boolean isInitialized() {
            return mInitialized;
        }

        int getWidth() {
            return mSize.getWidth();
        }

        int getHeight() {
            return mSize.getHeight();
        }

        abstract void close();

        abstract Surface getSurface();
    }

    private static final class PreviewSurface extends CameraSurface {
        private static Size chooseOptimalSize(Size[] choices, int width, int height) {
            if (DEBUG) {
                Log.d(TAG, String.format("Initializing PreviewSurface with width=%s and height=%s", width, height));
            }
            // Collect the supported resolutions that are at least as big as the preview Surface
            List<Size> bigEnough = new ArrayList<>();
            for (Size option : choices) {
                if (option.getWidth() >= width && option.getHeight() >= height) {
                    bigEnough.add(option);
                }
            }

            // Pick the smallest of those, assuming we found any
            if (bigEnough.size() > 0) {
                return Collections.min(bigEnough, new CompareSizesByArea());
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size");
                return choices[0];
            }
        }

        private Surface mSurface;

        PreviewSurface(Camera2Module cameraView) {
            super(cameraView);
        }

        void initialize(StreamConfigurationMap map) {
            super.initialize(chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), mCameraView.getWidth(), mCameraView.getHeight()));

            SurfaceTexture texture = mCameraView.getSurfaceTexture();
            texture.setDefaultBufferSize(getWidth(), getHeight());

            // This is the output Surface we need to start preview.
            mSurface = new Surface(texture);
        }

        @Override
        Surface getSurface() {
            return mSurface;
        }

        @Override
        void close() {

        }
    }

    private static final class PhotoSurface extends CameraSurface {
        private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mCameraView.mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
            }
        };

        private ImageReader mImageReader;
        private OnImageCapturedListener mPhotoListener;
        private File mFile;

        PhotoSurface(Camera2Module cameraView) {
            super(cameraView);
        }

        void initialize(StreamConfigurationMap map) {
            super.initialize(Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea()));

            mImageReader = ImageReader.newInstance(getWidth(), getHeight(), ImageFormat.JPEG, 2 /*maxImages*/);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraView.mBackgroundHandler);
        }

        void takePicture(File file, OnImageCapturedListener listener) {
            mFile = file;
            mPhotoListener = listener;
        }

        void onPictureTaken() {
            mPhotoListener.onImageCaptured(mFile);
        }

        @Override
        Surface getSurface() {
            return mImageReader.getSurface();
        }

        @Override
        void close() {
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        }
    }

    private static final class VideoSurface extends CameraSurface {
        private static Size chooseVideoSize(Size[] choices) {
            for (Size size : choices) {
                if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                    return size;
                }
            }
            Log.e(TAG, "Couldn't find any suitable video size");
            return choices[0];
        }

        private boolean mIsRecordingVideo;
        private MediaRecorder mMediaRecorder;
        private OnVideoCapturedListener mVideoListener;
        private File mFile;

        VideoSurface(Camera2Module cameraView) {
            super(cameraView);
        }

        void initialize(StreamConfigurationMap map) {
            super.initialize(chooseVideoSize(map.getOutputSizes(MediaRecorder.class)));

//            mMediaRecorder = new MediaRecorder();
//            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//            mMediaRecorder.setOutputFile(mFile.getAbsolutePath());
//            mMediaRecorder.setVideoEncodingBitRate(10000000);
//            mMediaRecorder.setVideoFrameRate(30);
//            mMediaRecorder.setVideoSize(getWidth(), getHeight());
//            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//            int rotation = mCameraView.getDisplayRotation();
//            int orientation = ORIENTATIONS.get(rotation);
//            mMediaRecorder.setOrientationHint(orientation);
//            try {
//                mMediaRecorder.prepare();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        @Override
        Surface getSurface() {
            return mMediaRecorder.getSurface();
        }

        void startRecording(File file, OnVideoCapturedListener listener) {
            mVideoListener = listener;
            try {
                mIsRecordingVideo = true;
                mMediaRecorder.start();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        void stopRecording() {
            mIsRecordingVideo = false;
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            if (mVideoListener != null) {
                mVideoListener.onVideoCaptured(mFile);
            }
        }

        @Override
        void close() {
            if (mMediaRecorder != null) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        }
    }
}
