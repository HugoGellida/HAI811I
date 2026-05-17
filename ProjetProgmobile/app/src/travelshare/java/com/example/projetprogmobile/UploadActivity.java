package com.example.projetprogmobile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.travelpath.MapActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UploadActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_PHOTO_ID = "com.example.projetprogmobile.extra.EDIT_PHOTO_ID";
    private static final String TAG = "UploadActivity";

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
    private String editingPhotoId;
    private String existingImageBase64;
    private boolean editMode;
    private Photo editablePhoto;

    private FirebaseFirestore db;
    private String currentAuthorDisplayName;
    private String currentAuthorAvatarBase64;

    private static final int PICK_IMAGE = 1;
    private static final int PICK_LOCATION = 2;

    public static Intent createEditIntent(@NonNull Context context, @NonNull String photoId) {
        Intent intent = new Intent(context, UploadActivity.class);
        intent.putExtra(EXTRA_EDIT_PHOTO_ID, photoId);
        return intent;
    }

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
        editingPhotoId = getIntent().getStringExtra(EXTRA_EDIT_PHOTO_ID);
        editMode = editingPhotoId != null && !editingPhotoId.trim().isEmpty();

        db = FirebaseFirestore.getInstance();
        loadCurrentAuthorProfile();

        selectBtn.setOnClickListener(v -> selectImage());
        selectLocationBtn.setOnClickListener(v -> openLocationPicker());

        uploadBtn.setOnClickListener(v -> {
            if (!hasImageForSubmission()) {
                Toast.makeText(this, "Choisis une image", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadImage();
        });

        if (editMode) {
            setTitle(R.string.travelshare_edit_post_title);
            uploadBtn.setText(R.string.travelshare_edit_post_confirm);
            loadPhotoForEditing();
        }

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

        String base64Image = imageUri != null ? imageToBase64(imageUri) : existingImageBase64;

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
        photo.setLikes(editablePhoto != null ? editablePhoto.getLikes() : 0);
        photo.setLikedByUserIds(editablePhoto != null ? editablePhoto.getLikedByUserIds() : new ArrayList<>());
        photo.setCommentsCount(editablePhoto != null ? editablePhoto.getCommentsCount() : 0);

        if (selectedLocationName != null && !selectedLocationName.trim().isEmpty()) {
            photo.setLatitude(selectedLatitude);
            photo.setLongitude(selectedLongitude);
            photo.setLocationName(selectedLocationName);
        }

        photo.setKeywords(parseKeywords());

        if (editMode) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("imageBase64", photo.getImageBase64());
            updates.put("description", photo.getDescription());
            updates.put("userId", photo.getUserId());
            updates.put("authorDisplayName", photo.getAuthorDisplayName());
            updates.put("authorAvatarBase64", photo.getAuthorAvatarBase64());
            updates.put("keywords", photo.getKeywords());
            updates.put("latitude", photo.getLatitude());
            updates.put("longitude", photo.getLongitude());
            updates.put("locationName", photo.getLocationName());
            updates.put("timestamp", System.currentTimeMillis());
            db.collection("photos")
                    .document(editingPhotoId)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, R.string.travelshare_edit_success, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(error -> Toast.makeText(this, R.string.travelshare_edit_failure, Toast.LENGTH_SHORT).show());
            return;
        }

        db.collection("photos")
                .add(photo)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Upload réussi", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Erreur upload", Toast.LENGTH_SHORT).show());
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

    private boolean hasImageForSubmission() {
        return imageUri != null || (existingImageBase64 != null && !existingImageBase64.trim().isEmpty());
    }

    private void loadPhotoForEditing() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getUid() == null) {
            finish();
            return;
        }

        db.collection("photos")
                .document(editingPhotoId)
                .get()
                .addOnSuccessListener(this::bindEditablePhoto)
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Impossible de charger le post a modifier", error);
                    Toast.makeText(this, R.string.travelshare_edit_failure, Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindEditablePhoto(@NonNull DocumentSnapshot documentSnapshot) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Photo photo = documentSnapshot.toObject(Photo.class);

        if (currentUser == null || photo == null || photo.getUserId() == null || !photo.getUserId().equals(currentUser.getUid())) {
            Toast.makeText(this, R.string.travelshare_edit_not_allowed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editablePhoto = photo;
        existingImageBase64 = photo.getImageBase64();
        description.setText(photo.getDescription() != null ? photo.getDescription() : "");
        keywordsInput.setText(buildKeywordsInput(photo));

        if (photo.hasTaggedLocation()) {
            selectedLatitude = photo.getLatitude();
            selectedLongitude = photo.getLongitude();
            selectedLocationName = photo.getLocationName();
        }

        renderExistingImage(existingImageBase64);
        updateSelectedLocationUi();
    }

    private void renderExistingImage(@Nullable String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }

        try {
            byte[] decodedBytes = Base64.decode(base64Image.trim(), Base64.NO_WRAP);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                return;
            }
        } catch (IllegalArgumentException error) {
            Log.e(TAG, "Base64 image invalide pour l edition", error);
        }

        imageView.setImageResource(android.R.drawable.ic_menu_report_image);
    }

    @NonNull
    private String buildKeywordsInput(@NonNull Photo photo) {
        if (!photo.hasKeywords()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String keyword : photo.getKeywords()) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(keyword.trim());
        }

        return builder.toString();
    }
}