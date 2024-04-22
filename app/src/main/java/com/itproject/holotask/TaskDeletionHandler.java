package com.itproject.holotask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class TaskDeletionHandler {

    public static void deleteTask(final Context context, final String taskID) {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete this task?");

        // Positive and negative buttons
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteTask(context, taskID);  // Call delete method
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Show the confirmation dialog
        builder.show();
    }

    public interface OnTaskDeletedListener {
        void onTaskDeleted(String deletedTaskID);
    }

    public static void deleteTask(final Context context, final String taskID, final OnTaskDeletedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("UserTasks")
                .whereEqualTo("taskID", taskID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        documentSnapshot.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    if (listener != null) {
                                        listener.onTaskDeleted(taskID);
                                    }
                                    Toast.makeText(context, "Task deleted successfully!", Toast.LENGTH_SHORT).show();

                                    // Redirect to MainActivity
                                    Intent intent = new Intent(context, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("Firestore", "Error deleting document", e);
                                    Toast.makeText(context, "Failed to delete task!", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error querying documents", e);
                    Toast.makeText(context, "Failed to delete task!", Toast.LENGTH_SHORT).show();
                });
    }
}
