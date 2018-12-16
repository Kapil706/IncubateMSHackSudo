package com.sudo.campusambassdor.controllers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.sudo.campusambassdor.ApplicationHelper;
import com.sudo.campusambassdor.R;
import com.sudo.campusambassdor.activities.BaseActivity;
import com.sudo.campusambassdor.activities.MainActivity;
import com.sudo.campusambassdor.enums.ProfileStatus;
import com.sudo.campusambassdor.managers.ProfileManager;
import com.sudo.campusambassdor.model.Post;



public class LikeController {

    private static final int ANIMATION_DURATION = 300;

    public enum AnimationType {
        COLOR_ANIM, BOUNCE_ANIM
    }

    private Context context;
    private String postId;

    private AnimationType likeAnimationType = LikeController.AnimationType.BOUNCE_ANIM;

    private TextView likeCounterTextView;
    private ImageView likesImageView;

    private boolean isListView = false;

    private boolean isLiked = false;
    private boolean likeIconInitialized = false;
    private boolean updatingLikeCounter = true;

    public LikeController(Context context, String postId, TextView likeCounterTextView,
                          ImageView likesImageView, boolean isListView) {
        this.context = context;
        this.postId = postId;
        this.likeCounterTextView = likeCounterTextView;
        this.likesImageView = likesImageView;
        this.isListView = isListView;
    }

    public void likeClickAction(long prevValue) {
        if (!updatingLikeCounter) {
            startAnimateLikeButton(likeAnimationType);

            if (!isLiked) {
                addLike(prevValue);
            } else {
                removeLike(prevValue);
            }
        }
    }

    public void likeClickActionLocal(Post post) {
        setUpdatingLikeCounter(false);
        likeClickAction(post.getLikesCount());
        updateLocalPostLikeCounter(post);
    }

    private void addLike(long prevValue) {
        updatingLikeCounter = true;
        isLiked = true;
        likeCounterTextView.setText(String.valueOf(prevValue + 1));
        ApplicationHelper.getDatabaseHelper().createOrUpdateLike(postId);
    }

    private void removeLike(long prevValue) {
        updatingLikeCounter = true;
        isLiked = false;
        likeCounterTextView.setText(String.valueOf(prevValue - 1));
        ApplicationHelper.getDatabaseHelper().removeLike(postId);
    }

    private void startAnimateLikeButton(AnimationType animationType) {
        switch (animationType) {
            case BOUNCE_ANIM:
                bounceAnimateImageView();
                break;
            case COLOR_ANIM:
                colorAnimateImageView();
                break;
        }
    }

    private void bounceAnimateImageView() {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(likesImageView, "scaleX", 0.2f, 1f);
        bounceAnimX.setDuration(ANIMATION_DURATION);
        bounceAnimX.setInterpolator(new BounceInterpolator());

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(likesImageView, "scaleY", 0.2f, 1f);
        bounceAnimY.setDuration(ANIMATION_DURATION);
        bounceAnimY.setInterpolator(new BounceInterpolator());
        bounceAnimY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                likesImageView.setImageResource(!isLiked ? R.drawable.ic_like_active
                        : R.drawable.ic_like);
            }
        });

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });

        animatorSet.play(bounceAnimX).with(bounceAnimY);
        animatorSet.start();
    }

    private void colorAnimateImageView() {
        final int activatedColor = context.getResources().getColor(R.color.like_icon_activated);

        final ValueAnimator colorAnim = !isLiked ? ObjectAnimator.ofFloat(0f, 1f)
                : ObjectAnimator.ofFloat(1f, 0f);
        colorAnim.setDuration(ANIMATION_DURATION);
        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float mul = (Float) animation.getAnimatedValue();
                int alpha = adjustAlpha(activatedColor, mul);
                likesImageView.setColorFilter(alpha, PorterDuff.Mode.SRC_ATOP);
                if (mul == 0.0) {
                    likesImageView.setColorFilter(null);
                }
            }
        });

        colorAnim.start();
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public AnimationType getLikeAnimationType() {
        return likeAnimationType;
    }

    public void setLikeAnimationType(AnimationType likeAnimationType) {
        this.likeAnimationType = likeAnimationType;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public boolean isLikeIconInitialized() {
        return likeIconInitialized;
    }

    public void setLikeIconInitialized(boolean likeIconInitialized) {
        this.likeIconInitialized = likeIconInitialized;
    }

    public boolean isUpdatingLikeCounter() {
        return updatingLikeCounter;
    }

    public void setUpdatingLikeCounter(boolean updatingLikeCounter) {
        this.updatingLikeCounter = updatingLikeCounter;
    }

    public void initLike(boolean isLiked) {
        if (!likeIconInitialized) {
            likesImageView.setImageResource(isLiked ? R.drawable.ic_like_active : R.drawable.ic_like);
            likeIconInitialized = true;
            this.isLiked = isLiked;
        }
    }

    private void updateLocalPostLikeCounter(Post post) {
        if (isLiked) {
            post.setLikesCount(post.getLikesCount() + 1);
        } else {
            post.setLikesCount(post.getLikesCount() - 1);
        }
    }

    public void handleLikeClickAction(BaseActivity baseActivity, Post post) {
        if (baseActivity.hasInternetConnection()) {
            ProfileStatus profileStatus = ProfileManager.getInstance(baseActivity).checkProfile();

            if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                if (isListView) {
                    likeClickActionLocal(post);
                } else {
                    likeClickAction(post.getLikesCount());
                }
            } else {
                baseActivity.doAuthorization(profileStatus);
            }
        } else {
            int message = R.string.internet_connection_failed;
            if (baseActivity instanceof MainActivity) {
                ((MainActivity) baseActivity).showFloatButtonRelatedSnackBar(message);
            } else {
                baseActivity.showSnackBar(message);
            }
        }
    }
}
