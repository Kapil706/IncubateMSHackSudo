package com.sudo.campusambassdor.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sudo.campusambassdor.ApplicationHelper;
import com.sudo.campusambassdor.R;
import com.sudo.campusambassdor.adapters.CommentsAdapter;
import com.sudo.campusambassdor.controllers.LikeController;
import com.sudo.campusambassdor.enums.ProfileStatus;
import com.sudo.campusambassdor.managers.PostManager;
import com.sudo.campusambassdor.managers.ProfileManager;
import com.sudo.campusambassdor.managers.listeners.OnDataChangedListener;
import com.sudo.campusambassdor.managers.listeners.OnObjectChangedListener;
import com.sudo.campusambassdor.managers.listeners.OnObjectExistListener;
import com.sudo.campusambassdor.model.Comment;
import com.sudo.campusambassdor.model.Like;
import com.sudo.campusambassdor.model.Post;
import com.sudo.campusambassdor.model.Profile;
import com.sudo.campusambassdor.utils.FormatterUtil;
import com.sudo.campusambassdor.utils.ImageUtil;

import java.util.List;

public class PostDetailsActivity extends BaseActivity {

    public static final String POST_EXTRA_KEY = "PostDetailsActivity.POST_EXTRA_KEY";
    private static final int TIME_OUT_LOADING_COMMENTS = 30000;

    private EditText commentEditText;
    private Post post;
    private ScrollView scrollView;
    private ViewGroup likesContainer;
    private ImageView likesImageView;
    private TextView commentsLabel;
    private TextView likeCounterTextView;
    private TextView commentsCountTextView;
    private TextView authorTextView;
    private TextView dateTextView;
    private ImageView authorImageView;
    private ProgressBar progressBar;
    private ImageView postImageView;
    private TextView titleTextView;
    private TextView descriptionEditText;
    private ProgressBar commentsProgressBar;
    private LinearLayout commentsContainer;
    private TextView warningCommentsTextView;

    private boolean attemptToLoadComments = false;
    private CommentsAdapter commentsAdapter;

    private MenuItem complainActionMenuItem;

    private String postId;

    private PostManager postManager;
    private ProfileManager profileManager;
    private ImageUtil imageUtil;
    private LikeController likeController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        profileManager = ProfileManager.getInstance(this);
        postManager = PostManager.getInstance(this);
        imageUtil = ImageUtil.getInstance(this);

        post = (Post) getIntent().getSerializableExtra(POST_EXTRA_KEY);
        postId = post.getId();

