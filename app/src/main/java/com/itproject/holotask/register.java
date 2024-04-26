package com.itproject.holotask;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class register extends AppCompatActivity {
    Button googleAuth;
    FirebaseAuth auth;
    GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 20;
    EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    Button buttonRegister;

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

        setContentView(R.layout.activity_register);

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance();

        // Find the TextView in your layout
        TextView textView = findViewById(R.id.loginToHoloTask);

        // Create a SpannableString for login link
        SpannableString spannableString = new SpannableString("Already have an account? Login now");

        // Create a ClickableSpan for login link
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Handle click event to navigate to login activity
                Intent intent = new Intent(register.this, login.class);
                startActivity(intent);
            }
        };

        // Set the ClickableSpan for login link in the SpannableString
        spannableString.setSpan(clickableSpan, spannableString.length() - "Login now".length(), spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the SpannableString to the TextView
        textView.setText(spannableString);

        // Make the TextView clickable and handle links
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        // Initialize EditText fields and Register button
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextConfirmPassword = findViewById(R.id.confirmpassword);
        buttonRegister = findViewById(R.id.registerbutton);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                String confirmPassword = editTextConfirmPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    showToast("Enter email");
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    showToast("Enter password");
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    showToast("Passwords do not match");
                    return;
                }

                // Create user with email and password
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(register.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    showToast("Account created successfully");
                                    // Proceed with Firestore document creation
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user != null) {
                                        String userGmail = user.getEmail();
                                        String userName = userGmail.split("@")[0];

                                        // Initialize Firestore instance
                                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                                        // Data to be stored in Firestore
                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("userDiscord", "");
                                        userData.put("userGmail", userGmail);
                                        userData.put("userName", userName);

                                        // Add document to Users collection
                                        db.collection("Users")
                                                .document(user.getUid())
                                                .set(userData)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        // Document written successfully
                                                        redirectToMainActivity();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Handle Firestore document write failure
                                                        showToast("Firestore Error: " + e.getMessage());
                                                    }
                                                });
                                    }
                                } else {
                                    // Handle account creation failure
                                    showToast("Account creation failed: " + Objects.requireNonNull(task.getException()).getMessage());
                                }
                            }
                        });
            }
        });

        // Initialize Google Sign-In options and client
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set click listener for Google sign-in button
        googleAuth = findViewById(R.id.btnContinueWithGoogle);
        googleAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });

        // Set click listener for back button (Discord sign-in)
        Button backButton = findViewById(R.id.btnContinueWithDiscord);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(register.this, continueWithDiscord.class);
                startActivity(intent);
            }
        });
    }

    private void googleSignIn() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Start sign-in process after signing out
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    // Sign in with Firebase using Google credentials
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    showToast("Google Sign-In Failed");
                }
            } catch (ApiException e) {
                showToast("Google Sign-In Failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (auth == null) {
            showToast("FirebaseAuth instance is null");
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(register.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Firebase authentication successful
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                // Check if user document already exists in Firestore
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection("Users")
                                        .document(user.getUid())
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if (document != null && document.exists()) {
                                                        // User document exists, redirect to main activity
                                                        showToast("Login with Google successful");
                                                        redirectToMainActivity();
                                                    } else {
                                                        // User document does not exist, proceed with Firestore data creation
                                                        String userGmail = user.getEmail();
                                                        String userName = userGmail.split("@")[0];

                                                        // Data to be stored in Firestore
                                                        Map<String, Object> userData = new HashMap<>();
                                                        userData.put("userDiscord", ""); // Set any initial value
                                                        userData.put("userGmail", userGmail);
                                                        userData.put("userName", userName);

                                                        // Add document to Users collection in Firestore
                                                        db.collection("Users")
                                                                .document(user.getUid())
                                                                .set(userData)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        showToast("Account creation using Google successful");
                                                                        redirectToMainActivity();
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        showToast("Firestore Error: " + e.getMessage());
                                                                    }
                                                                });
                                                    }
                                                } else {
                                                    showToast("Firestore Error: " + Objects.requireNonNull(task.getException()).getMessage());
                                                }
                                            }
                                        });
                            }
                        } else {
                            // Firebase authentication failed
                            showToast("Google Sign-In Failed");
                        }
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(register.this, message, Toast.LENGTH_SHORT).show();
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(register.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
