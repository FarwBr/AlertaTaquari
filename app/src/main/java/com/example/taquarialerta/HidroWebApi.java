package com.example.taquarialerta;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class HidroWebApi {

    private static final String BASE_URL = "https://www.ana.gov.br/hidrowebservice/EstacoesTelemetricas";
    private static final OkHttpClient client = new OkHttpClient();

    public interface TokenCallback {
        void onTokenReceived(String token);
        void onError(Exception e);
    }

    public interface DataCallback {
        void onDataReceived(String json);
        void onError(Exception e);
    }

    public static void obterToken(String identificador, String senha, TokenCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/OAUth/v1")
                .addHeader("Identificador", identificador)
                .addHeader("Senha", senha)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new Exception("HTTP erro: " + response.code()));
                    return;
                }
                String responseBody = response.body().string();
                Log.d("HidroWebApi", "Resposta token: " + responseBody);
                String token = parseTokenFromJson(responseBody);
                if (token != null) {
                    callback.onTokenReceived(token);
                } else {
                    callback.onError(new Exception("Token não encontrado na resposta"));
                }
            }
        });
    }

    public static void consultarDados(String token, String codigoEstacao, DataCallback callback) {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/HidroinfoanaSerieTelemetricaAdotada/v1")
                .newBuilder()
                .addQueryParameter("Código da Estação", codigoEstacao)
                .addQueryParameter("Tipo Filtro Data", "DATA_LEITURA")
                .addQueryParameter("Data de Busca (yyyy-MM-dd)", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()))
                .addQueryParameter("Range Intervalo de busca", "HORA_1")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new Exception("HTTP erro: " + response.code()));
                    return;
                }
                callback.onDataReceived(response.body().string());
            }
        });
    }

    private static String parseTokenFromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONObject items = obj.getJSONObject("items");
            return items.getString("tokenautenticacao");
        } catch (Exception e) {
            return null;
        }
    }
}
