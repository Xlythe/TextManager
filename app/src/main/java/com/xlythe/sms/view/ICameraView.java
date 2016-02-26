package com.xlythe.sms.view;

import java.io.File;

public interface ICameraView {
    /**
     * Connects to the camera and starts displaying a live feed. This will crash if you do not have
     * {@link android.Manifest.permission.CAMERA} permissions.
     */
    void open();

    /**
     * Releases the camera
     */
    void close();

    /**
     * Takes a picture. Set a {@link CameraListener} to be
     * notified of when the picture has finished saving.
     */
    void takePicture(CameraListener listener);

    /**
     * Records a video. Set a {@link CameraListener} to be notified of when
     * the video has finished saving.
     */
    void startRecording(CameraListener listener);

    /**
     * Stops recording the video. It's recommended that you set a timeout when recording to avoid
     * excessively large files.
     */
    void stopRecording();

    /**
     * Returns true if recording.
     */
    boolean isRecording();

    boolean hasFrontFacingCamera();

    void toggleCamera();

    interface CameraListener {
        void onCaptured(File file);
    }
}
