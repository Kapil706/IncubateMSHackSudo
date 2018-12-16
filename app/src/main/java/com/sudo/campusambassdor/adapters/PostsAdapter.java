package com.sudo.campusambassdor.adapters;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.sudo.campusambassdor.R;
import com.sudo.campusambassdor.activities.MainActivity;
import com.sudo.campusambassdor.adapters.holders.LoadViewHolder;
import com.sudo.campusambassdor.adapters.holders.PostViewHolder;
import com.sudo.campusambassdor.controllers.LikeController;
import com.sudo.campusambassdor.enums.ItemType;
import com.sudo.campusambassdor.managers.PostManager;
import com.sudo.campusambassdor.managers.listeners.OnDataChangedListener;
import com.sudo.campusambassdor.managers.listeners.OnObjectChangedListener;
import com.sudo.campusambassdor.model.Post;

import java.util.LinkedList;
import java.util.List;


public class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = PostsAdapter.class.getSimpleName();

    private List<Post> postList = new LinkedList<>();
    private OnItemClickListener onItemClickListener;
    private MainActivity activity;
    private boolean isLoading = false;
    private boolean isMoreDataAvailable = true;
    private SwipeRefreshLayout swipeContainer;
    private OnObjectChangedListener<Post> onSelectedPostChangeListener;
    private int selectedPostPosition = -1;
    private boolean attemptToLoadPosts = false;

    public PostsAdapter(final MainActivity activity, SwipeRefreshLayout swipeContainer) {
        this.activity = activity;
        this.swipeContainer = swipeContainer;
        initRefreshLayout();
    }

    private void initRefreshLayout() {
        if (swipeContainer != null) {
            this.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    onRefreshAction();
                }
            });
        }
    }

    private void onRefreshAction() {
        if (activity.hasInternetConnection()) {
            loadFirstPage();
            cleanSelectedPostInformation();
        } else {
            swipeContainer.setRefreshing(false);
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
        }
    }

    private void cleanSelectedPostInformation() {
        onSelectedPostChangeListener = null;
        selectedPostPosition = -1;
    }


    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ItemType.ITEM.getTypeCode()) {
            return new PostViewHolder(inflater.inflate(R.layout.post_item_list_view, parent, false),
                    createOnItemClickListener());
        } else {
            return new LoadViewHolder(inflater.inflate(R.layout.loading_view, parent, false));
        }
    }

    private PostViewHolder.OnClickListener createOnItemClickListener() {
        return new PostViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position) {
                if (onItemClickListener != null) {
                    selectedPostPosition = position;
                    onSelectedPostChangeListener = createOnPostChangeListener(selectedPostPosition);
                    onItemClickListener.onItemClick(getItemByPosition(position));
                }
            }

            @Override
            public void onLikeClick(LikeController likeController, int position) {
                Post post = getItemByPosition(position);
                likeController.handleLikeClickAction(activity, post);
            }
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position >= getItemCount() - 1 && isMoreDataAvailable && !isLoading) {
            long lastItemCreatedDate = postList.get(postList.size() - 1).getCreatedDate();
            final long nextItemCreatedDate = lastItemCreatedDate - 1;

            android.os.Handler mHandler = activity.getWindow().getDecorView().getHandler();
            mHandler.post(new Runnable() {
                public void run() {
                    //change adapter contents
                    if (activity.hasInternetConnection()) {
                        isLoading = true;
                        postList.add(new Post(ItemType.LOAD));
                        notifyItemInserted(postList.size());
                        loadNext(nextItemCreatedDate);
                    } else {
                        activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    }
                }
            });


        }

        if (getItemViewType(position) != ItemType.LOAD.getTypeCode()) {
            ((PostViewHolder) holder).bindData(postList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    @Override
    public int getItemViewType(int position) {
        // TODO: 09.12.16 remove after clearing DB
        if (postList.get(position).getItemType() == null) {
            return ItemType.ITEM.getTypeCode();
        }

        return postList.get(position).getItemType().getTypeCode();
    }

    private Post getItemByPosition(int position) {
        return postList.get(position);
    }

    private void addList(List<Post> list) {
        this.postList.addAll(list);
        notifyDataSetChanged();
        isLoading = false;
    }

    public interface OnItemClickListener {
        void onItemClick(Post post);
    }

    public void loadFirstPage() {
        loadNext(0);
    }

    private void loadNext(final long nextItemCreatedDate) {

        if (!activity.hasInternetConnection()) {
            activity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
            hideProgress();
            return;
        }

        OnDataChangedListener<Post> onPostsDataChangedListener = new OnDataChangedListener<Post>() {
            @Override
            public void onListChanged(List<Post> list) {

                if (nextItemCreatedDate == 0) {
                    postList.clear();
                    swipeContainer.setRefreshing(false);
                }

                hideProgress();

                if (!list.isEmpty()) {
                    addList(list);
                    isMoreDataAvailable = true;
                } else {
                    isMoreDataAvailable = false;
                }
            }
        };

        PostManager.getInstance(activity).getPostsList(onPostsDataChangedListener, nextItemCreatedDate);
    }

    private void hideProgress() {
        if (!postList.isEmpty() && getItemViewType(postList.size() - 1) == ItemType.LOAD.getTypeCode()) {
            postList.remove(postList.size() - 1);
            notifyItemRemoved(postList.size() - 1);
        }
    }

    private OnObjectChangedListener<Post> createOnPostChangeListener(final int postPosition) {
        return new OnObjectChangedListener<Post>() {
            @Override
            public void onObjectChanged(Post obj) {
                postList.set(postPosition, obj);
                notifyItemChanged(postPosition);
            }
        };
    }

    public void updateSelectedPost() {
        if (onSelectedPostChangeListener != null && selectedPostPosition != -1) {
            Post selectedPost = getItemByPosition(selectedPostPosition);
            PostManager.getInstance(activity).getSinglePostValue(selectedPost.getId(), createOnPostChangeListener(selectedPostPosition));
        }
    }
}
