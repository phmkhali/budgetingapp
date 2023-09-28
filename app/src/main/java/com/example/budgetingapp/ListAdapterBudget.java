package com.example.budgetingapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class ListAdapterBudget extends ArrayAdapter<Budget> {

    private Context context;
    private int resource;

    public ListAdapterBudget(Context context, int resource, ArrayList<Budget> arrList) {
        super(context, resource,arrList);
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        convertView = layoutInflater.inflate(resource, parent,false);

        TextView txtBudgetName = convertView.findViewById(R.id.budgetName);
        TextView txtBudgetAmount = convertView.findViewById(R.id.budgetAmountLeft);
        TextView txtBudgetEndDate = convertView.findViewById(R.id.budgetEndDate);
        TextView txtBudgetRemaining = convertView.findViewById(R.id.budgetRemaining);

        txtBudgetName.setText(getItem(position).getBudgetName());
        txtBudgetAmount.setText(getItem(position).getBudgetAmount()+"€");
        txtBudgetEndDate.setText(getItem(position).getBudgetEndDate());
        txtBudgetRemaining.setText(getItem(position).getBudgetAmountRemaining()+"€ remaining");
            if(getItem(position).getBudgetAmountRemaining()<0) {
                txtBudgetRemaining.setTextColor(Color.parseColor("#8b0000"));
            }

        return convertView;
    }
}
