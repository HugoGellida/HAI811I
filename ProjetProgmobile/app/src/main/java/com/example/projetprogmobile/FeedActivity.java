package com.example.projetprogmobile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button uploadButton;
    private PhotoAdapter adapter;
    private List<Photo> photoList = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PhotoAdapter(photoList);
        recyclerView.setAdapter(adapter);

        uploadButton = findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(v -> {
            startActivity(new Intent(this, UploadActivity.class));
        });


        db = FirebaseFirestore.getInstance();

        loadPhotos();
    }

    private void loadPhotos() {
        db.collection("photos")
                .addSnapshotListener((value, error) -> {
                    photoList.clear();
                    if (value == null) return;
                    for(DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            Photo p = doc.toObject(Photo.class);

                            if (p != null) {
                                photoList.add(p);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("FIRESTORE", "Erreur doc: " + doc.getId());
                        }

                    }
                    adapter.notifyDataSetChanged();
                });
    }
}