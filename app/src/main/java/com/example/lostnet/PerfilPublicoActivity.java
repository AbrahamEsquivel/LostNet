package com.example.lostnet;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de Perfil Público de Usuario orientada a la confianza comunitaria y gamificación.
 * Permite visualizar el Nivel de Honor, estadísticas y reputación en LostNet.
 */
public class PerfilPublicoActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    public static final String EXTRA_EMAIL = "EXTRA_EMAIL";

    private ImageView imgAvatarPublico;
    private TextView txtNombrePublico;
    private TextView txtEmailPublico;
    private TextView txtNivelHonor;
    private TextView txtConfianzaPorcentaje;
    private ProgressBar progressConfianza;
    private TextView txtReportesExitosos;
    private TextView txtDevoluciones;
    private TextView txtAntiguedad;
    private ImageButton btnVolver;
    private Button btnCerrarPerfil;

    private LostNetApi apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_publico);

        inicializarVistas();

        apiService = RetrofitClient.getApiService();

        // Obtener extras de la intención
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        String email = getIntent().getStringExtra(EXTRA_EMAIL);

        configurarListeners();
        cargarPerfilUsuario(userId, email);
    }

    private void inicializarVistas() {
        imgAvatarPublico = findViewById(R.id.imgAvatarPublico);
        txtNombrePublico = findViewById(R.id.txtNombrePublico);
        txtEmailPublico = findViewById(R.id.txtEmailPublico);
        txtNivelHonor = findViewById(R.id.txtNivelHonor);
        txtConfianzaPorcentaje = findViewById(R.id.txtConfianzaPorcentaje);
        progressConfianza = findViewById(R.id.progressConfianza);
        txtReportesExitosos = findViewById(R.id.txtReportesExitosos);
        txtDevoluciones = findViewById(R.id.txtDevoluciones);
        txtAntiguedad = findViewById(R.id.txtAntiguedad);
        btnVolver = findViewById(R.id.btnVolver);
        btnCerrarPerfil = findViewById(R.id.btnCerrarPerfil);
    }

    private void configurarListeners() {
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }
        if (btnCerrarPerfil != null) {
            btnCerrarPerfil.setOnClickListener(v -> finish());
        }
    }

    private void cargarPerfilUsuario(String userId, String email) {
        String correoMostrar = (email != null && !email.isEmpty()) ? email : "usuario@lostnet.com";
        String correoEnmascarado = LostNetUtils.enmascararEmail(correoMostrar);

        txtEmailPublico.setText(correoEnmascarado);

        // Nombre de usuario derivado del correo
        String nombreUsuario = "Usuario LostNet";
        if (correoMostrar.contains("@")) {
            nombreUsuario = correoMostrar.split("@")[0];
            if (nombreUsuario.length() > 1) {
                nombreUsuario = nombreUsuario.substring(0, 1).toUpperCase() + nombreUsuario.substring(1);
            }
        }
        txtNombrePublico.setText(nombreUsuario);

        // Cargar imagen de avatar predeterminada o avatar simulado de alta calidad
        Glide.with(this)
                .load("https://ui-avatars.com/api/?name=" + nombreUsuario + "&background=6C63FF&color=fff&size=256")
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_camera)
                .circleCrop()
                .into(imgAvatarPublico);

        // Consultar el tipo de usuario (gamificación) al backend si tenemos el correo
        if (email != null && !email.isEmpty() && apiService != null) {
            apiService.obtenerTipoUsuario(email).enqueue(new Callback<UsuarioGamificacion>() {
                @Override
                public void onResponse(Call<UsuarioGamificacion> call, Response<UsuarioGamificacion> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String tipo = response.body().getTipo();
                        aplicarGamificacion(tipo);
                    } else {
                        aplicarGamificacion("BUSCADOR");
                    }
                }

                @Override
                public void onFailure(Call<UsuarioGamificacion> call, Throwable t) {
                    Log.e("PerfilPublico", "Error al obtener tipo de usuario: " + t.getMessage());
                    aplicarGamificacion("BUSCADOR");
                }
            });
        } else {
            aplicarGamificacion("BUSCADOR");
        }
    }

    private void aplicarGamificacion(String tipo) {
        if (tipo == null) tipo = "BUSCADOR";

        String nivelHonorText;
        int badgeBackgroundRes = R.drawable.bg_badge_buscador;
        int porcentajeConfianza = 95;
        int reportesExitosos = 10;
        int devoluciones = 6;
        String antiguedad = "4m";

        if (tipo.equalsIgnoreCase("SABUESO")) {
            nivelHonorText = "🕵️‍♂️ Sabueso Comunitario (Héroe Local)";
            badgeBackgroundRes = R.drawable.bg_badge_sabueso;
            porcentajeConfianza = 99;
            reportesExitosos = 24;
            devoluciones = 18;
            antiguedad = "1 año";
        } else if (tipo.equalsIgnoreCase("PERDEDOR")) {
            nivelHonorText = "🤦‍♂️ En búsqueda activa (Novato)";
            badgeBackgroundRes = R.drawable.bg_badge_perdedor;
            porcentajeConfianza = 88;
            reportesExitosos = 3;
            devoluciones = 1;
            antiguedad = "1m";
        } else {
            nivelHonorText = "🔍 Buscador Confiable";
            badgeBackgroundRes = R.drawable.bg_badge_buscador;
            porcentajeConfianza = 95;
            reportesExitosos = 12;
            devoluciones = 8;
            antiguedad = "6m";
        }

        txtNivelHonor.setText(nivelHonorText);
        txtNivelHonor.setBackgroundResource(badgeBackgroundRes);

        txtConfianzaPorcentaje.setText(porcentajeConfianza + "%");
        progressConfianza.setProgress(porcentajeConfianza);

        txtReportesExitosos.setText(String.valueOf(reportesExitosos));
        txtDevoluciones.setText(String.valueOf(devoluciones));
        txtAntiguedad.setText(antiguedad);
    }
}
