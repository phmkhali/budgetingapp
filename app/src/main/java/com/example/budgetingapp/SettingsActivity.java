package com.example.budgetingapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {

    Button btnLogOut, btnDeleteAcc, btnChangePass;
    ImageButton btnBack;
    EditText etNewPass, etConfirmPass;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    TextView currUser;
    String strMail, docID, oldPassword, newPassword, confirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btnLogOut = findViewById(R.id.btnLogout);
        btnDeleteAcc = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);
        btnChangePass = findViewById(R.id.btnChangePass);
        etNewPass = findViewById(R.id.etNewPass);
        etConfirmPass = findViewById(R.id.etConfirmPass);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currUser = findViewById(R.id.tvCurrentUser);
        String regexPass = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,20}$";

        strMail = mAuth.getCurrentUser().getEmail();


        db.collection("users").whereEqualTo("email", strMail).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "success", Toast.LENGTH_SHORT);
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                currUser.setText(document.getString("email"));
                                Log.d(TAG, "mail " + document.getString("email") + "; password: " + document.getString("password"));
                            }
                        } else {
                            Toast.makeText(SettingsActivity.this, "fail reading db", Toast.LENGTH_SHORT);
                        }
                    }
                });

        db.collection("users").whereEqualTo("email", mAuth.getCurrentUser().getEmail()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                oldPassword = document.getString("password");
                                Log.d(TAG, "old pass for reauth = "+oldPassword);
                            }
                        } else {
                            Log.d(TAG, "fail getting password");
                        }
                    }
                });

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                finish();
                startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
            }
        });

        btnChangePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newPassword = etNewPass.getText().toString();
                confirmPassword = etConfirmPass.getText().toString();

                if(TextUtils.isEmpty(newPassword)) {
                    Toast.makeText(SettingsActivity.this, "Enter a new password", Toast.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(confirmPassword)) {
                    Toast.makeText(SettingsActivity.this, "Confirm the password", Toast.LENGTH_SHORT).show();
                }
                // digit, lower and upper case, no white spaces, between 8-20 chars
                else if (!validatePassword(newPassword, regexPass)) {
                    etNewPass.setError("Your password should have upper and lower case letters, a length between 8-20, a digit and no white spaces");
                    etNewPass.requestFocus();
                }

                // password confirmation
                else if (!newPassword.equals(confirmPassword)) {
                    etConfirmPass.setError("The passwords do not match!");
                    etConfirmPass.requestFocus();
                    etNewPass.setText("");
                    etConfirmPass.setText("");
                }
                else {
                    updatePassword(oldPassword, newPassword);
                }
            }
        });

        btnDeleteAcc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                // confirmation dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setCancelable(true);
                builder.setTitle("Confirm delete account");
                builder.setMessage("All account related data will be lost. Are you sure you want to delete your account?");

                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAcc(strMail);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG,"delete account cancelled");
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingsActivity.this, HomepageActivity.class));
                overridePendingTransition(0, 0);
            }
        });

    }

    public void deleteAcc(String username) {
        mAuth.getCurrentUser().delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    mAuth.signOut();
                    startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                    finish();

                    // delete from users collection
                    db.collection("users").whereEqualTo("email", username).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                            docID = document.getId();
                                        }
                                        db.collection("users").document(docID).delete();
                                    } else {
                                        Log.e(TAG, "failed to get docID to delete user document");
                                    }
                                }
                            });

                    // delete from budgets collection
                    db.collection("budgets").whereEqualTo("email", username).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                    docID = document.getId();
                                }
                                db.collection("budgets").document(docID).delete();
                            } else {
                                Log.e(TAG, "failed to get docID to delete budget document");
                            }
                        }
                    });

                    // delete from expense collection
                    db.collection("expenses").whereEqualTo("email", username).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                    docID = document.getId();
                                }
                                db.collection("expenses").document(docID).delete();
                            } else {
                                Log.e(TAG, "failed to get docID to delete expenses document");
                            }
                        }
                    });


                    Toast.makeText(SettingsActivity.this, "Your account has been deleted.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(SettingsActivity.this, "Failed to delete your account.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }




    public boolean validatePassword(String password, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    public void updatePassword(String oldPassword, String newPassword) {
        AuthCredential authCredential = EmailAuthProvider.getCredential(strMail,oldPassword);

        mAuth.getCurrentUser().reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"User reauthenticated");
                        mAuth.getCurrentUser().updatePassword(newPassword);
                        db.collection("users").whereEqualTo("email", strMail).get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                db.collection("users").document(document.getId()).update("password",newPassword);
                                                Toast.makeText(SettingsActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                                                etNewPass.setText("");
                                                etConfirmPass.setText("");
                                            }
                                        }
                                    }
                                }
                                ).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG,"Failed to change password in users table");
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Failed to reauthenticate user. Email: "+strMail+", password: "+oldPassword);
                    }
                });
    }
}