package com.example.travelpath.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        return !getNormalizedRequestedPlaces().isEmpty();
    }

    public String getNormalizedActivityCategory() {
        return normalize(activityCategory);
    }

    public String getNormalizedPlaceToInclude() {
        return normalize(placeToInclude);
    }

    public List<String> getRequestedPlaces() {
        Set<String> requestedPlaces = new LinkedHashSet<>();
        if (placeToInclude == null) {
            return new ArrayList<>();
        }

        for (String rawPart : placeToInclude.split(",")) {
            String trimmedPart = rawPart == null ? "" : rawPart.trim();
            if (!trimmedPart.isEmpty()) {
                requestedPlaces.add(trimmedPart);
            }
        }

        return new ArrayList<>(requestedPlaces);
    }

    public List<String> getNormalizedRequestedPlaces() {
        List<String> normalizedPlaces = new ArrayList<>();
        for (String requestedPlace : getRequestedPlaces()) {
            String normalizedPlace = normalize(requestedPlace);
            if (!normalizedPlace.isEmpty()) {
                normalizedPlaces.add(normalizedPlace);
            }
        }

        return normalizedPlaces;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}