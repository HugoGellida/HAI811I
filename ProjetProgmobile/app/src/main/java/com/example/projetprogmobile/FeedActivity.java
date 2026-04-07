package com.example.projetprogmobile;

import android.os.Bundle;
import android.widget.Button;

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
            // Handle upload button click
        });


        db = FirebaseFirestore.getInstance();

        loadPhotos();
    }

    private void loadPhotos() {
        db.collection("photos")
                .addSnapshotListener((value, error) -> {
                    photoList.clear();
                    assert value != null;
                    for(DocumentSnapshot doc : value.getDocuments()) {
                        Photo p = doc.toObject(Photo.class);
                        photoList.add(p);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}