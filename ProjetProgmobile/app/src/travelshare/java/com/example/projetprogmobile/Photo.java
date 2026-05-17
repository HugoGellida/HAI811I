package com.example.projetprogmobile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Photo {
    private String id;
    private String imageBase64;
    private String description;
    private String userId;
    private String locationName;
    private List<String> keywords;
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

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public List<String> getKeywords() {
        if (keywords == null) {
            keywords = new ArrayList<>();
        }

        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
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

    public boolean hasTaggedLocation() {
        return (locationName != null && !locationName.trim().isEmpty())
                || latitude != 0d
                || longitude != 0d;
    }

    public boolean hasKeywords() {
        return keywords != null && !keywords.isEmpty();
    }

    public String getDisplayLocationName() {
        if (locationName != null && !locationName.trim().isEmpty()) {
            return locationName;
        }

        return String.format(Locale.US, "%.4f, %.4f", latitude, longitude);
    }

    public String getDisplayKeywords() {
        if (!hasKeywords()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String keyword : getKeywords()) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append("  ");
            }

            builder.append('#').append(keyword.trim());
        }

        return builder.toString();
    }

    public boolean matchesTextQuery(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        if (normalize(description).contains(normalizedQuery)) {
            return true;
        }

        if (normalize(locationName).contains(normalizedQuery)) {
            return true;
        }

        for (String keyword : getKeywords()) {
            if (normalize(keyword).contains(normalizedQuery)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
