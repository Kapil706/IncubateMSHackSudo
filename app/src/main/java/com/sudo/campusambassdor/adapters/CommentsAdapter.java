package com.sudo.campusambassdor.adapters;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sudo.campusambassdor.ExpandableTextView;
import com.sudo.campusambassdor.R;
import com.sudo.campusambassdor.managers.ProfileManager;
import com.sudo.campusambassdor.managers.listeners.OnObjectChangedListener;
import com.sudo.campusambassdor.model.Comment;
import com.sudo.campusambassdor.model.Profile;
import com.sudo.campusambassdor.utils.FormatterUtil;
import com.sudo.campusambassdor.utils.ImageUtil;

import java.util.ArrayList;
import java.util.List;


public class CommentsAdapter {
    private static final String TAG = CommentsAdapter.class.getName();

    private ViewGroup parent;
    private List<Comment> commentList = new ArrayList<>();
    private ImageUtil imageUtil;
    private LayoutInflater inflater;
    private ProfileManager profileManager;


    public CommentsAdapter(ViewGroup parent) {
        this.parent = parent;
        imageUtil = ImageUtil.getInstance(parent.getContext().getApplicationContext());
        inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        profileManager = ProfileManager.getInstance(parent.getContext().getApplicationContext());
    }

    private void initListView() {
        parent.removeAllViews();
        for (int i = 0; i < commentList.size(); i++) {
            inflater.inflate(R.layout.custom_divider, parent, true);
            Comment comment = commentList.get(i);
            parent.addView(getView(comment));
        }
    }

    private View getView(Comment comment) {
        final View convertView = inflater.inflate(R.layout.comment_list_item, parent, false);

        ImageView avatarImageView = (ImageView) convertView.findViewById(R.id.avatarImageView);
        ExpandableTextView commentTextView = (ExpandableTextView) convertView.findViewById(R.id.commentText);
        TextView dateTextView = (TextView) convertView.findViewById(R.id.dateTextView);

        String authorId = comment.getAuthorId();
        if (authorId != null)
            profileManager.getProfileSingleValue(authorId, createOnProfileChangeListener(commentTextView,
                    avatarImageView, comment.getText()));

        commentTextView.setText(comment.getText());

        CharSequence date = FormatterUtil.getRelativeTimeSpanString(parent.getContext(), comment.getCreatedDate());
        dateTextView.setText(date);

        return convertView;
    }

    private OnObjectChangedListener<Profile> createOnProfileChangeListener(final ExpandableTextView expandableTextView, final ImageView avatarImageView, final String comment) {
        return new OnObjectChangedListener<Profile>() {
            @Override
            public void onObjectChanged(Profile obj) {
                String userName = obj.getUsername();
                fillComment(userName, comment, expandableTextView);

                if (obj.getPhotoUrl() != null) {
                    imageUtil.getImageThumb(obj.getPhotoUrl(), avatarImageView, R.drawable.ic_stub, R.drawable.ic_stub);
                }
            }
        };
    }

    private void fillComment(String userName, String comment, ExpandableTextView commentTextView) {
        Spannable contentString = new SpannableStringBuilder(userName + " " + comment);
        contentString.setSpan(new ForegroundColorSpan(parent.getResources().getColor(R.color.highlight_text)),
                0, userName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        commentTextView.setText(contentString);

    }

    public void setList(List<Comment> commentList) {
        this.commentList = commentList;
        initListView();
    }


}
