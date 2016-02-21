package com.xlythe.sms;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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

        getSupportActionBar().hide();

        mText = getIntent().getParcelableExtra(EXTRA_TEXT);

        VideoView video = (VideoView) findViewById(R.id.video);
        SubsamplingScaleImageView image = (SubsamplingScaleImageView) findViewById(R.id.image);

        Uri uri = mText.getAttachment().getUri();

        if (mText.getAttachment() instanceof VideoAttachment) {
            image.setVisibility(View.GONE);
            video.setVisibility(View.VISIBLE);
            video.setVideoURI(uri);
            video.start();
        } else {
            video.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            image.setImage(ImageSource.uri(mText.getAttachment().getUri()));
        }
    }
}
