package com.example.lostnet;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    public static final String EXTRA_PHOTO_URL = "EXTRA_PHOTO_URL";

    private ImageView imgAvatarPublico;
    private TextView txtNombrePublico;
    private TextView txtEmailPublico;
    private TextView txtNivelHonor;
    private TextView txtConfianzaPorcentaje;
    private ProgressBar progressConfianza;
    private TextView txtReportesExitosos;
    private TextView txtDevoluciones;
    private TextView txtAntiguedad;
    private TextView badgeLogro1;
    private TextView badgeLogro2;
    private TextView badgeLogro3;
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
        String photoUrl = getIntent().getStringExtra(EXTRA_PHOTO_URL);

        configurarListeners();
        cargarPerfilUsuario(userId, email, photoUrl);
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
        badgeLogro1 = findViewById(R.id.badgeLogro1);
        badgeLogro2 = findViewById(R.id.badgeLogro2);
        badgeLogro3 = findViewById(R.id.badgeLogro3);
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

    private void cargarPerfilUsuario(String userId, String email, String photoUrl) {
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

        // Cargar imagen de avatar (Si el usuario tiene foto usa esa, si no, usa el fallback generado)
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_menu_camera)
                    .circleCrop()
                    .into(imgAvatarPublico);
        } else {
            Glide.with(this)
                    .load("https://ui-avatars.com/api/?name=" + nombreUsuario + "&background=00B894&color=fff&size=256")
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_menu_camera)
                    .circleCrop()
                    .into(imgAvatarPublico);
        }

        // Honor y estadísticas reales calculados por el servidor
        mostrarCargando();
        if (email != null && !email.isEmpty() && !email.contains("*") && apiService != null) {
            apiService.obtenerPerfilPublico(email).enqueue(new Callback<PerfilPublicoModelo>() {
                @Override
                public void onResponse(Call<PerfilPublicoModelo> call, Response<PerfilPublicoModelo> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        aplicarGamificacion(response.body());
                    } else {
                        Log.w("PerfilPublico", "Perfil no disponible: HTTP " + response.code());
                        mostrarSinDatos();
                    }
                }

                @Override
                public void onFailure(Call<PerfilPublicoModelo> call, Throwable t) {
                    Log.e("PerfilPublico", "Error al obtener el perfil público: " + t.getMessage());
                    mostrarSinDatos();
                }
            });
        } else {
            mostrarSinDatos();
        }
    }

    private void mostrarCargando() {
        txtNivelHonor.setText("Calculando honor…");
        txtConfianzaPorcentaje.setText("…");
        progressConfianza.setProgress(0);
        txtReportesExitosos.setText("…");
        txtDevoluciones.setText("…");
        txtAntiguedad.setText("…");
        marcarLogros(null);
    }

    /** Sin conexión o sin datos: no se muestran cifras inventadas. */
    private void mostrarSinDatos() {
        txtNivelHonor.setText(LostNetUtils.textoNivelHonor("BUSCADOR"));
        txtNivelHonor.setBackgroundResource(R.drawable.bg_badge_buscador);
        txtConfianzaPorcentaje.setText("—");
        progressConfianza.setProgress(0);
        txtReportesExitosos.setText("—");
        txtDevoluciones.setText("—");
        txtAntiguedad.setText("—");
        marcarLogros(null);
    }

    private void aplicarGamificacion(PerfilPublicoModelo perfil) {
        String rol = perfil.getRol();
        int badgeBackgroundRes = R.drawable.bg_badge_buscador;
        if (rol.equalsIgnoreCase("SABUESO")) {
            badgeBackgroundRes = R.drawable.bg_badge_sabueso;
        } else if (rol.equalsIgnoreCase("PERDEDOR")) {
            badgeBackgroundRes = R.drawable.bg_badge_perdedor;
        }

        txtNivelHonor.setText(LostNetUtils.textoNivelHonor(rol));
        txtNivelHonor.setBackgroundResource(badgeBackgroundRes);

        int confianza = perfil.getConfianza();
        txtConfianzaPorcentaje.setText(confianza + "%");
        progressConfianza.setProgress(confianza);

        txtReportesExitosos.setText(String.valueOf(perfil.getReportesPublicados()));
        txtDevoluciones.setText(String.valueOf(perfil.getObjetosEncontrados()));
        txtAntiguedad.setText(LostNetUtils.textoAntiguedad(perfil.getDiasEnLostnet()));
        marcarLogros(perfil);
    }

    /** Los logros desbloqueados se ven completos; los bloqueados, atenuados. */
    private void marcarLogros(PerfilPublicoModelo perfil) {
        aplicarLogro(badgeLogro1, perfil != null && perfil.tieneLogro("SABUESO"));
        aplicarLogro(badgeLogro2, perfil != null && perfil.tieneLogro("DEVOLVEDOR"));
        aplicarLogro(badgeLogro3, perfil != null && perfil.tieneLogro("VERIFICADO"));
    }

    private void aplicarLogro(View badge, boolean desbloqueado) {
        if (badge != null) badge.setAlpha(desbloqueado ? 1f : 0.3f);
    }
}
