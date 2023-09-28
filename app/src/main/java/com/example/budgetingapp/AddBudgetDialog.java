package com.example.budgetingapp;


import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddBudgetDialog extends AppCompatDialogFragment {

    EditText editBudgetName, editBudgetAmount, editBudgetEndDate;
    String strBudgetName, strBudgetAmount, strBudgetEndDate;
    double dblBudgetAmount;
    boolean match;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    Calendar calendar;
    Context context;
    ArrayList<String> budgetNames;

    @SuppressLint("SuspiciousIndentation")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        context = getContext();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        calendar = Calendar.getInstance();
        budgetNames = new ArrayList<>();
        getBudgetNames();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_addbudget, null);

        editBudgetName = (EditText) view.findViewById(R.id.editBudgetName);
        editBudgetAmount = (EditText) view.findViewById(R.id.editAmount);
        editBudgetEndDate = (EditText) view.findViewById(R.id.editDate);

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
        // min date tomorrow
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() + 86400000);

        editBudgetEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog.show();
            }
        });

        builder.setView(view).setTitle("Creating new budget")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editBudgetName.setText("");
                        editBudgetAmount.setText("");
                        editBudgetEndDate.setText("");
                    }
                })
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        strBudgetName = editBudgetName.getText().toString();
                        strBudgetAmount = editBudgetAmount.getText().toString();
                        strBudgetEndDate = editBudgetEndDate.getText().toString();

                        if (TextUtils.isEmpty(strBudgetName)) {
                            Toast.makeText(getContext(), "Budget creation failed. Enter a budget name!", Toast.LENGTH_SHORT).show();
                        } else if (TextUtils.isEmpty(strBudgetAmount)) {
                            Toast.makeText(getContext(), "Budget creation failed. Enter a budget amount!", Toast.LENGTH_SHORT).show();
                        } else if (TextUtils.isEmpty(strBudgetEndDate)) {
                            Toast.makeText(getContext(), "Budget creation failed. Enter a due date!", Toast.LENGTH_SHORT).show();
                        } else {
                            dblBudgetAmount = Double.parseDouble(strBudgetAmount);

                            match = false;
                            for(String str : budgetNames) {
                                if(str.equals(strBudgetName)) {
                                    match = true;
                                }
                            }
                            if(!match) {
                                addBudgetToDB(mAuth.getCurrentUser().getEmail(),strBudgetName,dblBudgetAmount,strBudgetEndDate);
                                Log.d(TAG,"added budget");
                            } else {
                                Toast.makeText(context, "budget "+strBudgetName+" already exists. Choose another name.", Toast.LENGTH_SHORT).show();
                            }

                            editBudgetName.setText("");
                            editBudgetAmount.setText("");
                            editBudgetEndDate.setText("");
                            startActivity(new Intent(getContext(), MainActivity.class));
                        }
                    }
                });
        return builder.create();
    }

    public void addBudgetToDB(String username, String budgetName, double budgetAmount, String budgetEndDate) {
        Map<String, Object> budgets = new HashMap<>();
        budgets.put("email", username);
        budgets.put("budgetName", budgetName);
        budgets.put("budgetAmount", budgetAmount);
        budgets.put("budgetEndDate", budgetEndDate);
        budgets.put("budgetRemaining", budgetAmount);
        budgets.put("id", null);

        db.collection("budgets")
                .add(budgets)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                        db.collection("budgets").document(documentReference.getId()).update("id", documentReference.getId());

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
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


