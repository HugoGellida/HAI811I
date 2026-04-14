package com.example.projetprogmobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
        startActivityForResult(intent, PICK_IMAGE); // 🔥 IMPORTANT
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }

    private void uploadImage() {
        StorageReference ref = storage.getReference("photos/" + UUID.randomUUID());

        ref.putFile(imageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {

                    String desc = description.getText().toString();

                    Photo photo = new Photo(
                            uri.toString(),
                            desc,
                            FirebaseAuth.getInstance().getUid()
                    );

                    db.collection("photos").add(photo);

                    Toast.makeText(this, "Upload réussi", Toast.LENGTH_SHORT).show();

                    // 🔥 Retour au Feed
                    finish();
                }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur upload", Toast.LENGTH_SHORT).show()
                );
    }
}