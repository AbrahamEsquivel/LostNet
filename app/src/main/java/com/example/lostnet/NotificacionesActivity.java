package com.example.lostnet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private AlertasAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones); // Asegúrate que este XML tenga un RecyclerView

        // 1. Configurar RecyclerView
        recycler = findViewById(R.id.recyclerNotificaciones); // <--- OJO: En tu XML debe tener este ID
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // 2. Obtener email del usuario
        SharedPreferences prefs = getSharedPreferences("LostNetPrefs", MODE_PRIVATE);
        String email = prefs.getString("email", null);

        if (email != null) {
            cargarAlertas(email);
        } else {
            Toast.makeText(this, "Error: No hay sesión activa", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarAlertas(String email) {
        LostNetApi api = RetrofitClient.getRetrofitInstance().create(LostNetApi.class);

        api.obtenerAlertas(email).enqueue(new Callback<List<AlertaModelo>>() {
            @Override
            public void onResponse(Call<List<AlertaModelo>> call, Response<List<AlertaModelo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AlertaModelo> alertas = response.body();

                    // Si no hay alertas, avisar
                    if (alertas.isEmpty()) {
                        Toast.makeText(NotificacionesActivity.this, "No tienes nuevas alertas", Toast.LENGTH_SHORT).show();
                    }

                    // --- AQUÍ ESTÁ EL CAMBIO PARA REDIRECCIONAR ---
                    adapter = new AlertasAdapter(alertas, new AlertasAdapter.OnAlertaClickListener() {
                        @Override
                        public void onAlertaClick(AlertaModelo alerta) {
                            // 1. Preparamos el viaje al Mapa
                            Intent intent = new Intent(NotificacionesActivity.this, MainActivity.class);

                            // 2. Empacamos las coordenadas del objeto perdido
                            intent.putExtra("LAT_DESTINO", alerta.getLatObjeto());
                            intent.putExtra("LON_DESTINO", alerta.getLonObjeto());

                            // 3. Flags para volver a la pantalla principal sin crear duplicados
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                            startActivity(intent);
                            finish(); // Cerramos notificaciones para que no estorbe
                        }
                    });

                    recycler.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<AlertaModelo>> call, Throwable t) {
                Log.e("ALERTAS", "Error cargando: " + t.getMessage());
                Toast.makeText(NotificacionesActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}