package com.itproject.holotask;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class accountDetails extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private EditText editTextUsername, editTextUserDiscord, editTextNewPassword;
    private Button buttonSaveChanges;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Setup navigation menu using existing navigationManager class
        navigationManager.setupNavigationMenu(this, drawerLayout, navigationView, toolbar);

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextUserDiscord = findViewById(R.id.editTextUserDiscord);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);
        Button buttonBack = findViewById(R.id.buttonBack);

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // If user is not authenticated, redirect to login screen
            startActivity(new Intent(this, login.class));
            finish();
            return;
        }

        // Load user data from Firestore
        loadUserData();

        // Handle save changes button click
        buttonSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to MainActivity
                startActivity(new Intent(accountDetails.this, MainActivity.class));
                finish(); // Close the current activity
            }
        });
    }

    private void loadUserData() {
        DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String username = document.getString("userName");
                        String userDiscord = document.getString("userDiscord");

                        editTextUsername.setText(username);
                        editTextUserDiscord.setText(userDiscord);
                    }
                } else {
                    Toast.makeText(accountDetails.this,
                            "Failed to load user data: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveChanges() {
        final String newUsername = editTextUsername.getText().toString().trim();
        final String newDiscord = editTextUserDiscord.getText().toString().trim();
        final String newPassword = editTextNewPassword.getText().toString();

        if (!TextUtils.isEmpty(newDiscord)) {
            // Check Discord ID availability only if a new Discord ID is provided
            checkDiscordAvailability(newUsername, newDiscord, newPassword);
        } else {
            // Update profile with new username and/or password only
            updateUserProfile(newUsername, newDiscord, newPassword);
        }

        if (!TextUtils.isEmpty(newUsername)) {
            // Check username availability only if new username is provided
            checkUsernameAvailability(newUsername, newDiscord, newPassword);
        } else {
            // Update profile with new Discord and/or password only
            updateUserProfile(newUsername, newDiscord, newPassword);
        }
    }

    private void checkDiscordAvailability(final String newUsername, final String newDiscord, final String newPassword) {
        db.collection("Users")
                .whereEqualTo("userDiscord", newDiscord)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                updateUserProfile(newUsername, newDiscord, newPassword);
                            } else {
                                Toast.makeText(accountDetails.this,
                                        "Discord ID already exists. Please choose another one.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(accountDetails.this,
                                    "Error checking Discord ID availability: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkUsernameAvailability(final String newUsername, final String newDiscord, final String newPassword) {
        db.collection("Users")
                .whereEqualTo("userName", newUsername)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()) {
                                updateUserProfile(newUsername, newDiscord, newPassword);
                            } else {
                                Toast.makeText(accountDetails.this,
                                        "Username already exists. Please choose another one.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(accountDetails.this,
                                    "Error checking username availability: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateUserProfile(String newUsername, String newDiscord, String newPassword) {
        // Update user data in Firestore
        DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
        userRef.update("userName", newUsername, "userDiscord", newDiscord)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(accountDetails.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                            // Update password if newPassword is not empty
                            if (!TextUtils.isEmpty(newPassword)) {
                                currentUser.updatePassword(newPassword)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(accountDetails.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(accountDetails.this, "Failed to update password: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(accountDetails.this,
                                    "Failed to update profile: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