        titleTextView = (TextView) findViewById(R.id.titleTextView);
        descriptionEditText = (TextView) findViewById(R.id.descriptionEditText);
        postImageView = (ImageView) findViewById(R.id.postImageView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        commentsContainer = (LinearLayout) findViewById(R.id.commentsContainer);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        commentsLabel = (TextView) findViewById(R.id.commentsLabel);
        commentEditText = (EditText) findViewById(R.id.commentEditText);
        likesContainer = (ViewGroup) findViewById(R.id.likesContainer);
        likesImageView = (ImageView) findViewById(R.id.likesImageView);
        authorImageView = (ImageView) findViewById(R.id.authorImageView);
        authorTextView = (TextView) findViewById(R.id.authorTextView);
        likeCounterTextView = (TextView) findViewById(R.id.likeCounterTextView);
        commentsCountTextView = (TextView) findViewById(R.id.commentsCountTextView);
        dateTextView = (TextView) findViewById(R.id.dateTextView);
        commentsProgressBar = (ProgressBar) findViewById(R.id.commentsProgressBar);
        warningCommentsTextView = (TextView) findViewById(R.id.warningCommentsTextView);

        final Button sendButton = (Button) findViewById(R.id.sendButton);

        commentsAdapter = new CommentsAdapter(commentsContainer);
        initLikes();

        postManager.getPost(this, postId, createOnPostChangeListener());
        postManager.getCommentsList(this, postId, createOnCommentsChangedDataListener(commentsAdapter));

        postImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageDetailScreen();
            }
        });

        commentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                sendButton.setEnabled(charSequence.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasInternetConnection()) {
                    ProfileStatus profileStatus = ProfileManager.getInstance(PostDetailsActivity.this).checkProfile();

                    if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                        sendComment();
                    } else {
                        doAuthorization(profileStatus);
                    }
                } else {
                    showSnackBar(R.string.internet_connection_failed);
                }
            }
        });

        commentsCountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollToFirstComment();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        postManager.closeListeners(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getIntent().putExtra(POST_EXTRA_KEY, post);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    hideKeyBoard();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private OnObjectChangedListener<Post> createOnPostChangeListener() {
        return new OnObjectChangedListener<Post>() {
            @Override
            public void onObjectChanged(Post obj) {
                post = obj;
                fillPostFields();
                updateCounters();
                initLikeButtonState();
            }
        };
    }

    private void scrollToFirstComment() {
        if (post.getCommentsCount() > 0) {
            scrollView.smoothScrollTo(0, commentsLabel.getTop());
        }
    }

    private void fillPostFields() {
        titleTextView.setText(post.getTitle());
        descriptionEditText.setText(post.getDescription());
        loadPostDetailsImage();
        loadAuthorImage();
    }

    private void loadPostDetailsImage() {
        String imageUrl = post.getImagePath();
        imageUtil.getFullImage(imageUrl, postImageView, R.drawable.ic_stub);
    }

    private void loadAuthorImage() {
        if (post.getAuthorId() != null) {
            profileManager.getProfile(this, post.getAuthorId(), createProfileChangeListener());
        }
    }

    private void updateCounters() {
        long commentsCount = post.getCommentsCount();
        commentsCountTextView.setText(String.valueOf(commentsCount));
        commentsLabel.setText(String.format(getString(R.string.label_comments), commentsCount));
        likeCounterTextView.setText(String.valueOf(post.getLikesCount()));
        likeController.setUpdatingLikeCounter(false);

        CharSequence date = FormatterUtil.getRelativeTimeSpanString(this, post.getCreatedDate());
        dateTextView.setText(date);

        if (commentsCount == 0) {
            commentsLabel.setVisibility(View.GONE);
            commentsProgressBar.setVisibility(View.GONE);
        } else if (commentsLabel.getVisibility() != View.VISIBLE) {
            commentsLabel.setVisibility(View.VISIBLE);
        }
    }

    private OnObjectChangedListener<Profile> createProfileChangeListener() {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                if (obj.getPhotoUrl() != null) {
                    imageUtil.getImageThumb(obj.getPhotoUrl(),
                            authorImageView, R.drawable.ic_stub, R.drawable.ic_stub);
                }

                authorTextView.setText(obj.getUsername());
            }
        };
    }

    private OnDataChangedListener<Comment> createOnCommentsChangedDataListener(final CommentsAdapter commentsAdapter) {
        attemptToLoadComments = true;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (attemptToLoadComments) {
                    commentsProgressBar.setVisibility(View.GONE);
                    warningCommentsTextView.setVisibility(View.VISIBLE);
                }
            }
        }, TIME_OUT_LOADING_COMMENTS);


        return new OnDataChangedListener<Comment>() {
            @Override
            public void onListChanged(List<Comment> list) {
                attemptToLoadComments = false;
                commentsProgressBar.setVisibility(View.GONE);
                if (list.size() > 0) {
                    commentsContainer.setVisibility(View.VISIBLE);
                    warningCommentsTextView.setVisibility(View.GONE);
                    commentsAdapter.setList(list);
                }
            }
        };
    }

    private void openImageDetailScreen() {
        Intent intent = new Intent(this, ImageDetailActivity.class);
        intent.putExtra(ImageDetailActivity.IMAGE_URL_EXTRA_KEY, post.getImagePath());
        startActivity(intent);
    }

    private OnObjectExistListener<Like> createOnLikeObjectExistListener() {
        return new OnObjectExistListener<Like>() {
            @Override
            public void onDataChanged(boolean exist) {
                likeController.initLike(exist);
            }
        };
    }

    private void initLikeButtonState() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            postManager.hasCurrentUserLike(post.getId(), firebaseUser.getUid(), createOnLikeObjectExistListener());
        }
    }

    private void initLikes() {
        likeController = new LikeController(this, post.getId(), likeCounterTextView, likesImageView, false);

        likesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeController.handleLikeClickAction(PostDetailsActivity.this, post);
            }
        });

        //long click for changing animation
        likesContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (likeController.getLikeAnimationType() == LikeController.AnimationType.BOUNCE_ANIM) {
                    likeController.setLikeAnimationType(LikeController.AnimationType.COLOR_ANIM);
                } else {
                    likeController.setLikeAnimationType(LikeController.AnimationType.BOUNCE_ANIM);
                }

                Snackbar snackbar = Snackbar
                        .make(likesContainer, "Animation was changed", Snackbar.LENGTH_LONG);

                snackbar.show();
                return true;
            }
        });
    }

    private void sendComment() {
        String commentText = commentEditText.getText().toString();

        if (commentText.length() > 0) {
            ApplicationHelper.getDatabaseHelper().createOrUpdateComment(commentText, post.getId());
            commentEditText.setText(null);
            commentEditText.clearFocus();
            hideKeyBoard();
            scrollToFirstComment();
        }
    }

    private void hideKeyBoard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_details_menu, menu);
        complainActionMenuItem = menu.findItem(R.id.complain_action);
        if (post.isHasComplain()) {
            complainActionMenuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.complain_action:
                ProfileStatus profileStatus = profileManager.checkProfile();

                if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                    openComplainDialog();
                } else {
                    doAuthorization(profileStatus);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openComplainDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_complain)
                .setMessage(R.string.complain_text)
                .setNegativeButton(R.string.button_title_cancel, null)
                .setPositiveButton(R.string.add_complain, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addComplain();
                    }
                });

        builder.create().show();
    }

    private void addComplain() {
        postManager.addComplain(post);
        complainActionMenuItem.setVisible(false);
        showSnackBar(R.string.complain_sent);
    }

}
