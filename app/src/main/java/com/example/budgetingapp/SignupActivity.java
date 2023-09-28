package com.example.budgetingapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignupActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    EditText username, password, password2;
    Button createAcc, returnLogin;
    String strUsername;
    String strPassword;
    String strPassword2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        username = (EditText) findViewById(R.id.signupUsername);
        password = (EditText) findViewById(R.id.signupPassword);
        password2 = (EditText) findViewById(R.id.signupPassword2);
        createAcc = (Button) findViewById(R.id.btnCreateAcc);
        returnLogin = (Button) findViewById(R.id.btnReturnLogin);

        createAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // boxes cant be empty
                strUsername = username.getText().toString();
                strPassword = password.getText().toString();
                strPassword2 = password2.getText().toString();
                String regexUser = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@" + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
                String regexPass = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,20}$";

                if (TextUtils.isEmpty(strUsername)) {
                    username.setError("You have to enter an email!");
                    username.requestFocus();
                }
                // if mail exists
                else {
                    mAuth.fetchSignInMethodsForEmail(strUsername).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                        @Override
                        public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                            if (task.isSuccessful()) {
                                boolean check = !task.getResult().getSignInMethods().isEmpty();
                                if (check) {
                                    username.setError("Another account is using the same email address.");
                                    username.requestFocus();
                                }
                            }
                        }
                    });
                }

                if (TextUtils.isEmpty(strPassword)) {
                    password.setError("You have to enter a password!");
                    password.requestFocus();
                } else if (TextUtils.isEmpty(strPassword2)) {
                    password2.setError("Confirm your password!");
                    password2.requestFocus();
                }

                // mail correct?
                else if (!validateMail(strUsername, regexUser)) {
                    username.setError("This email is invalid");
                    username.requestFocus();
                }

                // digit, lower and upper case, no white spaces, between 8-20 chars
                else if (!validatePassword(strPassword, regexPass)) {
                    password.setError("Your password should have upper and lower case letters, a length between 8-20, a digit and no white spaces");
                    password.requestFocus();
                }

                // password confirmation
                else if (!strPassword.equals(strPassword2)) {
                    password2.setError("The passwords do not match!");
                    password2.requestFocus();
                    password.setText("");
                    password2.setText("");
                }

                // add user to db
                else {
                    signupUser(strUsername,strPassword);
                    addUserToDB(strUsername, strPassword);
                    username.setText("");
                    password.setText("");
                    password2.setText("");
                }
            }
        });


        returnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                username.setText("");
                password.setText("");
                password2.setText("");
            }
        });
    }

    public boolean validatePassword(String password, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    public boolean validateMail(String mail, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(mail);
        return matcher.matches();
    }

    public void signupUser(String username, String password) {
        mAuth.createUserWithEmailAndPassword(username, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(SignupActivity.this, "Registration successful. Verify your email to login!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                overridePendingTransition(0, 0);
                            } else {
                                Toast.makeText(SignupActivity.this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }


    public void addUserToDB(String username, String password) {
        Map<String, Object> users = new HashMap<>();
        users.put("email", username);
        users.put("password", password);

        db.collection("users")
                .add(users)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }
}