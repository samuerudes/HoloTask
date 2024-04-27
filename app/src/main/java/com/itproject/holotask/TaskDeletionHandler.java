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
