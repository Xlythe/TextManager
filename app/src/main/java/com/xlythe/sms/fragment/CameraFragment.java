package com.xlythe.sms.fragment;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xlythe.sms.R;
import com.xlythe.sms.view.camera.BaseCameraView;
import com.xlythe.textmanager.text.ImageAttachment;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.TextManager;
import com.xlythe.textmanager.text.VideoAttachment;

import java.io.File;

import static com.xlythe.sms.util.PermissionUtils.hasPermissions;

public class CameraFragment extends Fragment implements BaseCameraView.OnImageCapturedListener, BaseCameraView.OnVideoCapturedListener {
    public static final String ARG_MESSAGE = "message";

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 3;

    private View mCameraHolder;
    private View mPermissionPrompt;
    private BaseCameraView mCamera;

    private Text mText;

    public static CameraFragment newInstance(Text text) {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSAGE, text);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onImageCaptured(final File file) {
        TextManager.getInstance(getContext()).send(new ImageAttachment(Uri.fromFile(file))).to(mText);
    }

    @Override
    public void onVideoCaptured(final File file) {
        TextManager.getInstance(getContext()).send(new VideoAttachment(Uri.fromFile(file))).to(mText);
    }

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
        mCameraHolder.setVisibility(View.VISIBLE);
        mPermissionPrompt.setVisibility(View.GONE);
        mCamera.open();
    }

    private void showPermissionPrompt() {
        mCameraHolder.setVisibility(View.GONE);
        mPermissionPrompt.setVisibility(View.VISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_camera, container, false);

        mText = getArguments().getParcelable(ARG_MESSAGE);

        mCameraHolder = rootView.findViewById(R.id.layout_camera);
        mCamera = (BaseCameraView) rootView.findViewById(R.id.camera);

        mCamera.setOnImageCapturedListener(this);
        mCamera.setOnVideoCapturedListener(this);

        final ImageView toggleCamera = (ImageView) mCameraHolder.findViewById(R.id.btn_toggle_camera);
        toggleCamera.setVisibility(mCamera.hasFrontFacingCamera() ? View.VISIBLE : View.GONE);
        toggleCamera.setImageResource(mCamera.isUsingFrontFacingCamera() ? R.drawable.camera_back : R.drawable.camera_front);
        toggleCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.toggleCamera();
                toggleCamera.setImageResource(mCamera.isUsingFrontFacingCamera() ? R.drawable.camera_back : R.drawable.camera_front);
            }
        });

        ImageView capture = (ImageView) mCameraHolder.findViewById(R.id.btn_capture);
        capture.setOnTouchListener(new View.OnTouchListener() {
            private final int TAP = 1;
            private final int HOLD = 2;
            private final int RELEASE = 3;

            private final long LONG_PRESS = ViewConfiguration.getLongPressTimeout();
            private final long RELEASE_TIMEOUT = LONG_PRESS + 10 * 1000; // max 10 seconds

            private long start;
            private final Handler mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case TAP:
                            onTap();
                            break;
                        case HOLD:
                            onHold();
                            break;
                        case RELEASE:
                            onRelease();
                            break;
                    }
                }
            };

            private void onTap() {
                mCamera.takePicture(new File(getContext().getCacheDir(), "TODO.jpg"));
            }

            private void onHold() {
                vibrate();
                mCamera.startRecording(new File(getContext().getCacheDir(), "TODO.mp4"));
            }

            private void onRelease() {
                mCamera.stopRecording();
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        start = System.currentTimeMillis();
                        mHandler.sendEmptyMessageDelayed(HOLD, LONG_PRESS);
                        mHandler.sendEmptyMessageDelayed(RELEASE, RELEASE_TIMEOUT);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        clearHandler();
                        if (delta() > LONG_PRESS && delta() < RELEASE_TIMEOUT) {
                            mHandler.sendEmptyMessage(RELEASE);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        clearHandler();
                        if (delta() < LONG_PRESS) {
                            mHandler.sendEmptyMessage(TAP);
                        } else if (delta() < RELEASE_TIMEOUT) {
                            mHandler.sendEmptyMessage(RELEASE);
                        }
                        break;
                }
                return true;
            }

            private long delta() {
                return System.currentTimeMillis() - start;
            }

            private void vibrate() {
                Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(25);
                }
            }

            private void clearHandler() {
                mHandler.removeMessages(TAP);
                mHandler.removeMessages(HOLD);
                mHandler.removeMessages(RELEASE);
            }
        });

        mPermissionPrompt = rootView.findViewById(R.id.permission_error);
        mPermissionPrompt.findViewById(R.id.request_permissions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        });

        return rootView;
    }

    @Override
    public void onPause() {
        mCamera.close();
        super.onPause();
    }
}
