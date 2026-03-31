package com.example.projetprogmobile;

public class Photo {
    private String id;
    private String imageUrl;
    private String description;
    private String userId;
    private double latitude;
    private double longitude;
    private long timestamp;
    private int likes;

    public Photo() {}

    public Photo(String imageUrl, String description, String userId) {
        this.imageUrl = imageUrl;
        this.description = description;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
        this.likes = 0;
    }

    // Getters & Setters
}
