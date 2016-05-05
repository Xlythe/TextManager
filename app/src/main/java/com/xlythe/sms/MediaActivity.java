package com.xlythe.sms;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.xlythe.textmanager.text.Text;
import com.xlythe.textmanager.text.VideoAttachment;

public class MediaActivity extends AppCompatActivity {
    public static final String EXTRA_TEXT = "text";
    private Text mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        mText = getIntent().getParcelableExtra(EXTRA_TEXT);

        final VideoView video = (VideoView) findViewById(R.id.video);
        final Button play = (Button) findViewById(R.id.play);
        final TextView duration = (TextView) findViewById(R.id.duration);
        final SeekBar seek = (SeekBar) findViewById(R.id.seek);
        final SubsamplingScaleImageView image = (SubsamplingScaleImageView) findViewById(R.id.image);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (video.isPlaying()) {
                    video.pause();
                } else {
                    video.start();
                }
            }
        });

        Uri uri = mText.getAttachment().getUri();

        if (mText.getAttachment() instanceof VideoAttachment) {
            image.setVisibility(View.GONE);
            video.setVisibility(View.VISIBLE);
            video.setVideoURI(uri);
            video.start();
            duration.setText(video.getDuration() + "");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (video.getCurrentPosition() < video.getDuration()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            return;
                        } catch (Exception e) {
                            return;
                        }
                        seek.setProgress(video.getCurrentPosition());
                    }
                }
            });
        } else {
            video.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            image.setImage(ImageSource.uri(mText.getAttachment().getUri()));
        }
    }
}
