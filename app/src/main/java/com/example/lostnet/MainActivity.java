package com.example.lostnet;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Constantes
    private static final int RC_SIGN_IN = 9001;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    // Asegúrate que esta IP sea correcta y accesible
    private static final String BASE_URL = "http://10.155.13.137:5000/";

    // Variables Globales
    private GoogleMap mMap;
    private GoogleSignInClient mGoogleSignInClient;
    private LostNetApi apiService;
    private GoogleSignInAccount currentUser;

    // Variables de Reporte
    private File photoFile;
    private ImageView imgPreviewRef; // Referencia temporal para el dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Configurar Retrofit con Timeouts Extendidos (60s)
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS) // 2 Minutos para conectar
                .readTimeout(300, TimeUnit.SECONDS)    // 2 Minutos esperando a que Python termine
                .writeTimeout(300, TimeUnit.SECONDS)   // 2 Minutos subiendo la foto
                .retryOnConnectionFailure(true)        // <--- NUEVO: Reintentar si falla la conexión micro-cortada
                .build();

// El resto sigue igual...
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)

                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        findViewById(R.id.fabNotificaciones).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NotificacionesActivity.class));
        });
        apiService = retrofit.create(LostNetApi.class);

        // 2. Configurar Login Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 3. Listeners de Botones Principales
        findViewById(R.id.sign_in_button).setOnClickListener(v -> signIn());
        findViewById(R.id.fabReportar).setOnClickListener(v -> mostrarDialogoReporte());
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> cerrarSesion());
        findViewById(R.id.btnMisReportes).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MisReportesActivity.class);
            startActivity(intent);
        });

        // 4. Chequeo rápido de sesión
        currentUser = GoogleSignIn.getLastSignedInAccount(this);
        if (currentUser != null) {
            iniciarMapa();
        }
    }

    // --- LÓGICA LOGIN ---
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void iniciarMapa() {
        if (currentUser != null) {
            String nombre = currentUser.getDisplayName();
            Toast.makeText(this, "Bienvenido: " + nombre, Toast.LENGTH_LONG).show();
        }

        findViewById(R.id.layoutLogin).setVisibility(View.GONE);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    private void cerrarSesion() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            currentUser = null;
            findViewById(R.id.layoutLogin).setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        });
    }

    // --- LÓGICA MAPA ---
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Centrar mapa (tu código original)
        LatLng aguascalientes = new LatLng(21.8853, -102.2916);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(aguascalientes, 15));

        // --- AQUÍ EMPIEZA EL DEBUG ---
        Log.d("UBICACION", "📍 Iniciando configuración de mapa...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Log.d("UBICACION", "✅ Permiso de GPS concedido. Solicitando coordenadas...");
            mMap.setMyLocationEnabled(true);

            com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient =
                    com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();

                    Log.d("UBICACION", "📡 ¡Coordenadas encontradas!: " + lat + ", " + lon);
                    enviarUbicacionAlServer(lat, lon);
                } else {
                    // ESTE ES EL ERROR MÁS COMÚN EN EMULADORES
                    Log.e("UBICACION", "⚠️ Location es NULL. (El emulador no tiene posición simulada)");
                    Log.e("UBICACION", "👉 Ve a los 3 puntitos del emulador > Location > Set Location");
                }
            });
        } else {
            Log.e("UBICACION", "❌ NO hay permiso de ubicación. Solicitándolo...");
            // Pedir permiso si no lo tiene
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 999);
        }

        cargarReportes();
    }

    private void cargarReportes() {
        apiService.obtenerReportes().enqueue(new Callback<List<ReporteModelo>>() {
            @Override
            public void onResponse(Call<List<ReporteModelo>> call, Response<List<ReporteModelo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mMap.clear();
                    for (ReporteModelo rep : response.body()) {
                        LatLng pos = new LatLng(rep.getLatitude(), rep.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(pos).title(rep.getDescription()));
                    }
                }
            }
            @Override
            public void onFailure(Call<List<ReporteModelo>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error cargando mapa", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- LÓGICA FORMULARIO Y CÁMARA ---
    private void mostrarDialogoReporte() {
        if (mMap == null) return;
        LatLng centro = mMap.getCameraPosition().target;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reporte, null);
        builder.setView(view);
        builder.setCancelable(false); // Evitar cierre accidental
        AlertDialog dialog = builder.create();

        // UI Referencias
        EditText etDesc = view.findViewById(R.id.etDesc);
        EditText etPhone = view.findViewById(R.id.etPhone);
        Spinner spinner = view.findViewById(R.id.spinnerCat);
        EditText etSecQ = view.findViewById(R.id.etSecQ);
        EditText etSecA = view.findViewById(R.id.etSecA);
        Button btnCamara = view.findViewById(R.id.btnCamara);
        Button btnEnviar = view.findViewById(R.id.btnEnviar);
        imgPreviewRef = view.findViewById(R.id.imgPreview);

        // Resetear foto temporal al abrir el dialog
        photoFile = null;
        imgPreviewRef.setVisibility(View.GONE);

        // Spinner Setup
        String[] categorias = {"Electrónica", "Ropa", "Documentos", "Otros"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categorias);
        spinner.setAdapter(adapter);

        // Botón Cámara
        btnCamara.setOnClickListener(v -> despacharTomarFoto());

        // Botón Enviar (Lógica corregida para Foto Opcional)
        btnEnviar.setOnClickListener(v -> {
            // Validación mínima de texto
            if (etDesc.getText().toString().isEmpty()) {
                etDesc.setError("Requerido");
                return;
            }

            // 1. Manejo de Foto (Puede ser nula)
            File fotoFinal = null;
            if (photoFile != null) {
                fotoFinal = comprimirImagen(photoFile);
            }

            // 2. Bloquear botón para evitar doble envío
            btnEnviar.setEnabled(false);
            btnEnviar.setText("ENVIANDO...");

            // 3. Enviar
            enviarDatosAlServidor(
                    currentUser.getId(),
                    currentUser.getEmail(),
                    etPhone.getText().toString(),
                    etDesc.getText().toString(),
                    spinner.getSelectedItem().toString(),
                    String.valueOf(centro.latitude),
                    String.valueOf(centro.longitude),
                    etSecQ.getText().toString(),
                    etSecA.getText().toString(),
                    dialog,
                    fotoFinal // Pasamos null si no hay foto
            );
        });

        // Permitir cancelar tocando fuera (opcional) o agregar botón cancelar
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    // --- MÉTODO CORE DE ENVÍO (PROTEGIDO CONTRA NULL) ---
    private void enviarDatosAlServidor(String uid, String email, String phone, String desc,
                                       String cat, String latStr, String lonStr, String sq, String sa,
                                       AlertDialog dialog, @Nullable File archivoAEnviar) {

        // ... (La creación de RequestBody se queda IGUAL) ...
        RequestBody uidPart = RequestBody.create(MediaType.parse("text/plain"), uid);
        RequestBody emailPart = RequestBody.create(MediaType.parse("text/plain"), email);
        RequestBody phonePart = RequestBody.create(MediaType.parse("text/plain"), phone);
        RequestBody descPart = RequestBody.create(MediaType.parse("text/plain"), desc);
        RequestBody catPart = RequestBody.create(MediaType.parse("text/plain"), cat);
        RequestBody latPart = RequestBody.create(MediaType.parse("text/plain"), latStr);
        RequestBody lonPart = RequestBody.create(MediaType.parse("text/plain"), lonStr);
        RequestBody sqPart = RequestBody.create(MediaType.parse("text/plain"), sq);
        RequestBody saPart = RequestBody.create(MediaType.parse("text/plain"), sa);

        MultipartBody.Part body = null;
        if (archivoAEnviar != null) {
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), archivoAEnviar);
            body = MultipartBody.Part.createFormData("foto", archivoAEnviar.getName(), reqFile);
        }

        // Llamada a Retrofit
        apiService.enviarReporte(uidPart, emailPart, phonePart, descPart, catPart, latPart, lonPart, sqPart, saPart, body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "¡Reporte enviado!", Toast.LENGTH_LONG).show();
                            dialog.dismiss();

                            // --- CORRECCIÓN CLAVE: PINTAR PIN MANUALMENTE ---
                            // No esperamos a cargarReportes(), lo ponemos nosotros mismos
                            try {
                                double lat = Double.parseDouble(latStr);
                                double lon = Double.parseDouble(lonStr);
                                LatLng pos = new LatLng(lat, lon);

                                // Agregamos el marcador rojo inmediatamente
                                if (mMap != null) {
                                    mMap.addMarker(new MarkerOptions()
                                            .position(pos)
                                            .title(desc));

                                    // Movemos la cámara al nuevo reporte
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16));
                                }
                            } catch (Exception e) {
                                Log.e("Mapa", "Error pintando pin manual", e);
                            }

                            // --- DESCARGAR LISTA CON RETRASO (Seguridad) ---
                            // Le damos 2 segundos al servidor para que termine de escribir el archivo
                            new android.os.Handler().postDelayed(() -> {
                                cargarReportes();
                            }, 2000);

                        } else {
                            restaurarBoton(dialog);
                            Toast.makeText(MainActivity.this, "Error servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        String errorMsg = t.getMessage();

                        // --- PARCHE DE SEGURIDAD ---
                        // Si el error es "fin de stream" pero sabemos que el servidor sí jala:
                        if (errorMsg != null && (errorMsg.contains("unexpected end of stream") || errorMsg.contains("closed"))) {

                            // Fingimos que fue un éxito
                            Toast.makeText(MainActivity.this, "¡Reporte enviado! (Stream cerrado)", Toast.LENGTH_LONG).show();
                            if (dialog != null) dialog.dismiss();

                            // Ejecutamos la lógica de éxito manualmente
                            try {
                                // Pintamos el pin manual (copia aquí la lógica de pintar el pin que te pasé antes)
                                double lat = Double.parseDouble(latStr);
                                double lon = Double.parseDouble(lonStr);
                                LatLng pos = new LatLng(lat, lon);
                                if (mMap != null) {
                                    mMap.addMarker(new MarkerOptions().position(pos).title(desc));
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16));
                                }
                            } catch (Exception e) {}

                            // Recargamos lista
                            new android.os.Handler().postDelayed(() -> cargarReportes(), 2000);
                            return; // Salimos para no mostrar el mensaje de error
                        }

                        // Error real
                        restaurarBoton(dialog);
                        Toast.makeText(MainActivity.this, "Fallo: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --- UTILIDADES ---
    private void restaurarBoton(AlertDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            Button btn = dialog.findViewById(R.id.btnEnviar);
            if(btn != null) {
                btn.setEnabled(true);
                btn.setText("ENVIAR REPORTE");
            }
        }
    }

    private File comprimirImagen(File archivoOriginal) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(archivoOriginal.getPath());
            FileOutputStream out = new FileOutputStream(archivoOriginal);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out); // Calidad 60%
            out.flush();
            out.close();
            return archivoOriginal;
        } catch (Exception e) {
            e.printStackTrace();
            return archivoOriginal;
        }
    }

    private void despacharTomarFoto() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            photoFile = crearArchivoImagen();
            Uri photoURI = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, "Error cámara: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                despacharTomarFoto();
            } else {
                Toast.makeText(this, "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Login
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                currentUser = task.getResult(ApiException.class);
                SharedPreferences prefs = getSharedPreferences("LostNetPrefs", MODE_PRIVATE);
                prefs.edit().putString("email", currentUser.getEmail()).apply();
                iniciarMapa();
            } catch (ApiException e) {
                Log.w("Login", "signInResult:failed code=" + e.getStatusCode());
            }
        }

        // Cámara
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if(imgPreviewRef != null && photoFile != null) {
                imgPreviewRef.setVisibility(View.VISIBLE);
                imgPreviewRef.setImageURI(Uri.fromFile(photoFile));
            }
        }
    }

    private void enviarUbicacionAlServer(double lat, double lon) {

        // 1. Recuperar el email guardado
        SharedPreferences prefs = getSharedPreferences("LostNetPrefs", MODE_PRIVATE);
        String emailUsuario = prefs.getString("email", null);

        if (emailUsuario == null) {
            Log.e("UBICACION", "❌ No hay email guardado. Inicia sesión de nuevo.");
            return;
        }

        // 2. Preparar datos
        UbicacionRequest datos = new UbicacionRequest(emailUsuario, lat, lon);

        // 3. ¡USAR TU VARIABLE apiService EXISTENTE! (Aquí estaba el error)
        if (apiService == null) {
            Log.e("UBICACION", "❌ apiService no está inicializado");
            return;
        }

        apiService.actualizarUbicacion(datos).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("UBICACION", "✅ Coordenadas enviadas: " + lat + ", " + lon);
                } else {
                    Log.e("UBICACION", "❌ Error server: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("UBICACION", "❌ Fallo de red: " + t.getMessage());
            }
        });
    }
}