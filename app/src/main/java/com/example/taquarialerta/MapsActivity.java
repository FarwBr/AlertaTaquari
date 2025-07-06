package com.example.taquarialerta;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
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

        consultarNivelRio();
        carregarAlertasDoFirestore();

        // Adiciona alerta ao clicar no mapa
        mMap.setOnMapClickListener(this::abrirDialogAlerta);

        // Permite remover marcador ao clicar com confirmação
        mMap.setOnMarkerClickListener(marker -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Remover alerta")
                    .setMessage("Deseja realmente remover este alerta?")
                    .setPositiveButton("Remover", (dialog, which) -> removerAlertaDoFirestore(marker.getPosition(), marker))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return true; // Consome o clique
        });
    }

    private void consultarNivelRio() {
        String identificador = "03437037005";
        String senha = "t9tnzz7q";
        String codigoEstacao = "86879300";

        HidroWebApi.obterToken(identificador, senha, new HidroWebApi.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                if (token == null || token.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(MapsActivity.this, "Token inválido ou não recebido", Toast.LENGTH_SHORT).show());
                    Log.e("HidroWebApi", "Token nulo ou vazio!");
                    return;
                }

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
                        runOnUiThread(() -> Toast.makeText(MapsActivity.this, "Erro consultando nível do rio: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("HidroWebApi", "Erro ao obter token", e);
                runOnUiThread(() -> Toast.makeText(MapsActivity.this, "Erro autenticando na API: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
    private void atualizarNivelNaTela(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray items = obj.getJSONArray("items");
            if (items.length() > 0) {
                JSONObject dado = items.getJSONObject(0);
                double cota = dado.getDouble("Cota_Adotada") / 100.0;

                String data = dado.getString("Data_Hora_Medicao");
                TextView textNivelRio = findViewById(R.id.textNivelRio);
                textNivelRio.setText(String.format("Nível atual: %.2f m\n(%s)", cota, data));

                if (cota <= 15.0f) {
                    textNivelRio.setBackgroundColor(0xAA2196F3); // azul
                } else if (cota > 15.0f && cota < 19.0f) {
                    textNivelRio.setBackgroundColor(0xAAFFFF00); // amarelo
                } else if (cota >= 19.0f) {
                    textNivelRio.setBackgroundColor(0xAAFF0000); // vermelho
                }
            } else {
                Toast.makeText(this, "Nenhum dado retornado da estação", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
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
                    Toast.makeText(this, "Alerta \"" + tipo + "\" adicionado!", Toast.LENGTH_SHORT).show();
                    adicionarMarcador(latLng, tipo);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao salvar alerta", Toast.LENGTH_SHORT).show());
    }

    private void removerAlertaDoFirestore(LatLng latLng, Marker marker) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("alertas")
                .whereEqualTo("latitude", latLng.latitude)
                .whereEqualTo("longitude", latLng.longitude)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot) {
                            db.collection("alertas").document(doc.getId()).delete();
                        }
                        marker.remove();
                        Toast.makeText(this, "Alerta removido!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Alerta não encontrado no banco.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao remover alerta do banco.", Toast.LENGTH_SHORT).show());
    }

    private void adicionarMarcador(LatLng latLng, String tipo) {
        int iconeRes;
        switch (tipo) {
            case "Alagado": iconeRes = R.drawable.ic_alagado; break;
            case "Ajuda": iconeRes = R.drawable.ic_ajuda; break;
            case "Doação": iconeRes = R.drawable.ic_doacao; break;
            default: Log.w("MapsActivity", "Tipo de alerta desconhecido: " + tipo); return;
        }

        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), iconeRes);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 48, 48, false);

        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(tipo)
                .snippet("Local marcado como " + tipo)
                .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap)));
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
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao carregar alertas", Toast.LENGTH_SHORT).show());
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
