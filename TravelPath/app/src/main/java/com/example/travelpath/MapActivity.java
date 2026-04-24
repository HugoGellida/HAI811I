package com.example.travelpath;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Button btnAddTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        btnAddTrip = findViewById(R.id.btnAddTrip);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        btnAddTrip.setOnClickListener(v -> {
            startActivity(new Intent(this, AddTripActivity.class));
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Position par défaut (ex : France)
        LatLng defaultLocation = new LatLng(43.5297, 5.4474); // Salon-de-Provence approx

        mMap.addMarker(new MarkerOptions()
                .position(defaultLocation)
                .title("TravelPath Start"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
    }
}