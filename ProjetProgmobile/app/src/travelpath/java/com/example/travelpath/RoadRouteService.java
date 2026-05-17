package com.example.travelpath;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoadRouteService {

    private static final String OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/";
    private static final String[] OSRM_PROFILES = {"walking", "driving"};

    @NonNull
    public List<LatLng> fetchRoadPath(@NonNull List<LatLng> stops) throws Exception {
        if (stops.size() < 2) {
            return new ArrayList<>(stops);
        }

        StringBuilder coordinatesBuilder = new StringBuilder();
        for (int index = 0; index < stops.size(); index++) {
            LatLng stop = stops.get(index);
            if (index > 0) {
                coordinatesBuilder.append(';');
            }

            coordinatesBuilder.append(String.format(Locale.US, "%f,%f", stop.longitude, stop.latitude));
        }

        Exception lastError = null;
        for (String profile : OSRM_PROFILES) {
            try {
                return fetchRoadPathForProfile(coordinatesBuilder.toString(), profile);
            } catch (Exception error) {
                lastError = error;
            }
        }

        if (lastError != null) {
            throw lastError;
        }

        throw new IllegalStateException("No road route profile available.");
    }

    @NonNull
    private List<LatLng> fetchRoadPathForProfile(@NonNull String coordinates, @NonNull String profile) throws Exception {
        String requestUrl = OSRM_BASE_URL
                + profile
                + "/"
                + coordinates
                + "?overview=full&geometries=geojson";

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "ProjetProgmobile/1.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String responseBody = readFully(responseStream);
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("OSRM error " + responseCode + ": " + responseBody);
            }

            JSONObject responseJson = new JSONObject(responseBody);
            if (!"Ok".equalsIgnoreCase(responseJson.optString("code", ""))) {
                throw new IllegalStateException("OSRM route error: " + responseBody);
            }

            JSONArray routes = responseJson.optJSONArray("routes");
            if (routes == null || routes.length() == 0) {
                throw new IllegalStateException("No road route returned.");
            }

            JSONObject geometry = routes.getJSONObject(0).optJSONObject("geometry");
            if (geometry == null) {
                throw new IllegalStateException("Empty road geometry.");
            }

            JSONArray coordinatesArray = geometry.optJSONArray("coordinates");
            if (coordinatesArray == null || coordinatesArray.length() == 0) {
                throw new IllegalStateException("No road coordinates returned.");
            }

            return toLatLngList(coordinatesArray);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    private String readFully(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }

        return builder.toString();
    }

    @NonNull
    private List<LatLng> toLatLngList(@NonNull JSONArray coordinatesArray) throws Exception {
        List<LatLng> points = new ArrayList<>();
        for (int index = 0; index < coordinatesArray.length(); index++) {
            JSONArray coordinate = coordinatesArray.getJSONArray(index);
            double longitude = coordinate.getDouble(0);
            double latitude = coordinate.getDouble(1);
            points.add(new LatLng(latitude, longitude));
        }

        return points;
    }
}