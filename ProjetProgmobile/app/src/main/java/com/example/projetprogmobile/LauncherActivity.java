package com.example.projetprogmobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelpath.TravelPathMainActivity;

public class LauncherActivity extends AppCompatActivity {

    private Button travelShareButton;
    private Button travelPathButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        travelShareButton = findViewById(R.id.button_open_travelshare);
        travelPathButton = findViewById(R.id.button_open_travelpath);

        travelShareButton.setOnClickListener(v -> {
            startActivity(FeatureNavigation.createPostAuthIntent(this, FeatureNavigation.DESTINATION_TRAVEL_SHARE));
        });

        travelPathButton.setOnClickListener(v -> {
            startActivity(new Intent(this, TravelPathMainActivity.class));
        });
    }
}