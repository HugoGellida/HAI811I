package com.example.travelpath;

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
                Toast.makeText(this, "Titre obligatoire", Toast.LENGTH_SHORT).show();
                return;
            }

            // Version simple (à améliorer ensuite avec stockage)
            Toast.makeText(this,
                    "Trajet ajouté : " + title,
                    Toast.LENGTH_SHORT).show();

            finish();
        });
    }
}