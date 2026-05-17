package com.example.travelpath.model;

import java.util.Locale;

public class RouteSearchCriteria {

    private int maxBudgetEuros;
    private String activityCategory;
    private String placeToInclude;
    private int availableDurationMinutes;
    private boolean relaxedEffort;
    private OutdoorPreference outdoorPreference = OutdoorPreference.ANY;

    public int getMaxBudgetEuros() {
        return maxBudgetEuros;
    }

    public void setMaxBudgetEuros(int maxBudgetEuros) {
        this.maxBudgetEuros = maxBudgetEuros;
    }

    public String getActivityCategory() {
        return activityCategory;
    }

    public void setActivityCategory(String activityCategory) {
        this.activityCategory = activityCategory;
    }

    public String getPlaceToInclude() {
        return placeToInclude;
    }

    public void setPlaceToInclude(String placeToInclude) {
        this.placeToInclude = placeToInclude;
    }

    public int getAvailableDurationMinutes() {
        return availableDurationMinutes;
    }

    public void setAvailableDurationMinutes(int availableDurationMinutes) {
        this.availableDurationMinutes = availableDurationMinutes;
    }

    public boolean isRelaxedEffort() {
        return relaxedEffort;
    }

    public void setRelaxedEffort(boolean relaxedEffort) {
        this.relaxedEffort = relaxedEffort;
    }

    public OutdoorPreference getOutdoorPreference() {
        return outdoorPreference == null ? OutdoorPreference.ANY : outdoorPreference;
    }

    public void setOutdoorPreference(OutdoorPreference outdoorPreference) {
        this.outdoorPreference = outdoorPreference == null ? OutdoorPreference.ANY : outdoorPreference;
    }

    public boolean hasActivityCategory() {
        String normalized = normalize(activityCategory);
        return !normalized.isEmpty() && !"toutes".equals(normalized);
    }

    public boolean hasPlaceToInclude() {
        return !normalize(placeToInclude).isEmpty();
    }

    public String getNormalizedActivityCategory() {
        return normalize(activityCategory);
    }

    public String getNormalizedPlaceToInclude() {
        return normalize(placeToInclude);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}