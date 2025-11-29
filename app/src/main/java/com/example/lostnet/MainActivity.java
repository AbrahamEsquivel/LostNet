package com.example.lostnet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Google Maps
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// Google Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

// Retrofit
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ImageView imgPreviewRef;
    private File archivoFotoFinal;
    private GoogleSignInClient mGoogleSignInClient;
    private LostNetApi api;
    private GoogleSignInAccount usuarioActual;
    private static final int RC_SIGN_IN = 9001;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if(imgPreviewRef != null) imgPreviewRef.setImageBitmap(photo);
                    // Guardar bitmap en archivo temporal para enviar
                    archivoFotoFinal = bitmapToFile(photo);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. CONFIGURAR RETROFIT (Backend P2)
        // ¡CAMBIAR IP POR LA DE ZEROTIER DEL APP-SERVER!
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.155.13.137:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(LostNetApi.class);

        // 2. CONFIGURAR GOOGLE AUTH
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 3. INICIAR MAPA
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 4. BOTONES
        findViewById(R.id.btnGoogle).setOnClickListener(v -> signIn());

        findViewById(R.id.btnReportar).setOnClickListener(v -> {
            if (usuarioActual == null) {
                Toast.makeText(this, "Primero inicia sesión", Toast.LENGTH_SHORT).show();
                signIn();
            } else {
                reportarUbicacionCentral();
            }
        });
    }

    // --- LÓGICA DEL MAPA ---
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Mover cámara a Aguascalientes (o donde quieran iniciar)
        LatLng aguascalientes = new LatLng(21.88, -102.29);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(aguascalientes, 14));

        // Cargar los reportes existentes (GET)
        cargarPinesDelServidor();
    }

    private void cargarPinesDelServidor() {
        api.obtenerReportes().enqueue(new Callback<List<ReporteModelo>>() {
            @Override
            public void onResponse(Call<List<ReporteModelo>> call, Response<List<ReporteModelo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mMap.clear(); // Limpiar para no duplicar
                    for (ReporteModelo reporte : response.body()) {
                        LatLng pos = new LatLng(reporte.latitude, reporte.longitude);
                        mMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(reporte.description)
                                .snippet("Usuario: " + reporte.email));
                    }
                }
            }
            @Override
            public void onFailure(Call<List<ReporteModelo>> call, Throwable t) {
                Log.e("LostNet", "Error cargando mapa: " + t.getMessage());
            }
        });
    }

    // --- LÓGICA DE REPORTE (POST) ---
    private void reportarUbicacionCentral() {
        // 1. Inflar el diseño del diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reporte, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // 2. Vincular vistas
        EditText etDesc = view.findViewById(R.id.etDescripcion);
        EditText etPreg = view.findViewById(R.id.etPregunta);
        EditText etResp = view.findViewById(R.id.etRespuesta);
        Spinner spinner = view.findViewById(R.id.spinnerCategoria);
        imgPreviewRef = view.findViewById(R.id.imgPreview);

        // Llenar Spinner
        String[] categorias = {"Electrónica", "Documentos", "Ropa", "Llaves", "Otros"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categorias);
        spinner.setAdapter(adapter);

        // 3. Botón Cámara
        view.findViewById(R.id.btnTomarFoto).setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        // 4. Botón Enviar (El Disparo Final)
        view.findViewById(R.id.btnEnviarFinal).setOnClickListener(v -> {
            LatLng centro = mMap.getCameraPosition().target;

            // Validar datos mínimos
            if(etDesc.getText().toString().isEmpty()) {
                etDesc.setError("Requerido"); return;
            }

            enviarDatosAlServidor(
                    etDesc.getText().toString(),
                    spinner.getSelectedItem().toString(),
                    etPreg.getText().toString(),
                    etResp.getText().toString(),
                    centro
            );
            dialog.dismiss();
        });

        dialog.show();
    }

    // --- FUNCIÓN DE ENVÍO RETROFIT (MULTIPART) ---
    private void enviarDatosAlServidor(String desc, String cat, String preg, String resp, LatLng gps) {
        Toast.makeText(this, "Subiendo reporte...", Toast.LENGTH_SHORT).show();

        // Crear partes de texto
        RequestBody idPart = RequestBody.create(MediaType.parse("text/plain"), usuarioActual.getId());
        RequestBody emailPart = RequestBody.create(MediaType.parse("text/plain"), usuarioActual.getEmail());
        RequestBody phonePart = RequestBody.create(MediaType.parse("text/plain"), "555-APP-REAL"); // Aquí podrías pedir el tel real
        RequestBody descPart = RequestBody.create(MediaType.parse("text/plain"), desc);
        RequestBody catPart = RequestBody.create(MediaType.parse("text/plain"), cat);
        RequestBody latPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(gps.latitude));
        RequestBody lonPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(gps.longitude));
        RequestBody pregPart = RequestBody.create(MediaType.parse("text/plain"), preg);
        RequestBody respPart = RequestBody.create(MediaType.parse("text/plain"), resp);

        // Crear parte de la foto
        MultipartBody.Part fotoPart = null;
        if (archivoFotoFinal != null) {
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), archivoFotoFinal);
            fotoPart = MultipartBody.Part.createFormData("foto", archivoFotoFinal.getName(), requestFile);
        }

        // Ejecutar
        api.enviarReporteCompleto(idPart, emailPart, phonePart, descPart, catPart, latPart, lonPart, pregPart, respPart, fotoPart)
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        // 1. FORZAR HILO PRINCIPAL (UI THREAD)
                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "✅ ¡Reporte Publicado!", Toast.LENGTH_LONG).show();

                                // 2. AGREGAR EL PIN MANUALMENTE (Para verlo al instante)
                                LatLng pos = mMap.getCameraPosition().target;
                                mMap.addMarker(new MarkerOptions()
                                        .position(pos)
                                        .title("Tu Nuevo Reporte")
                                        .snippet("Acabas de subir esto"));

                                // 3. RECARGAR DEL SERVIDOR (Opcional, para asegurar)
                                cargarPinesDelServidor();

                            } else {
                                Toast.makeText(MainActivity.this, "❌ Error Servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        // TAMBIÉN AQUÍ
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "💀 Fallo red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    // --- UTILIDAD: CONVERTIR BITMAP A ARCHIVO ---
    private File bitmapToFile(Bitmap bitmap) {
        try {
            File f = new File(getCacheDir(), "foto_temp.jpg");
            f.createNewFile();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] bitmapdata = bos.toByteArray();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            return f;
        } catch (Exception e) { return null; }
    }


    // --- LÓGICA DE LOGIN GOOGLE ---
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                usuarioActual = task.getResult(ApiException.class);
                Toast.makeText(this, "Hola: " + usuarioActual.getDisplayName(), Toast.LENGTH_SHORT).show();
                // Ocultar botón de login o cambiar texto
                Button btn = findViewById(R.id.btnGoogle);
                btn.setText("👤 " + usuarioActual.getDisplayName());
            } catch (ApiException e) {
                Log.w("LostNet", "Login fallido code=" + e.getStatusCode());
            }
        }
    }
}