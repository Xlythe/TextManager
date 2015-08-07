package com.xlythe.sms;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;


public class CameraFragment extends Fragment {

    private Camera mCamera;
    private CameraPreview mPreview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_camera, container, false);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        //ThreadActivity activity = (ThreadActivity) getActivity();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(getActivity(), mCamera);
        FrameLayout preview = (FrameLayout) rootView.findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Log.d("camera","get camera");
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            Log.d("camera","camera active");
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.d("camera","camera failed");
        }
        return c; // returns null if camera is unavailable
    }
}
