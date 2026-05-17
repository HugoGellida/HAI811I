package com.example.travelpath.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.travelpath.model.Poi;
import com.example.travelpath.model.TravelRoute;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LocalSavedRoutesStore {

    private static final String PREFS_NAME = "travelpath_saved_routes";
    private static final String KEY_PREFIX = "saved_routes_";

    public void saveRoute(@NonNull Context context, @NonNull String userId, @NonNull TravelRoute route) {
        List<TravelRoute> routes = loadRoutes(context, userId);
        boolean replaced = false;

        for (int index = 0; index < routes.size(); index++) {
            TravelRoute currentRoute = routes.get(index);
            if (currentRoute == null || currentRoute.getId() == null || !currentRoute.getId().equals(route.getId())) {
                continue;
            }

            routes.set(index, route);
            replaced = true;
            break;
        }

        if (!replaced) {
            routes.add(0, route);
        }

        writeRoutes(context, userId, routes);
    }

    public void deleteRoute(@NonNull Context context, @NonNull String userId, @NonNull TravelRoute route) {
        List<TravelRoute> routes = loadRoutes(context, userId);
        List<TravelRoute> remainingRoutes = new ArrayList<>();

        for (TravelRoute currentRoute : routes) {
            if (currentRoute == null || currentRoute.getId() == null || route.getId() == null) {
                continue;
            }

            if (!currentRoute.getId().equals(route.getId())) {
                remainingRoutes.add(currentRoute);
            }
        }

        writeRoutes(context, userId, remainingRoutes);
    }

    public List<TravelRoute> loadRoutes(@NonNull Context context, @NonNull String userId) {
        SharedPreferences preferences = getPreferences(context);
        String json = preferences.getString(buildKey(userId), "[]");
        List<TravelRoute> routes = new ArrayList<>();

        try {
            JSONArray routesArray = new JSONArray(json);
            for (int index = 0; index < routesArray.length(); index++) {
                JSONObject routeObject = routesArray.getJSONObject(index);
                routes.add(fromJson(routeObject));
            }
        } catch (JSONException error) {
            throw new IllegalStateException("Impossible de lire les trajets locaux.", error);
        }

        routes.sort(Comparator.comparingLong(TravelRoute::getSavedAt).reversed());
        return routes;
    }

    private void writeRoutes(@NonNull Context context, @NonNull String userId, @NonNull List<TravelRoute> routes) {
        JSONArray routesArray = new JSONArray();

        try {
            for (TravelRoute route : routes) {
                if (route == null) {
                    continue;
                }

                routesArray.put(toJson(route));
            }
        } catch (JSONException error) {
            throw new IllegalStateException("Impossible de serialiser les trajets locaux.", error);
        }

        getPreferences(context)
                .edit()
                .putString(buildKey(userId), routesArray.toString())
                .apply();
    }

    private JSONObject toJson(TravelRoute route) throws JSONException {
        JSONObject routeObject = new JSONObject();
        routeObject.put("id", route.getId());
        routeObject.put("title", route.getTitle());
        routeObject.put("summary", route.getSummary());
        routeObject.put("referenceCity", route.getReferenceCity());
        routeObject.put("activityCategory", route.getActivityCategory());
        routeObject.put("requestedPlace", route.getRequestedPlace());
        routeObject.put("totalBudgetEuros", route.getTotalBudgetEuros());
        routeObject.put("totalDurationMinutes", route.getTotalDurationMinutes());
        routeObject.put("relaxedEffort", route.isRelaxedEffort());
        routeObject.put("generatedAt", route.getGeneratedAt());
        routeObject.put("savedAt", route.getSavedAt());
        routeObject.put("saved", route.isSaved());

        JSONArray stopsArray = new JSONArray();
        for (Poi stop : route.getStops()) {
            if (stop == null) {
                continue;
            }

            stopsArray.put(toJson(stop));
        }
        routeObject.put("stops", stopsArray);
        return routeObject;
    }

    private JSONObject toJson(Poi poi) throws JSONException {
        JSONObject poiObject = new JSONObject();
        poiObject.put("id", poi.getId());
        poiObject.put("name", poi.getName());
        poiObject.put("activityCategory", poi.getActivityCategory());
        poiObject.put("area", poi.getArea());
        poiObject.put("description", poi.getDescription());
        poiObject.put("latitude", poi.getLatitude());
        poiObject.put("longitude", poi.getLongitude());
        poiObject.put("budgetEuros", poi.getBudgetEuros());
        poiObject.put("durationMinutes", poi.getDurationMinutes());
        poiObject.put("outdoor", poi.isOutdoor());
        poiObject.put("seniorFriendly", poi.isSeniorFriendly());
        poiObject.put("childFriendly", poi.isChildFriendly());

        JSONArray keywordsArray = new JSONArray();
        for (String keyword : poi.getKeywords()) {
            keywordsArray.put(keyword);
        }
        poiObject.put("keywords", keywordsArray);
        return poiObject;
    }

    private TravelRoute fromJson(JSONObject routeObject) throws JSONException {
        TravelRoute route = new TravelRoute();
        route.setId(routeObject.optString("id", null));
        route.setTitle(routeObject.optString("title", null));
        route.setSummary(routeObject.optString("summary", null));
        route.setReferenceCity(routeObject.optString("referenceCity", null));
        route.setActivityCategory(routeObject.optString("activityCategory", null));
        route.setRequestedPlace(routeObject.optString("requestedPlace", null));
        route.setTotalBudgetEuros(routeObject.optInt("totalBudgetEuros"));
        route.setTotalDurationMinutes(routeObject.optInt("totalDurationMinutes"));
        route.setRelaxedEffort(routeObject.optBoolean("relaxedEffort"));
        route.setGeneratedAt(routeObject.optLong("generatedAt"));
        route.setSavedAt(routeObject.optLong("savedAt"));
        route.setSaved(routeObject.optBoolean("saved", true));

        JSONArray stopsArray = routeObject.optJSONArray("stops");
        List<Poi> stops = new ArrayList<>();
        if (stopsArray != null) {
            for (int index = 0; index < stopsArray.length(); index++) {
                stops.add(poiFromJson(stopsArray.getJSONObject(index)));
            }
        }
        route.setStops(stops);
        return route;
    }

    private Poi poiFromJson(JSONObject poiObject) {
        Poi poi = new Poi();
        poi.setId(poiObject.optString("id", null));
        poi.setName(poiObject.optString("name", null));
        poi.setActivityCategory(poiObject.optString("activityCategory", null));
        poi.setArea(poiObject.optString("area", null));
        poi.setDescription(poiObject.optString("description", null));
        poi.setLatitude(poiObject.optDouble("latitude"));
        poi.setLongitude(poiObject.optDouble("longitude"));
        poi.setBudgetEuros(poiObject.optInt("budgetEuros"));
        poi.setDurationMinutes(poiObject.optInt("durationMinutes"));
        poi.setOutdoor(poiObject.optBoolean("outdoor"));
        poi.setSeniorFriendly(poiObject.optBoolean("seniorFriendly"));
        poi.setChildFriendly(poiObject.optBoolean("childFriendly"));

        JSONArray keywordsArray = poiObject.optJSONArray("keywords");
        List<String> keywords = new ArrayList<>();
        if (keywordsArray != null) {
            for (int index = 0; index < keywordsArray.length(); index++) {
                keywords.add(keywordsArray.optString(index));
            }
        }
        poi.setKeywords(keywords);
        return poi;
    }

    private SharedPreferences getPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private String buildKey(@NonNull String userId) {
        return KEY_PREFIX + userId;
    }
}