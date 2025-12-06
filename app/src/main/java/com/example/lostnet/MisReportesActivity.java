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
import java.util.concurrent.TimeUnit; // IMPORTANTE

import okhttp3.OkHttpClient; // IMPORTANTE
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MisReportesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReportesAdapter adapter;
    private List<ReporteModelo> misReportesList;
    private LostNetApi apiService;
    private String myUserId;

    // Asegúrate que esta IP sea la misma que en MainActivity
    private static final String BASE_URL = "http://10.155.13.137:5000/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_reportes);

        recyclerView = findViewById(R.id.recyclerMisReportes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        misReportesList = new ArrayList<>();

        // Obtener usuario actual
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            myUserId = account.getId();
        }

        // --- CORRECCIÓN CRÍTICA: IGUALAR LA PACIENCIA A 120 SEGUNDOS ---
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS) // 2 Minutos
                .readTimeout(120, TimeUnit.SECONDS)    // 2 Minutos
                .writeTimeout(120, TimeUnit.SECONDS)   // 2 Minutos
                .retryOnConnectionFailure(true)        // Reintentar si se corta
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient) // <--- Vinculamos el cliente potente aquí
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(LostNetApi.class);

        cargarMisReportes();
    }

    private void cargarMisReportes() {
        if (apiService == null) return;

        apiService.obtenerReportes().enqueue(new Callback<List<ReporteModelo>>() {
            @Override
            public void onResponse(Call<List<ReporteModelo>> call, Response<List<ReporteModelo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    misReportesList.clear();

                    // Filtrado: Solo mostramos mis reportes (ID coincidente)
                    for (ReporteModelo r : response.body()) {
                        if (r.getUserId() != null && r.getUserId().equals(myUserId)) {
                            misReportesList.add(r);
                        }
                    }

                    // --- AQUÍ CONECTAMOS EL CLIC PARA IR AL MAPA ---
                    adapter = new ReportesAdapter(misReportesList, new ReportesAdapter.OnItemClickListener() {

                        // Acción 1: Botón Eliminar
                        @Override
                        public void onEliminarClick(String idReporte, int position) {
                            eliminarReporte(idReporte, position);
                        }

                        // Acción 2: Clic en la tarjeta (Ir al mapa)
                        @Override
                        public void onItemClick(ReporteModelo reporte) {
                            Intent intent = new Intent(MisReportesActivity.this, MainActivity.class);

                            // Mandamos las coordenadas para que el mapa sepa a dónde volar
                            intent.putExtra("LAT_DESTINO", reporte.getLatitude());
                            intent.putExtra("LON_DESTINO", reporte.getLongitude());
                            intent.putExtra("ID_DESTINO", reporte.getId());

                            // Flags importantes:
                            // CLEAR_TOP: Si MainActivity ya estaba abierta abajo, borra lo que esté encima.
                            // SINGLE_TOP: Reutiliza la MainActivity existente en vez de crear una nueva.
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                            startActivity(intent);
                            finish(); // Opcional: Cierra la lista para ahorrar memoria
                        }
                    });

                    recyclerView.setAdapter(adapter);

                    if (misReportesList.isEmpty()) {
                        Toast.makeText(MisReportesActivity.this, "No tienes reportes activos", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MisReportesActivity.this, "Error servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ReporteModelo>> call, Throwable t) {
                Toast.makeText(MisReportesActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void eliminarReporte(String idReporte, int position) {
        apiService.borrarReporte(idReporte).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MisReportesActivity.this, "Reporte eliminado", Toast.LENGTH_SHORT).show();

                    // Actualizar lista visualmente
                    if (position >= 0 && position < misReportesList.size()) {
                        misReportesList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, misReportesList.size());
                    }
                } else {
                    Toast.makeText(MisReportesActivity.this, "Error al borrar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MisReportesActivity.this, "Fallo de red al borrar", Toast.LENGTH_SHORT).show();
            }
        });
    }
}