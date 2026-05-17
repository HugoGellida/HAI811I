package com.example.projetprogmobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    public interface OnLikeCommentListener {
        void onLikeComment(Comment comment);
    }

    public interface OnDeleteCommentListener {
        void onDeleteComment(Comment comment);
    }

    private final List<Comment> comments;
    private final OnLikeCommentListener onLikeCommentListener;
    private final OnDeleteCommentListener onDeleteCommentListener;
    private String currentUserId;

    public CommentAdapter(
            List<Comment> comments,
            OnLikeCommentListener onLikeCommentListener,
            OnDeleteCommentListener onDeleteCommentListener
    ) {
        this.comments = comments;
        this.onLikeCommentListener = onLikeCommentListener;
        this.onDeleteCommentListener = onDeleteCommentListener;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);

        if (comment == null) {
            ProfileAvatarUtils.applyAvatar(holder.authorAvatarView, null, 6);
            holder.authorNameView.setText("Voyageur");
            holder.messageView.setText("");
            holder.metaView.setText("");
            holder.likeButton.setVisibility(View.GONE);
            holder.likeCountView.setText("0");
            holder.deleteButton.setVisibility(View.GONE);
            return;
        }

        ProfileAvatarUtils.applyAvatar(holder.authorAvatarView, comment.getAuthorAvatarBase64(), 6);
        holder.authorNameView.setText(comment.getDisplayAuthorName());
        holder.messageView.setText(comment.getMessage() != null ? comment.getMessage() : "");
        holder.metaView.setText(comment.getDisplayTimestamp());

        boolean likedByCurrentUser = comment.isLikedBy(currentUserId);
        holder.likeButton.setVisibility(View.VISIBLE);
        View.OnClickListener likeClickListener = view -> onLikeCommentListener.onLikeComment(comment);
        holder.likeButton.setImageResource(likedByCurrentUser ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.likeButton.setContentDescription(holder.itemView.getContext().getString(
            likedByCurrentUser ? R.string.travelshare_unlike_content_description : R.string.travelshare_like_content_description));
        holder.likeButton.setOnClickListener(likeClickListener);
        holder.likeAction.setOnClickListener(likeClickListener);
        holder.likeCountView.setText(String.valueOf(comment.getLikes()));

        boolean canDelete = currentUserId != null
                && currentUserId.equals(comment.getUserId())
                && comment.getId() != null
                && !comment.getId().trim().isEmpty();
        holder.deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        holder.deleteButton.setOnClickListener(canDelete ? view -> onDeleteCommentListener.onDeleteComment(comment) : null);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView authorAvatarView;
        private final TextView authorNameView;
        private final TextView messageView;
        private final TextView metaView;
        private final LinearLayout likeAction;
        private final ImageButton likeButton;
        private final TextView likeCountView;
        private final ImageButton deleteButton;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            authorAvatarView = itemView.findViewById(R.id.comment_author_avatar_view);
            authorNameView = itemView.findViewById(R.id.comment_author_name_text);
            messageView = itemView.findViewById(R.id.comment_message_text);
            metaView = itemView.findViewById(R.id.comment_meta_text);
            likeAction = itemView.findViewById(R.id.comment_like_action);
            likeButton = itemView.findViewById(R.id.comment_like_button);
            likeCountView = itemView.findViewById(R.id.comment_like_count_text);
            deleteButton = itemView.findViewById(R.id.comment_delete_button);
        }
    }
}