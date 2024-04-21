package com.itproject.holotask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TaskDeletionHandler.OnTaskDeletedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private GridView gridView;
    private CustomAdapter adapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String[]> data = new ArrayList<>();  // Declare and initialize data list

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id));
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, 100);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getString(R.string.default_notification_channel_description));
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Setup navigation menu using existing navigationManager class
        navigationManager.setupNavigationMenu(this, drawerLayout, navigationView, toolbar);

        // Initialize GridView and Adapter
        gridView = findViewById(R.id.gridView);

        // Fetch tasks from Firestore and populate the GridView
        retrieveTasksFromFirestore();

        // Set click listener for "Create Task" button
        Button createTaskButton = findViewById(R.id.button8);
        createTaskButton.setOnClickListener(v -> showCreateTaskDialog());

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            String[] taskData = data.get(position);
            launchTaskActivity(taskData);
        });
    }

    // Retrieve tasks from Firestore and populate the GridView
    private void retrieveTasksFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String userId = user.getUid();
        db.collection("UserTasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String[]> updatedData = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String taskID = document.getString("taskID");
                            String taskName = document.getString("taskName");
                            String status = document.getString("taskStatus");
                            String deadline = document.getString("endDateTime");
                            String description = document.getString("taskDescription");

                            // Determine task status based on current date and deadline
                            String updatedStatus = calculateTaskStatus(status, deadline);

                            updatedData.add(new String[]{taskID, taskName, updatedStatus, deadline, description});
                        }
                        // Sort tasks based on status and deadline
                        Collections.sort(updatedData, new Comparator<String[]>() {
                            @Override
                            public int compare(String[] task1, String[] task2) {
                                String status1 = task1[2];
                                String status2 = task2[2];
                                String deadline1 = task1[3];
                                String deadline2 = task2[3];

                                // Compare by status first (Overdue > Ongoing > Completed)
                                if (status1.equals("Overdue")) {
                                    if (!status2.equals("Overdue")) {
                                        return -1; // task1 is "Overdue", should come before task2
                                    }
                                } else if (status1.equals("Ongoing")) {
                                    if (status2.equals("Completed")) {
                                        return -1; // Ongoing tasks before Completed tasks
                                    } else if (status2.equals("Overdue")) {
                                        return 1; // Ongoing tasks after Overdue tasks
                                    }
                                } else if (status1.equals("Completed")) {
                                    if (!status2.equals("Completed")) {
                                        return 1; // task1 is "Completed", should come after task2
                                    }
                                }

                                // If statuses are the same, compare by deadline
                                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                                try {
                                    Date date1 = dateFormat.parse(deadline1);
                                    Date date2 = dateFormat.parse(deadline2);
                                    return date1.compareTo(date2); // Compare by date
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                return 0;
                            }
                        });

                        // Update data and set adapter for the GridView
                        data = updatedData;
                        adapter = new CustomAdapter(MainActivity.this, data);
                        gridView.setAdapter(adapter);

                    } else {
                        Log.w("Firestore", "Error fetching documents.", task.getException());
                    }
                });
    }
    // Calculate task status based on deadline and current date
    private String calculateTaskStatus(String currentStatus, String deadline) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date currentDate = new Date();
            Date deadlineDate = dateFormat.parse(deadline);

            // Create a Calendar instance for the deadline date
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(deadlineDate);

            // Add one day to the deadline date
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date deadlinePlusOneDay = calendar.getTime();

            if (currentStatus.equals("Completed")) {
                return "Completed"; // Keep status as completed if already completed
            } else if (currentDate.after(deadlinePlusOneDay)) {
                return "Overdue"; // Set status to "Overdue" if deadline has passed
            } else {
                return "Ongoing"; // Default status for ongoing tasks
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return currentStatus; // Return current status in case of error
        }
    }




    // Show dialog to create a new task
    private void showCreateTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Create New Task");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_task, null);
        builder.setView(dialogView);

        EditText taskNameInput = dialogView.findViewById(R.id.taskNameInput);
        EditText deadlineInput = dialogView.findViewById(R.id.deadlineInput);
        EditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        CheckBox addToCalendarCheckbox = dialogView.findViewById(R.id.addToCalendarCheckbox);

        deadlineInput.setOnClickListener(v -> showDatePicker(deadlineInput));

        builder.setPositiveButton("Create", (dialog, which) -> {
            String taskName = taskNameInput.getText().toString();
            String deadline = deadlineInput.getText().toString();
            String description = descriptionInput.getText().toString();

            // Determine initial status based on the selected deadline
            String initialStatus = calculateTaskStatus("Ongoing", deadline);

            // Create new task in Firestore
            createNewTask(taskName, initialStatus, deadline, description, addToCalendarCheckbox.isChecked());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // Show DatePickerDialog to pick a deadline
    private void showDatePicker(final EditText deadlineInput) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, monthOfYear, dayOfMonth);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    deadlineInput.setText(dateFormat.format(selectedDate.getTime()));
                }, year, month, day);

        datePickerDialog.show();
    }

    // Create a new task in Firestore
    private void createNewTask(String taskName, String status, String deadline, String description, boolean addToCalendar) {
        String taskID = UUID.randomUUID().toString();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        String userId = user.getUid();
        HashMap<String, Object> newTask = new HashMap<>();
        newTask.put("taskID", taskID);
        newTask.put("taskName", taskName);
        newTask.put("taskStatus", status);
        newTask.put("endDateTime", deadline);
        newTask.put("taskDescription", description);
        newTask.put("userId", userId);

        db.collection("UserTasks")
                .add(newTask)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, "Task created successfully!", Toast.LENGTH_SHORT).show();
                    retrieveTasksFromFirestore(); // Refresh tasks after adding a new task

                    if (addToCalendar) {
                        addToCalendar(taskName, deadline);

                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error adding document", e);
                    Toast.makeText(MainActivity.this, "Failed to create task!", Toast.LENGTH_SHORT).show();
                });

        String notificationTitle = "New Task Created!";
        String notificationBody = "You've created a new task: " + taskName;

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                .setSmallIcon(R.drawable.holotask)
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationBody))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, notificationBuilder.build());
        } else {
            // Handle the case where permission is not granted
            Toast.makeText(this, "Notification permission is required to show task creation notifications. Please enable it in app settings.", Toast.LENGTH_LONG).show();
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, notificationBuilder.build()); // Notification ID: 1
    }
    // Add task to Google Calendar
    private void addToCalendar(String taskName, String deadline) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date deadlineDate = dateFormat.parse(deadline);

            Calendar calendarEvent = Calendar.getInstance();
            calendarEvent.setTime(deadlineDate);

            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, taskName)
                    .putExtra(CalendarContract.Events.ALL_DAY, true)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendarEvent.getTimeInMillis());

            startActivity(intent);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    private void launchTaskActivity(String[] taskData) {
        Intent intent = new Intent(MainActivity.this, TaskActivity.class);
        intent.putExtra("taskID", taskData[0]);
        intent.putExtra("taskName", taskData[1]);
        intent.putExtra("status", taskData[2]);
        intent.putExtra("deadline", taskData[3]);
        intent.putExtra("description", taskData[4]);
        startActivity(intent);
    }
    private void logoutUser() {
        // Implement your logout logic here
        // For example:
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(MainActivity.this, login.class));
        finish(); // Close MainActivity after logout
    }
    @Override
    public void onTaskDeleted(String deletedTaskID) {
        // Remove the deleted task from the data list
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i)[0].equals(deletedTaskID)) {
                data.remove(i);
                break;
            }
        }
        // Notify the adapter about the data change
        adapter.notifyDataSetChanged();
    }

}

