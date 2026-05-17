package com.example.projetprogmobile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int PROFILE_AVATAR_PLACEHOLDER_PADDING_DP = 18;

    private EditText usernameInput;
    private EditText passwordInput;
    private TextView emailView;
    private TextView statusView;
    private Button saveButton;
    private Button changePhotoButton;
    private ImageView avatarView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String avatarBase64;
    private boolean avatarChanged;
    private final ActivityResultLauncher<String> avatarPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleSelectedAvatar);

    public static Intent createIntent(Context context, @Nullable String destination) {
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra(FeatureNavigation.EXTRA_DESTINATION, FeatureNavigation.resolveDestination(destination));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setTitle(R.string.profile_title);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(FeatureNavigation.createLoginIntent(this, FeatureNavigation.resolveDestination(getIntent())));
            finish();
            return;
        }

        usernameInput = findViewById(R.id.profile_username_input);
        passwordInput = findViewById(R.id.profile_password_input);
        emailView = findViewById(R.id.profile_email_text);
        statusView = findViewById(R.id.profile_status_text);
        saveButton = findViewById(R.id.profile_save_button);
        changePhotoButton = findViewById(R.id.profile_change_photo_button);
        avatarView = findViewById(R.id.profile_avatar_view);

        saveButton.setOnClickListener(v -> saveProfile());
        changePhotoButton.setOnClickListener(v -> openAvatarPicker());
        avatarView.setOnClickListener(v -> openAvatarPicker());
        ProfileAvatarUtils.applyAvatar(avatarView, null, PROFILE_AVATAR_PLACEHOLDER_PADDING_DP);
        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        emailView.setText(user.getEmail() != null ? user.getEmail() : "");
        usernameInput.setText(resolveFallbackUsername(user));
        updateStatus(getString(R.string.profile_loading), false);

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String displayName = documentSnapshot.getString("displayName");
                    avatarBase64 = documentSnapshot.getString("avatarBase64");
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        usernameInput.setText(displayName.trim());
                    }
                    ProfileAvatarUtils.applyAvatar(avatarView, avatarBase64, PROFILE_AVATAR_PLACEHOLDER_PADDING_DP);
                    updateStatus(null, false);
                })
                .addOnFailureListener(error -> updateStatus(getString(R.string.profile_load_warning), true));
    }

    private void saveProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(FeatureNavigation.createLoginIntent(this, FeatureNavigation.resolveDestination(getIntent())));
            finish();
            return;
        }

        String displayName = usernameInput.getText() == null ? "" : usernameInput.getText().toString().trim();
        String newPassword = passwordInput.getText() == null ? "" : passwordInput.getText().toString().trim();

        if (displayName.isEmpty()) {
            usernameInput.setError(getString(R.string.profile_username_required));
            return;
        }

        usernameInput.setError(null);

        if (!newPassword.isEmpty() && newPassword.length() < 6) {
            passwordInput.setError(getString(R.string.profile_password_too_short));
            return;
        }

        passwordInput.setError(null);
        saveButton.setEnabled(false);
        updateStatus(getString(R.string.profile_save_in_progress), false);

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        Task<Void> authTask = user.updateProfile(profileUpdates);

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("displayName", displayName);
        profileData.put("email", user.getEmail());
        profileData.put("updatedAt", FieldValue.serverTimestamp());
        if (avatarChanged && avatarBase64 != null && !avatarBase64.trim().isEmpty()) {
            profileData.put("avatarBase64", avatarBase64);
        }

        Task<Void> firestoreTask = db.collection("users")
                .document(user.getUid())
                .set(profileData, SetOptions.merge());

        Tasks.whenAllComplete(authTask, firestoreTask)
                .addOnCompleteListener(unused -> {
                syncAuthorProfileToPosts(user.getUid(), displayName, avatarBase64);
                    if (!newPassword.isEmpty()) {
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(passwordTask -> finishSave(authTask, firestoreTask, passwordTask));
                        return;
                    }

                    finishSave(authTask, firestoreTask, null);
                });
    }

    private void finishSave(
            @NonNull Task<Void> authTask,
            @NonNull Task<Void> firestoreTask,
            @Nullable Task<Void> passwordTask
    ) {
        saveButton.setEnabled(true);

        if (authTask.isSuccessful() && firestoreTask.isSuccessful() && (passwordTask == null || passwordTask.isSuccessful())) {
            passwordInput.setText("");
            avatarChanged = false;
            updateStatus(getString(R.string.profile_save_success), false);
            return;
        }

        if (authTask.isSuccessful() && !firestoreTask.isSuccessful() && (passwordTask == null || passwordTask.isSuccessful())) {
            passwordInput.setText("");
            updateStatus(getString(R.string.profile_save_partial_username), true);
            return;
        }

        if (passwordTask != null && passwordTask.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
            updateStatus(getString(R.string.profile_password_recent_login), true);
            return;
        }

        String errorMessage = firstErrorMessage(authTask, firestoreTask, passwordTask);
        updateStatus(errorMessage != null ? errorMessage : getString(R.string.profile_save_failure), true);
    }

    @Nullable
    private String firstErrorMessage(
            @NonNull Task<Void> authTask,
            @NonNull Task<Void> firestoreTask,
            @Nullable Task<Void> passwordTask
    ) {
        if (!authTask.isSuccessful() && authTask.getException() != null && authTask.getException().getMessage() != null) {
            return authTask.getException().getMessage();
        }

        if (!firestoreTask.isSuccessful() && firestoreTask.getException() != null && firestoreTask.getException().getMessage() != null) {
            return firestoreTask.getException().getMessage();
        }

        if (passwordTask != null && !passwordTask.isSuccessful()
                && passwordTask.getException() != null && passwordTask.getException().getMessage() != null) {
            return passwordTask.getException().getMessage();
        }

        return null;
    }

    @NonNull
    private String resolveFallbackUsername(@NonNull FirebaseUser user) {
        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }

        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }

        return String.format(Locale.ROOT, "voyageur_%s", user.getUid().substring(0, Math.min(6, user.getUid().length())));
    }

    private void updateStatus(@Nullable String message, boolean isError) {
        boolean hasMessage = message != null && !message.trim().isEmpty();
        statusView.setVisibility(hasMessage ? View.VISIBLE : View.GONE);
        statusView.setText(hasMessage ? message : "");
        statusView.setTextColor(isError ? 0xFFC62828 : 0xFF2E7D32);
    }

    private void syncAuthorProfileToPosts(@NonNull String userId, @NonNull String displayName, @Nullable String avatarValue) {
        db.collection("photos")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("authorDisplayName", displayName);
                        updates.put("authorAvatarBase64", avatarValue);
                        document.getReference().set(updates, SetOptions.merge());
                    }
                });
    }

    private void openAvatarPicker() {
        avatarPickerLauncher.launch("image/*");
    }

    private void handleSelectedAvatar(@Nullable Uri selectedImageUri) {
        if (selectedImageUri == null) {
            return;
        }

        String encodedAvatar = ProfileAvatarUtils.encodeAvatar(this, selectedImageUri);
        if (encodedAvatar == null) {
            Toast.makeText(this, R.string.profile_avatar_selection_error, Toast.LENGTH_SHORT).show();
            return;
        }

        avatarBase64 = encodedAvatar;
        avatarChanged = true;
        ProfileAvatarUtils.applyAvatar(avatarView, avatarBase64, PROFILE_AVATAR_PLACEHOLDER_PADDING_DP);
    }
}