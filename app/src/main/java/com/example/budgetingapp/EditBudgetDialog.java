package com.example.budgetingapp;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class EditBudgetDialog extends AppCompatDialogFragment {


    EditText editBudgetName, editBudgetAmount, editBudgetEndDate;
    String strBudgetName, strBudgetEndDate, strBudgetAmount, oldBudgetName, oldBudgetEndDate, oldBudgetAmount;
    double dblBudgetAmount, oldDblBudgetAmount;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    Calendar calendar;
    Context context;
    ArrayList<String> budgetNames;
    boolean match;

    public EditBudgetDialog(String name, double amount, String date) {
        oldBudgetName = name;
        oldDblBudgetAmount = amount;
        oldBudgetEndDate = date;

    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        calendar = Calendar.getInstance();
        context = getContext();
        budgetNames = new ArrayList<>();
        getBudgetNames();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_editbudget, null);

        editBudgetName = (EditText) view.findViewById(R.id.editBudgetName);
        editBudgetName.setText(oldBudgetName);
        editBudgetAmount = (EditText) view.findViewById(R.id.editAmount);
        oldBudgetAmount = String.valueOf(oldDblBudgetAmount);
        editBudgetAmount.setText(oldBudgetAmount);
        editBudgetEndDate = (EditText) view.findViewById(R.id.editDate);
        editBudgetEndDate.setText(oldBudgetEndDate);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(year, monthOfYear, dayOfMonth);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
                editBudgetEndDate.setText(simpleDateFormat.format(calendar.getTime()));
            }
        }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        editBudgetEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog.show();
            }
        });

        builder.setView(view).setTitle("Editing budget")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                })
                .setNeutralButton("Delete Budget", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteBudget(editBudgetName.getText().toString());
                        startActivity(new Intent(getContext(), MainActivity.class));
                        dismiss();
                        Toast.makeText(getContext(), "Deleted budget: " + editBudgetName.getText().toString(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        strBudgetName = editBudgetName.getText().toString();
                        strBudgetAmount = editBudgetAmount.getText().toString();
                        strBudgetEndDate = editBudgetEndDate.getText().toString();

                        if (TextUtils.isEmpty(strBudgetName)) {
                            Toast.makeText(getContext(), "Edit failed. Enter a budget name!", Toast.LENGTH_SHORT).show();
                        } else if (TextUtils.isEmpty(strBudgetAmount)) {
                            Toast.makeText(getContext(), "Edit failed. Enter a budget amount!", Toast.LENGTH_SHORT).show();
                        } else if (TextUtils.isEmpty(strBudgetEndDate)) {
                            Toast.makeText(getContext(), "Edit failed. Enter a due date!", Toast.LENGTH_SHORT).show();
                        } else {
                            dblBudgetAmount = Double.parseDouble(strBudgetAmount);

                            match = false;
                            for(String str : budgetNames) {
                                if(str.equals(strBudgetName)) {
                                    match = true;
                                }
                            }
                            if(!match) {
                                updateBudget();
                                Log.d(TAG,"edited budget");
                            } else {
                                Toast.makeText(context, "budget "+strBudgetName+" already exists. Choose another name.", Toast.LENGTH_SHORT).show();
                            }
                            startActivity(new Intent(getContext(), MainActivity.class));
                        }
                    }
                });

        AlertDialog dialog = builder.create();
        Button b = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        if (b != null) {
            b.setTextColor(Color.parseColor("#8b0000"));
        }
        return dialog;
    }

    // delete budgets and related expenses
    public void deleteBudget(String strBudgetName) {
        db.collection("budgets").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        String budgetName = document.getString("budgetName");
                        if (budgetName.equals(strBudgetName)) {
                            db.collection("budgets").document(document.getId()).delete();
                        }
                    }
                } else {
                    Log.e(TAG, "failed to get docID to delete budget");
                }
            }
        });

        db.collection("expenses").whereEqualTo("expenseBudgetName", strBudgetName).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        String username = document.getString("email");
                        if (username.equals(mAuth.getCurrentUser().getEmail())) {
                            db.collection("expenses").document(document.getId()).delete();
                        }
                    }
                } else {
                    Log.e(TAG, "failed to get docID to delete expense");
                }
            }
        });
    }

    public void updateBudget() {
        db.collection("budgets").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String budgetName = document.getString("budgetName");
                                if (budgetName.equals(oldBudgetName)) {
                                    db.collection("budgets").document(document.getId()).update("budgetName", strBudgetName);
                                    db.collection("budgets").document(document.getId()).update("budgetAmount", dblBudgetAmount);
                                    db.collection("budgets").document(document.getId()).update("budgetEndDate", strBudgetEndDate);
                                    db.collection("budgets").document(document.getId()).update("budgetRemaining", document.getDouble("budgetRemaining") - (oldDblBudgetAmount - dblBudgetAmount));

                                    db.collection("expenses").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                        String budgetName = document.getString("expenseBudgetName");
                                                        if (budgetName.equals(oldBudgetName)) {
                                                            db.collection("expenses").document(document.getId()).update("expenseBudgetName", strBudgetName);
                                                        }
                                                    }
                                                }
                                            });
                                }
                            }
                        } else {
                            Log.d(TAG, "failed to update budget");
                        }
                    }
                });
    }

    public void getBudgetNames() {
        db.collection("budgets").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        // if name exists
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            budgetNames.add(document.getString("budgetName"));
                        }
                        Log.d(TAG,"budget Names: "+budgetNames.toString());
                    }
                });

    }
}

