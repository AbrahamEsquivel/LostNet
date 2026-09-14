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

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Muestra la lista de reportes creados por el usuario activo.
 * Permite eliminar reportes y navegar al mapa para ver la ubicación de cada uno.
 */
public class MisReportesActivity extends AppCompatActivity {

    private RecyclerView recyclerMisReportes;
    private ReportesAdapter adapter;
    private List<ReporteModelo> misReportesList;
    private LostNetApi apiService;
    private String myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_reportes);

        recyclerMisReportes = findViewById(R.id.recyclerMisReportes);
        recyclerMisReportes.setLayoutManager(new LinearLayoutManager(this));
        misReportesList = new ArrayList<>();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            myUserId = account.getId();
        }

        apiService = RetrofitClient.getApiService();
        cargarMisReportes();
    }

    private void cargarMisReportes() {
        if (apiService == null) return;

        apiService.obtenerReportes().enqueue(new Callback<List<ReporteModelo>>() {
            @Override
            public void onResponse(Call<List<ReporteModelo>> call, Response<List<ReporteModelo>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(MisReportesActivity.this,
                            "Error servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                misReportesList.clear();
                for (ReporteModelo reporte : response.body()) {
                    if (reporte.getUserId() != null && reporte.getUserId().equals(myUserId)) {
                        misReportesList.add(reporte);
                    }
                }

                adapter = new ReportesAdapter(MisReportesActivity.this, misReportesList, new ReportesAdapter.OnItemClickListener() {
                    @Override
                    public void onEliminarClick(String idReporte, int position) {
                        eliminarReporte(idReporte, position);
                    }

                    @Override
                    public void onItemClick(ReporteModelo reporte) {
                        navegarAlMapa(reporte);
                    }
                });

                recyclerMisReportes.setAdapter(adapter);

                if (misReportesList.isEmpty()) {
                    Toast.makeText(MisReportesActivity.this,
                            "No tienes reportes activos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ReporteModelo>> call, Throwable t) {
                Toast.makeText(MisReportesActivity.this,
                        "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void eliminarReporte(String idReporte, int position) {
        apiService.borrarReporte(idReporte).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MisReportesActivity.this,
                            "Reporte eliminado", Toast.LENGTH_SHORT).show();
                    if (position >= 0 && position < misReportesList.size()) {
                        misReportesList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, misReportesList.size());
                    }
                } else {
                    Toast.makeText(MisReportesActivity.this,
                            "Error al borrar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MisReportesActivity.this,
                        "Fallo de red al borrar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navegarAlMapa(ReporteModelo reporte) {
        Intent intent = new Intent(MisReportesActivity.this, MainActivity.class);
        intent.putExtra("LAT_DESTINO", reporte.getLatitude());
        intent.putExtra("LON_DESTINO", reporte.getLongitude());
        intent.putExtra("ID_DESTINO",  reporte.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}