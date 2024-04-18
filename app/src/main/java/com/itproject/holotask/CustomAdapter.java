package com.itproject.holotask;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomAdapter extends BaseAdapter {

    private Context context;
    private List<String[]> data;

    public CustomAdapter(Context context, List<String[]> data) {
        this.context = context;
        this.data = data;
    }

    public void setData(List<String[]> data) {
        this.data = data;  // Convert List to array
        notifyDataSetChanged();  // Notify the adapter of data change
    }

    @Override
    public int getCount() {
        return data.size();

    }

    @Override
    public Object getItem(int position) {
        return data.get(position);  // Retrieve the String[] at the given position
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            // Inflate custom layout for each item
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        }


        // Get references to text views for sub-items
        TextView taskText = convertView.findViewById(R.id.task_text);
        TextView statusText = convertView.findViewById(R.id.status_text);
        TextView deadlineText = convertView.findViewById(R.id.deadline_text);
        TextView descriptionText = convertView.findViewById(R.id.description_text);

        // Set text for each sub-item
        String[] taskData = data.get(position); // Get the String[] representing a task
        taskText.setText(taskData[1]); // Access elements within the String[]
        statusText.setText(taskData[2]);
        deadlineText.setText(taskData[3]);
        descriptionText.setText(taskData[4]);

        return convertView;


    }

}