package com.example.travelpath;

import com.example.projetprogmobile.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.example.projetprogmobile.FeatureNavigation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String EXTRA_PICK_MODE = "com.example.travelpath.extra.PICK_MODE";
    public static final String EXTRA_FOCUS_LATITUDE = "com.example.travelpath.extra.FOCUS_LATITUDE";
    public static final String EXTRA_FOCUS_LONGITUDE = "com.example.travelpath.extra.FOCUS_LONGITUDE";
    public static final String EXTRA_FOCUS_LABEL = "com.example.travelpath.extra.FOCUS_LABEL";
    public static final String EXTRA_SELECTED_LATITUDE = "com.example.travelpath.extra.SELECTED_LATITUDE";
    public static final String EXTRA_SELECTED_LONGITUDE = "com.example.travelpath.extra.SELECTED_LONGITUDE";
    public static final String EXTRA_SELECTED_LABEL = "com.example.travelpath.extra.SELECTED_LABEL";

    private static final float DEFAULT_ZOOM = 12f;

    private GoogleMap mMap;
    private Button btnAddTrip;
    private Button btnAuth;
    private TextView mapHintView;
    private boolean pickMode;
    private LatLng selectedLocation;
    private String selectedLocationName;

    public static Intent createLocationPickerIntent(
            Context context,
            @Nullable Double latitude,
            @Nullable Double longitude,
            @Nullable String label
    ) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(EXTRA_PICK_MODE, true);

        if (latitude != null && longitude != null) {
            intent.putExtra(EXTRA_FOCUS_LATITUDE, latitude);
            intent.putExtra(EXTRA_FOCUS_LONGITUDE, longitude);
        }

        if (label != null && !label.trim().isEmpty()) {
            intent.putExtra(EXTRA_FOCUS_LABEL, label);
        }

        return intent;
    }

    public static Intent createFocusedLocationIntent(
            Context context,
            double latitude,
            double longitude,
            @Nullable String label
    ) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(EXTRA_FOCUS_LATITUDE, latitude);
        intent.putExtra(EXTRA_FOCUS_LONGITUDE, longitude);

        if (label != null && !label.trim().isEmpty()) {
            intent.putExtra(EXTRA_FOCUS_LABEL, label);
        }

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);

        btnAddTrip = findViewById(R.id.btnAddTrip);
        btnAuth = findViewById(R.id.btnAuth);
        mapHintView = findViewById(R.id.textMapHint);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        if (pickMode) {
            configurePickerMode();
            return;
        }

        configureDefaultMode();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!pickMode) {
            updateAuthButton();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (pickMode) {
            mMap.setOnPoiClickListener(this::selectPointOfInterest);
            mMap.setOnMapLongClickListener(latLng -> selectLocation(latLng, formatCoordinates(latLng)));
        }

        LatLng focusLocation = readFocusLocation();
        if (focusLocation != null) {
            String focusLabel = resolveLabel(readFocusLabel(), focusLocation);

            if (pickMode) {
                selectLocation(focusLocation, focusLabel);
            } else {
                showLocation(focusLocation, focusLabel);
                showViewingHint(focusLabel);
            }
            return;
        }

        LatLng defaultLocation = new LatLng(43.5297, 5.4474);
        showLocation(defaultLocation, getString(R.string.travelpath_start_marker));

        if (pickMode) {
            mapHintView.setVisibility(View.VISIBLE);
            mapHintView.setText(R.string.travelpath_pick_hint);
            updateSelectionButtonState();
            return;
        }

        mapHintView.setVisibility(View.GONE);
    }

    private void configureDefaultMode() {
        btnAddTrip.setOnClickListener(v -> startActivity(new Intent(this, AddTripActivity.class)));

        btnAuth.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                startActivity(FeatureNavigation.createLoginIntent(this, FeatureNavigation.DESTINATION_TRAVEL_PATH));
                return;
            }

            startActivity(FeatureNavigation.createPostAuthIntent(this, FeatureNavigation.DESTINATION_TRAVEL_SHARE));
        });

        updateAuthButton();
    }

    private void configurePickerMode() {
        mapHintView.setVisibility(View.VISIBLE);
        mapHintView.setText(R.string.travelpath_pick_hint);

        btnAuth.setText(R.string.travelpath_pick_cancel);
        btnAuth.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnAddTrip.setText(R.string.travelpath_pick_confirm);
        btnAddTrip.setOnClickListener(v -> finishSelection());
        updateSelectionButtonState();
    }

    private void selectPointOfInterest(PointOfInterest pointOfInterest) {
        selectLocation(pointOfInterest.latLng, pointOfInterest.name);
    }

    private void selectLocation(LatLng latLng, @Nullable String label) {
        selectedLocation = latLng;
        selectedLocationName = resolveLabel(label, latLng);

        showLocation(latLng, selectedLocationName);
        mapHintView.setVisibility(View.VISIBLE);
        mapHintView.setText(getString(R.string.travelpath_pick_selected, selectedLocationName));
        updateSelectionButtonState();
    }

    private void finishSelection() {
        if (selectedLocation == null) {
            return;
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_SELECTED_LATITUDE, selectedLocation.latitude);
        result.putExtra(EXTRA_SELECTED_LONGITUDE, selectedLocation.longitude);
        result.putExtra(EXTRA_SELECTED_LABEL, selectedLocationName);
        setResult(RESULT_OK, result);
        finish();
    }

    private void showLocation(LatLng latLng, String label) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(label));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
    }

    private void showViewingHint(String label) {
        mapHintView.setVisibility(View.VISIBLE);
        mapHintView.setText(getString(R.string.travelpath_viewing_place, label));
    }

    private void updateSelectionButtonState() {
        if (!pickMode) {
            return;
        }

        boolean hasSelection = selectedLocation != null;
        btnAddTrip.setEnabled(hasSelection);
        btnAddTrip.setAlpha(hasSelection ? 1f : 0.6f);
    }

    private void updateAuthButton() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            btnAuth.setText(R.string.travelpath_sign_in);
            return;
        }

        btnAuth.setText(R.string.travelpath_open_travelshare);
    }

    @Nullable
    private LatLng readFocusLocation() {
        if (!getIntent().hasExtra(EXTRA_FOCUS_LATITUDE) || !getIntent().hasExtra(EXTRA_FOCUS_LONGITUDE)) {
            return null;
        }

        return new LatLng(
                getIntent().getDoubleExtra(EXTRA_FOCUS_LATITUDE, 0d),
                getIntent().getDoubleExtra(EXTRA_FOCUS_LONGITUDE, 0d));
    }

    @Nullable
    private String readFocusLabel() {
        return getIntent().getStringExtra(EXTRA_FOCUS_LABEL);
    }

    private String resolveLabel(@Nullable String label, LatLng latLng) {
        if (label != null && !label.trim().isEmpty()) {
            return label;
        }

        return formatCoordinates(latLng);
    }

    private String formatCoordinates(LatLng latLng) {
        return String.format(Locale.US, "%.4f, %.4f", latLng.latitude, latLng.longitude);
    }
}
