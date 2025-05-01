package xyz.ziadboukhalkhal.shopsmart.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import xyz.ziadboukhalkhal.shopsmart.R;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            String email = ((EditText)findViewById(R.id.et_email)).getText().toString();
            String password = ((EditText)findViewById(R.id.et_password)).getText().toString();

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

}