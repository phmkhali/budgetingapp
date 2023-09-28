package com.example.budgetingapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class ListAdapterExpense extends ArrayAdapter<Expense> {

    private Context context;
    private int resource;

    public ListAdapterExpense(Context context, int resource, ArrayList<Expense> arrList) {
        super(context, resource,arrList);
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        convertView = layoutInflater.inflate(resource, parent,false);

        TextView txtBudgetName = convertView.findViewById(R.id.expenseBudgetName);
        TextView txtExpenseAmount = convertView.findViewById(R.id.expenseAmount);
        TextView txtExpenseDesc = convertView.findViewById(R.id.expenseDesc);
        TextView txtExpenseDate = convertView.findViewById(R.id.expenseDate);

        txtBudgetName.setText(getItem(position).getExpenseBudgetName());
        txtExpenseAmount.setText("- "+getItem(position).getExpenseAmount()+"€");
        txtExpenseDesc.setText(getItem(position).getExpenseDesc());
        txtExpenseDate.setText(getItem(position).getExpenseDate());

        return convertView;
    }
}
