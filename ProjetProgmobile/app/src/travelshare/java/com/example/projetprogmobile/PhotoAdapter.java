package com.example.projetprogmobile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
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

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    public interface OnDeletePhotoListener {
        void onDeletePhoto(Photo photo);
    }

    public interface OnOpenLocationListener {
        void onOpenLocation(Photo photo);
    }

    public interface OnEditPhotoListener {
        void onEditPhoto(Photo photo);
    }

    public interface OnLikePhotoListener {
        void onLikePhoto(Photo photo);
    }

    public interface OnCommentPhotoListener {
        void onCommentPhoto(Photo photo);
    }

    private final List<Photo> photoList;
    private final OnDeletePhotoListener onDeletePhotoListener;
    private final OnOpenLocationListener onOpenLocationListener;
    private final OnEditPhotoListener onEditPhotoListener;
    private final OnLikePhotoListener onLikePhotoListener;
    private final OnCommentPhotoListener onCommentPhotoListener;
    private String currentUserId;

    public PhotoAdapter(
            List<Photo> photoList,
            OnDeletePhotoListener onDeletePhotoListener,
            OnOpenLocationListener onOpenLocationListener,
            OnEditPhotoListener onEditPhotoListener,
            OnLikePhotoListener onLikePhotoListener,
            OnCommentPhotoListener onCommentPhotoListener
    ) {
        this.photoList = photoList;
        this.onDeletePhotoListener = onDeletePhotoListener;
        this.onOpenLocationListener = onOpenLocationListener;
        this.onEditPhotoListener = onEditPhotoListener;
        this.onLikePhotoListener = onLikePhotoListener;
        this.onCommentPhotoListener = onCommentPhotoListener;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Photo photo = photoList.get(position);

        if (photo == null) {
            ProfileAvatarUtils.applyAvatar(holder.authorAvatarView, null, 8);
            holder.authorNameView.setText("Voyageur");
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            holder.description.setText("");
            holder.likeButton.setImageResource(R.drawable.ic_heart_outline);
            holder.likeButton.setContentDescription(holder.itemView.getContext().getString(R.string.travelshare_like_content_description));
            holder.likeCountView.setText("0");
            holder.commentCountView.setText("0");
            holder.deleteButton.setVisibility(View.GONE);
            holder.editButton.setVisibility(View.GONE);
            holder.keywordsView.setVisibility(View.GONE);
            holder.locationView.setVisibility(View.GONE);
            return;
        }

        holder.authorNameView.setText(photo.getDisplayAuthorName());
        ProfileAvatarUtils.applyAvatar(holder.authorAvatarView, photo.getAuthorAvatarBase64(), 8);

        holder.description.setText(photo.getDescription() != null ? photo.getDescription() : "");

        boolean hasKeywords = photo.hasKeywords();
        holder.keywordsView.setVisibility(hasKeywords ? View.VISIBLE : View.GONE);
        holder.keywordsView.setText(hasKeywords ? photo.getDisplayKeywords() : "");

        boolean hasLocation = photo.hasTaggedLocation();
        holder.locationView.setVisibility(hasLocation ? View.VISIBLE : View.GONE);
        holder.locationView.setText(hasLocation ? photo.getDisplayLocationName() : "");
        holder.locationView.setOnClickListener(hasLocation
                ? view -> onOpenLocationListener.onOpenLocation(photo)
                : null);

        boolean likedByCurrentUser = photo.isLikedBy(currentUserId);
        View.OnClickListener likeClickListener = view -> onLikePhotoListener.onLikePhoto(photo);
        holder.likeButton.setImageResource(likedByCurrentUser ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.likeButton.setContentDescription(holder.itemView.getContext().getString(
                likedByCurrentUser ? R.string.travelshare_unlike_content_description : R.string.travelshare_like_content_description));
        holder.likeButton.setOnClickListener(likeClickListener);
        holder.likeAction.setOnClickListener(likeClickListener);
        holder.likeCountView.setText(String.valueOf(photo.getLikes()));

        View.OnClickListener commentClickListener = view -> onCommentPhotoListener.onCommentPhoto(photo);
        holder.commentButton.setOnClickListener(commentClickListener);
        holder.commentAction.setOnClickListener(commentClickListener);
        holder.commentCountView.setText(String.valueOf(photo.getCommentsCount()));

        boolean canDelete = currentUserId != null
                && currentUserId.equals(photo.getUserId())
                && photo.getId() != null
                && !photo.getId().trim().isEmpty();

        holder.deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        holder.deleteButton.setOnClickListener(canDelete ? view -> onDeletePhotoListener.onDeletePhoto(photo) : null);
        holder.editButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        holder.editButton.setOnClickListener(canDelete ? view -> onEditPhotoListener.onEditPhoto(photo) : null);

        String base64 = photo.getImageBase64();

        // 🔥 Protection MAX
        if (base64 == null || base64.trim().isEmpty()) {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }

        try {
            byte[] decodedBytes = Base64.decode(base64.trim(), Base64.NO_WRAP);

            if (decodedBytes == null || decodedBytes.length == 0) {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                return;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);

            if (bitmap == null) {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            } else {
                holder.imageView.setImageBitmap(bitmap);
            }

        } catch (IllegalArgumentException e) {
            // 🔥 Base64 invalide
            e.printStackTrace();
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);

        } catch (Exception e) {
            e.printStackTrace();
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        ImageView authorAvatarView;
        TextView authorNameView;
        TextView description;
        TextView keywordsView;
        TextView locationView;
        LinearLayout likeAction;
        LinearLayout commentAction;
        ImageButton likeButton;
        TextView likeCountView;
        ImageButton commentButton;
        TextView commentCountView;
        ImageButton editButton;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
            authorAvatarView = itemView.findViewById(R.id.author_avatar_view);
            authorNameView = itemView.findViewById(R.id.author_name_text);
            description = itemView.findViewById(R.id.description);
            keywordsView = itemView.findViewById(R.id.keywords_text);
            locationView = itemView.findViewById(R.id.location_text);
            likeAction = itemView.findViewById(R.id.like_action);
            likeButton = itemView.findViewById(R.id.like_button);
            likeCountView = itemView.findViewById(R.id.like_count_text);
            commentAction = itemView.findViewById(R.id.comment_action);
            commentButton = itemView.findViewById(R.id.comment_button);
            commentCountView = itemView.findViewById(R.id.comment_count_text);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}