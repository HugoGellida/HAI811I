package com.example.projetprogmobile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

    private final List<Photo> photoList;
    private final OnDeletePhotoListener onDeletePhotoListener;
    private final OnOpenLocationListener onOpenLocationListener;
    private String currentUserId;

    public PhotoAdapter(
            List<Photo> photoList,
            OnDeletePhotoListener onDeletePhotoListener,
            OnOpenLocationListener onOpenLocationListener
    ) {
        this.photoList = photoList;
        this.onDeletePhotoListener = onDeletePhotoListener;
        this.onOpenLocationListener = onOpenLocationListener;
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
            holder.deleteButton.setVisibility(View.GONE);
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

        boolean canDelete = currentUserId != null
                && currentUserId.equals(photo.getUserId())
                && photo.getId() != null
                && !photo.getId().trim().isEmpty();

        holder.deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        holder.deleteButton.setOnClickListener(canDelete ? view -> onDeletePhotoListener.onDeletePhoto(photo) : null);

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
        Button deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
            authorAvatarView = itemView.findViewById(R.id.author_avatar_view);
            authorNameView = itemView.findViewById(R.id.author_name_text);
            description = itemView.findViewById(R.id.description);
            keywordsView = itemView.findViewById(R.id.keywords_text);
            locationView = itemView.findViewById(R.id.location_text);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}