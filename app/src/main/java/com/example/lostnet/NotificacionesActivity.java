package com.example.lostnet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Muestra la lista de alertas de proximidad del usuario activo.
 * Al tocar una alerta, navega al mapa centrando la cámara en el objeto reportado.
 */
public class NotificacionesActivity extends AppCompatActivity {

    private static final String TAG = "NotificacionesActivity";

    private RecyclerView recyclerAlertas;
    private AlertasAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        recyclerAlertas = findViewById(R.id.recyclerNotificaciones);
        recyclerAlertas.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        String email = prefs.getString(AppConstants.PREF_KEY_EMAIL, null);

        if (email != null) {
            cargarAlertas(email);
        } else {
            Toast.makeText(this, "Error: No hay sesión activa", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarAlertas(String email) {
        LostNetApi api = RetrofitClient.getApiService();

        api.obtenerAlertas(email).enqueue(new Callback<List<AlertaModelo>>() {
            @Override
            public void onResponse(Call<List<AlertaModelo>> call, Response<List<AlertaModelo>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<AlertaModelo> alertas = response.body();

                if (alertas.isEmpty()) {
                    Toast.makeText(NotificacionesActivity.this,
                            "No tienes nuevas alertas", Toast.LENGTH_SHORT).show();
                }

                adapter = new AlertasAdapter(alertas, alerta -> {
                    Intent intent = new Intent(NotificacionesActivity.this, MainActivity.class);
                    intent.putExtra("LAT_DESTINO", alerta.getLatObjeto());
                    intent.putExtra("LON_DESTINO", alerta.getLonObjeto());
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                });

                recyclerAlertas.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<List<AlertaModelo>> call, Throwable t) {
                Log.e(TAG, "Error cargando alertas: " + t.getMessage());
                Toast.makeText(NotificacionesActivity.this,
                        "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}