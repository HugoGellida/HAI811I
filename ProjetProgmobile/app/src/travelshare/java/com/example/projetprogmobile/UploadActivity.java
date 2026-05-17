package com.example.projetprogmobile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.travelpath.MapActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UploadActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button selectBtn;
    private Button selectLocationBtn;
    private Button uploadBtn;
    private EditText description;
    private EditText keywordsInput;
    private TextView locationValue;
    private Uri imageUri;
    private double selectedLatitude;
    private double selectedLongitude;
    private String selectedLocationName;

    private FirebaseFirestore db;
    private String currentAuthorDisplayName;
    private String currentAuthorAvatarBase64;

    private static final int PICK_IMAGE = 1;
    private static final int PICK_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, R.string.travelshare_post_requires_auth, Toast.LENGTH_SHORT).show();
            startActivity(FeatureNavigation.createPostAuthIntent(this, FeatureNavigation.DESTINATION_TRAVEL_SHARE));
            finish();
            return;
        }

        setContentView(R.layout.activity_upload);

        imageView = findViewById(R.id.imageView);
        selectBtn = findViewById(R.id.selectBtn);
        selectLocationBtn = findViewById(R.id.selectLocationBtn);
        uploadBtn = findViewById(R.id.uploadBtn);
        description = findViewById(R.id.description);
        keywordsInput = findViewById(R.id.keywordsInput);
        locationValue = findViewById(R.id.locationValue);

        db = FirebaseFirestore.getInstance();
        loadCurrentAuthorProfile();

        selectBtn.setOnClickListener(v -> selectImage());
        selectLocationBtn.setOnClickListener(v -> openLocationPicker());

        uploadBtn.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImage();
            } else {
                Toast.makeText(this, "Choisis une image", Toast.LENGTH_SHORT).show();
            }
        });

        updateSelectedLocationUi();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_LOCATION) {
            if (resultCode == RESULT_OK && data != null) {
                selectedLatitude = data.getDoubleExtra(MapActivity.EXTRA_SELECTED_LATITUDE, 0d);
                selectedLongitude = data.getDoubleExtra(MapActivity.EXTRA_SELECTED_LONGITUDE, 0d);
                selectedLocationName = data.getStringExtra(MapActivity.EXTRA_SELECTED_LABEL);
                updateSelectedLocationUi();
            }
            return;
        }

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }

    private void openLocationPicker() {
        startActivityForResult(
                MapActivity.createLocationPickerIntent(
                        this,
                        selectedLocationName != null ? selectedLatitude : null,
                        selectedLocationName != null ? selectedLongitude : null,
                        selectedLocationName),
                PICK_LOCATION);
    }

    private String imageToBase64(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            // 🔥 REDIMENSION (évite crash mémoire)
            int maxSize = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            float ratio = (float) width / height;

            if (ratio > 1) {
                width = maxSize;
                height = (int) (width / ratio);
            } else {
                height = maxSize;
                width = (int) (height * ratio);
            }

            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);

            byte[] imageBytes = baos.toByteArray();

            // 🔥 IMPORTANT
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void uploadImage() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, R.string.travelshare_post_requires_auth, Toast.LENGTH_SHORT).show();
            return;
        }

        String base64Image = imageToBase64(imageUri);

        if (base64Image == null) {
            Toast.makeText(this, "Erreur image", Toast.LENGTH_SHORT).show();
            return;
        }

        String desc = description.getText().toString();

        Photo photo = new Photo(
                base64Image,
                desc,
                FirebaseAuth.getInstance().getUid()
        );
        photo.setAuthorDisplayName(resolveCurrentAuthorDisplayName());
        photo.setAuthorAvatarBase64(currentAuthorAvatarBase64);

        if (selectedLocationName != null && !selectedLocationName.trim().isEmpty()) {
            photo.setLatitude(selectedLatitude);
            photo.setLongitude(selectedLongitude);
            photo.setLocationName(selectedLocationName);
        }

        photo.setKeywords(parseKeywords());

        db.collection("photos").add(photo);

        Toast.makeText(this, "Upload réussi", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateSelectedLocationUi() {
        boolean hasLocation = selectedLocationName != null && !selectedLocationName.trim().isEmpty();

        locationValue.setText(hasLocation
                ? getString(R.string.travelshare_location_selected, selectedLocationName)
                : getString(R.string.travelshare_location_none));
        selectLocationBtn.setText(hasLocation
                ? R.string.travelshare_location_change
                : R.string.travelshare_location_tag);
    }

    private List<String> parseKeywords() {
        Set<String> uniqueKeywords = new LinkedHashSet<>();
        String rawKeywords = keywordsInput.getText().toString();

        for (String value : rawKeywords.split("[,;]")) {
            String keyword = value.trim();
            if (!keyword.isEmpty()) {
                uniqueKeywords.add(keyword);
            }
        }

        return new ArrayList<>(uniqueKeywords);
    }

    private void loadCurrentAuthorProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid() == null) {
            return;
        }

        currentAuthorDisplayName = user.getDisplayName();

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String displayName = documentSnapshot.getString("displayName");
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        currentAuthorDisplayName = displayName.trim();
                    }

                    currentAuthorAvatarBase64 = documentSnapshot.getString("avatarBase64");
                });
    }

    private String resolveCurrentAuthorDisplayName() {
        if (currentAuthorDisplayName != null && !currentAuthorDisplayName.trim().isEmpty()) {
            return currentAuthorDisplayName.trim();
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }

        return "Voyageur";
    }
}