package com.example.taquarialerta;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // >>> Etapa 5: Inicializar o Firebase
        FirebaseApp.initializeApp(this);

        // >>> LOGS DE DEBUG DA CONFIGURAÇÃO DO FIREBASE
        FirebaseApp firebaseApp = FirebaseApp.getInstance();
        FirebaseOptions options = firebaseApp.getOptions();
        Log.d("FirebaseTest", "Firebase ProjectID: " + options.getProjectId());
        Log.d("FirebaseTest", "Firebase ApplicationID: " + options.getApplicationId());
        Log.d("FirebaseTest", "Firebase APIKey: " + options.getApiKey());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Pegando os botões pelo ID
        Button btnQueroAjudar = findViewById(R.id.buttonQueroAjudar);
        Button btnPrecisoAjuda = findViewById(R.id.buttonPrecisoAjuda);

        // Intent para MapsActivity
        Intent mapsIntent = new Intent(this, MapsActivity.class);

        // Definindo listeners para os botões
        btnQueroAjudar.setOnClickListener(v -> startActivity(mapsIntent));
        btnPrecisoAjuda.setOnClickListener(v -> startActivity(mapsIntent));

        // >>> TESTE FIRESTORE: login anônimo e gravação de teste
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseTest", "Login anônimo bem-sucedido");
                        testarFirestore();
                    } else {
                        Log.e("FirebaseTest", "Erro no login anônimo", task.getException());
                    }
                });
    }

    private void testarFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> dadosTeste = new HashMap<>();
        dadosTeste.put("mensagem", "Olá, Firestore!");
        dadosTeste.put("timestamp", FieldValue.serverTimestamp());

        db.collection("testes").document("testeSimples")
                .set(dadosTeste)
                .addOnSuccessListener(aVoid -> Log.d("FirebaseTest", "Documento gravado com sucesso!"))
                .addOnFailureListener(e -> Log.e("FirebaseTest", "Falha ao gravar documento", e));
    }
}
