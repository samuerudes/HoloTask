package com.itproject.holotask;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.app.NotificationManager.Policy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class TaskActivity extends AppCompatActivity {

    TextView TaskName;
    TextView deadlineTextView;
    TextView descTextView;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the current theme mode from shared preferences
        SharedPreferences sharedPref1 = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = sharedPref1.getBoolean("theme_mode", false);

        // Apply the theme based on the saved mode
        if (isDarkMode) {
            setTheme(R.style.AppTheme_Dark);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            setTheme(R.style.AppTheme);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_task);  // Replace with your task layout


        // Get references to TextViews in the layout
        TaskName = findViewById(R.id.TaskName);
        deadlineTextView = findViewById(R.id.dueByDate);
        descTextView = findViewById(R.id.descTask);
        TextView timeRemainTextView = findViewById(R.id.timeRemain);
        Button backButton = findViewById(R.id.backButton);
        Button editButton = findViewById(R.id.editTaskButton);
        Button deleteButton = findViewById(R.id.deleteTaskButton);
        Button completeTaskButton = findViewById(R.id.completeTaskButton);
        seekBar = findViewById(R.id.seekBar);

        SharedPreferences sharedPref = getSharedPreferences("taskProgress", MODE_PRIVATE);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TaskActivity.this, MainActivity.class);  // Replace "this" with your current activity class name
                startActivity(intent);
            }
        });
        // Retrieve task data passed from MainActivity
        Intent intent = getIntent();
        String taskName = intent.getStringExtra("taskName");
        String deadline = intent.getStringExtra("deadline");
        String description = intent.getStringExtra("description");
        String taskID = intent.getStringExtra("taskID");
        String status = intent.getStringExtra("status");

        int seekbarProgress = sharedPref.getInt(taskID, 0);
        seekBar.setProgress(seekbarProgress);

        // Set TextViews with retrieved data
        TaskName.setText(taskName);
        deadlineTextView.setText(deadline);
        descTextView.setText(description);

        String deadlineString = intent.getStringExtra("deadline");

        completeTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call a method to update the task status to "Complete"
                updateTaskStatusToComplete(taskID);
            }
        });

// Format deadline string to Date object (assuming format matches parsing)
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");  // Adjust format if needed
        Date deadlinecalc;
        try {
            deadlinecalc = dateFormat.parse(deadlineString);
        } catch (ParseException e) {
            e.printStackTrace();
            // Handle parsing error (e.g., display default message)
            timeRemainTextView.setText("Error: Invalid Deadline Format");  // Example handling
            return;
        }

// Get current time in milliseconds
        Calendar nowCalendar = Calendar.getInstance();
        long now = nowCalendar.getTimeInMillis();

