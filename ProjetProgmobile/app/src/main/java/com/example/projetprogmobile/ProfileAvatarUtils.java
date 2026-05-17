package com.example.projetprogmobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ProfileAvatarUtils {

    private static final int MAX_IMAGE_SIZE_PX = 480;
    private static final int JPEG_QUALITY = 70;

    private ProfileAvatarUtils() {
    }

    public static void applyAvatar(@NonNull ImageView imageView, @Nullable String avatarBase64, int placeholderPaddingDp) {
        Bitmap avatarBitmap = decodeAvatar(avatarBase64);
        if (avatarBitmap != null) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setImageBitmap(toCircularBitmap(avatarBitmap));
            return;
        }

        int paddingPx = dpToPx(imageView.getContext(), placeholderPaddingDp);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        imageView.setImageResource(R.drawable.ic_profile_placeholder);
    }

    @Nullable
    public static String encodeAvatar(@NonNull Context context, @NonNull Uri imageUri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            if (inputStream == null) {
                return null;
            }

            Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream);
            if (decodedBitmap == null) {
                return null;
            }

            Bitmap scaledBitmap = scaleBitmap(decodedBitmap);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
        } catch (IOException error) {
            return null;
        }
    }

    @Nullable
    public static Bitmap decodeAvatar(@Nullable String avatarBase64) {
        if (avatarBase64 == null || avatarBase64.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.decode(avatarBase64.trim(), Base64.NO_WRAP);
            if (decodedBytes.length == 0) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    @NonNull
    private static Bitmap scaleBitmap(@NonNull Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= MAX_IMAGE_SIZE_PX && height <= MAX_IMAGE_SIZE_PX) {
            return bitmap;
        }

        float ratio = (float) width / (float) height;
        int targetWidth;
        int targetHeight;
        if (ratio > 1f) {
            targetWidth = MAX_IMAGE_SIZE_PX;
            targetHeight = Math.round(MAX_IMAGE_SIZE_PX / ratio);
        } else {
            targetHeight = MAX_IMAGE_SIZE_PX;
            targetWidth = Math.round(MAX_IMAGE_SIZE_PX * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    @NonNull
    private static Bitmap toCircularBitmap(@NonNull Bitmap sourceBitmap) {
        int size = Math.min(sourceBitmap.getWidth(), sourceBitmap.getHeight());
        int xOffset = (sourceBitmap.getWidth() - size) / 2;
        int yOffset = (sourceBitmap.getHeight() - size) / 2;
        Bitmap squaredBitmap = Bitmap.createBitmap(sourceBitmap, xOffset, yOffset, size, size);
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        return output;
    }

    private static int dpToPx(@NonNull Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}