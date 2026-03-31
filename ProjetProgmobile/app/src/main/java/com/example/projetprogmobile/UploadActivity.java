package com.example.projetprogmobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
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
    private Button uploadBtn;
    private Uri imageUri;

    private FirebaseStorage storage;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        imageView = findViewById(R.id.imageView);
        uploadBtn = findViewById(R.id.uploadBtn);

        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();

        imageView.setOnClickListener(v -> selectImage());
        uploadBtn.setOnClickListener(v -> uploadImage());
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }

    private void uploadImage() {
        StorageReference ref = storage.getReference("photos/" + UUID.randomUUID());

        ref.putFile(imageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {

                    Photo photo = new Photo(
                            uri.toString(),
                            "Description test",
                            FirebaseAuth.getInstance().getUid()
                    );

                    db.collection("photos").add(photo);
                    Toast.makeText(this, "Upload réussi", Toast.LENGTH_SHORT).show();
                }));
    }
}