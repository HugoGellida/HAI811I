package com.example.projetprogmobile;

public class Photo {
    private String id;
    private String imageBase64;
    private String description;
    private String userId;
    private double latitude;
    private double longitude;
    private long timestamp;
    private int likes;

    public Photo() {}

    public Photo(String imageBase64, String description, String userId) {
        this.imageBase64 = imageBase64;
        this.description = description;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
        this.likes = 0;
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageUrl) {
        this.imageBase64 = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public void like() {
        likes++;
    }

    public void unlike() {
        likes--;
    }
}
