package com.itproject.holotask;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class accountDetails extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private EditText editTextUsername, editTextUserDiscord, editTextNewPassword;
    private String currentUsername, currentDiscord;
    private Button buttonSaveChanges;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the current theme mode from shared preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = sharedPref.getBoolean("theme_mode", false);

        // Apply the theme based on the saved mode
        if (isDarkMode) {
            setTheme(R.style.AppTheme_Dark);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            setTheme(R.style.AppTheme);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

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
                        currentUsername = username; // Assign correct variable here
                        String userDiscord = document.getString("userDiscord");
                        currentDiscord = userDiscord; // Assign correct variable here

                        editTextUsername.setText(username);
                        editTextUserDiscord.setText(userDiscord);
                    } else {
                        // Handle case where document does not exist
                        Toast.makeText(accountDetails.this,
                                "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle failure to load user data
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

        if (newUsername.equals(currentUsername) && newDiscord.equals(currentDiscord) && !TextUtils.isEmpty(newPassword)) {
            updatePassword(newPassword);
        } else if (!newUsername.equals(currentUsername) && newDiscord.equals(currentDiscord) && TextUtils.isEmpty(newPassword)) {
            updateUserName(newUsername);
        } else if (newUsername.equals(currentUsername) && !newDiscord.equals(currentDiscord) && TextUtils.isEmpty(newPassword)) {
            updateUserDiscord(newDiscord);
        } else if (!newUsername.equals(currentUsername) && !newDiscord.equals(currentDiscord) && TextUtils.isEmpty(newPassword)) {
            updateUserName(newUsername);
            updateUserDiscord(newDiscord);
        } else if (!newUsername.equals(currentUsername) && newDiscord.equals(currentDiscord) && !TextUtils.isEmpty(newPassword)) {
            updateUserName(newUsername);
            updatePassword(newPassword);
        } else if (newUsername.equals(currentUsername) && !newDiscord.equals(currentDiscord) && !TextUtils.isEmpty(newPassword)) {
            updateUserDiscord(newDiscord);
            updatePassword(newPassword);
        } else if (!newUsername.equals(currentUsername) && !newDiscord.equals(currentDiscord) && !TextUtils.isEmpty(newPassword)) {
            updateUserName(newUsername);
            updateUserDiscord(newDiscord);
            updatePassword(newPassword);
        } else {
            Toast.makeText(accountDetails.this,
                    "There are no changes to update.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePassword(String newPassword) {
        // Update password logic using Firebase Auth
        currentUser.updatePassword(newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(accountDetails.this, "Password updated successfully", LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(accountDetails.this, "Failed to update password: " + task.getException().getMessage(), LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateUserName(final String newUsername) {
        // Check for duplicate usernames before updating
        Query usernameQuery = db.collection("Users").whereEqualTo("userName", newUsername);
        usernameQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().isEmpty()) {
                        // Username is unique, proceed with update
                        DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
                        userRef.update("userName", newUsername)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> updateTask) {
                                        if (updateTask.isSuccessful()) {
                                            // Update succeeded
                                            currentUsername = newUsername;
                                            editTextUsername.setText(newUsername);
                                            Toast.makeText(accountDetails.this,
                                                    "Username updated successfully",
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Update failed
                                            Toast.makeText(accountDetails.this,
                                                    "Failed to update username: " + updateTask.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        // Username already exists, prompt user to choose a different one
                        Toast.makeText(accountDetails.this,
                                "Username already taken. Please choose a different one.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Error occurred while checking for duplicate usernames
                    Toast.makeText(accountDetails.this,
                            "Failed to check for duplicate usernames: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUserDiscord(String newDiscord) {
        if (newDiscord != null) {
            // Check if newDiscord is not null and proceed with updating Firestore
            DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
            userRef.update("userDiscord", newDiscord)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> updateTask) {
                            if (updateTask.isSuccessful()) {
                                // Update successful
                                currentDiscord = newDiscord;
                                editTextUserDiscord.setText(newDiscord);
                                Toast.makeText(accountDetails.this,
                                        "Discord tag updated successfully",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // Failed to update Discord tag
                                Toast.makeText(accountDetails.this,
                                        "Failed to update Discord tag: " + updateTask.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            // Handle case where newDiscord is null
            DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
            userRef.update("userDiscord", FieldValue.delete()) // Remove userDiscord field
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> updateTask) {
                            if (updateTask.isSuccessful()) {
                                // Successfully removed userDiscord field
                                currentDiscord = null;
                                editTextUserDiscord.setText(""); // Clear EditText if needed
                                Toast.makeText(accountDetails.this,
                                        "Discord tag cleared successfully",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // Failed to update Discord tag (removal)
                                Toast.makeText(accountDetails.this,
                                        "Failed to clear Discord tag: " + updateTask.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}
