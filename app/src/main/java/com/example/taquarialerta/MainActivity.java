package com.example.taquarialerta;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp; // << IMPORTANTE

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // >>> Etapa 5: Inicializar o Firebase
        FirebaseApp.initializeApp(this);

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
    }
}
