package com.xlythe.sms;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.xlythe.sms.view.camera.CameraView;

public class CameraActivity extends AppCompatActivity {
    private CameraView mCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCameraView = new CameraView(this);
        mCameraView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 900));
        setContentView(mCameraView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCameraView.open();
    }

    @Override
    protected void onStop() {
        mCameraView.close();
        super.onStop();
    }
}