package com.example.projetprogmobile;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelpath.MapActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeedActivity extends AppCompatActivity {

    public static final String EXTRA_FOCUSED_PHOTO_ID = "com.example.projetprogmobile.extra.FOCUSED_PHOTO_ID";
    public static final String EXTRA_FOCUSED_PHOTO_LABEL = "com.example.projetprogmobile.extra.FOCUSED_PHOTO_LABEL";
    private static final double PLACE_SEARCH_RADIUS_KM = 100d;

    private RecyclerView recyclerView;
    private Button uploadButton;
    private Button authButton;
    private Button placeModeButton;
    private SearchView searchView;
    private ImageButton profileButton;
    private TextView authStatusView;
    private TextView feedStatusView;
    private TextView searchSummaryView;
    private PhotoAdapter adapter;
    private final List<Photo> photoList = new ArrayList<>();
    private final List<Photo> allPhotos = new ArrayList<>();
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentSearchQuery = "";
    private LatLng currentSearchCenter;
    private String currentSearchLocationLabel;
    private String focusedPhotoId;
    private String focusedPhotoLabel;
    private int searchRequestToken;
    private boolean forcePlaceMode;
    private boolean placeSearchPending;

    public static Intent createFocusedPhotoIntent(Context context, @NonNull String photoId, @Nullable String label) {
        Intent intent = new Intent(context, FeedActivity.class);
        intent.putExtra(EXTRA_FOCUSED_PHOTO_ID, photoId);
        if (label != null && !label.trim().isEmpty()) {
            intent.putExtra(EXTRA_FOCUSED_PHOTO_LABEL, label);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PhotoAdapter(photoList, this::confirmDeletePhoto, this::openPhotoLocation);
        recyclerView.setAdapter(adapter);

        placeModeButton = findViewById(R.id.place_mode_button);
        searchView = findViewById(R.id.search_view);
        profileButton = findViewById(R.id.profile_button);
        authStatusView = findViewById(R.id.auth_status_text);
        feedStatusView = findViewById(R.id.feed_status_text);
        searchSummaryView = findViewById(R.id.search_summary_text);
        authButton = findViewById(R.id.auth_button);
        uploadButton = findViewById(R.id.upload_button);

        auth = FirebaseAuth.getInstance();

        authButton.setOnClickListener(v -> {
            startActivity(FeatureNavigation.createLoginIntent(this, FeatureNavigation.DESTINATION_TRAVEL_SHARE));
        });

        profileButton.setOnClickListener(v -> startActivity(
                ProfileActivity.createIntent(this, FeatureNavigation.DESTINATION_TRAVEL_SHARE)));

        uploadButton.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, R.string.travelshare_post_requires_auth, Toast.LENGTH_SHORT).show();
                return;
            }

            startActivity(new Intent(this, UploadActivity.class));
        });

        db = FirebaseFirestore.getInstance();
        readFocusedPhotoIntent();

        configureSearch();
        configurePlaceModeButton();
        updateAuthenticationUi();
        loadPhotos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthenticationUi();
        refreshProfileAvatar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geocoderExecutor.shutdownNow();
    }

    private void loadPhotos() {
        db.collection("photos")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIRESTORE", "Erreur lecture photos", error);
                        allPhotos.clear();
                        photoList.clear();
                        adapter.notifyDataSetChanged();
                        updateFeedStatus(getString(
                                R.string.travelshare_feed_error_with_code,
                                error.getCode().name()));
                        return;
                    }

                    allPhotos.clear();
                    if (value == null || value.isEmpty()) {
                        applyFilters();
                        return;
                    }

                    for(DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            Photo p = doc.toObject(Photo.class);

                            if (p != null) {
                                p.setId(doc.getId());
                                allPhotos.add(p);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("FIRESTORE", "Erreur doc: " + doc.getId());
                        }

                        }
                        applyFilters();
                });
    }

    private void updateFeedStatus(String message) {
        boolean hasMessage = message != null && !message.trim().isEmpty();
        feedStatusView.setVisibility(hasMessage ? View.VISIBLE : View.GONE);
        feedStatusView.setText(hasMessage ? message : "");
    }

    private void updateAuthenticationUi() {
        boolean isAuthenticated = auth.getCurrentUser() != null;
        adapter.setCurrentUserId(auth.getUid());
        adapter.notifyDataSetChanged();

        authButton.setVisibility(isAuthenticated ? View.GONE : View.VISIBLE);
        profileButton.setVisibility(isAuthenticated ? View.VISIBLE : View.GONE);
        if (!isAuthenticated) {
            ProfileAvatarUtils.applyAvatar(profileButton, null, 8);
        }
        authStatusView.setText(isAuthenticated
                ? R.string.travelshare_feed_signed_in
                : R.string.travelshare_feed_guest_hint);
        uploadButton.setText(isAuthenticated
                ? R.string.travelshare_publish
                : R.string.travelshare_publish_locked);
        uploadButton.setAlpha(isAuthenticated ? 1f : 0.7f);
    }

    private void refreshProfileAvatar() {
        if (auth.getCurrentUser() == null || auth.getUid() == null) {
            ProfileAvatarUtils.applyAvatar(profileButton, null, 8);
            return;
        }

        db.collection("users")
                .document(auth.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> ProfileAvatarUtils.applyAvatar(
                        profileButton,
                        documentSnapshot.getString("avatarBase64"),
                        8))
                .addOnFailureListener(error -> ProfileAvatarUtils.applyAvatar(profileButton, null, 8));
    }

    private void configureSearch() {
        searchView.setQueryHint(getString(R.string.travelshare_search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                updateSearchQuery(query, forcePlaceMode);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateSearchQuery(newText, forcePlaceMode);
                return true;
            }
        });
    }

    private void configurePlaceModeButton() {
        updatePlaceModeButton();
        placeModeButton.setOnClickListener(v -> {
            clearFocusedPhotoFilter();
            forcePlaceMode = !forcePlaceMode;
            updatePlaceModeButton();
            updateSearchQuery(currentSearchQuery, forcePlaceMode);
        });
    }

    private void updateSearchQuery(String rawQuery, boolean resolvePlace) {
        clearFocusedPhotoFilter();
        currentSearchQuery = rawQuery == null ? "" : rawQuery.trim();
        currentSearchCenter = null;
        currentSearchLocationLabel = null;
        searchRequestToken++;
        placeSearchPending = forcePlaceMode && !currentSearchQuery.isEmpty();

        applyFilters();

        if (!resolvePlace || currentSearchQuery.isEmpty() || !Geocoder.isPresent()) {
            placeSearchPending = false;
            if (forcePlaceMode && !currentSearchQuery.isEmpty() && !Geocoder.isPresent()) {
                applyFilters();
            }
            return;
        }

        resolvePlaceSearch(currentSearchQuery, searchRequestToken);
    }

    private void resolvePlaceSearch(String query, int requestToken) {
        geocoderExecutor.execute(() -> {
            LatLng resolvedLocation = null;
            String resolvedLabel = query;

            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    resolvedLocation = new LatLng(address.getLatitude(), address.getLongitude());
                    if (address.getFeatureName() != null && !address.getFeatureName().trim().isEmpty()) {
                        resolvedLabel = address.getFeatureName();
                    }
                }
            } catch (IOException | IllegalArgumentException error) {
                Log.e("SEARCH", "Erreur geocoding: " + query, error);
            }

            LatLng finalResolvedLocation = resolvedLocation;
            String finalResolvedLabel = resolvedLabel;
            mainHandler.post(() -> {
                if (requestToken != searchRequestToken || !query.equals(currentSearchQuery)) {
                    return;
                }

                placeSearchPending = false;
                currentSearchCenter = finalResolvedLocation;
                currentSearchLocationLabel = finalResolvedLocation != null ? finalResolvedLabel : null;
                applyFilters();
            });
        });
    }

    private void applyFilters() {
        photoList.clear();

        if (focusedPhotoId != null && !focusedPhotoId.trim().isEmpty()) {
            for (Photo photo : allPhotos) {
                if (photo != null && focusedPhotoId.equals(photo.getId())) {
                    photoList.add(photo);
                    break;
                }
            }

            updateSearchSummary(photoList.isEmpty()
                    ? getString(R.string.travelshare_feed_focus_missing)
                    : getString(
                    R.string.travelshare_feed_focus_summary,
                    focusedPhotoLabel != null && !focusedPhotoLabel.trim().isEmpty()
                            ? focusedPhotoLabel
                            : photoList.get(0).getDisplayLocationName()));
            updateFeedStatus(photoList.isEmpty() ? getString(R.string.travelshare_feed_focus_missing) : null);
            adapter.notifyDataSetChanged();
            return;
        }

        if (currentSearchQuery.isEmpty()) {
            photoList.addAll(allPhotos);
            adapter.notifyDataSetChanged();
            updateSearchSummary(null);
            updateFeedStatus(photoList.isEmpty() ? getString(R.string.travelshare_feed_empty) : null);
            return;
        }

        if (forcePlaceMode) {
            if (placeSearchPending) {
                adapter.notifyDataSetChanged();
                updateSearchSummary(getString(R.string.travelshare_search_place_loading, currentSearchQuery));
                updateFeedStatus(null);
                return;
            }

            if (currentSearchCenter == null) {
                adapter.notifyDataSetChanged();
                updateSearchSummary(getString(R.string.travelshare_search_place_mode_summary, currentSearchQuery));
                updateFeedStatus(getString(R.string.travelshare_search_place_not_found, currentSearchQuery));
                return;
            }
        }

        if (currentSearchCenter != null) {
            List<PhotoDistance> matches = new ArrayList<>();
            for (Photo photo : allPhotos) {
                if (photo == null || !photo.hasTaggedLocation()) {
                    continue;
                }

                double distanceKm = distanceBetween(currentSearchCenter, photo);
                if (distanceKm <= PLACE_SEARCH_RADIUS_KM) {
                    matches.add(new PhotoDistance(photo, distanceKm));
                }
            }

            matches.sort((left, right) -> Double.compare(left.distanceKm, right.distanceKm));
            for (PhotoDistance match : matches) {
                photoList.add(match.photo);
            }

            updateSearchSummary(getString(
                    R.string.travelshare_search_place_summary,
                    currentSearchLocationLabel,
                    (int) PLACE_SEARCH_RADIUS_KM));
            updateFeedStatus(photoList.isEmpty()
                    ? getString(R.string.travelshare_search_place_empty, currentSearchLocationLabel)
                    : null);
            adapter.notifyDataSetChanged();
            return;
        }

        for (Photo photo : allPhotos) {
            if (photo != null && photo.matchesTextQuery(currentSearchQuery)) {
                photoList.add(photo);
            }
        }

        updateSearchSummary(getString(R.string.travelshare_search_keyword_summary, currentSearchQuery));
        updateFeedStatus(photoList.isEmpty()
                ? getString(R.string.travelshare_search_no_results, currentSearchQuery)
                : null);
        adapter.notifyDataSetChanged();
    }

    private void updateSearchSummary(String summary) {
        boolean hasSummary = summary != null && !summary.trim().isEmpty();
        searchSummaryView.setVisibility(hasSummary ? View.VISIBLE : View.GONE);
        searchSummaryView.setText(hasSummary ? summary : "");
    }

    private void updatePlaceModeButton() {
        placeModeButton.setText(forcePlaceMode
                ? R.string.travelshare_place_mode_on
                : R.string.travelshare_place_mode_off);
        placeModeButton.setAlpha(forcePlaceMode ? 1f : 0.75f);
    }

    private double distanceBetween(LatLng searchCenter, Photo photo) {
        float[] results = new float[1];
        Location.distanceBetween(
                searchCenter.latitude,
                searchCenter.longitude,
                photo.getLatitude(),
                photo.getLongitude(),
                results);
        return results[0] / 1000d;
    }

    private void confirmDeletePhoto(Photo photo) {
        String currentUserId = auth.getUid();

        if (photo == null || photo.getId() == null || currentUserId == null || !currentUserId.equals(photo.getUserId())) {
            Toast.makeText(this, R.string.travelshare_delete_not_allowed, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.travelshare_delete_post_title)
                .setMessage(R.string.travelshare_delete_post_message)
                .setNegativeButton(R.string.travelshare_delete_post_cancel, null)
                .setPositiveButton(R.string.travelshare_delete_post_confirm, (dialog, which) -> deletePhoto(photo))
                .show();
    }

    private void deletePhoto(Photo photo) {
        db.collection("photos")
                .document(photo.getId())
                .delete()
                .addOnSuccessListener(unused -> Toast.makeText(this, R.string.travelshare_delete_success, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(error -> Toast.makeText(this, R.string.travelshare_delete_failure, Toast.LENGTH_SHORT).show());
    }

    private void openPhotoLocation(Photo photo) {
        if (photo == null || !photo.hasTaggedLocation()) {
            return;
        }

        startActivity(MapActivity.createFocusedLocationIntent(
                this,
                photo.getLatitude(),
                photo.getLongitude(),
                photo.getDisplayLocationName()));
    }

    private void readFocusedPhotoIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        focusedPhotoId = intent.getStringExtra(EXTRA_FOCUSED_PHOTO_ID);
        focusedPhotoLabel = intent.getStringExtra(EXTRA_FOCUSED_PHOTO_LABEL);
    }

    private void clearFocusedPhotoFilter() {
        if (focusedPhotoId == null && focusedPhotoLabel == null) {
            return;
        }

        focusedPhotoId = null;
        focusedPhotoLabel = null;
    }

    private static final class PhotoDistance {
        private final Photo photo;
        private final double distanceKm;

        private PhotoDistance(Photo photo, double distanceKm) {
            this.photo = photo;
            this.distanceKm = distanceKm;
        }
    }
}