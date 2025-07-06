package com.example.taquarialerta;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maps);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configura os botões
        Button btnDoacao = findViewById(R.id.buttonDoacao);
        Button btnAjuda = findViewById(R.id.buttonAjuda);
        Button btnAlagado = findViewById(R.id.buttonAlagado);

        btnDoacao.setOnClickListener(v ->
                Toast.makeText(this, "Botão Doação clicado!", Toast.LENGTH_SHORT).show());

        btnAjuda.setOnClickListener(v ->
                Toast.makeText(this, "Botão Ajuda clicado!", Toast.LENGTH_SHORT).show());

        btnAlagado.setOnClickListener(v ->
                Toast.makeText(this, "Botão Alagado clicado!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (checkLocationPermission()) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Chama a API da ANA ao iniciar o mapa
        consultarNivelRio();

        // Carrega alertas salvos do Firestore
        carregarAlertasDoFirestore();

        // Permite clicar no mapa para marcar um alerta
        mMap.setOnMapClickListener(this::abrirDialogAlerta);
    }

    private void consultarNivelRio() {
        String identificador = "03437037005";
        String senha = "t9tnzz7q";
        String codigoEstacao = "87450100";

        HidroWebApi.obterToken(identificador, senha, new HidroWebApi.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                Log.d("HidroWebApi", "Token recebido: " + token);
                HidroWebApi.consultarDados(token, codigoEstacao, new HidroWebApi.DataCallback() {
                    @Override
                    public void onDataReceived(String json) {
                        Log.d("HidroWebApi", "Dados recebidos: " + json);
                        runOnUiThread(() -> atualizarNivelNaTela(json));
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("HidroWebApi", "Erro ao consultar dados", e);
                        runOnUiThread(() -> Toast.makeText(MapsActivity.this, "Erro consultando nível do rio", Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("HidroWebApi", "Erro ao obter token", e);
                runOnUiThread(() -> Toast.makeText(MapsActivity.this, "Erro autenticando na API", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void atualizarNivelNaTela(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray items = obj.getJSONArray("items");
            if (items.length() > 0) {
                JSONObject dado = items.getJSONObject(0);
                double cota = dado.getDouble("valor");
                String data = dado.getString("dataHora");
                TextView textNivelRio = findViewById(R.id.textNivelRio);
                textNivelRio.setText(String.format("Nível atual: %.2f m\n(%s)", cota, data));

                // Ajusta cor do fundo baseado no nível
                if (cota <= 3.0f) {
                    textNivelRio.setBackgroundColor(0xAA2196F3); // azul
                } else if (cota > 3.0f && cota < 6.0f) {
                    textNivelRio.setBackgroundColor(0xAAFFFF00); // amarelo
                } else if (cota >= 6.0f) {
                    textNivelRio.setBackgroundColor(0xAAFF0000); // vermelho
                }
            } else {
                Toast.makeText(this, "Nenhum dado retornado da estação", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("HidroWebApi", "Erro processando JSON", e);
            Toast.makeText(this, "Erro processando dados do rio", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirDialogAlerta(LatLng latLng) {
        String[] opcoes = {"Alagado", "Ajuda", "Doação"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Selecione o tipo de alerta")
                .setItems(opcoes, (dialog, which) -> {
                    String tipoAlerta = opcoes[which];
                    salvarAlertaNoFirestore(latLng, tipoAlerta);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void salvarAlertaNoFirestore(LatLng latLng, String tipo) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> alerta = new HashMap<>();
        alerta.put("latitude", latLng.latitude);
        alerta.put("longitude", latLng.longitude);
        alerta.put("tipo", tipo);
        alerta.put("timestamp", FieldValue.serverTimestamp());

        db.collection("alertas")
                .add(alerta)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Alerta salvo!", Toast.LENGTH_SHORT).show();
                    adicionarMarcador(latLng, tipo);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao salvar alerta", Toast.LENGTH_SHORT).show();
                });
    }

    private void adicionarMarcador(LatLng latLng, String tipo) {
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(tipo)
                .snippet("Local marcado como " + tipo));
    }

    private void carregarAlertasDoFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("alertas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        String tipo = doc.getString("tipo");
                        if (lat != null && lng != null && tipo != null) {
                            adicionarMarcador(new LatLng(lat, lng), tipo);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao carregar alertas", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Você está aqui"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            }
        }
    }
}
