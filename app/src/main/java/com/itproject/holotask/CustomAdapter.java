package com.itproject.holotask;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class CustomAdapter extends BaseAdapter {

    private Context context;
    private List<String[]> data;

    public CustomAdapter(Context context, List<String[]> data) {
        this.context = context;
        this.data = data;
    }

    public void setData(List<String[]> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        }

        TextView taskText = convertView.findViewById(R.id.task_text);
        TextView statusText = convertView.findViewById(R.id.status_text);
        TextView deadlineText = convertView.findViewById(R.id.deadline_text);
        TextView descriptionText = convertView.findViewById(R.id.description_text);

        String[] taskData = data.get(position);
        taskText.setText(taskData[1]); // Task name
        deadlineText.setText(taskData[3]); // Deadline
        descriptionText.setText(taskData[4]); // Description

        // Set task status text
        String status = taskData[2].toUpperCase(); // Get status and convert to uppercase
        statusText.setText(status);
        statusText.setAllCaps(true); // Ensure all caps for status text

        // Set text color and style based on status
        if (status.equals("OVERDUE")) {
            statusText.setTextColor(Color.RED);
        } else if (status.equals("ONGOING")) {
            statusText.setTextColor(Color.YELLOW);
        } else if (status.equals("COMPLETED")) {
            statusText.setTextColor(Color.GREEN);
        } else {
            // Default color and style
            statusText.setTextColor(Color.BLACK);
        }

        // Optionally, make status text bold
        statusText.setPaintFlags(statusText.getPaintFlags() | android.graphics.Paint.FAKE_BOLD_TEXT_FLAG);

        return convertView;
    }
}
