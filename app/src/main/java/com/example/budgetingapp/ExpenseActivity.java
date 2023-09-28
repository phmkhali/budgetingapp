package com.example.budgetingapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class ExpenseActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ImageButton menuButton;
    Button btnBudget;
    Button btnAdd;
    ArrayList<Expense> arrayList;
    TextView emptyText;
    ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        btnAdd = findViewById(R.id.btnAdd);

        // LIST VIEW
        lv = findViewById(R.id.lv_expense);
        arrayList = new ArrayList<Expense>();
        fillList();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EditExpenseDialog dialog = new EditExpenseDialog(arrayList.get(position).getExpenseBudgetName(),arrayList
                        .get(position).getExpenseAmount(),arrayList.get(position).getExpenseDesc(),arrayList.get(position).getId());
                dialog.show(getSupportFragmentManager(), "editing expense");
            }
        });
        
        menuButton = findViewById(R.id.btnMenu);
        PopupMenu dropDownMenu = new PopupMenu(getApplicationContext(),menuButton);
        Menu menu = dropDownMenu.getMenu();

        menu.add(0, 0, 0, "Account settings");
        menu.add(0, 1, 0, "Logout");

        dropDownMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 0:
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case 1:
                        mAuth.signOut();
                        startActivity(new Intent(ExpenseActivity.this, LoginActivity.class));
                        finish();
                        return true;
                }
                return false;
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dropDownMenu.show();
            }
        });

        btnBudget = findViewById(R.id.btnBudget);
        btnBudget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), HomepageActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkBudgetExists();
            }
        });

    }
    public void fillList() {
        db.collection("expenses").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                arrayList.add(new Expense(mAuth.getCurrentUser().getEmail(), document.getString("expenseBudgetName"),
                                                document.getString("expenseDesc") ,document.getDouble("expenseAmount"),
                                                document.getString("expenseDate"), document.getString("id")));
                                Log.d(TAG, mAuth.getCurrentUser().getEmail()+": added expense "+document.getString("expenseDesc"));
                            }
                        } else {
                            Log.d(TAG, "getExpenseList failed");
                        }
                        Log.d(TAG,"current arrayList: " + arrayList.toString());

                        // IF EMPTY
                        emptyText = findViewById(R.id.empty);
                        lv.setEmptyView(emptyText);

                        ListAdapterExpense listAdapterExpense = new ListAdapterExpense(getApplicationContext(),R.layout.listitem_expense, arrayList);
                        lv.setAdapter(listAdapterExpense);

                    }
                });
    }
    public void checkBudgetExists() {
        db.collection("budgets").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(ExpenseActivity.this, "You have to create a budget first!", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            btnAdd.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    AddExpenseDialog dialog = new AddExpenseDialog();
                                    dialog.show(getSupportFragmentManager(), "Adding expense");
                                }
                            });
                        }
                    }
                });
    }
}