package com.sudo.campusambassdor.activities;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.sudo.campusambassdor.R;
import com.sudo.campusambassdor.managers.PostManager;
import com.sudo.campusambassdor.managers.listeners.OnPostCreatedListener;
import com.sudo.campusambassdor.model.Post;
import com.sudo.campusambassdor.utils.ValidationUtil;

public class CreatePostActivity extends PickImageActivity implements OnPostCreatedListener {
    private static final String TAG = CreatePostActivity.class.getSimpleName();
    public static final int CREATE_NEW_POST_REQUEST = 1;

    private ImageView imageView;
    private ProgressBar progressBar;
    private EditText titleEditText;
    private EditText descriptionEditText;

    private PostManager postManager;
    private boolean creatingPost = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_post_activity);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        postManager = PostManager.getInstance(CreatePostActivity.this);

        titleEditText = (EditText) findViewById(R.id.titleEditText);
        descriptionEditText = (EditText) findViewById(R.id.descriptionEditText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        imageView = (ImageView) findViewById(R.id.imageView);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSelectImageClick(v);
            }
        });
    }

    @Override
    public ProgressBar getProgressView() {
        return progressBar;
    }

    @Override
    public ImageView getImageView() {
        return imageView;
    }

    @Override
    public void onImagePikedAction() {
        loadImageToImageView();
    }

    private void attemptCreatePost() {
        // Reset errors.
        titleEditText.setError(null);
        descriptionEditText.setError(null);

        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        View focusView = null;
        boolean cancel = false;

        if (TextUtils.isEmpty(description)) {
            descriptionEditText.setError(getString(R.string.warning_empty_description));
            focusView = descriptionEditText;
            cancel = true;
        }

        if (TextUtils.isEmpty(title)) {
            titleEditText.setError(getString(R.string.warning_empty_title));
            focusView = titleEditText;
            cancel = true;
        } else if (!ValidationUtil.isPostTitleValid(title)) {
            titleEditText.setError(getString(R.string.error_post_title_length));
            focusView = titleEditText;
            cancel = true;
        }

        if (imageUri == null) {
            showWarningDialog(R.string.warning_empty_image);
            focusView = imageView;
            cancel = true;
        }

        if (!cancel) {
            creatingPost = true;
            showProgress(R.string.message_creating_post);
            hideKeyboard();

            Post post = new Post();
            post.setTitle(title);
            post.setDescription(description);
            post.setAuthorId(FirebaseAuth.getInstance().getCurrentUser().getUid());
            postManager.createPostWithImage(imageUri, CreatePostActivity.this, post);
        } else if (focusView != null) {
            focusView.requestFocus();
        }
    }

    @Override
    public void onPostCreated(boolean success) {
        hideProgress();
        Snackbar snackbar;

        if (success) {
            setResult(RESULT_OK);
            snackbar = Snackbar
                    .make(findViewById(android.R.id.content), R.string.message_post_was_created, Snackbar.LENGTH_LONG)
                    .setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            super.onDismissed(snackbar, event);
                            CreatePostActivity.this.finish();
                        }
                    });

        } else {
            creatingPost = false;
            snackbar = Snackbar.make(findViewById(android.R.id.content),
                    R.string.error_fail_create_post, Snackbar.LENGTH_LONG);
        }

        snackbar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.create_post_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.post:
                if (!creatingPost) {
                    if (hasInternetConnection()) {
                        attemptCreatePost();
                    } else {
                        showSnackBar(R.string.internet_connection_failed);
                    }
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
