package com.itproject.holotask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
public class appSettings extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private Button buttonLightMode;
    private Button buttonDarkMode;
    private Switch notificationSwitch;
    private SharedPreferences sharedPreferences;
    private static final String SHARED_PREFS_KEY = "notification_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);

        notificationSwitch = findViewById(R.id.switchNotif);
        sharedPreferences = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        buttonLightMode = findViewById(R.id.buttonLightMode);
        buttonDarkMode = findViewById(R.id.buttonDarkMode);


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);


        // Setup navigation menu using existing navigationManager class
        navigationManager.setupNavigationMenu(this, drawerLayout, navigationView, toolbar);

        notificationSwitch.setChecked(sharedPreferences.getBoolean(SHARED_PREFS_KEY, true));

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save switch state in SharedPreferences
            sharedPreferences.edit().putBoolean(SHARED_PREFS_KEY, isChecked).apply();

            String message;
            if (isChecked) {
                message = "Notifications enabled!";
            } else {
                message = "Notifications disabled.";
            }
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        });

// Set click listeners for buttons
        buttonLightMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set light mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

            }
        });
        buttonDarkMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set dark mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

            }
        });


    }
}

