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

import com.squareup.picasso.Picasso;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    private List<Photo> photoList;

    public PhotoAdapter(List<Photo> photoList) {
        this.photoList = photoList;
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
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }

        holder.description.setText(photo.getDescription() != null ? photo.getDescription() : "");

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
        TextView description;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
            description = itemView.findViewById(R.id.description);
        }
    }
}