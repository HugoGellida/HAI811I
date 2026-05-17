package com.example.travelpath;
import com.example.projetprogmobile.R;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddTripActivity extends AppCompatActivity {

    EditText inputTitle;
    Button btnSaveTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        inputTitle = findViewById(R.id.inputTitle);
        btnSaveTrip = findViewById(R.id.btnSaveTrip);

        btnSaveTrip.setOnClickListener(v -> {
            String title = inputTitle.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, R.string.travelpath_trip_title_required, Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this,
                    getString(R.string.travelpath_trip_added, title),
                    Toast.LENGTH_SHORT).show();

            finish();
        });
    }
}
