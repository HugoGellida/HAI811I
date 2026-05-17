package com.example.travelpath.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TravelRoute implements Serializable {

    private String id;
    private String title;
    private String summary;
    private String referenceCity;
    private String activityCategory;
    private String requestedPlace;
    private int totalBudgetEuros;
    private int totalDurationMinutes;
    private boolean relaxedEffort;
    private long generatedAt;
    private long savedAt;
    private boolean saved;
    private List<Poi> stops;

    public TravelRoute() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getReferenceCity() {
        return referenceCity;
    }

    public void setReferenceCity(String referenceCity) {
        this.referenceCity = referenceCity;
    }

    public String getActivityCategory() {
        return activityCategory;
    }

    public void setActivityCategory(String activityCategory) {
        this.activityCategory = activityCategory;
    }

    public String getRequestedPlace() {
        return requestedPlace;
    }

    public void setRequestedPlace(String requestedPlace) {
        this.requestedPlace = requestedPlace;
    }

    public int getTotalBudgetEuros() {
        return totalBudgetEuros;
    }

    public void setTotalBudgetEuros(int totalBudgetEuros) {
        this.totalBudgetEuros = totalBudgetEuros;
    }

    public int getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    public void setTotalDurationMinutes(int totalDurationMinutes) {
        this.totalDurationMinutes = totalDurationMinutes;
    }

    public boolean isRelaxedEffort() {
        return relaxedEffort;
    }

    public void setRelaxedEffort(boolean relaxedEffort) {
        this.relaxedEffort = relaxedEffort;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public long getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(long savedAt) {
        this.savedAt = savedAt;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public List<Poi> getStops() {
        if (stops == null) {
            stops = new ArrayList<>();
        }

        return stops;
    }

    public void setStops(List<Poi> stops) {
        this.stops = stops == null ? new ArrayList<>() : new ArrayList<>(stops);
    }

    public boolean isAllOutdoor() {
        List<Poi> currentStops = getStops();
        if (currentStops.isEmpty()) {
            return false;
        }

        for (Poi stop : currentStops) {
            if (stop == null || !stop.isOutdoor()) {
                return false;
            }
        }

        return true;
    }

    public boolean isAllIndoor() {
        List<Poi> currentStops = getStops();
        if (currentStops.isEmpty()) {
            return false;
        }

        for (Poi stop : currentStops) {
            if (stop == null || stop.isOutdoor()) {
                return false;
            }
        }

        return true;
    }

    public String getEnvironmentLabel() {
        if (isAllOutdoor()) {
            return "Exterieur";
        }

        if (isAllIndoor()) {
            return "Interieur";
        }

        return "Mixte";
    }

    public String getDisplayBudget() {
        return totalBudgetEuros + " EUR";
    }

    public String getDisplayDuration() {
        int hours = totalDurationMinutes / 60;
        int minutes = totalDurationMinutes % 60;

        if (hours > 0 && minutes > 0) {
            return String.format(Locale.ROOT, "%d h %02d", hours, minutes);
        }

        if (hours > 0) {
            return String.format(Locale.ROOT, "%d h", hours);
        }

        return String.format(Locale.ROOT, "%d min", minutes);
    }

    public String getStopsSummary() {
        StringBuilder builder = new StringBuilder();

        for (Poi stop : getStops()) {
            if (stop == null || stop.getDisplayName().trim().isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append("  >  ");
            }

            builder.append(stop.getDisplayName());
        }

        return builder.toString();
    }
}