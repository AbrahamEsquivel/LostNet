package com.example.lostnet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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
 * Incluye:
 *  - Contador de caducidad (los reportes se eliminan automáticamente en 5 días).
 *  - Indicador del límite diario de 5 reportes.
 */
public class MisReportesActivity extends AppCompatActivity {

    private static final int LIMITE_DIARIO = 5;

    private RecyclerView recyclerMisReportes;
    private ReportesGmapsAdapter adapter;
    private List<ReporteModelo> misReportesList;
    private LostNetApi apiService;
    private String myUserId;

    /** Barra de límite diario (definida en el layout). */
    private TextView txtLimiteDiario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_reportes);

        recyclerMisReportes = findViewById(R.id.recyclerMisReportes);
        recyclerMisReportes.setLayoutManager(new LinearLayoutManager(this));
        misReportesList = new ArrayList<>();

        txtLimiteDiario = findViewById(R.id.txtLimiteDiario);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            myUserId = account.getId();
        }

        apiService = RetrofitClient.getApiService();
        cargarMisReportes();
    }

    // ─────────────────────────────────────────────────────────────
    //  Carga de reportes
    // ─────────────────────────────────────────────────────────────

    private void cargarMisReportes() {
        if (apiService == null || myUserId == null) return;

        // Endpoint dedicado que filtra por user_id en el servidor
        apiService.obtenerMisReportes(myUserId).enqueue(new Callback<List<ReporteModelo>>() {
            @Override
            public void onResponse(Call<List<ReporteModelo>> call, Response<List<ReporteModelo>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    cargarMisReportesFallback();
                    return;
                }
                procesarListaReportes(response.body());
            }

            @Override
            public void onFailure(Call<List<ReporteModelo>> call, Throwable t) {
                cargarMisReportesFallback();
            }
        });
    }

    /** Fallback: obtiene todos los reportes y filtra localmente por user_id. */
    private void cargarMisReportesFallback() {
        apiService.obtenerReportes().enqueue(new Callback<List<ReporteModelo>>() {
            @Override
            public void onResponse(Call<List<ReporteModelo>> call, Response<List<ReporteModelo>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(MisReportesActivity.this,
                            "Error servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }
                List<ReporteModelo> filtrados = new ArrayList<>();
                for (ReporteModelo r : response.body()) {
                    if (r.getUserId() != null && r.getUserId().equals(myUserId)) {
                        filtrados.add(r);
                    }
                }
                procesarListaReportes(filtrados);
            }

            @Override
            public void onFailure(Call<List<ReporteModelo>> call, Throwable t) {
                Toast.makeText(MisReportesActivity.this,
                        "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  Procesamiento y UI
    // ─────────────────────────────────────────────────────────────

    private void procesarListaReportes(List<ReporteModelo> reportes) {
        misReportesList.clear();
        misReportesList.addAll(reportes);

        adapter = new ReportesGmapsAdapter(this, misReportesList,
                reporte -> navegarAlMapa(reporte));
        adapter.setMostrarExpiry(true); // activa el countdown de 5 días
        recyclerMisReportes.setAdapter(adapter);

        actualizarBarraLimiteDiario();

        if (misReportesList.isEmpty()) {
            Toast.makeText(this, "No tienes reportes activos", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Muestra cuántos reportes envió el usuario hoy.
     * Un reporte creado hoy tiene secondsUntilExpiry > 4 días (>345,600 s),
     * porque el servidor asigna exactamente 5 días de vida al crear.
     * Si secondsUntilExpiry == 0 usamos expires_at_epoch como respaldo.
     */
    private void actualizarBarraLimiteDiario() {
        if (txtLimiteDiario == null) return;

        long ahoraSegundos = System.currentTimeMillis() / 1000L;
        // Umbral: reporte creado hoy → le quedan > 4 días
        final long CUATRO_DIAS_SEG = 4L * 24 * 60 * 60;

        int reportesHoy = 0;
        for (ReporteModelo r : misReportesList) {
            long segundosRestantes = r.getSecondsUntilExpiry();

            // Si el servidor no mandó secondsUntilExpiry, lo calculamos
            if (segundosRestantes <= 0 && r.getExpiresAtEpoch() > 0) {
                segundosRestantes = r.getExpiresAtEpoch() - ahoraSegundos;
            }

            if (segundosRestantes > CUATRO_DIAS_SEG) {
                reportesHoy++;
            }
        }

        txtLimiteDiario.setVisibility(View.VISIBLE);
        if (reportesHoy >= LIMITE_DIARIO) {
            txtLimiteDiario.setText(
                    "🚫 Límite diario alcanzado (" + LIMITE_DIARIO + "/" + LIMITE_DIARIO
                            + " reportes). Vuelve mañana.");
            txtLimiteDiario.setTextColor(0xFFD93025);
        } else {
            int restantes = LIMITE_DIARIO - reportesHoy;
            txtLimiteDiario.setText(
                    "📋 " + reportesHoy + "/" + LIMITE_DIARIO
                            + " reportes hoy  ·  Te quedan " + restantes);
            txtLimiteDiario.setTextColor(0xFF137333);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Eliminar reporte
    // ─────────────────────────────────────────────────────────────

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
                        actualizarBarraLimiteDiario();
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

    // ─────────────────────────────────────────────────────────────
    //  Navegación
    // ─────────────────────────────────────────────────────────────

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