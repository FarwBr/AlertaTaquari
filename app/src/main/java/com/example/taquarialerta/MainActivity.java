package com.example.taquarialerta;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextSenha;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextSenha = findViewById(R.id.editTextSenha);
        Button btnEntrar = findViewById(R.id.buttonEntrar);
        Button btnRegistrar = findViewById(R.id.buttonRegistrar);
        Button btnConvidado = findViewById(R.id.buttonConvidado);

        btnEntrar.setOnClickListener(v -> login());
        btnRegistrar.setOnClickListener(v -> registrar());
        btnConvidado.setOnClickListener(v -> loginAnonimo());
    }

    private void login() {
        String email = editTextEmail.getText().toString();
        String senha = editTextSenha.getText().toString();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha email e senha.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseAuth", "Login bem-sucedido");
                        startMapsActivity();
                    } else {
                        Log.e("FirebaseAuth", "Falha no login", task.getException());
                        Toast.makeText(this, "Erro no login: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registrar() {
        String email = editTextEmail.getText().toString();
        String senha = editTextSenha.getText().toString();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha email e senha.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseAuth", "Registro bem-sucedido");
                        startMapsActivity();
                    } else {
                        Log.e("FirebaseAuth", "Erro ao registrar", task.getException());
                        Toast.makeText(this, "Erro no registro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginAnonimo() {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseAuth", "Login anônimo bem-sucedido");
                        startMapsActivity();
                    } else {
                        Log.e("FirebaseAuth", "Erro no login anônimo", task.getException());
                        Toast.makeText(this, "Erro no login anônimo", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startMapsActivity() {
        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }
}
