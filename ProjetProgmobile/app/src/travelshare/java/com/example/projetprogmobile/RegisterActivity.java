package com.example.projetprogmobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText email, password;
    private Button registerBtn, goToLoginBtn;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        registerBtn = findViewById(R.id.registerBtn);
        goToLoginBtn = findViewById(R.id.goToLoginBtn);

        registerBtn.setOnClickListener(v -> registerUser());

        goToLoginBtn.setOnClickListener(v -> {
            startActivity(FeatureNavigation.createLoginIntent(this, FeatureNavigation.resolveDestination(getIntent())));
            finish();
        });
    }

    private void registerUser() {
        String mail = email.getText().toString();
        String pass = password.getText().toString();

        if(mail.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Remplis tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if(pass.length() < 6) {
            Toast.makeText(this, "Mot de passe trop court (min 6)", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(mail, pass)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        Toast.makeText(this, "Compte créé !", Toast.LENGTH_SHORT).show();
                        startActivity(FeatureNavigation.createPostAuthIntent(this, FeatureNavigation.resolveDestination(getIntent())));
                        finish();
                    } else {
                        Toast.makeText(this, "Erreur: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}