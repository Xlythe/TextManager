package com.xlythe.sms;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.VideoAttachment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MediaActivity extends AppCompatActivity {
    public static final String EXTRA_TEXT = "text";
    private Text mText;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        mText = getIntent().getParcelableExtra(EXTRA_TEXT);

        final VideoView video = (VideoView) findViewById(R.id.video);
        final ImageButton play = (ImageButton) findViewById(R.id.play);
        final TextView duration = (TextView) findViewById(R.id.duration);
        final ProgressBar seek = (ProgressBar) findViewById(R.id.seek);
        final SubsamplingScaleImageView image = (SubsamplingScaleImageView) findViewById(R.id.image);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (video.isPlaying()) {
                    video.pause();
                    play.setBackgroundResource(R.drawable.ic_play);
                } else {
                    video.start();
                    play.setBackgroundResource(R.drawable.ic_pause);
                }
            }
        });

        Uri uri = mText.getAttachment().getUri();

        if (mText.getAttachment() instanceof VideoAttachment) {
            image.setVisibility(View.GONE);
            video.setVisibility(View.VISIBLE);
            video.setVideoURI(uri);
            video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    video.start();
                    seek.setMax(video.getDuration());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            seek.setProgress(video.getCurrentPosition());
                            DateFormat milliFormat = new SimpleDateFormat("m:ss");
                            String millis = milliFormat.format(video.getCurrentPosition());
                            duration.setText(millis);
                            mHandler.post(this);
                        }
                    });
                }
            });

        } else {
            video.setVisibility(View.GONE);
            play.setVisibility(View.GONE);
            duration.setVisibility(View.GONE);
            seek.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            image.setImage(ImageSource.uri(mText.getAttachment().getUri()));
        }
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
