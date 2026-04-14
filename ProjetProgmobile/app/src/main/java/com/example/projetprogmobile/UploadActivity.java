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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {

    private ImageView imageView;
    Button selectBtn, uploadBtn;
    EditText description;
    private Uri imageUri;

    private FirebaseStorage storage;
    private FirebaseFirestore db;

    private static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        imageView = findViewById(R.id.imageView);
        selectBtn = findViewById(R.id.selectBtn);
        uploadBtn = findViewById(R.id.uploadBtn);
        description = findViewById(R.id.description);

        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();

        selectBtn.setOnClickListener(v -> selectImage());

        uploadBtn.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImage();
            } else {
                Toast.makeText(this, "Choisis une image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
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

        db.collection("photos").add(photo);

        Toast.makeText(this, "Upload réussi", Toast.LENGTH_SHORT).show();
        finish();
    }
}