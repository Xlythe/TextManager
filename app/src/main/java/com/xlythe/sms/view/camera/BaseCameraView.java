package com.xlythe.sms.view.camera;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.ViewConfiguration;
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

    // For tap-to-focus
    private final Rect mFocusingRect = new Rect();
    private final Rect mMeteringRect = new Rect();

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

    public abstract void focus(Rect focus, Rect metering);

    protected abstract int getRelativeCameraOrientation();

    private long start;

    private long delta() {
        return System.currentTimeMillis() - start;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                start = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                if (delta() < ViewConfiguration.getLongPressTimeout()) {
                    calculateTapArea(mFocusingRect, event.getX(), event.getY(), 1f);
                    calculateTapArea(mMeteringRect, event.getX(), event.getY(), 1.5f);
                    if (area(mFocusingRect) == 0 || area(mMeteringRect) == 0) {
                        break;
                    }
                    focus(mFocusingRect, mMeteringRect);
                }
                break;
        }
        return true;
    }

    /**
     * Returns the width * height of the given rect
     */
    private int area(Rect rect) {
        return rect.width() * rect.height();
    }

    /**
     * The area must be between -1000,-1000 and 1000,1000
     */
    private Rect calculateTapArea(Rect rect, float x, float y, float coefficient) {
        // Default to 300 (1/6th the total area) and scale by the coefficient
        int areaSize = Float.valueOf(300 * coefficient).intValue();

        // Rotate the coordinates if the camera orientation is different
        int width = getWidth();
        int height = getHeight();

        int relativeCameraOrientation = getRelativeCameraOrientation();
        int temp = -1;
        float tempf = -1f;
        switch (relativeCameraOrientation) {
            case 90:
                // Fall through
            case 270:
                // We're horizontal. Swap width/height. Swap x/y.
                temp = width;
                width = height;
                height = temp;

                tempf = x;
                x = y;
                y = tempf;
                break;
        }
        switch (relativeCameraOrientation) {
            case 180:
                // Fall through
            case 270:
                // We're up side down. Fix x/y.
                x = width - x;
                y = height - y;
                break;
        }

        // Grab the x, y position from within the View and normalize it to -1000 to 1000
        x = -1000 + 2000 * (x / width);
        y = -1000 + 2000 * (y / height);


        // Modify the rect to the bounding area
        rect.top = (int) y - areaSize / 2;
        rect.left = (int) x - areaSize / 2;
        rect.bottom = rect.top + areaSize;
        rect.right = rect.left + areaSize;

        // Cap at -1000 to 1000
        rect.top = rangeLimit(rect.top);
        rect.left = rangeLimit(rect.left);
        rect.bottom = rangeLimit(rect.bottom);
        rect.right = rangeLimit(rect.right);

        return rect;
    }

    private int rangeLimit(int val) {
        int floor = Math.max(val, -1000);
        int ceiling = Math.min(floor, 1000);
        return ceiling;
    }

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
