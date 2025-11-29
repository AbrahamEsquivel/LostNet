package com.example.lostnet; // Asegúrate que coincida con tu paquete

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Imports de Google Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

// Imports de Retrofit
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private TextView tvEstado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEstado = findViewById(R.id.tvEstado);
        Button btnGoogle = findViewById(R.id.btnGoogle);

        // 1. Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 2. Acción del Botón
        btnGoogle.setOnClickListener(view -> signIn());
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // ¡LOGIN EXITOSO!
            String email = account.getEmail();
            String name = account.getDisplayName();
            String id = account.getId();

            tvEstado.setText("Hola, " + name + "\nEnviando reporte...");

            // 3. MANDAR REPORTE A LA API CON DATOS REALES
            enviarReporteAlBackend(id, email, name);

        } catch (ApiException e) {
            Log.w("LostNet", "Fallo Login Google code=" + e.getStatusCode());
            tvEstado.setText("Error de Login: " + e.getStatusCode());
        }
    }

    private void enviarReporteAlBackend(String userId, String email, String nombre) {
        // Configurar Retrofit (CAMBIA LA IP POR LA TUYA DE ZEROTIER)
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.155.13.137:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        LostNetApi api = retrofit.create(LostNetApi.class);

        // Datos Reales + Coordenadas Simuladas (Para que llegue el Whats)
        ReporteModelo datos = new ReporteModelo(
                userId,
                email,
                "555-REAL",
                "Objeto perdido por " + nombre,
                21.8725,
                -102.3406
        );

        api.enviarReporte(datos).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful()) {
                    tvEstado.setText("✅ Reporte Enviado Exitosamente\nCheca tu WhatsApp");
                } else {
                    tvEstado.setText("❌ Error Servidor: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                tvEstado.setText("💀 Fallo Conexión: " + t.getMessage());
            }
        });
    }
}