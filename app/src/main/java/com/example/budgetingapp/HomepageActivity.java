package com.example.budgetingapp;


import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class HomepageActivity extends AppCompatActivity {

    ImageButton menuButton;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    Button btnExpense;
    ListView lv;
    ArrayList<Budget> arrayList;
    ArrayList<String> exceededBudgets;
    TextView emptyText;
    Button btnAdd;
    String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        menuButton = findViewById(R.id.btnMenu);
        btnExpense = findViewById(R.id.btnExpense);
        btnAdd = findViewById(R.id.btnAdd);

        // get sysdate
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        date = simpleDateFormat.format(calendar.getTime());

        // LIST VIEW
        lv = findViewById(R.id.lv_budget);
        arrayList = new ArrayList<Budget>();
        fillList();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EditBudgetDialog dialog = new EditBudgetDialog(arrayList.get(position).getBudgetName(), arrayList.get(position)
                        .getBudgetAmount(), arrayList.get(position).getBudgetEndDate());
                dialog.show(getSupportFragmentManager(), "editing budget");
            }
        });

        // MENU
        PopupMenu dropDownMenu = new PopupMenu(getApplicationContext(), menuButton);
        Menu menu = dropDownMenu.getMenu();

        menu.add(0, 0, 0, "Account settings");
        menu.add(0, 1, 0, "Logout");
        Log.d(TAG, "dropdown menu created");

        dropDownMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 0:
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case 1:
                        mAuth.signOut();
                        startActivity(new Intent(HomepageActivity.this, LoginActivity.class));
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

        btnExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ExpenseActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AddBudgetDialog dialog = new AddBudgetDialog();
                dialog.show(getSupportFragmentManager(), "Creating new budget");
            }
        });
    }

    public void fillList() {
        db.collection("budgets").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                // delete budget if end date is reached

                                if (document.getString("budgetEndDate").equals(date)) {
                                    db.collection("budgets").document(document.getId()).delete();

                                    db.collection("expenses").whereEqualTo("expenseBudgetName", document.getString("budgetName")).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                                    String username = document.getString("email");
                                                    if (username.equals(mAuth.getCurrentUser().getEmail())) {
                                                        db.collection("expenses").document(document.getId()).delete();
                                                    }
                                                }
                                            }
                                        }
                                    });
                                    Log.d(TAG, "deleted doc" + document.getId());
                                }

                                else {
                                    arrayList.add(new Budget(mAuth.getCurrentUser().getEmail(), document.getString("budgetName"), document.getDouble("budgetAmount"), document.getString("budgetEndDate"), document.getDouble("budgetRemaining")));
                                    Log.d(TAG, mAuth.getCurrentUser().getEmail() + ": added budget " + document.getString("budgetName") + ", amount: " + document.getDouble("budgetAmount") + ", due: " + document.getString("budgetEndDate"));
                                }
                                exceededBudgets = new ArrayList<String>();

                                // make sure it's only the users
                                for (Budget b : arrayList) {
                                    if (b.getBudgetAmountRemaining() < 0 && b.getBudgetUser().equals(mAuth.getCurrentUser().getEmail())) {
                                        exceededBudgets.add(b.getBudgetName());
                                        sendNotification(exceededBudgets.toString());
                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, "getBudgetList failed");
                        }
                        Log.d(TAG, "current arrayList: " + arrayList.toString());

                        // IF EMPTY
                        emptyText = findViewById(R.id.empty);
                        lv.setEmptyView(emptyText);

                        ListAdapterBudget listAdapterBudget = new ListAdapterBudget(getApplicationContext(), R.layout.listitem_budget, arrayList);
                        lv.setAdapter(listAdapterBudget);
                    }
                });
    }

    public void sendNotification(String strBudgetName) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1", "Budget Exceeded", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1")
                .setSmallIcon(R.drawable.imgcash)
                .setContentTitle("Money Alarm!")
                .setContentText("You've exceeded your " + strBudgetName + " budget(s)!");

        Notification notification = builder.build();
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
        managerCompat.notify(0, notification);
    }
}