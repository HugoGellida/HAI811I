package com.example.travelpath;

import com.example.travelpath.model.OutdoorPreference;
import com.example.travelpath.model.Poi;
import com.example.travelpath.model.RouteSearchCriteria;
import com.example.travelpath.model.TravelRoute;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RouteGenerator {

    private static final double AIX_LATITUDE = 43.5297d;
    private static final double AIX_LONGITUDE = 5.4474d;
    private static final int MAX_ROUTE_RESULTS = 3;
    private static final int MAX_ROUTE_STOPS = 4;

    public List<TravelRoute> generateRoutes(List<Poi> allPois, RouteSearchCriteria criteria) {
        if (allPois == null || allPois.isEmpty()) {
            return Collections.emptyList();
        }

        RouteSearchCriteria safeCriteria = criteria == null ? new RouteSearchCriteria() : criteria;
        List<Poi> filteredPois = filterCandidates(allPois, safeCriteria);
        List<String> requestedPlaces = safeCriteria.getRequestedPlaces();

        if (filteredPois.isEmpty()) {
            return Collections.emptyList();
        }

        List<Poi> seeds = new ArrayList<>();
        for (Poi poi : filteredPois) {
            if (poi == null) {
                continue;
            }

            if (requestedPlaces.isEmpty() || matchesAnyRequestedPlace(poi, requestedPlaces)) {
                seeds.add(poi);
            }
        }

        if (!requestedPlaces.isEmpty() && seeds.isEmpty()) {
            return Collections.emptyList();
        }

        if (seeds.isEmpty()) {
            seeds.addAll(filteredPois);
        }

        seeds.sort((left, right) -> {
            int scoreComparison = Double.compare(scoreSeed(right, safeCriteria), scoreSeed(left, safeCriteria));
            if (scoreComparison != 0) {
                return scoreComparison;
            }

            return Double.compare(distanceFromAix(left), distanceFromAix(right));
        });

        List<TravelRoute> routes = new ArrayList<>();
        Set<String> signatures = new HashSet<>();
        for (Poi seed : seeds) {
            TravelRoute route = buildRoute(seed, filteredPois, safeCriteria);
            if (route == null) {
                continue;
            }

            String signature = buildSignature(route);
            if (!signatures.add(signature)) {
                continue;
            }

            routes.add(route);
            if (routes.size() == MAX_ROUTE_RESULTS) {
                break;
            }
        }

        return routes;
    }

    private List<Poi> filterCandidates(List<Poi> allPois, RouteSearchCriteria criteria) {
        List<Poi> filtered = new ArrayList<>();

        for (Poi poi : allPois) {
            if (poi == null) {
                continue;
            }

            if (criteria.getMaxBudgetEuros() > 0 && poi.getBudgetEuros() > criteria.getMaxBudgetEuros()) {
                continue;
            }

            if (criteria.getAvailableDurationMinutes() > 0 && poi.getDurationMinutes() > criteria.getAvailableDurationMinutes()) {
                continue;
            }

            if (criteria.hasActivityCategory()
                    && !normalize(poi.getActivityCategory()).equals(criteria.getNormalizedActivityCategory())) {
                continue;
            }

            if (criteria.getOutdoorPreference() == OutdoorPreference.OUTDOOR_ONLY && !poi.isOutdoor()) {
                continue;
            }

            if (criteria.getOutdoorPreference() == OutdoorPreference.INDOOR_ONLY && poi.isOutdoor()) {
                continue;
            }

            if (criteria.isRelaxedEffort() && !poi.isSeniorFriendly() && !poi.isChildFriendly()) {
                continue;
            }

            filtered.add(poi);
        }

        return filtered;
    }

    private TravelRoute buildRoute(Poi seed, List<Poi> candidates, RouteSearchCriteria criteria) {
        List<Poi> routeStops = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();
        int remainingBudget = criteria.getMaxBudgetEuros() > 0 ? criteria.getMaxBudgetEuros() : Integer.MAX_VALUE;
        int remainingDuration = criteria.getAvailableDurationMinutes() > 0
                ? criteria.getAvailableDurationMinutes()
                : Integer.MAX_VALUE;
        int maxRouteStops = Math.max(MAX_ROUTE_STOPS, criteria.getRequestedPlaces().size());

        if (!canAdd(seed, remainingBudget, remainingDuration)) {
            return null;
        }

        routeStops.add(seed);
        usedIds.add(seed.getId());
        remainingBudget -= seed.getBudgetEuros();
        remainingDuration -= seed.getDurationMinutes();

        Poi currentStop = seed;
        while (routeStops.size() < maxRouteStops) {
            Poi nextStop = selectNextStop(currentStop, candidates, usedIds, remainingBudget, remainingDuration, criteria);
            if (nextStop == null) {
                break;
            }

            routeStops.add(nextStop);
            usedIds.add(nextStop.getId());
            remainingBudget -= nextStop.getBudgetEuros();
            remainingDuration -= nextStop.getDurationMinutes();
            currentStop = nextStop;
        }

        if (!coversAllRequestedPlaces(routeStops, criteria.getRequestedPlaces())) {
                return null;
        }

        return toRoute(routeStops, criteria);
    }

    private Poi selectNextStop(
            Poi currentStop,
            List<Poi> candidates,
            Set<String> usedIds,
            int remainingBudget,
            int remainingDuration,
            RouteSearchCriteria criteria
    ) {
        Set<String> remainingRequestedPlaces = remainingRequestedPlaces(criteria, usedIds, candidates);
        Poi bestRequestedCandidate = selectBestCandidate(
                currentStop,
                candidates,
                usedIds,
                remainingBudget,
                remainingDuration,
                criteria,
                remainingRequestedPlaces.isEmpty() ? null : remainingRequestedPlaces);
        if (bestRequestedCandidate != null) {
            return bestRequestedCandidate;
        }

        return selectBestCandidate(currentStop, candidates, usedIds, remainingBudget, remainingDuration, criteria, null);
    }

    @Nullable
    private Poi selectBestCandidate(
            Poi currentStop,
            List<Poi> candidates,
            Set<String> usedIds,
            int remainingBudget,
            int remainingDuration,
            RouteSearchCriteria criteria,
            @Nullable Set<String> requiredPlaces
    ) {
        Poi bestCandidate = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Poi candidate : candidates) {
            if (candidate == null || usedIds.contains(candidate.getId())) {
                continue;
            }

            if (!canAdd(candidate, remainingBudget, remainingDuration)) {
                continue;
            }

            if (requiredPlaces != null && !requiredPlaces.isEmpty() && !matchesAnyRequestedPlace(candidate, requiredPlaces)) {
                continue;
            }

            double score = scoreSeed(candidate, criteria) - (distanceBetween(currentStop, candidate) * 3d);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }

    private boolean canAdd(Poi poi, int remainingBudget, int remainingDuration) {
        return poi.getBudgetEuros() <= remainingBudget && poi.getDurationMinutes() <= remainingDuration;
    }

    private TravelRoute toRoute(List<Poi> routeStops, RouteSearchCriteria criteria) {
        TravelRoute route = new TravelRoute();
        route.setReferenceCity("Aix-en-Provence");
        route.setGeneratedAt(System.currentTimeMillis());
        route.setStops(routeStops);
        route.setRelaxedEffort(criteria.isRelaxedEffort());
        route.setRequestedPlace(criteria.getPlaceToInclude());
        route.setActivityCategory(resolveActivityCategory(routeStops, criteria));

        int totalBudget = 0;
        int totalDuration = 0;
        for (Poi stop : routeStops) {
            totalBudget += stop.getBudgetEuros();
            totalDuration += stop.getDurationMinutes();
        }

        route.setTotalBudgetEuros(totalBudget);
        route.setTotalDurationMinutes(totalDuration);
        route.setTitle(buildTitle(routeStops, criteria));
        route.setSummary(buildSummary(routeStops, criteria, totalBudget, totalDuration));
        route.setId(buildRouteId(routeStops, criteria));
        return route;
    }

    private String resolveActivityCategory(List<Poi> routeStops, RouteSearchCriteria criteria) {
        if (criteria.hasActivityCategory()) {
            return criteria.getActivityCategory();
        }

        if (routeStops.isEmpty() || routeStops.get(0) == null) {
            return "balade";
        }

        return routeStops.get(0).getActivityCategory();
    }

    private String buildTitle(List<Poi> routeStops, RouteSearchCriteria criteria) {
        String prefix;
        if (criteria.hasPlaceToInclude()) {
            prefix = "Autour de " + criteria.getPlaceToInclude().trim();
        } else if (criteria.hasActivityCategory()) {
            prefix = capitalize(criteria.getActivityCategory().trim()) + " autour d Aix";
        } else {
            prefix = "Escapade autour d Aix";
        }

        return prefix + " - " + routeStops.size() + " etapes";
    }

    private String buildSummary(List<Poi> routeStops, RouteSearchCriteria criteria, int totalBudget, int totalDuration) {
        StringBuilder builder = new StringBuilder();
        builder.append(routeStops.size())
                .append(" etapes")
                .append("  |  ")
                .append(totalBudget)
                .append(" EUR")
                .append("  |  ")
                .append(formatDuration(totalDuration));

        if (criteria.isRelaxedEffort()) {
            builder.append("  |  rythme doux");
        }

        if (criteria.getOutdoorPreference() == OutdoorPreference.OUTDOOR_ONLY) {
            builder.append("  |  exterieur");
        } else if (criteria.getOutdoorPreference() == OutdoorPreference.INDOOR_ONLY) {
            builder.append("  |  interieur");
        }

        return builder.toString();
    }

    private String buildRouteId(List<Poi> routeStops, RouteSearchCriteria criteria) {
        StringBuilder builder = new StringBuilder();
        builder.append(normalize(criteria.getActivityCategory())).append('_')
                .append(normalize(criteria.getPlaceToInclude())).append('_')
                .append(criteria.getMaxBudgetEuros()).append('_')
                .append(criteria.getAvailableDurationMinutes());

        for (Poi stop : routeStops) {
            builder.append('_').append(stop.getId());
        }

        return builder.toString().replace(' ', '_');
    }

    private String buildSignature(TravelRoute route) {
        StringBuilder builder = new StringBuilder();
        for (Poi stop : route.getStops()) {
            if (stop == null) {
                continue;
            }

            builder.append(stop.getId()).append('|');
        }

        return builder.toString();
    }

    private double scoreSeed(Poi poi, RouteSearchCriteria criteria) {
        double score = 0d;

        if (criteria.hasPlaceToInclude()) {
            score += 45d * countRequestedPlaceMatches(poi, criteria.getRequestedPlaces());
        }

        if (criteria.hasActivityCategory() && normalize(poi.getActivityCategory()).equals(criteria.getNormalizedActivityCategory())) {
            score += 20d;
        }

        if (criteria.isRelaxedEffort()) {
            if (poi.isSeniorFriendly()) {
                score += 8d;
            }
            if (poi.isChildFriendly()) {
                score += 8d;
            }
        }

        if (criteria.getOutdoorPreference() == OutdoorPreference.ANY) {
            score += poi.isOutdoor() ? 3d : 2d;
        }

        score += Math.max(0d, 14d - poi.getBudgetEuros());
        score += Math.max(0d, 8d - (poi.getDurationMinutes() / 30d));
        score -= distanceFromAix(poi) * 0.8d;
        return score;
    }

    private int countRequestedPlaceMatches(@Nullable Poi poi, List<String> requestedPlaces) {
        if (poi == null || requestedPlaces == null || requestedPlaces.isEmpty()) {
            return 0;
        }

        int matches = 0;
        for (String requestedPlace : requestedPlaces) {
            if (poi.matchesPlace(requestedPlace)) {
                matches++;
            }
        }

        return matches;
    }

    private boolean matchesAnyRequestedPlace(@Nullable Poi poi, Iterable<String> requestedPlaces) {
        if (poi == null || requestedPlaces == null) {
            return false;
        }

        for (String requestedPlace : requestedPlaces) {
            if (poi.matchesPlace(requestedPlace)) {
                return true;
            }
        }

        return false;
    }

    private boolean coversAllRequestedPlaces(List<Poi> routeStops, List<String> requestedPlaces) {
        if (requestedPlaces == null || requestedPlaces.isEmpty()) {
            return true;
        }

        Set<String> coveredPlaces = new LinkedHashSet<>();
        for (Poi stop : routeStops) {
            for (String requestedPlace : requestedPlaces) {
                if (stop != null && stop.matchesPlace(requestedPlace)) {
                    coveredPlaces.add(normalize(requestedPlace));
                }
            }
        }

        for (String requestedPlace : requestedPlaces) {
            if (!coveredPlaces.contains(normalize(requestedPlace))) {
                return false;
            }
        }

        return true;
    }

    private Set<String> remainingRequestedPlaces(
            RouteSearchCriteria criteria,
            Set<String> usedIds,
            List<Poi> candidates
    ) {
        Set<String> remainingPlaces = new LinkedHashSet<>();
        for (String requestedPlace : criteria.getRequestedPlaces()) {
            remainingPlaces.add(requestedPlace);
        }

        if (remainingPlaces.isEmpty()) {
            return remainingPlaces;
        }

        for (Poi candidate : candidates) {
            if (candidate == null || !usedIds.contains(candidate.getId())) {
                continue;
            }

            remainingPlaces.removeIf(candidate::matchesPlace);
            if (remainingPlaces.isEmpty()) {
                break;
            }
        }

        return remainingPlaces;
    }

    private double distanceFromAix(Poi poi) {
        return distanceBetween(AIX_LATITUDE, AIX_LONGITUDE, poi.getLatitude(), poi.getLongitude());
    }

    private double distanceBetween(Poi left, Poi right) {
        return distanceBetween(left.getLatitude(), left.getLongitude(), right.getLatitude(), right.getLongitude());
    }

    private double distanceBetween(double leftLat, double leftLon, double rightLat, double rightLon) {
        double latDistance = Math.toRadians(rightLat - leftLat);
        double lonDistance = Math.toRadians(rightLon - leftLon);
        double a = Math.sin(latDistance / 2d) * Math.sin(latDistance / 2d)
                + Math.cos(Math.toRadians(leftLat))
                * Math.cos(Math.toRadians(rightLat))
                * Math.sin(lonDistance / 2d)
                * Math.sin(lonDistance / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return 6371d * c;
    }

    private String formatDuration(int totalDurationMinutes) {
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

    private String capitalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        String trimmed = value.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}