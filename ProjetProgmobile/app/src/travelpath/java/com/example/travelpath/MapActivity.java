package com.example.travelpath;

import com.example.projetprogmobile.FeedActivity;
import com.example.projetprogmobile.R;
import com.example.projetprogmobile.Photo;
import com.example.travelpath.model.Poi;
import com.example.travelpath.model.TravelRoute;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String EXTRA_PICK_MODE = "com.example.travelpath.extra.PICK_MODE";
    public static final String EXTRA_FOCUS_LATITUDE = "com.example.travelpath.extra.FOCUS_LATITUDE";
    public static final String EXTRA_FOCUS_LONGITUDE = "com.example.travelpath.extra.FOCUS_LONGITUDE";
    public static final String EXTRA_FOCUS_LABEL = "com.example.travelpath.extra.FOCUS_LABEL";
    public static final String EXTRA_SELECTED_LATITUDE = "com.example.travelpath.extra.SELECTED_LATITUDE";
    public static final String EXTRA_SELECTED_LONGITUDE = "com.example.travelpath.extra.SELECTED_LONGITUDE";
    public static final String EXTRA_SELECTED_LABEL = "com.example.travelpath.extra.SELECTED_LABEL";
    public static final String EXTRA_GENERATED_ROUTE = "com.example.travelpath.extra.GENERATED_ROUTE";

    private static final float DEFAULT_ZOOM = 12f;
    private static final String TAG = "TravelPathMap";

    private GoogleMap mMap;
    private Button btnAddTrip;
    private Button btnAuth;
    private TextView mapHintView;
    private boolean pickMode;
    private LatLng selectedLocation;
    private String selectedLocationName;
    private TravelRoute generatedRoute;
    private FirebaseFirestore db;
    private ListenerRegistration taggedPhotosRegistration;
    private final List<Photo> taggedTravelSharePhotos = new ArrayList<>();
    private final List<Marker> taggedPhotoMarkers = new ArrayList<>();

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

    public static Intent createGeneratedRouteIntent(Context context, @NonNull TravelRoute route) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(EXTRA_GENERATED_ROUTE, route);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);
        generatedRoute = readGeneratedRoute();
        db = FirebaseFirestore.getInstance();

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

        if (generatedRoute != null) {
            configureGeneratedRouteMode();
            return;
        }

        configureDefaultMode();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!pickMode) {
            attachTaggedPhotosListener();
        }

        if (!pickMode && generatedRoute == null) {
            updateAuthButton();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachTaggedPhotosListener();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (pickMode) {
            mMap.setOnPoiClickListener(this::selectPointOfInterest);
            mMap.setOnMapLongClickListener(latLng -> selectLocation(latLng, formatCoordinates(latLng)));
        } else {
            mMap.setOnMarkerClickListener(this::handleMarkerClick);
        }

        if (generatedRoute != null) {
            showGeneratedRoute(generatedRoute);
            return;
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
        renderTaggedTravelSharePhotoMarkers();
    }

    private void configureDefaultMode() {
        btnAddTrip.setText(R.string.travelpath_open_planner);
        btnAddTrip.setOnClickListener(v -> startActivity(new Intent(this, TravelPathMainActivity.class)));

        btnAuth.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                startActivity(FeatureNavigation.createLoginIntent(this, FeatureNavigation.DESTINATION_TRAVEL_PATH));
                return;
            }

            startActivity(FeatureNavigation.createPostAuthIntent(this, FeatureNavigation.DESTINATION_TRAVEL_SHARE));
        });

        updateAuthButton();
    }

    private void configureGeneratedRouteMode() {
        btnAuth.setVisibility(View.GONE);
        btnAddTrip.setText(R.string.travelpath_back_to_search);
        btnAddTrip.setEnabled(true);
        btnAddTrip.setAlpha(1f);
        btnAddTrip.setOnClickListener(v -> finish());
        mapHintView.setVisibility(View.VISIBLE);
        mapHintView.setText(R.string.travelpath_route_map_loading);
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
        taggedPhotoMarkers.clear();
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(label));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
        renderTaggedTravelSharePhotoMarkers();
    }

    private void showGeneratedRoute(@NonNull TravelRoute route) {
        List<Poi> routeStops = route.getStops();
        if (routeStops.isEmpty()) {
            mapHintView.setVisibility(View.VISIBLE);
            mapHintView.setText(R.string.travelpath_route_map_empty);
            return;
        }

        mMap.clear();
        taggedPhotoMarkers.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        PolylineOptions polylineOptions = new PolylineOptions().color(0xFF1565C0).width(8f);
        Poi firstStop = routeStops.get(0);

        for (int index = 0; index < routeStops.size(); index++) {
            Poi stop = routeStops.get(index);
            LatLng position = new LatLng(stop.getLatitude(), stop.getLongitude());
            boundsBuilder.include(position);
            polylineOptions.add(position);
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title((index + 1) + ". " + stop.getDisplayName())
                    .snippet(stop.getDescription()));
        }

        if (routeStops.size() > 1) {
            mMap.addPolyline(polylineOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 140));
        } else {
            LatLng singlePosition = new LatLng(firstStop.getLatitude(), firstStop.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singlePosition, DEFAULT_ZOOM));
        }

        mapHintView.setVisibility(View.VISIBLE);
        mapHintView.setText(getString(R.string.travelpath_route_map_summary, route.getTitle(), route.getSummary()));
        renderTaggedTravelSharePhotoMarkers();
    }

    private void attachTaggedPhotosListener() {
        if (taggedPhotosRegistration != null) {
            return;
        }

        taggedPhotosRegistration = db.collection("photos")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Impossible de charger les posts TravelShare geolocalises.", error);
                        return;
                    }

                    taggedTravelSharePhotos.clear();
                    if (value != null) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            Photo photo = document.toObject(Photo.class);
                            if (!hasRenderablePhotoLocation(photo)) {
                                continue;
                            }

                            photo.setId(document.getId());
                            taggedTravelSharePhotos.add(photo);
                        }
                    }

                    renderTaggedTravelSharePhotoMarkers();
                });
    }

    private void detachTaggedPhotosListener() {
        if (taggedPhotosRegistration == null) {
            return;
        }

        taggedPhotosRegistration.remove();
        taggedPhotosRegistration = null;
    }

    private void renderTaggedTravelSharePhotoMarkers() {
        if (mMap == null || pickMode) {
            return;
        }

        clearTaggedTravelSharePhotoMarkers();
        for (Photo photo : taggedTravelSharePhotos) {
            LatLng position = new LatLng(photo.getLatitude(), photo.getLongitude());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(photo.getDisplayLocationName())
                    .snippet(resolvePhotoSnippet(photo))
                    .anchor(0.5f, 1f)
                    .zIndex(0f)
                    .icon(BitmapDescriptorFactory.fromBitmap(createPhotoBubbleBitmap(photo))));

            if (marker != null) {
                marker.setTag(photo);
                taggedPhotoMarkers.add(marker);
            }
        }
    }

    private boolean handleMarkerClick(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (!(tag instanceof Photo)) {
            return false;
        }

        Photo photo = (Photo) tag;
        if (photo.getId() == null || photo.getId().trim().isEmpty()) {
            return false;
        }

        startActivity(FeedActivity.createFocusedPhotoIntent(
                this,
                photo.getId(),
                photo.getDisplayLocationName()));
        return true;
    }

    private void clearTaggedTravelSharePhotoMarkers() {
        for (Marker marker : taggedPhotoMarkers) {
            if (marker != null) {
                marker.remove();
            }
        }

        taggedPhotoMarkers.clear();
    }

    private boolean hasRenderablePhotoLocation(@Nullable Photo photo) {
        return photo != null && (photo.getLatitude() != 0d || photo.getLongitude() != 0d);
    }

    @NonNull
    private String resolvePhotoSnippet(@NonNull Photo photo) {
        if (photo.getDescription() != null && !photo.getDescription().trim().isEmpty()) {
            return photo.getDescription().trim();
        }

        if (photo.hasKeywords()) {
            return photo.getDisplayKeywords();
        }

        return photo.getDisplayLocationName();
    }

    @NonNull
    private Bitmap createPhotoBubbleBitmap(@NonNull Photo photo) {
        int bubbleWidth = dpToPx(58);
        int bubbleHeight = dpToPx(70);
        int tailHeight = dpToPx(12);
        int outerPadding = dpToPx(3);
        float cornerRadius = dpToPx(12);

        Bitmap output = Bitmap.createBitmap(bubbleWidth, bubbleHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        RectF bubbleRect = new RectF(0f, 0f, bubbleWidth, bubbleHeight - tailHeight);
        float centerX = bubbleWidth / 2f;
        float tailHalfWidth = dpToPx(8);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(bubbleRect, cornerRadius, cornerRadius, fillPaint);

        Path tailPath = new Path();
        tailPath.moveTo(centerX - tailHalfWidth, bubbleRect.bottom - dpToPx(1));
        tailPath.lineTo(centerX, bubbleHeight);
        tailPath.lineTo(centerX + tailHalfWidth, bubbleRect.bottom - dpToPx(1));
        tailPath.close();
        canvas.drawPath(tailPath, fillPaint);

        RectF imageRect = new RectF(
                outerPadding,
                outerPadding,
                bubbleWidth - outerPadding,
                bubbleRect.bottom - outerPadding);

        Bitmap photoBitmap = decodePhotoBitmap(photo);
        if (photoBitmap != null) {
            Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            BitmapShader shader = new BitmapShader(photoBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            Matrix matrix = new Matrix();
            float scale = Math.max(
                    imageRect.width() / photoBitmap.getWidth(),
                    imageRect.height() / photoBitmap.getHeight());
            float dx = imageRect.left + (imageRect.width() - (photoBitmap.getWidth() * scale)) / 2f;
            float dy = imageRect.top + (imageRect.height() - (photoBitmap.getHeight() * scale)) / 2f;
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            shader.setLocalMatrix(matrix);
            imagePaint.setShader(shader);
            canvas.drawRoundRect(imageRect, cornerRadius - outerPadding, cornerRadius - outerPadding, imagePaint);
        } else {
            Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            placeholderPaint.setColor(0xFF1976D2);
            canvas.drawRoundRect(imageRect, cornerRadius - outerPadding, cornerRadius - outerPadding, placeholderPaint);

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(dpToPx(12));
            float textY = imageRect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f);
            canvas.drawText("TS", imageRect.centerX(), textY, textPaint);
        }

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dpToPx(1));
        strokePaint.setColor(0x22000000);
        canvas.drawRoundRect(bubbleRect, cornerRadius, cornerRadius, strokePaint);
        canvas.drawPath(tailPath, strokePaint);

        return output;
    }

    @Nullable
    private Bitmap decodePhotoBitmap(@NonNull Photo photo) {
        String base64 = photo.getImageBase64();
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] bytes = Base64.decode(base64.trim(), Base64.NO_WRAP);
            if (bytes.length == 0) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (IllegalArgumentException error) {
            Log.e(TAG, "Base64 photo invalide pour le marqueur de carte.", error);
            return null;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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

    @Nullable
    private TravelRoute readGeneratedRoute() {
        Serializable serializedRoute = getIntent().getSerializableExtra(EXTRA_GENERATED_ROUTE);
        if (serializedRoute instanceof TravelRoute) {
            return (TravelRoute) serializedRoute;
        }

        return null;
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
