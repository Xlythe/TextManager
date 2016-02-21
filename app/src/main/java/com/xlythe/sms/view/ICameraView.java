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
     * Takes a picture. Set a {@link PictureListener} to be
     * notified of when the picture has finished saving.
     */
    void takePicture();

    /**
     * Records a video. Set a {@link VideoListener} to be notified of when
     * the video has finished saving.
     */
    void startRecording();

    /**
     * Stops recording the video. It's recommended that you set a timeout when recording to avoid
     * excessively large files.
     */
    void stopRecording();

    /**
     * Returns true if recording.
     */
    boolean isRecording();

    /**
     * Listens for captured pictures.
     */
    void setPictureListener(PictureListener listener);

    /**
     * Listens for captured videos.
     */
    void setVideoListener(VideoListener listener);

    interface PictureListener {
        void onImageCaptured(File file);
    }

    interface VideoListener {
        void onVideoCaptured(File file);
    }
}
