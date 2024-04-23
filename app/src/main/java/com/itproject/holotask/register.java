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
import android.widget.ProgressBar;
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {
    Button googleAuth;
    FirebaseAuth auth;
    FirebaseDatabase database;
    GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 20;
    EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    Button buttonRegister;
    FirebaseAuth mAuth;
    ProgressBar progressBar;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            redirectToMainActivity();
        }
    }

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

        // Find the TextView in your layout
        TextView textView = findViewById(R.id.loginToHoloTask);

        // Create a SpannableString
        SpannableString spannableString = new SpannableString("Already have an account? Login now");

        // Create a ClickableSpan for "Join now"
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Handle the click event, for example, open a new activity or a web link
                Intent intent = new Intent(register.this, login.class);
                startActivity(intent);
            }
        };

        // Set the ClickableSpan for "Join now" in the SpannableString
        spannableString.setSpan(clickableSpan, spannableString.length() - "Login now".length(), spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the SpannableString to the TextView
        textView.setText(spannableString);

        // Make the TextView clickable and handle links
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextConfirmPassword = findViewById(R.id.confirmpassword);
        buttonRegister = findViewById(R.id.registerbutton);
        progressBar = findViewById(R.id.progressbar);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email, password, confirmPassword;
                email = editTextEmail.getText().toString();
                password = editTextPassword.getText().toString();
                confirmPassword = editTextConfirmPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    showToast("Enter username");
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    showToast("Enter password");
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    showToast("Passwords do not match");
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    showToast("Account created successfully.");

                                    // Firebase user after successful creation
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    // Proceed with Firestore document creation
                                    if (user != null) {
                                        String userGmail = user.getEmail();
                                        String userName = userGmail.split("@")[0];

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
                                    } else {
                                        showToast("User not found after account creation.");
                                    }
                                } else {
                                    // Handle account creation failure
                                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                        showToast("An account with this email already exists.");
                                    } else {
                                        showToast("Account creation failed: " + task.getException().getMessage());
                                    }
                                }
                            }
                        });
            }
        });

        googleAuth = findViewById(R.id.btnContinueWithGoogle);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();

        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);

        googleAuth.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                googleSignIn();
            }
        });

        Button backButton = findViewById(R.id.btnContinueWithDiscord);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(register.this, continueWithDiscord.class);
                startActivity(intent);
            }
        });
    }

    private void googleSignIn(){
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Start sign-in process after signing out
                Intent intent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_SIGN_IN);
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
                    firebaseAuth(account.getIdToken());
                } else {
                    Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                // Check if the user document already exists in Firestore
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
                                                        // Document exists, do not overwrite
                                                        Toast.makeText(register.this, "Login with Google successful.", Toast.LENGTH_SHORT).show();
                                                        redirectToMainActivity();
                                                    } else {
                                                        // Document doesn't exist, proceed with writing new data
                                                        String userGmail = user.getEmail();
                                                        String userName = userGmail.split("@")[0];

                                                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                                                        // Create a new document in "Users" collection with UID as document ID
                                                        Map<String, Object> userData = new HashMap<>();
                                                        userData.put("userDiscord", ""); // You can set any initial value
                                                        userData.put("userGmail", userGmail);
                                                        userData.put("userName", userName);

                                                        // Add the document to Firestore
                                                        db.collection("Users")
                                                                .document(user.getUid())
                                                                .set(userData)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        // Document has been successfully written
                                                                        Toast.makeText(register.this, "Account creation using Google successful.", Toast.LENGTH_SHORT).show();
                                                                        redirectToMainActivity();
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        // Handle any errors
                                                                        Toast.makeText(register.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    }
                                                } else {
                                                    // Handle Firestore read failure
                                                    showToast("Firestore Error: " + task.getException().getMessage());
                                                }
                                            }
                                        });
                            }
                        } else {
                            // Handle Google Sign-In failure
                            Toast.makeText(register.this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(register.this, message, Toast.LENGTH_SHORT).show();
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(register.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
