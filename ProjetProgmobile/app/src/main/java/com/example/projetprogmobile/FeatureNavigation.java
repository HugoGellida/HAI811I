package com.example.projetprogmobile;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.example.travelpath.MapActivity;
import com.example.travelpath.TravelPathMainActivity;

public final class FeatureNavigation {

    public static final String EXTRA_DESTINATION = "com.example.projetprogmobile.extra.DESTINATION";
    public static final String DESTINATION_TRAVEL_SHARE = "travelshare";
    public static final String DESTINATION_TRAVEL_PATH = "travelpath";

    private FeatureNavigation() {
    }

    public static Intent createLoginIntent(Context context, @Nullable String destination) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(EXTRA_DESTINATION, resolveDestination(destination));
        return intent;
    }

    public static Intent createPostAuthIntent(Context context, @Nullable String destination) {
        if (DESTINATION_TRAVEL_PATH.equals(resolveDestination(destination))) {
            return new Intent(context, TravelPathMainActivity.class);
        }

        return new Intent(context, FeedActivity.class);
    }

    public static String resolveDestination(@Nullable Intent intent) {
        if (intent == null) {
            return DESTINATION_TRAVEL_SHARE;
        }

        return resolveDestination(intent.getStringExtra(EXTRA_DESTINATION));
    }

    public static String resolveDestination(@Nullable String destination) {
        if (DESTINATION_TRAVEL_PATH.equals(destination)) {
            return DESTINATION_TRAVEL_PATH;
        }

        return DESTINATION_TRAVEL_SHARE;
    }
}