package com.example.budgetingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    EditText username, password;
    Button signup, login;
    String strUsername;
    String strPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        signup = (Button) findViewById(R.id.btnsignup);
        login = (Button) findViewById(R.id.btnlogin);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);

                // clear text boxes
                username.setText("");
                password.setText("");
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if boxes are empty
                strUsername = username.getText().toString();
                strPassword = password.getText().toString();

                if (TextUtils.isEmpty(strUsername)) {
                    username.setError("Enter your email!");
                    username.requestFocus();
                } else if (TextUtils.isEmpty(strPassword)) {
                    password.setError("Enter your password!");
                    password.requestFocus();
                } else {
                    mAuth.signInWithEmailAndPassword(strUsername, strPassword).addOnCompleteListener((task) -> {
                        if (task.isSuccessful()) {
                            if (mAuth.getCurrentUser().isEmailVerified()) {
                                startActivity(new Intent(getApplicationContext(), HomepageActivity.class));
                                overridePendingTransition(0, 0);
                                finish();
                                // clear text boxes
                                username.setText("");
                                password.setText("");
                            } else {
                                Toast.makeText(LoginActivity.this, "You have to verify your email first", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Email not registered or password wrong", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }


}