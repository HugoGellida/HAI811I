package com.example.projetprogmobile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CommentThreadActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_ID = "com.example.projetprogmobile.extra.PHOTO_ID";
    public static final String EXTRA_PHOTO_AUTHOR = "com.example.projetprogmobile.extra.PHOTO_AUTHOR";
    public static final String EXTRA_PHOTO_DESCRIPTION = "com.example.projetprogmobile.extra.PHOTO_DESCRIPTION";

    private final List<Comment> comments = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private CommentAdapter adapter;
    private ListenerRegistration commentsRegistration;
    private String photoId;
    private String currentAuthorDisplayName;
    private String currentAuthorAvatarBase64;

    private TextView threadHeaderView;
    private TextView statusView;
    private EditText commentInput;
    private Button sendButton;

    public static Intent createIntent(@NonNull Context context, @NonNull Photo photo) {
        Intent intent = new Intent(context, CommentThreadActivity.class);
        intent.putExtra(EXTRA_PHOTO_ID, photo.getId());
        intent.putExtra(EXTRA_PHOTO_AUTHOR, photo.getDisplayAuthorName());
        intent.putExtra(EXTRA_PHOTO_DESCRIPTION, photo.getDescription());
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_thread);
        setTitle(R.string.travelshare_comment_thread_title);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        photoId = getIntent().getStringExtra(EXTRA_PHOTO_ID);

        if (photoId == null || photoId.trim().isEmpty()) {
            finish();
            return;
        }

        threadHeaderView = findViewById(R.id.comment_thread_header_text);
        statusView = findViewById(R.id.comment_thread_status_text);
        commentInput = findViewById(R.id.comment_input);
        sendButton = findViewById(R.id.send_comment_button);
        RecyclerView recyclerView = findViewById(R.id.comment_recycler_view);

        adapter = new CommentAdapter(comments, this::toggleLikeComment, this::deleteComment);
        adapter.setCurrentUserId(auth.getUid());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        bindThreadHeader();
        loadCurrentAuthorProfile();
        sendButton.setOnClickListener(v -> sendComment());
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachCommentsListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.setCurrentUserId(auth.getUid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (commentsRegistration != null) {
            commentsRegistration.remove();
            commentsRegistration = null;
        }
    }

    private void bindThreadHeader() {
        String author = getIntent().getStringExtra(EXTRA_PHOTO_AUTHOR);
        String description = getIntent().getStringExtra(EXTRA_PHOTO_DESCRIPTION);
        String safeAuthor = author != null && !author.trim().isEmpty() ? author.trim() : getString(R.string.travelshare_comment_unknown_author);
        String safeDescription = description != null && !description.trim().isEmpty()
                ? description.trim()
                : getString(R.string.travelshare_comment_no_description);
        threadHeaderView.setText(getString(R.string.travelshare_comment_thread_header, safeAuthor, safeDescription));
    }

    private void attachCommentsListener() {
        commentsRegistration = db.collection("photos")
                .document(photoId)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        updateStatus(getString(R.string.travelshare_comment_load_failure));
                        return;
                    }

                    comments.clear();
                    if (value != null) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            Comment comment = document.toObject(Comment.class);
                            if (comment == null) {
                                continue;
                            }

                            comment.setId(document.getId());
                            comments.add(comment);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateStatus(comments.isEmpty() ? getString(R.string.travelshare_comment_empty) : null);
                });
    }

    private void sendComment() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(FeatureNavigation.createLoginIntent(this, FeatureNavigation.DESTINATION_TRAVEL_SHARE));
            return;
        }

        String message = commentInput.getText() == null ? "" : commentInput.getText().toString().trim();
        if (message.isEmpty()) {
            commentInput.setError(getString(R.string.travelshare_comment_required));
            return;
        }

        commentInput.setError(null);
        Comment comment = new Comment(
                photoId,
                currentUser.getUid(),
                resolveCurrentAuthorDisplayName(currentUser),
                currentAuthorAvatarBase64,
                message);

        db.collection("photos")
                .document(photoId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(unused -> {
                    setResult(RESULT_OK);
                    commentInput.setText("");
                    updateStatus(null);
                })
                .addOnFailureListener(error -> updateStatus(getString(R.string.travelshare_comment_send_failure)));
    }

    private void toggleLikeComment(Comment comment) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(FeatureNavigation.createLoginIntent(this, FeatureNavigation.DESTINATION_TRAVEL_SHARE));
            return;
        }

        if (comment == null || comment.getId() == null || comment.getId().trim().isEmpty()) {
            return;
        }

        boolean alreadyLiked = comment.isLikedBy(currentUser.getUid());
        db.collection("photos")
                .document(photoId)
                .collection("comments")
                .document(comment.getId())
                .update(
                        "likedByUserIds", alreadyLiked
                                ? FieldValue.arrayRemove(currentUser.getUid())
                                : FieldValue.arrayUnion(currentUser.getUid()),
                        "likes", FieldValue.increment(alreadyLiked ? -1 : 1))
                .addOnFailureListener(error -> Toast.makeText(this, R.string.travelshare_comment_like_failure, Toast.LENGTH_SHORT).show());
    }

    private void deleteComment(Comment comment) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || comment == null || comment.getId() == null || !currentUser.getUid().equals(comment.getUserId())) {
            Toast.makeText(this, R.string.travelshare_comment_delete_not_allowed, Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("photos")
                .document(photoId)
                .collection("comments")
                .document(comment.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    setResult(RESULT_OK);
                    Toast.makeText(this, R.string.travelshare_comment_delete_success, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> Toast.makeText(this, R.string.travelshare_comment_delete_failure, Toast.LENGTH_SHORT).show());
    }

    private void loadCurrentAuthorProfile() {
        FirebaseUser user = auth.getCurrentUser();
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

    private String resolveCurrentAuthorDisplayName(@NonNull FirebaseUser user) {
        if (currentAuthorDisplayName != null && !currentAuthorDisplayName.trim().isEmpty()) {
            return currentAuthorDisplayName.trim();
        }

        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }

        return getString(R.string.travelshare_comment_unknown_author);
    }

    private void updateStatus(@Nullable String message) {
        boolean hasMessage = message != null && !message.trim().isEmpty();
        statusView.setVisibility(hasMessage ? View.VISIBLE : View.GONE);
        statusView.setText(hasMessage ? message : "");
    }
}