package com.example.travelpath.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Poi implements Serializable {

    private String id;
    private String name;
    private String activityCategory;
    private String area;
    private String description;
    private double latitude;
    private double longitude;
    private int budgetEuros;
    private int durationMinutes;
    private boolean outdoor;
    private boolean seniorFriendly;
    private boolean childFriendly;
    private List<String> keywords;

    public Poi() {
    }

    public Poi(
            String id,
            String name,
            String activityCategory,
            String area,
            String description,
            double latitude,
            double longitude,
            int budgetEuros,
            int durationMinutes,
            boolean outdoor,
            boolean seniorFriendly,
            boolean childFriendly,
            List<String> keywords
    ) {
        this.id = id;
        this.name = name;
        this.activityCategory = activityCategory;
        this.area = area;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.budgetEuros = budgetEuros;
        this.durationMinutes = durationMinutes;
        this.outdoor = outdoor;
        this.seniorFriendly = seniorFriendly;
        this.childFriendly = childFriendly;
        setKeywords(keywords);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActivityCategory() {
        return activityCategory;
    }

    public void setActivityCategory(String activityCategory) {
        this.activityCategory = activityCategory;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public int getBudgetEuros() {
        return budgetEuros;
    }

    public void setBudgetEuros(int budgetEuros) {
        this.budgetEuros = budgetEuros;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isOutdoor() {
        return outdoor;
    }

    public void setOutdoor(boolean outdoor) {
        this.outdoor = outdoor;
    }

    public boolean isSeniorFriendly() {
        return seniorFriendly;
    }

    public void setSeniorFriendly(boolean seniorFriendly) {
        this.seniorFriendly = seniorFriendly;
    }

    public boolean isChildFriendly() {
        return childFriendly;
    }

    public void setChildFriendly(boolean childFriendly) {
        this.childFriendly = childFriendly;
    }

    public List<String> getKeywords() {
        if (keywords == null) {
            keywords = new ArrayList<>();
        }

        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords == null ? new ArrayList<>() : new ArrayList<>(keywords);
    }

    public boolean matchesPlace(String rawQuery) {
        String normalizedQuery = normalize(rawQuery);
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        if (normalize(name).contains(normalizedQuery)) {
            return true;
        }

        if (normalize(area).contains(normalizedQuery)) {
            return true;
        }

        if (normalize(description).contains(normalizedQuery)) {
            return true;
        }

        for (String keyword : getKeywords()) {
            if (normalize(keyword).contains(normalizedQuery)) {
                return true;
            }
        }

        return false;
    }

    public String getDisplayName() {
        return name == null ? "" : name;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}