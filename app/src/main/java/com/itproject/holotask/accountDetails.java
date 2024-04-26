package com.itproject.holotask;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

public class accountDetails extends AppCompatActivity {

    private EditText editTextUsername, editTextUserDiscord, editTextNewPassword;
    private String currentUsername, currentDiscord;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Setup navigation menu using existing navigationManager class
        navigationManager.setupNavigationMenu(this, drawerLayout, navigationView, toolbar);

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextUserDiscord = findViewById(R.id.editTextUserDiscord);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        Button buttonSaveChanges = findViewById(R.id.buttonSaveChanges);
        Button buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);
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

        buttonDeleteAccount.setBackgroundColor(getResources().getColor(R.color.red));
        buttonDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show confirmation dialog before deleting account
                showDeleteAccountDialog();
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
                            "Failed to load user data: " + Objects.requireNonNull(task.getException()).getMessage(),
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
        if (!Objects.equals(newDiscord, "")) {
            // Check for duplicate Discord before updating
            Query discordQuery = db.collection("Users").whereEqualTo("userDiscord", newDiscord);
            discordQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // Discord is unique, proceed with update
                            DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
                            userRef.update("userDiscord", newDiscord)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> updateTask) {
                                            if (updateTask.isSuccessful()) {
                                                // Update succeeded
                                                currentDiscord = newDiscord;
                                                editTextUserDiscord.setText(newDiscord);
                                                Toast.makeText(accountDetails.this,
                                                        "Discord ID updated successfully",
                                                        Toast.LENGTH_SHORT).show();
                                            } else {
                                                // Update failed
                                                Toast.makeText(accountDetails.this,
                                                        "Failed to update Discord ID: " + updateTask.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            // Discord already exists, prompt user to choose a different one
                            Toast.makeText(accountDetails.this,
                                    "Discord ID already in use. Please choose a different one.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Error occurred while checking for duplicate usernames
                        Toast.makeText(accountDetails.this,
                                "Failed to check for duplicate Discord ID: " + task.getException().getMessage(),
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

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("DELETE ACCOUNT");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.");

        // Set up the buttons
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked Yes, delete the account
                deleteAccount();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked No, dismiss the dialog
                dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteAccount() {
        // Get the current user's UID
        String userId = currentUser.getUid();

        // Delete user data from Firestore (if applicable)
        deleteUser(userId);
        deleteUserTasks(userId);
        currentUser.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Account deletion successful, navigate to login screen
                            Toast.makeText(accountDetails.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(accountDetails.this, login.class));
                            finish(); // Close the current activity
                        } else {
                            // Account deletion failed
                            Toast.makeText(accountDetails.this,
                                    "Failed to delete account: " + Objects.requireNonNull(task.getException()).getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void deleteUser(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .document(userId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // User data deleted from Firestore
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle Firestore deletion error
                        Toast.makeText(accountDetails.this,
                                "Failed to delete user data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteUserTasks(String userId) {
        // Create a query to find all tasks where "userId" field matches the current user
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection("UserTasks")
                .whereEqualTo("userId", userId);

        // Delete all matching tasks in a batch
        db.collection("UserTasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                document.getReference().delete();
                            }
                            // All tasks deleted successfully
                        } else {
                            // Handle error retrieving tasks
                            Toast.makeText(accountDetails.this,
                                    "Failed to delete user tasks: " + Objects.requireNonNull(task.getException()).getMessage(),
                                    LENGTH_SHORT).show();
                        }
                    }
                });
    }

}
