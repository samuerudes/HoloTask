package com.itproject.holotask;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class generalMethods {

    // Method to create an Intent for MainActivity and handle activity navigation
    public static Intent redirectToMainActivity(Context context) {
        // Create an Intent to start the MainActivity
        Intent intent = new Intent(context, MainActivity.class);

        // Add flags to clear the previous activities from the stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    // Method to start MainActivity and finish the current activity
    public static void startMainActivity(Context context) {
        Intent intent = redirectToMainActivity(context);

        // Start the MainActivity
        context.startActivity(intent);

        // Finish the current activity to prevent the user from going back
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).finish();
        }
    }
}