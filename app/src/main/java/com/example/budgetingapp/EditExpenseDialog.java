package com.example.budgetingapp;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class EditExpenseDialog extends AppCompatDialogFragment {

    EditText editExpenseAmount, editExpenseDesc;
    String strExpenseBudgetName, strExpenseDesc, strExpenseAmount, oldExpenseDesc, oldEditExpenseAmount, strId;
    double dblExpenseAmount, oldDblExpenseAmount;
    FirebaseFirestore db;
    FirebaseAuth mAuth;

    public EditExpenseDialog(String expenseBudgetName, double expenseAmount, String expenseDesc, String id) {
        strExpenseBudgetName = expenseBudgetName;
        oldDblExpenseAmount = expenseAmount;
        oldExpenseDesc = expenseDesc;
        strId = id;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_editexpense, null);

        editExpenseAmount = (EditText) view.findViewById(R.id.editExpenseAmount);
        oldEditExpenseAmount = String.valueOf(oldDblExpenseAmount);
        editExpenseAmount.setText(oldEditExpenseAmount);
        editExpenseDesc = (EditText) view.findViewById(R.id.editExpenseDesc);
        editExpenseDesc.setText(oldExpenseDesc);

        builder.setView(view).setTitle("Editing expense")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                })
                .setNeutralButton("Delete Expense", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteExpense(strId);
                        startActivity(new Intent(getContext(), ExpenseActivity.class));
                        dismiss();
                        Toast.makeText(getContext(), "Deleted expense.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        strExpenseAmount = editExpenseAmount.getText().toString();
                        strExpenseDesc = editExpenseDesc.getText().toString();

                        if (TextUtils.isEmpty(strExpenseAmount)) {
                            Toast.makeText(getContext(), "Edit failed! You have to enter an amount!", Toast.LENGTH_SHORT).show();
                        }
                        else if (TextUtils.isEmpty(strExpenseDesc)){
                            strExpenseDesc = strExpenseBudgetName;
                        } else {
                            dblExpenseAmount = Double.parseDouble(strExpenseAmount);
                            updateExpense(strId);
                            startActivity(new Intent(getContext(), ExpenseActivity.class));
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

    public void deleteExpense(String id) {
        db.collection("expenses").document(id).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(TAG, "expense deleted");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "failed to delete expense");
            }
        });
        // add money from deleted expense back to budget
        db.collection("budgets").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        String budgetName = document.getString("budgetName");
                        if (budgetName.equals(strExpenseBudgetName)) {
                            double oldRemaining = document.getDouble("budgetRemaining");
                            db.collection("budgets").document(document.getId()).update("budgetRemaining",oldRemaining+oldDblExpenseAmount);
                        }
                    }
                } else {
                    Log.e(TAG, "failed to get docID to update budget");
                }
            }
        });
        }

    public void updateExpense(String id) {
       db.collection("expenses").document(id).update("expenseAmount",dblExpenseAmount);
       db.collection("expenses").document(id).update("expenseDesc",strExpenseDesc);

       // adjust updated expense amount in budget

            db.collection("budgets").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            String budgetName = document.getString("budgetName");
                            if (budgetName.equals(strExpenseBudgetName)) {
                                double oldRemaining = document.getDouble("budgetRemaining");
                                db.collection("budgets").document(document.getId()).update("budgetRemaining",oldRemaining+(oldDblExpenseAmount-dblExpenseAmount));
                            }
                        }
                    } else {
                        Log.e(TAG, "failed to get docID to update budget");
                    }
                }
            });
    }


}

