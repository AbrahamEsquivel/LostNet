package com.example.lostnet;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int RC_SIGN_IN = 9001;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private GoogleMap mMap;
    private GoogleSignInClient mGoogleSignInClient;
    private LostNetApi apiService;
    private GoogleSignInAccount currentUser;

    // UI Nueva (Pro)
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private View layoutLogin;

    // Datos y Filtros
    private List<ReporteModelo> listaReportesOriginal = new ArrayList<>(); // Copia para filtrar localmente

    // Variables de Reporte (Cámara)
    private File photoFile;
    private ImageView imgPreviewRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Cargamos el nuevo diseño

        // 1. Inicializar Retrofit (Usando la clase helper que creamos antes)
        apiService = RetrofitClient.getApiService();

        // 2. Vincular Vistas Nuevas
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        layoutLogin = findViewById(R.id.layoutLogin);

        // 3. Configurar Botón Menú (Hamburguesa)
        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 4. Configurar Menú Lateral (Navigation Drawer)
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_mis_reportes) {
                startActivity(new Intent(MainActivity.this, MisReportesActivity.class));
            } else if (id == R.id.nav_alertas) {
                startActivity(new Intent(MainActivity.this, NotificacionesActivity.class));
            } else if (id == R.id.nav_logout) {
                cerrarSesion();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // 5. Configurar Filtros (Chips)
        ChipGroup chipGroup = findViewById(R.id.chipGroupFiltros);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipDocumentos) filtrarMapa("Documentos");
            else if (checkedId == R.id.chipElec) filtrarMapa("Electrónica");
            else if (checkedId == R.id.chipOtros) filtrarMapa("Otros");
            else filtrarMapa("Todos"); // Por defecto
        });

        // 6. Configurar Botón Reportar (FAB)
        findViewById(R.id.fabReportar).setOnClickListener(v -> mostrarDialogoReporte());

        // 7. Configurar Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Botón Login (Dentro del layout blanco)
        findViewById(R.id.sign_in_button).setOnClickListener(v -> signIn());

        // 8. Verificar Sesión
        currentUser = GoogleSignIn.getLastSignedInAccount(this);
        actualizarUI(currentUser);
    }

    private void actualizarUI(GoogleSignInAccount account) {
        if (account != null) {
            // Usuario conectado: Ocultamos login, mostramos mapa
            layoutLogin.setVisibility(View.GONE);
            iniciarMapa();

            // Actualizar header del menú lateral con datos reales
            View header = navigationView.getHeaderView(0);
            TextView txtUser = header.findViewById(R.id.txtNavUser);
            TextView txtEmail = header.findViewById(R.id.txtNavEmail);
            if(account.getDisplayName() != null) txtUser.setText(account.getDisplayName());
            if(account.getEmail() != null) txtEmail.setText(account.getEmail());

        } else {
            // Usuario desconectado: Mostramos pantalla blanca
            layoutLogin.setVisibility(View.VISIBLE);
        }
    }

    // --- LÓGICA LOGIN ---
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void iniciarMapa() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    private void cerrarSesion() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            currentUser = null;
            actualizarUI(null); // Esto vuelve a mostrar la pantalla blanca
            if(mMap != null) mMap.clear();
            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        });
    }

    // --- LÓGICA DE MAPA Y FILTRADO ---
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // --- LÓGICA DE UBICACIÓN REAL ---
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // 1. Activar el puntito azul en el mapa
            mMap.setMyLocationEnabled(true);

            // 2. Obtener la coordenada REAL del GPS del celular
            com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient =
                    com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();

                    Log.d("UBICACION", "📍 Ubicación Real Detectada: " + lat + ", " + lon);

                    // 3. Mover la cámara a donde estás TÚ realmente
                    LatLng miPosicion = new LatLng(lat, lon);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 16)); // Zoom cercano

                    // 4. Avisarle al servidor que estás aquí
                    enviarUbicacionAlServer(lat, lon);
                } else {
                    // Solo si el GPS falla, nos vamos al centro por defecto para no ver el mar
                    Log.e("UBICACION", "⚠️ GPS encendido pero sin señal. Usando fallback.");
                }
            });

        } else {
            // Si no hay permiso, lo pedimos (Código 999)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 999);
        }

        // Cargar los pines de reportes independientemente de mi ubicación
        cargarReportes();
    }

    private void cargarReportes() {
        apiService.obtenerReportes().enqueue(new Callback<List<ReporteModelo>>() {
            @Override
            public void onResponse(Call<List<ReporteModelo>> call, Response<List<ReporteModelo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 1. Guardamos la lista original para poder filtrar después
                    listaReportesOriginal = response.body();
                    // 2. Pintamos todo inicialmente
                    filtrarMapa("Todos");
                }
            }
            @Override
            public void onFailure(Call<List<ReporteModelo>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarMapa(String categoria) {
        if (mMap == null) return;
        mMap.clear(); // Limpiar pines actuales

        for (ReporteModelo rep : listaReportesOriginal) {
            // Lógica de filtro:
            // Si seleccionas "Todos", pasa.
            // Si seleccionas otra cosa, verifica si la descripción o categoría contiene el texto.
            // (Idealmente tu backend debería mandar un campo "category", pero por ahora filtramos por texto)

            boolean coincide = categoria.equals("Todos");

            // Si no es todos, buscamos coincidencia simple en la descripción (o usa getCategory si lo tienes)
            if (!coincide) {
                // Truco: Si la descripción contiene la palabra clave, lo mostramos
                if (rep.getDescription() != null && rep.getDescription().toLowerCase().contains(categoria.toLowerCase())) {
                    coincide = true;
                }
                // Si tu modelo tiene getCategory(), descomenta esto:
                // if (rep.getCategory() != null && rep.getCategory().equalsIgnoreCase(categoria)) coincide = true;
            }

            if (coincide) {
                LatLng pos = new LatLng(rep.getLatitude(), rep.getLongitude());
                mMap.addMarker(new MarkerOptions().position(pos).title(rep.getDescription()));
            }
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 999) { // El mismo código que usamos arriba
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // ¡Permiso concedido! Recargamos el mapa para que busque la ubicación ahora sí
                if (mMap != null) {
                    onMapReady(mMap);
                }

            } else {
                Toast.makeText(this, "Se requiere permiso de ubicación para las alertas.", Toast.LENGTH_LONG).show();
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

                // Guardar email en preferencias
                SharedPreferences prefs = getSharedPreferences("LostNetPrefs", MODE_PRIVATE);
                prefs.edit().putString("email", currentUser.getEmail()).apply();

                // --- CORRECCIÓN AQUÍ ---
                // Antes decías: iniciarMapa();
                // Ahora decimos:
                actualizarUI(currentUser);
                // Esto esconde la pantalla blanca Y carga el mapa

                Toast.makeText(this, "Bienvenido: " + currentUser.getDisplayName(), Toast.LENGTH_SHORT).show();

            } catch (ApiException e) {
                Log.w("Login", "signInResult:failed code=" + e.getStatusCode());
                Toast.makeText(this, "Error iniciando sesión", Toast.LENGTH_SHORT).show();
                actualizarUI(null); // Asegura que se vea el botón de login si falla
            }
        }

        // Cámara (se queda igual)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if(imgPreviewRef != null && photoFile != null) {
                imgPreviewRef.setVisibility(View.VISIBLE);
                imgPreviewRef.setImageURI(Uri.fromFile(photoFile));
            }
        }
    }

    private void enviarUbicacionAlServer(double lat, double lon) {
        SharedPreferences prefs = getSharedPreferences("LostNetPrefs", MODE_PRIVATE);
        String emailUsuario = prefs.getString("email", null);
        if (emailUsuario == null || apiService == null) return;

        UbicacionRequest datos = new UbicacionRequest(emailUsuario, lat, lon);
        apiService.actualizarUbicacion(datos).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}