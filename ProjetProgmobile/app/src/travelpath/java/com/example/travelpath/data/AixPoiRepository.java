package com.example.travelpath.data;

import com.example.projetprogmobile.R;
import com.example.travelpath.model.Poi;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AixPoiRepository {

    private static List<Poi> cachedPois;

    public List<Poi> loadPois(Context context) {
        if (cachedPois == null) {
            cachedPois = readPois(context.getApplicationContext());
        }

        return new ArrayList<>(cachedPois);
    }

    public List<String> loadPlaceSuggestions(Context context) {
        Set<String> suggestions = new LinkedHashSet<>();
        for (Poi poi : loadPois(context)) {
            if (poi == null || poi.getName() == null || poi.getName().trim().isEmpty()) {
                continue;
            }

            suggestions.add(poi.getName().trim());
        }

        return new ArrayList<>(suggestions);
    }

    public List<String> loadActivityCategories(Context context) {
        Set<String> categories = new LinkedHashSet<>();
        categories.add("Toutes");

        for (Poi poi : loadPois(context)) {
            if (poi == null || poi.getActivityCategory() == null || poi.getActivityCategory().trim().isEmpty()) {
                continue;
            }

            categories.add(capitalize(poi.getActivityCategory().trim()));
        }

        return new ArrayList<>(categories);
    }

    private List<Poi> readPois(Context context) {
        List<Poi> pois = new ArrayList<>();

        try (InputStream inputStream = context.getResources().openRawResource(R.raw.aix_pois);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            JSONArray jsonArray = new JSONArray(builder.toString());
            for (int index = 0; index < jsonArray.length(); index++) {
                JSONObject jsonObject = jsonArray.getJSONObject(index);
                pois.add(toPoi(jsonObject));
            }
        } catch (IOException | JSONException error) {
            throw new IllegalStateException("Impossible de charger les points d interet Aix-en-Provence.", error);
        }

        return pois;
    }

    private Poi toPoi(JSONObject jsonObject) throws JSONException {
        JSONArray keywordsArray = jsonObject.optJSONArray("keywords");
        List<String> keywords = new ArrayList<>();

        if (keywordsArray != null) {
            for (int index = 0; index < keywordsArray.length(); index++) {
                keywords.add(keywordsArray.optString(index));
            }
        }

        return new Poi(
                jsonObject.optString("id"),
                jsonObject.optString("name"),
                jsonObject.optString("activity"),
                jsonObject.optString("area"),
                jsonObject.optString("description"),
                jsonObject.optDouble("latitude"),
                jsonObject.optDouble("longitude"),
                jsonObject.optInt("budgetEuros"),
                jsonObject.optInt("durationMinutes"),
                jsonObject.optBoolean("outdoor"),
                jsonObject.optBoolean("seniorFriendly"),
                jsonObject.optBoolean("childFriendly"),
                keywords);
    }

    private String capitalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        String trimmed = value.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }
}