package com.itproject.holotask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
// Import FirebaseFirestore
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TaskDeletionHandler.OnTaskDeletedListener {

    private Calendar mCalendarService;
    private GridView gridView;
    private CustomAdapter adapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<String[]> data = new ArrayList<>();  // Declare and initialize data list


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get references to views
        gridView = findViewById(R.id.gridView);
        Button createTaskButton = findViewById(R.id.button8);

        // Fetch tasks from Firestore on startup
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String userId;
        if (user != null) {
            userId = user.getUid();
        } else {
            userId = "";
        }

        db.collection("UserTasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String taskID = document.getString("taskID");
                            String taskName = document.getString("taskName");
                            String status = document.getString("taskStatus");
                            String deadline = document.getString("endDateTime");
                            String description = document.getString("taskDescription");
                            data.add(new String[]{taskID, taskName, status, deadline, description});
                        }

                        // Create adapter with initial data
                        adapter = new CustomAdapter(this, data);
                        gridView.setAdapter(adapter);
                    } else {
                        Log.w("Firestore", "Error fetching documents.", task.getException());
                    }
                });

        // Set click listener for each item

        createTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an AlertDialog to prompt for task details

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Create New Task");

                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL); // Arrange fields vertically

                // Add input fields for task details
                final EditText taskNameInput = new EditText(MainActivity.this);
                taskNameInput.setHint("Task Name");
                layout.addView(taskNameInput);

                final EditText statusInput = new EditText(MainActivity.this);
                statusInput.setHint("Status");
                layout.addView(statusInput);

                final EditText deadlineInput = new EditText(MainActivity.this);
                deadlineInput.setHint("Due Date");
                deadlineInput.setFocusable(false);
                layout.addView(deadlineInput);

                deadlineInput.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Get current date
                        final Calendar calendar = Calendar.getInstance();
                        int year = calendar.get(Calendar.YEAR);
                        int month = calendar.get(Calendar.MONTH);
                        int day = calendar.get(Calendar.DAY_OF_MONTH);

                        // Create a new DatePickerDialog
                        DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                                new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                        // Update the deadline text with selected date
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

                                        // Use the passed parameters (year, monthOfYear, dayOfMonth) to create a Date object representing the selected date
                                        Calendar selectedDateCalendar = Calendar.getInstance();
                                        selectedDateCalendar.set(year, monthOfYear, dayOfMonth);
                                        String formattedDate = dateFormat.format(selectedDateCalendar.getTime()); // Use getTime() to get a Date object from the Calendar

                                        deadlineInput.setText(formattedDate);
                                    }
                                }, year, month, day);

                        // Show the date picker dialog
                        datePickerDialog.show();
                    }
                });


                final EditText descriptionInput = new EditText(MainActivity.this);
                descriptionInput.setHint("Description");
                layout.addView(descriptionInput);

                // Add checkbox for adding to Google Calendar
                final CheckBox addToCalendarCheckbox = new CheckBox(MainActivity.this);
                addToCalendarCheckbox.setText("Add to Google Calendar");
                layout.addView(addToCalendarCheckbox);

                builder.setView(layout);

                // Add buttons to save or cancel
                builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String taskID = UUID.randomUUID().toString();

                        // Get input values from all fields
                        String taskName = taskNameInput.getText().toString();
                        String status = statusInput.getText().toString();
                        String deadline = deadlineInput.getText().toString();
                        String description = descriptionInput.getText().toString();


                        // Create a new task object
                        HashMap<String, String> newTask = new HashMap<>();
                        newTask.put("taskID", taskID);
                        newTask.put("taskName", taskName);
                        newTask.put("taskStatus", status);
                        newTask.put("endDateTime", deadline);
                        newTask.put("taskDescription", description);
                        newTask.put("userId", userId);


                        boolean addToCalendar = addToCalendarCheckbox.isChecked();

                        db.collection("UserTasks")
                                .add(newTask)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Log.d("Firestore", "Task added with ID: " + documentReference.getId());


                                        Toast.makeText(MainActivity.this, "Task created successfully!", Toast.LENGTH_SHORT).show();

                                        String[] newTaskData = {taskID, taskName, status, deadline, description};

                                        // Update data list without overwriting
                                        data.add(newTaskData);  // Create newTaskData array as before

                                        // Optional: Notify adapter of data change
                                        adapter.notifyDataSetChanged();


                                        if (addToCalendar) {
                                            String taskName = taskNameInput.getText().toString();
                                            String deadline = deadlineInput.getText().toString();

                                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                                            // Parse deadline String into a Date object

                                            // Assuming deadline is already a valid date format
                                            Date deadlineDate = null;
                                            try {
                                                deadlineDate = dateFormat.parse(deadline);
                                            } catch (ParseException e) {
                                                throw new RuntimeException(e);
                                            }

                                            Calendar calendarEvent = Calendar.getInstance();
                                            calendarEvent.setTime(deadlineDate);

                                            Intent intent = new Intent(Intent.ACTION_INSERT)
                                                    .setData(CalendarContract.Events.CONTENT_URI)
                                                    .putExtra(CalendarContract.Events.TITLE, taskName)
                                                    .putExtra(CalendarContract.Events.ALL_DAY, true) // Set all-day event (optional)
                                                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendarEvent.getTimeInMillis());

                                            startActivity(intent);
                                        }

                                    }

                                })

                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("Firestore", "Error adding document", e);
                                        Toast.makeText(MainActivity.this, "Failed to create task!", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    }



                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                });


// Show the dialog
                builder.show();
            }
        });


        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String[] taskData = data.get(position);  // Retrieve selected task data
                String description = taskData[4];
                String deadline = taskData[3];
                String status = taskData[2];
                String taskName = taskData[1];
                String taskID = taskData[0];


                //  launch TaskActivity and pass data
                Intent intent = new Intent(MainActivity.this, TaskActivity.class);
                intent.putExtra("taskID", taskID);
                intent.putExtra("taskName", taskName);
                intent.putExtra("description", description);
                intent.putExtra("status", status);
                intent.putExtra("deadline", deadline);
                startActivity(intent);

            }
        });

    }

}