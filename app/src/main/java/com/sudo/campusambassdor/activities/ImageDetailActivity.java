package com.sudo.campusambassdor.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.sudo.campusambassdor.R;
import com.sudo.campusambassdor.utils.ImageUtil;
import com.sudo.campusambassdor.views.TouchImageView;

public class ImageDetailActivity extends BaseActivity {

    private static final String TAG = ImageDetailActivity.class.getSimpleName();

    public static final String IMAGE_URL_EXTRA_KEY = "ImageDetailActivity.IMAGE_URL_EXTRA_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        TouchImageView touchImageView = (TouchImageView) findViewById(R.id.touchImageView);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        final ViewGroup viewGroup = (ViewGroup) findViewById(R.id.image_detail_container);

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);

            viewGroup.setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int vis) {
                            if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                                actionBar.hide();
                            } else {
                                actionBar.show();
                            }
                        }
                    });

            // Start low profile mode and hide ActionBar
            viewGroup.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            actionBar.hide();
        }

        String imageUrl = getIntent().getStringExtra(IMAGE_URL_EXTRA_KEY);

        ImageUtil imageUtil = ImageUtil.getInstance(this);
        imageUtil.getFullImage(imageUrl, touchImageView, R.drawable.ic_stub);

        touchImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int vis = viewGroup.getSystemUiVisibility();
                if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                    viewGroup.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                } else {
                    viewGroup.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                }
            }
        });
    }
}