// Adjust deadline to 12:00 AM of the following day
        Calendar deadlineCalendar = Calendar.getInstance();
        deadlineCalendar.setTime(deadlinecalc);
        deadlineCalendar.set(Calendar.HOUR_OF_DAY, 0);
        deadlineCalendar.set(Calendar.MINUTE, 0);
        deadlineCalendar.set(Calendar.SECOND, 0);
        deadlineCalendar.add(Calendar.DAY_OF_MONTH, 1); // Add one day to get to the end of the following day

        long adjustedDeadline = deadlineCalendar.getTimeInMillis();

        if (status != null && status.equals("Completed")) {
            timeRemainTextView.setText("Completed");
        } else {
            // Calculate remaining or overdue time as before
            if (now > adjustedDeadline) {
                // Calculate overdue time
                long overdue = now - adjustedDeadline;
                int overdueDays = (int) (overdue / (1000 * 60 * 60 * 24));
                int overdueHours = (int) ((overdue % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                int overdueMinutes = (int) ((overdue % (1000 * 60 * 60)) / (1000 * 60));

                // Update timeRemain TextView with overdue message
                String overdueMessage = "Overdue by: " + overdueDays + " days, " + overdueHours + " hours, " + overdueMinutes + " minutes";
                timeRemainTextView.setText(overdueMessage);
            } else {
                // Calculate time remaining if not overdue
                long remaining = adjustedDeadline - now;
                int days = (int) (remaining / (1000 * 60 * 60 * 24));
                int hours = (int) ((remaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                int minutes = (int) ((remaining % (1000 * 60 * 60)) / (1000 * 60));

                // Update timeRemain TextView with remaining time
                String timeRemainingString = "Days: " + days + ", Hours: " + hours + ", Minutes: " + minutes;
                timeRemainTextView.setText(timeRemainingString);
            }
        }

        // Edit Button Click Listener
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTaskDialog(taskID); // Pass taskID and other details to dialog method
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskDeletionHandler.deleteTask(TaskActivity.this, taskID, null); // Pass context and taskID to delete method
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update progress in SharedPreferences
                SharedPreferences sharedPref = getSharedPreferences("taskProgress", MODE_PRIVATE);
                sharedPref.edit().putInt(taskID, progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void showEditTaskDialog(String taskID) {
        // Retrieve task data from Intent
        Intent intent = getIntent();
        String taskName = intent.getStringExtra("taskName");
        String deadline = intent.getStringExtra("deadline");
        String description = intent.getStringExtra("description");


        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
        builder.setTitle("Edit Task");

        // Inflate a layout for the dialog
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.edit_task_dialog, null);

        // Get references to input fields in the layout
        EditText taskNameInput = layout.findViewById(R.id.editTaskName);
        EditText deadlineInput = layout.findViewById(R.id.editDeadline);
        EditText descriptionInput = layout.findViewById(R.id.editDescription);

        // Set pre-filled values with current task data
        taskNameInput.setText(taskName);
        deadlineInput.setText(deadline);
        descriptionInput.setText(description);
        // Set deadline input to not be focusable initially
        deadlineInput.setFocusable(false);

        // Add date picker functionality on deadline click
        deadlineInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar myCalendar = Calendar.getInstance();
                int year = myCalendar.get(Calendar.YEAR);
                int month = myCalendar.get(Calendar.MONTH);
                int day = myCalendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(TaskActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                                myCalendar.set(Calendar.YEAR, selectedYear);
                                myCalendar.set(Calendar.MONTH, selectedMonth);
                                myCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                                // Set the time to 12:00 AM of the following day
                                myCalendar.set(Calendar.HOUR_OF_DAY, 0);
                                myCalendar.set(Calendar.MINUTE, 0);
                                myCalendar.set(Calendar.SECOND, 0);
                                String formattedDate = formatDate(myCalendar.getTime());  // Call method to format date
                                deadlineInput.setText(formattedDate);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        builder.setView(layout);

        // Add buttons to save or cancel changes
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get updated task details
                String editedTaskName = taskNameInput.getText().toString();
                String editedDeadline = deadlineInput.getText().toString();
                String editedDescription = descriptionInput.getText().toString();

                // Update task in Firestore
                updateTaskInFirestore(taskID, editedTaskName, editedDeadline, editedDescription);  // Call update method

                // Update UI with edited task details (optional)
                TaskName.setText(editedTaskName);
                deadlineTextView.setText(editedDeadline);
                descTextView.setText(editedDescription);

                Toast.makeText(TaskActivity.this, "Task edited successfully!", Toast.LENGTH_SHORT).show();
                redirectToMainActivity();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Show the edit task dialog
        builder.show();
    }

    // Method to format date (assuming format desired is "dd/MM/yyyy")
    private String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        return dateFormat.format(date);
    }

    private void updateTaskStatusToComplete(String taskID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("UserTasks")
                .whereEqualTo("taskID", taskID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        // Update the task status to "Completed"
                        db.collection("UserTasks")
                                .document(documentSnapshot.getId())
                                .update("taskStatus", "Completed")
                                .addOnSuccessListener(aVoid -> {
                                    // Show a toast indicating success
                                    Toast.makeText(TaskActivity.this, "Task completed successfully!", Toast.LENGTH_SHORT).show();

                                    // Redirect to MainActivity upon successful completion
                                    redirectToMainActivity();
                                })
                                .addOnFailureListener(e -> {
                                    // Show a toast indicating failure
                                    Toast.makeText(TaskActivity.this, "Failed to complete task!", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Show a toast indicating failure to retrieve the document
                    Toast.makeText(TaskActivity.this, "Failed to retrieve task!", Toast.LENGTH_SHORT).show();
                });
    }

    // Update task in Firestore (assuming Firebase is already set up)
    private void updateTaskInFirestore(String taskID, String editedTaskName, String editedDeadline, String editedDescription) {
        // Get an instance of Firebase Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (taskID == null) {
            Log.e("Firestore", "Task ID is null!");
            return; // Handle null case (e.g., show error message)
        }

        // Perform a query to find the document(s) with the matching taskID
        db.collection("UserTasks")
                .whereEqualTo("taskID", taskID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        // Update each matching document with the new data
                        documentSnapshot.getReference().update(
                                "taskName", editedTaskName,
                                "endDateTime", editedDeadline,
                                "taskDescription", editedDescription
                        ).addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Document successfully updated!");
                            Toast.makeText(TaskActivity.this, "Task edited successfully!", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            Log.w("Firestore", "Error updating document", e);
                            Toast.makeText(TaskActivity.this, "Failed to update task!", Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error querying documents", e);
                    Toast.makeText(TaskActivity.this, "Failed to update task!", Toast.LENGTH_SHORT).show();
                });
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(TaskActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish current activity to prevent going back to TaskActivity
    }

}

