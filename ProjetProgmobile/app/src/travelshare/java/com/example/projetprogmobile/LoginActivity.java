package com.example.projetprogmobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText email, password;
    private Button loginBtn;
    private Button goToRegisterBtn;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            startActivity(FeatureNavigation.createPostAuthIntent(this, FeatureNavigation.resolveDestination(getIntent())));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        goToRegisterBtn = findViewById(R.id.goToRegisterBtn);

        loginBtn.setOnClickListener(v -> loginUser());
        goToRegisterBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra(FeatureNavigation.EXTRA_DESTINATION, FeatureNavigation.resolveDestination(getIntent()));
            startActivity(intent);
        });
    }

    private void loginUser() {
        String mail = email.getText().toString();
        String pass = password.getText().toString();

        auth.signInWithEmailAndPassword(mail, pass)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        startActivity(FeatureNavigation.createPostAuthIntent(this, FeatureNavigation.resolveDestination(getIntent())));
                        finish();
                    } else {
                        Toast.makeText(this, "Erreur login", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}