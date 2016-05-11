package com.xlythe.sms;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.VideoAttachment;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

public class MediaActivity extends AppCompatActivity {
    public static final String EXTRA_TEXT = "text";
    private static final int SHOW_PROGRESS = 2;
    private Text mText;
    private Handler mHandler = new MessageHandler(this);
    private ProgressBar mProgress;
    private VideoView mPlayer;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private ImageButton mPauseButton;
    private TextView mCurrentTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        mText = getIntent().getParcelableExtra(EXTRA_TEXT);

        final SubsamplingScaleImageView image = (SubsamplingScaleImageView) findViewById(R.id.image);
        mPlayer = (VideoView) findViewById(R.id.video);
        mPauseButton = (ImageButton) findViewById(R.id.play);
        mCurrentTime = (TextView) findViewById(R.id.duration);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        mProgress = (ProgressBar) findViewById(R.id.seek);
        mProgress.setMax(1000);

        mPauseButton.setOnClickListener(mPauseListener);

        Uri uri = mText.getAttachment().getUri();

        if (mText.getAttachment() instanceof VideoAttachment) {
            image.setVisibility(View.GONE);
            mPlayer.setVisibility(View.VISIBLE);
            mPlayer.setVideoURI(uri);

            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mp) {
                    mPlayer.start();
                    show();
                }
            });

        } else {
            mPlayer.setVisibility(View.GONE);
            mPauseButton.setVisibility(View.GONE);
            mCurrentTime.setVisibility(View.GONE);
            mProgress.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            image.setImage(ImageSource.uri(mText.getAttachment().getUri()));
        }
    }

    public void show() {
        updatePausePlay();
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show();
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null) {
            return 0;
        }

        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // add 200ms because it always stops a bit short
                long pos = 1000L * (position + 200) / duration;
                mProgress.setProgress( (int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }

    public void updatePausePlay() {
        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            mPauseButton.setImageResource(R.drawable.ic_play);
        }
    }

    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<MediaActivity> mView;

        MessageHandler(MediaActivity view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaActivity view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            view.setProgress();
            if (view.mPlayer.isPlaying()) {
                msg = obtainMessage(SHOW_PROGRESS);
                sendMessageDelayed(msg, 0);
            }
            view.updatePausePlay();
        }
    }
}
