package com.example.budgetingapp;


import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddExpenseDialog extends AppCompatDialogFragment implements AdapterView.OnItemSelectedListener {

    Spinner editChooseBudget;
    EditText editExpenseDesc, editExpenseAmount;
    String strBudgetName, strExpenseDesc, strExpenseAmount, strExpenseDate;
    double dblExpenseAmount;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    ArrayAdapter<String> adapter;
    ArrayList<String> arrayList;

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_addexpense, null);

        editChooseBudget = (Spinner) view.findViewById(R.id.chooseBudget);
        editExpenseDesc = (EditText) view.findViewById(R.id.editExpenseDesc);
        editExpenseAmount = (EditText) view.findViewById(R.id.editExpenseAmount);

        arrayList = new ArrayList<String>();
        fillBudgetPopup(arrayList);

        // get sysdate
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        strExpenseDate = simpleDateFormat.format(calendar.getTime());


        builder.setView(view).setTitle("Adding expense")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editExpenseDesc.setText("");
                        editExpenseAmount.setText("");
                    }
                })
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        strExpenseDesc = editExpenseDesc.getText().toString();
                        strExpenseAmount = editExpenseAmount.getText().toString();

                        if (TextUtils.isEmpty(strBudgetName)) {
                            Toast.makeText(getContext(), "Expense creation failed. You must choose a budget!", Toast.LENGTH_SHORT).show();
                        } else if (TextUtils.isEmpty(strExpenseAmount)) {
                            Toast.makeText(getContext(), "Expense creation failed. Enter an expense amount!", Toast.LENGTH_SHORT).show();
                        } else {
                            if (TextUtils.isEmpty(strExpenseDesc)) {
                                strExpenseDesc = strBudgetName;
                            }
                            dblExpenseAmount = Double.parseDouble(strExpenseAmount);

                            addExpenseToDB(mAuth.getCurrentUser().getEmail(), strBudgetName, strExpenseDesc, dblExpenseAmount, strExpenseDate);
                            startActivity(new Intent(getContext(), ExpenseActivity.class));
                            Toast.makeText(getContext(), "Added expense " + strExpenseDesc, Toast.LENGTH_SHORT).show();
                            updateRemainingBudget(strBudgetName, dblExpenseAmount);
                        }
                    }
                });
        return builder.create();
    }

    public void addExpenseToDB(String username, String budgetName, String expenseDesc, double expenseAmount, String expenseDate) {
        Map<String, Object> expenses = new HashMap<>();
        expenses.put("email", username);
        expenses.put("expenseBudgetName", budgetName);
        expenses.put("expenseDesc", expenseDesc);
        expenses.put("expenseAmount", expenseAmount);
        expenses.put("expenseDate", expenseDate);
        expenses.put("id",null);

        db.collection("expenses")
                .add(expenses)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                        db.collection("expenses").document(documentReference.getId()).update("id",documentReference.getId());

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    public void fillBudgetPopup(ArrayList<String> arrayList) {
        db.collection("budgets").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                arrayList.add(document.getString("budgetName"));
                                Log.d(TAG, "fillBudgetPopup | added budget " + document.getString("budgetName") + ", amount: " + document.getDouble("budgetAmount") + ", due: " + document.getString("budgetEndDate"));
                            }
                            setDropdown();
                        } else {
                            Log.d(TAG, "getBudgetList failed");
                        }
                    }
                });
    }

    public void setDropdown() {
        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, arrayList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editChooseBudget.setAdapter(adapter);
        editChooseBudget.setOnItemSelectedListener(this);
        Log.d(TAG, "created adapter, adapterlist: " + arrayList.toString());
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String item = adapterView.getItemAtPosition(i).toString();
        strBudgetName = item;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    // subtract from budget if money is spent
    public void updateRemainingBudget(String strBudgetName, double dblExpenseAmount) {
        db.collection("budgets").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String budgetName = document.getString("budgetName");
                                if (budgetName.equals(strBudgetName)) {
                                    db.collection("budgets").document(document.getId()).update("budgetRemaining", document.getDouble("budgetRemaining") - dblExpenseAmount);
                                }
                            }
                        }
                    }
                });
    }
}
