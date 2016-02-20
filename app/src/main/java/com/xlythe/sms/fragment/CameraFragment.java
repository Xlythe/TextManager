package com.xlythe.sms.fragment;

import android.Manifest;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xlythe.sms.R;

import static com.xlythe.sms.util.PermissionUtils.hasPermissions;

public class CameraFragment extends Fragment {
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 2;

    private View mCamera;
    private View mPermissionPrompt;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            if (hasPermissions(getContext(), REQUIRED_PERMISSIONS)) {
                showCamera();
            } else {
                showPermissionPrompt();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasPermissions(getContext(), REQUIRED_PERMISSIONS)) {
            showCamera();
        } else {
            showPermissionPrompt();
        }
    }

    private void showCamera() {
        mCamera.setVisibility(View.VISIBLE);
        mPermissionPrompt.setVisibility(View.GONE);
    }

    private void showPermissionPrompt() {
        mCamera.setVisibility(View.GONE);
        mPermissionPrompt.setVisibility(View.VISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_camera, container, false);

        mCamera = rootView.findViewById(R.id.camera);

        mPermissionPrompt = rootView.findViewById(R.id.permission_error);
        mPermissionPrompt.findViewById(R.id.request_permissions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        });

        return rootView;
    }
}
