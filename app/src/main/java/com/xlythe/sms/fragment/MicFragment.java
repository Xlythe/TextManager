package com.xlythe.sms.fragment;

import android.animation.Animator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.xlythe.sms.R;

import java.io.IOException;

import androidx.fragment.app.Fragment;

public class MicFragment extends Fragment {
    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName;

    private double mLastLevel = 0;
    private Thread mThread;
    private static final int SAMPLE_DELAY = 75;
    private long startTime = 0L;

    private ImageView mMic;
    private ImageView mImageView;
    private ImageButton mRecord;
    private Button mPlay;
    private TextView mTextTimer;

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;

    long timeInMillies = 0L;
    long timeSwap = 0L;
    long finalTime = 0L;

    public MicFragment() {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/fail.3gp";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_mic, container, false);

        mMic = (ImageView) rootView.findViewById(R.id.mic_rec);
        mImageView = (ImageView) rootView.findViewById(R.id.level);
        mImageView.animate().scaleX(0).setDuration(0);
        mImageView.animate().scaleY(0).setDuration(0);
        mRecord = (ImageButton) rootView.findViewById(R.id.record);
        mPlay = (Button) rootView.findViewById(R.id.play);
        mTextTimer = (TextView) rootView.findViewById(R.id.timer);

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlay(true);
            }
        });

        mRecord.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mMic.getBackground().setColorFilter(Color.parseColor("#03a9f4"), PorterDuff.Mode.SRC_IN);
                    mRecord.getBackground().setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_IN);
                    Log.d("Recording","stop");
                    stopRecording();
                    return true;
                } else {
                    if (mRecorder==null) {
                        mMic.getBackground().setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_IN);
                        mRecord.getBackground().setColorFilter(Color.parseColor("#03a9f4"), PorterDuff.Mode.SRC_IN);
                        Log.d("Recording", "start");
                        startTime = SystemClock.uptimeMillis();
                        startRecording();
                        mThread = new Thread(new Runnable() {
                            public void run() {
                                while(mThread != null && !mThread.isInterrupted()){
                                    //Let's make the thread sleep for a the approximate sampling time
                                    try{Thread.sleep(SAMPLE_DELAY);}catch(InterruptedException ie){ie.printStackTrace();}

                                    timeInMillies = SystemClock.uptimeMillis() - startTime;
                                    finalTime = timeSwap + timeInMillies;

                                    if(mRecorder!=null) {
                                        mLastLevel = Math.min(mRecorder.getMaxAmplitude(), 30000);
                                    }
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int seconds = (int) (finalTime / 1000);
                                            int minutes = seconds / 60;
                                            seconds = seconds % 60;
                                            mTextTimer.setText("" + minutes + ":" + String.format("%02d", seconds));
                                            if (mLastLevel > mImageView.getScaleX() * 30000f) {
                                                float scaledLevel = Math.min(0.4f + (Float.parseFloat(mLastLevel + "") / 30000f), 1);
                                                Log.d("Volume", scaledLevel + "");
                                                mImageView.animate().cancel();
                                                mImageView.animate().scaleY(scaledLevel).scaleX(scaledLevel).setDuration(300).setListener(new Animator.AnimatorListener() {
                                                    @Override
                                                    public void onAnimationStart(Animator animator) {

                                                    }

                                                    @Override
                                                    public void onAnimationEnd(Animator animator) {
                                                        mImageView.animate().setListener(null);
                                                        mImageView.animate().cancel();
                                                        mImageView.animate().scaleY(0.4f).scaleX(0.4f).setDuration(1000);
                                                    }

                                                    @Override
                                                    public void onAnimationCancel(Animator animator) {
                                                    }

                                                    @Override
                                                    public void onAnimationRepeat(Animator animator) {

                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }
                        });
                        mThread.start();
                    }
                    return true;
                }
            }
        });

        return rootView;
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        if (mRecorder != null) {
            mRecorder.release();
        }
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mFileName);
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }

        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        mImageView.animate().cancel();
        mImageView.animate().scaleY(0).scaleX(0).setDuration(1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }
}
