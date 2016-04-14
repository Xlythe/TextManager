package com.xlythe.sms.view.camera;

import android.hardware.Camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LegacyPictureListener implements Camera.PictureCallback {
    private final File mFile;

    public LegacyPictureListener(File file) {
        mFile = file;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (!mFile.exists() && !mFile.mkdirs()) {
            throw new RuntimeException("Unable to create the file " + mFile);
        }

        try {
            FileOutputStream fos = new FileOutputStream(mFile);
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
