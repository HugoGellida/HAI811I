package com.example.projetprogmobile;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Comment {

    private String id;
    private String photoId;
    private String userId;
    private String authorDisplayName;
    private String authorAvatarBase64;
    private String message;
    private List<String> likedByUserIds;
    private int likes;
    private long timestamp;

    public Comment() {
    }

    public Comment(String photoId, String userId, String authorDisplayName, String authorAvatarBase64, String message) {
        this.photoId = photoId;
        this.userId = userId;
        this.authorDisplayName = authorDisplayName;
        this.authorAvatarBase64 = authorAvatarBase64;
        this.message = message;
        this.likedByUserIds = new ArrayList<>();
        this.likes = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
    }

    public String getAuthorAvatarBase64() {
        return authorAvatarBase64;
    }

    public void setAuthorAvatarBase64(String authorAvatarBase64) {
        this.authorAvatarBase64 = authorAvatarBase64;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getLikedByUserIds() {
        if (likedByUserIds == null) {
            likedByUserIds = new ArrayList<>();
        }

        return likedByUserIds;
    }

    public void setLikedByUserIds(List<String> likedByUserIds) {
        this.likedByUserIds = likedByUserIds != null ? new ArrayList<>(likedByUserIds) : new ArrayList<>();
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDisplayAuthorName() {
        if (authorDisplayName != null && !authorDisplayName.trim().isEmpty()) {
            return authorDisplayName.trim();
        }

        return "Voyageur";
    }

    public boolean isLikedBy(@Nullable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        for (String likedUserId : getLikedByUserIds()) {
            if (userId.equals(likedUserId)) {
                return true;
            }
        }

        return false;
    }

    public String getDisplayTimestamp() {
        return android.text.format.DateFormat.format("dd/MM HH:mm", timestamp).toString();
    }
}