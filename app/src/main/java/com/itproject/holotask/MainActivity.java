package com.itproject.holotask;

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
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
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

    private ActionBarDrawerToggle drawerToggle;
    private GridView gridView;
    private CustomAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String[]> data = new ArrayList<>();  // Declare and initialize data list
    private static final String SHARED_PREFS_KEY = "notification_enabled";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the current theme mode from shared preferences
        // Initialize sharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the current theme mode from shared preferences
        boolean isDarkMode = sharedPreferences.getBoolean("theme_mode", false);


        boolean notificationToastShown = sharedPreferences.getBoolean("notification_toast_shown", false);
        if (!notificationToastShown && !sharedPreferences.getBoolean(SHARED_PREFS_KEY, true)) {
            // Show the toast only if notifications are disabled and the toast hasn't been shown before
            Toast.makeText(this, "Notifications are currently disabled in app settings", Toast.LENGTH_SHORT).show();
            // Update the flag to indicate that the toast has been shown
            sharedPreferences.edit().putBoolean("notification_toast_shown", true).apply();
        }

        // Apply the theme based on the saved mode
        if (isDarkMode) {
            setTheme(R.style.AppTheme_Dark);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            setTheme(R.style.AppTheme);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_main);
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.grid_item, null);
        LinearLayout taskLayout = itemView.findViewById(R.id.taskLayout);

        // Set the background color based on the theme mode
        if (isDarkMode) {
            taskLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.darkBackgroundColor));
        } else {
            taskLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.lightBackgroundColor));
        }

        LayoutInflater inflater2 = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View appSettingsView = inflater2.inflate(R.layout.activity_app_settings, null);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        sharedPreferences = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        Switch notificationSwitch = appSettingsView.findViewById(R.id.switchNotif);
        notificationSwitch.setChecked(sharedPreferences.getBoolean(SHARED_PREFS_KEY, true)); // Set switch state based on saved preference
        gridView = findViewById(R.id.gridView);

        toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_ios_24);

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(SHARED_PREFS_KEY, isChecked).apply();
        });

        if (checkSelfPermission(Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, 100);
        }

        String channelId = getString(R.string.default_notification_channel_id);
        String channelName = getString(R.string.default_notification_channel_name);
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(getString(R.string.default_notification_channel_description));
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        // Set up navigation menu using navigationManager
        navigationManager.setupNavigationMenu(MainActivity.this, drawerLayout, navigationView, toolbar);

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
        boolean notificationsEnabled = sharedPreferences.getBoolean(SHARED_PREFS_KEY, true); // Retrieve notification state
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
                            String notificationKey = SHARED_PREFS_KEY + "_" + taskID; // Combine key with task ID

                            updatedData.add(new String[]{taskID, taskName, updatedStatus, deadline, description});
                            // Check for overdue tasks and send notifications
                            if (!hasNotificationShown(notificationKey)) {
                                if (updatedStatus.equals("Overdue") && notificationsEnabled) {
                                    // Send overdue notification
                                    sendOverdueNotification(taskName);
                                    setNotificationShown(notificationKey, true); // Update flag after sending notification
                                }
                            }
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
                                    assert date1 != null;
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
                        Log.w("Firestore", "Error fetching documents", task.getException());
                    }
                });
    }
    private boolean hasNotificationShown(String notificationKey) {
        return sharedPreferences.getBoolean(notificationKey, false);
    }

    private void setNotificationShown(String notificationKey, boolean shown) {
        sharedPreferences.edit().putBoolean(notificationKey, shown).apply();
    }
    private void sendOverdueNotification(String taskName) {

        if (sharedPreferences.getBoolean(SHARED_PREFS_KEY, true)) {

            String notificationTitle = "Overdue Task!";
            String notificationBody = taskName + " is overdue!";

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                    .setSmallIcon(R.drawable.ic_android_black) // Notif icon
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationBody)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationBody))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.notify(taskName.hashCode(), notificationBuilder.build()); // Task name hash for unique ID
            } else {
                // If permission is not granted
                Toast.makeText(this, "Notification permission is required to show overdue task notifications. Please enable it in app settings", Toast.LENGTH_LONG).show();
            }
        } else {
            // Toast indicating notifications are disabled
            Toast.makeText(this, "Notifications are currently disabled in app settings", Toast.LENGTH_SHORT).show();
        }
    }

    // Calculate task status based on deadline and current date
    private String calculateTaskStatus(String currentStatus, String deadline) {
        try {

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date currentDate = new Date();
            Date deadlineDate = dateFormat.parse(deadline);
            Calendar calendar = Calendar.getInstance(); // Create a Calendar instance for the deadline date
            assert deadlineDate != null;
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

        builder.setPositiveButton("Create", null); // Set initially to null

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String taskName = taskNameInput.getText().toString();
                String deadline = deadlineInput.getText().toString();
                String description = descriptionInput.getText().toString();

                // Check if any of the required fields are empty
                if (taskName.isEmpty() || deadline.isEmpty() || description.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    // Determine initial status based on the selected deadline
                    String initialStatus = calculateTaskStatus("Ongoing", deadline);

                    // Create new task in Firestore
                    createNewTask(taskName, initialStatus, deadline, description, addToCalendarCheckbox.isChecked());
                    dialog.dismiss(); // Dismiss the dialog only when all fields are filled in and task is created
                }
            });
        });
        dialog.show();
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
                        addToCalendar(taskName, deadline, description);

                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error adding document", e);
                    Toast.makeText(MainActivity.this, "Failed to create task!", Toast.LENGTH_SHORT).show();
                });

        String notificationTitle = "New Task Created!";
        String notificationBody = "You've created a new task: " + taskName;
        if (sharedPreferences.getBoolean(SHARED_PREFS_KEY, true)) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                .setSmallIcon(R.drawable.ic_android_black)
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationBody))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, notificationBuilder.build());
        } else {
            // Handle the case where permission is not granted
            Toast.makeText(this, "Notification permission is required to show task creation notifications. Please enable it in app settings", Toast.LENGTH_LONG).show();
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, notificationBuilder.build()); // Notification ID: 1
    }
    }

    // Add task to Google Calendar
    private void addToCalendar(String taskName, String deadline, String description) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date deadlineDate = dateFormat.parse(deadline);

            // Adding 1 day to the deadline date
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(deadlineDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1); // Adding 1 day

            // Create a calendar event with specific time
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, taskName)
                    .putExtra(CalendarContract.Events.DESCRIPTION, description)
                    .putExtra(CalendarContract.Events.ALL_DAY, true)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendar.getTimeInMillis());

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle toolbar item clicks (e.g., menu icon)
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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

