package com.example.lostnet;

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

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private AlertasAdapter adapter;
    private List<AlertaModelo> listaAlertas;

    // CAMBIA ESTO SI TU IP ES DIFERENTE
    private static final String BASE_URL = "http://10.155.13.137:5000/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones); // ¡Ahora sí existe!

        recycler = findViewById(R.id.recyclerAlertas); // ¡Ahora sí existe!
        recycler.setLayoutManager(new LinearLayoutManager(this));
        listaAlertas = new ArrayList<>();

        // Configuración Retrofit
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        LostNetApi api = retrofit.create(LostNetApi.class);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            cargarAlertas(api, account.getEmail());
        } else {
            Toast.makeText(this, "No se identificó el usuario", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarAlertas(LostNetApi api, String email) {
        api.obtenerMisAlertas(email).enqueue(new Callback<List<AlertaModelo>>() {
            @Override
            public void onResponse(Call<List<AlertaModelo>> call, Response<List<AlertaModelo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaAlertas = response.body();
                    adapter = new AlertasAdapter(listaAlertas);
                    recycler.setAdapter(adapter);

                    if(listaAlertas.isEmpty()){
                        Toast.makeText(NotificacionesActivity.this, "Sin notificaciones nuevas", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(NotificacionesActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AlertaModelo>> call, Throwable t) {
                Toast.makeText(NotificacionesActivity.this, "Error red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}