package com.itproject.holotask;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class navigationManager {

    public static void setupNavigationMenu(Activity activity, DrawerLayout drawerLayout, NavigationView navigationView, Toolbar toolbar) {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                activity, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                handleNavigationItemClick(activity, item, drawerLayout);
                return true;
            }
        });
    }

    public static void handleNavigationItemClick(Activity activity, MenuItem item, DrawerLayout drawerLayout) {

        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            navigateToActivity(activity, MainActivity.class);
        } else if (itemId == R.id.nav_app_settings) {
            navigateToActivity(activity, appSettings.class);
        } else if (itemId == R.id.nav_account_details) {
            navigateToActivity(activity, accountDetails.class);
        } else if (itemId == R.id.nav_logout) {
            logoutUser(activity);
        }

        drawerLayout.closeDrawers();
    }

    private static void navigateToActivity(Activity activity, Class<?> targetActivityClass) {
        Intent intent = new Intent(activity, targetActivityClass);
        activity.startActivity(intent);
    }

    private static void logoutUser(Activity activity) {
        FirebaseAuth.getInstance().signOut();
        activity.startActivity(new Intent(activity, login.class));
        activity.finish();
    }
}

