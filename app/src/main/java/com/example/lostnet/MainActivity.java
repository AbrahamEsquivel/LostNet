package com.example.lostnet;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int RC_SIGN_IN = 9001;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_SELECT = 2;
    private GoogleMap mMap;
    private GoogleSignInClient mGoogleSignInClient;
    private LostNetApi apiService;
    private GoogleSignInAccount currentUser;

    // UI Nueva (Pro)
    private View layoutLogin;
    private View profilePanelOverlay;
    private BottomSheetBehavior<View> sheetBehavior;

    // Datos y Filtros
    private List<ReporteModelo> listaReportesOriginal = new ArrayList<>(); // Copia para filtrar localmente
    private List<PuntoSeguroModelo> listaPuntosSeguros = new ArrayList<>();
    private Location userLocation;

    // Variables de Reporte (Cámara)
    private File photoFile;
    private ImageView imgPreviewRef;

    // Pégalo al inicio de la clase MainActivity, antes del onCreate
    private static final String BASE_URL = AppConstants.BASE_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
        );
        setContentView(R.layout.activity_main); // Cargamos el nuevo diseño

        // 1. Inicializar Retrofit (Usando la clase helper que creamos antes)
        apiService = RetrofitClient.getApiService();

        // 2. Vincular Vistas Nuevas
        layoutLogin = findViewById(R.id.layoutLogin);
        profilePanelOverlay = findViewById(R.id.profilePanelOverlay);

        // Configurar BottomSheetBehavior
        View bottomSheet = findViewById(R.id.bottomSheet);
        sheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // 3. Configurar Perfil (Avatar)
        findViewById(R.id.cardAvatar).setOnClickListener(v -> {
            if (profilePanelOverlay.getVisibility() == View.GONE) {
                profilePanelOverlay.setVisibility(View.VISIBLE);
                // Inflar el panel si no está
                if (((FrameLayout)profilePanelOverlay).getChildCount() == 0) {
                    LayoutInflater.from(this).inflate(R.layout.layout_profile_panel, (ViewGroup) profilePanelOverlay, true);
                    configurarPanelPerfil();
                }
            }
        });

        profilePanelOverlay.setOnClickListener(v -> profilePanelOverlay.setVisibility(View.GONE));

        findViewById(R.id.fabRefresh).setOnClickListener(v -> {
            Toast.makeText(this, "🔄 Reiniciando...", Toast.LENGTH_SHORT).show();

            // ESTA LÍNEA MÁGICA HACE EL "F5"
            recreate();
        });

        // 4. Configurar Botones del Bottom Sheet (Navegación por Tabs)
        View tabExplorar = findViewById(R.id.tabExplorar);
        View tabTu = findViewById(R.id.tabTu);
        View tabContribuir = findViewById(R.id.tabContribuir);

        View panelExplorar = findViewById(R.id.panelExplorar);
        View panelTu = findViewById(R.id.panelTu);
        View panelContribuir = findViewById(R.id.panelContribuir);

        tabExplorar.setOnClickListener(v -> {
            panelExplorar.setVisibility(View.VISIBLE);
            panelTu.setVisibility(View.GONE);
            panelContribuir.setVisibility(View.GONE);
            actualizarEstiloTabs(0);
        });

        tabTu.setOnClickListener(v -> {
            panelExplorar.setVisibility(View.GONE);
            panelTu.setVisibility(View.VISIBLE);
            panelContribuir.setVisibility(View.GONE);
            actualizarEstiloTabs(1);
        });

        tabContribuir.setOnClickListener(v -> {
            panelExplorar.setVisibility(View.GONE);
            panelTu.setVisibility(View.GONE);
            panelContribuir.setVisibility(View.VISIBLE);
            actualizarEstiloTabs(2);
        });

        // Inicializar estilo de pestañas
        actualizarEstiloTabs(0);

        // 5. Configurar Filtros (Chips)
        ChipGroup chipGroup = findViewById(R.id.chipGroupFiltros);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipDocumentos) filtrarMapa("Documentos");
            else if (checkedId == R.id.chipElec) filtrarMapa("Electrónica");
            else if (checkedId == R.id.chipRopa) filtrarMapa("Ropa"); // <--- ¡YA FUNCIONA!
            else if (checkedId == R.id.chipOtros) filtrarMapa("Otros");
            else filtrarMapa("Todos");
        });

        // 6. Configurar Botón Reportar (FAB)
        findViewById(R.id.fabReportar).setOnClickListener(v -> mostrarDialogoReporte());

        // 6.5 Configurar Botón Mi Ubicación (FAB)
        findViewById(R.id.fabMiUbicacion).setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                FusedLocationProviderClient fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(this);
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null && mMap != null) {
                        LatLng miPosicion = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 16));
                    }
                });
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 999);
            }
        });

        // 7. Configurar Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Botón Login (Dentro del layout blanco)
        findViewById(R.id.btnCustomGoogleLogin).setOnClickListener(v -> signIn());

        // 8. Verificar Sesión
        currentUser = GoogleSignIn.getLastSignedInAccount(this);
        actualizarUI(currentUser);

        verificarNavegacionDesdeLista(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        verificarNavegacionDesdeLista(intent);
    }

    private void verificarNavegacionDesdeLista(Intent intent) {
        if (intent != null && intent.hasExtra("LAT_DESTINO")) {
            double lat = intent.getDoubleExtra("LAT_DESTINO", 0);
            double lon = intent.getDoubleExtra("LON_DESTINO", 0);
            String idDestino = intent.getStringExtra("ID_DESTINO");

            // Esperar un poquito a que el mapa esté listo si venimos entrando
            new Handler().postDelayed(() -> {
                if (mMap != null) {
                    LatLng pos = new LatLng(lat, lon);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 18)); // Zoom muy cerca

                    // Opcional: Mostrar Toast
                    Toast.makeText(this, "Mostrando reporte seleccionado", Toast.LENGTH_SHORT).show();

                    // Si quisieras abrir el PopUp automáticamente, tendrías que buscar el Marker
                    // que tenga el tag con ese ID, pero con mover la cámara basta por ahora.
                }
            }, 1000); // 1 segundo de espera
        }
    }
    private void actualizarUI(GoogleSignInAccount account) {
        if (account != null) {
            // Usuario conectado: Ocultamos login, mostramos mapa
            layoutLogin.setVisibility(View.GONE);
            iniciarMapa();

            // Cargar Foto en el Avatar de la barra de búsqueda
            ImageView imgAvatar = findViewById(R.id.imgAvatar);
            if (account.getPhotoUrl() != null && imgAvatar != null) {
                Glide.with(this)
                        .load(account.getPhotoUrl())
                        .circleCrop()
                        .into(imgAvatar);
            }

            // Si el panel de perfil ya está inflado, actualizarlo
            actualizarDatosPerfil(account);

            // --- ¡NUEVO! LLAMAMOS A LA MEDALLA ---
            cargarMedallaUsuario(account.getEmail());

        } else {
            // Usuario desconectado: Mostramos pantalla blanca
            layoutLogin.setVisibility(View.VISIBLE);
        }
    }

    private void configurarPanelPerfil() {
        View panel = findViewById(R.id.profilePanelContainer);
        if (panel == null) return;

        panel.findViewById(R.id.btnCerrarPerfil).setOnClickListener(v -> profilePanelOverlay.setVisibility(View.GONE));
        panel.findViewById(R.id.menuCerrarSesion).setOnClickListener(v -> {
            profilePanelOverlay.setVisibility(View.GONE);
            cerrarSesion();
        });
        panel.findViewById(R.id.menuMisReportes).setOnClickListener(v -> startActivity(new Intent(this, MisReportesActivity.class)));
        panel.findViewById(R.id.menuMisAlertas).setOnClickListener(v -> startActivity(new Intent(this, NotificacionesActivity.class)));
        panel.findViewById(R.id.menuSobreApp).setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        panel.findViewById(R.id.menuCreditos).setOnClickListener(v -> startActivity(new Intent(this, CreditsActivity.class)));

        if (currentUser != null) actualizarDatosPerfil(currentUser);
    }

    private void actualizarEstiloTabs(int index) {
        ImageView iconExp = findViewById(R.id.iconExplorar);
        TextView txtExp = findViewById(R.id.txtExplorar);
        ImageView iconTu = findViewById(R.id.iconTu);
        TextView txtTu = findViewById(R.id.txtTu);
        ImageView iconCont = findViewById(R.id.iconContribuir);
        TextView txtCont = findViewById(R.id.txtContribuir);

        int colorActive = Color.parseColor("#1A73E8");
        int colorInactive = Color.parseColor("#9AA0A6");

        // Explorar
        iconExp.setColorFilter(index == 0 ? colorActive : colorInactive);
        txtExp.setTextColor(index == 0 ? colorActive : colorInactive);
        txtExp.setTypeface(null, index == 0 ? Typeface.BOLD : Typeface.NORMAL);

        // Tú
        iconTu.setColorFilter(index == 1 ? colorActive : colorInactive);
        txtTu.setTextColor(index == 1 ? colorActive : colorInactive);
        txtTu.setTypeface(null, index == 1 ? Typeface.BOLD : Typeface.NORMAL);

        // Contribuir (Cómo funciona)
        iconCont.setColorFilter(index == 2 ? colorActive : colorInactive);
        txtCont.setTextColor(index == 2 ? colorActive : colorInactive);
        txtCont.setTypeface(null, index == 2 ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void actualizarDatosPerfil(GoogleSignInAccount account) {
        View panel = findViewById(R.id.profilePanelContainer);
        if (panel != null && account != null) {
            TextView txtUser = panel.findViewById(R.id.txtPerfilNombre);
            TextView txtEmail = panel.findViewById(R.id.txtPerfilEmail);
            ImageView imgPerfil = panel.findViewById(R.id.imgPerfilAvatar);

            if (account.getDisplayName() != null) txtUser.setText(account.getDisplayName());
            if (account.getEmail() != null) txtEmail.setText(account.getEmail());

            if (account.getPhotoUrl() != null && imgPerfil != null) {
                Glide.with(this)
                        .load(account.getPhotoUrl())
                        .circleCrop()
                        .into(imgPerfil);
            }
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

        // Desactivar UI por defecto de Google Maps
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false); // También quita botones de "Abrir en Maps"

        // --- LÓGICA DE UBICACIÓN REAL ---
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // 1. Activar el puntito azul en el mapa
            mMap.setMyLocationEnabled(true);

            // 2. Obtener la coordenada REAL del GPS del celular
            FusedLocationProviderClient fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(this);

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLocation = location;
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();

                    Log.d("UBICACION", "📍 Ubicación Real Detectada: " + lat + ", " + lon);

                    // 3. Mover la cámara a donde estás TÚ realmente
                    LatLng miPosicion = new LatLng(lat, lon);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 16)); // Zoom cercano

                    // 4. Avisarle al servidor que estás aquí
                    enviarUbicacionAlServer(lat, lon);

                    // 5. Refrescar lista de cercanos ahora que tenemos ubicación
                    cargarExplorarCercanos();
                } else {
                    // Solo si el GPS falla, nos vamos al centro por defecto para no ver el mar
                    Log.e("UBICACION", "⚠️ GPS encendido pero sin señal. Usando fallback.");
                }
            });

        } else {
            // Si no hay permiso, lo pedimos (Código 999)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 999);
        }

        mMap.setOnMarkerClickListener(marker -> {
            // Recuperamos el objeto oculto en el tag
            Object tag = marker.getTag();

            if (tag != null && tag instanceof ReporteModelo) {
                ReporteModelo reporteSeleccionado = (ReporteModelo) tag;
                mostrarPopUpDetalle(reporteSeleccionado); // Llamamos a la función del paso 3
            }

            return false; // false = permite que el mapa siga haciendo su zoom default al pin
        });

        cargarReportes();
        cargarPuntosSeguros();
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
                    // 3. Cargar reportes cercanos en el BottomSheet
                    cargarExplorarCercanos();
                }
            }
            @Override
            public void onFailure(Call<List<ReporteModelo>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarExplorarCercanos() {
        if (userLocation == null || listaReportesOriginal.isEmpty()) return;

        List<ReporteModelo> ajenosCercanos = new ArrayList<>();
        String myEmail = (currentUser != null) ? currentUser.getEmail() : "";

        for (ReporteModelo r : listaReportesOriginal) {
            // Filtrar: Que NO sea mío
            if (myEmail != null && r.getEmail() != null && r.getEmail().equalsIgnoreCase(myEmail)) {
                continue;
            }

            float[] results = new float[1];
            Location.distanceBetween(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    r.getLatitude(), r.getLongitude(),
                    results
            );

            // Filtro: 2.5 km (2500 metros)
            if (results[0] <= 2500) {
                ajenosCercanos.add(r);
            }
        }

        RecyclerView rv = findViewById(R.id.recyclerExplorar);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new ReportesGmapsAdapter(this, ajenosCercanos, reporte -> {
                if (sheetBehavior != null) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                if (mMap != null) {
                    LatLng pos = new LatLng(reporte.getLatitude(), reporte.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17));
                }
            }));
        }
    }

    private void cargarMisReportesTab() {
        if (listaReportesOriginal.isEmpty() || currentUser == null) return;

        List<ReporteModelo> misReportes = new ArrayList<>();
        String myEmail = currentUser.getEmail();

        for (ReporteModelo r : listaReportesOriginal) {
            if (myEmail != null && r.getEmail() != null && r.getEmail().equalsIgnoreCase(myEmail)) {
                misReportes.add(r);
            }
        }

        RecyclerView rv = findViewById(R.id.recyclerTu);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new ReportesGmapsAdapter(this, misReportes, reporte -> {
                if (sheetBehavior != null) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                if (mMap != null) {
                    LatLng pos = new LatLng(reporte.getLatitude(), reporte.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17));
                }
            }));
        }
    }

    private void filtrarMapa(String categoriaFiltro) {
        if (mMap == null) return;

        // 1. Borramos TODO (Rojos, Azules y Verdes)
        mMap.clear();

        // 2. ¡RESTAURAMOS LOS VERDES INMEDIATAMENTE! (La solución)
        pintarPuntosSegurosEnMapa();

        // 3. Ahora sí, pintamos los reportes según el filtro (Rojos y Azules)
        for (ReporteModelo rep : listaReportesOriginal) {
            String catReporte = rep.getCategory() != null ? rep.getCategory() : "Otros";
            boolean mostrar = false;

            if (categoriaFiltro.equals("Todos")) {
                mostrar = true;
            } else if (catReporte.equalsIgnoreCase(categoriaFiltro)) {
                mostrar = true;
            }

            if (mostrar) {
                LatLng pos = new LatLng(rep.getLatitude(), rep.getLongitude());

                float colorPin;
                if (currentUser != null && rep.getEmail() != null &&
                        rep.getEmail().equals(currentUser.getEmail())) {
                    colorPin = BitmapDescriptorFactory.HUE_AZURE; // Míos (Azul)
                } else {
                    colorPin = BitmapDescriptorFactory.HUE_RED; // Otros (Rojo)
                }

                Marker marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(pos)
                                .title(rep.getDescription())
                                .icon(BitmapDescriptorFactory.defaultMarker(colorPin))
                );
                marker.setTag(rep);
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
        AutoCompleteTextView etCat = view.findViewById(R.id.etCategoria);
        EditText etSecQ = view.findViewById(R.id.etSecQ);
        EditText etSecA = view.findViewById(R.id.etSecA);
        View btnCamara = view.findViewById(R.id.btnCamara);
        Button btnEnviar = view.findViewById(R.id.btnEnviar);
        View btnGaleria = view.findViewById(R.id.btnGaleria);
        imgPreviewRef = view.findViewById(R.id.imgPreview);

        // --- REFERENCIA AL SWITCH ---
        SwitchMaterial switchTipo = view.findViewById(R.id.switchTipoReporte);
        TextView txtStatus = view.findViewById(R.id.txtStatusSwitch);
        ImageView iconStatus = view.findViewById(R.id.iconTipoReporte);

        if (switchTipo != null) {
            switchTipo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    txtStatus.setText("¡Objeto Encontrado!");
                    txtStatus.setTextColor(ContextCompat.getColor(this, R.color.primary));
                    iconStatus.setImageResource(android.R.drawable.ic_menu_search);
                    iconStatus.setColorFilter(ContextCompat.getColor(this, R.color.primary));
                } else {
                    txtStatus.setText("Objeto Perdido");
                    txtStatus.setTextColor(Color.parseColor("#202124"));
                    iconStatus.setImageResource(android.R.drawable.ic_menu_help);
                    iconStatus.setColorFilter(ContextCompat.getColor(this, R.color.danger));
                }
            });
        }

        // Resetear foto temporal al abrir el dialog
        photoFile = null;
        imgPreviewRef.setVisibility(View.GONE);

        // AutoCompleteTextView (Dropdown) Setup
        String[] categorias = {"Electrónica", "Ropa", "Documentos", "Otros"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categorias);
        etCat.setAdapter(adapter);
        etCat.setText(categorias[0], false); // Seleccionar primero por defecto

        // Botón Cámara
        btnCamara.setOnClickListener(v -> despacharTomarFoto());

        btnGaleria.setOnClickListener(v -> {
            // Intent para abrir la galería solo para imágenes
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*"); // <--- ESTO FILTRA SOLO IMÁGENES
            startActivityForResult(intent, REQUEST_GALLERY_SELECT);
        });

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

            // --- ¡NUEVA LÓGICA DEL SWITCH! ---
            // Si el switch está encendido, es FOUND (Encontrado). Si no, es LOST (Perdido).
            String statusReporte = (switchTipo != null && switchTipo.isChecked()) ? "FOUND" : "LOST";

            // 2. Bloquear botón para evitar doble envío
            btnEnviar.setEnabled(false);
            btnEnviar.setText("ENVIANDO...");

            // 3. Enviar
            enviarDatosAlServidor(
                    currentUser.getId(),
                    currentUser.getEmail(),
                    etPhone.getText().toString(),
                    etDesc.getText().toString(),
                    etCat.getText().toString(),
                    String.valueOf(centro.latitude),
                    String.valueOf(centro.longitude),
                    etSecQ.getText().toString(),
                    etSecA.getText().toString(),
                    statusReporte, // <--- Pasamos el nuevo dato aquí
                    dialog,
                    fotoFinal // Pasamos null si no hay foto
            );
        });

        // Configurar botón cerrar del header
        View btnX = view.findViewById(R.id.btnCerrarDialog);
        if (btnX != null) btnX.setOnClickListener(v -> dialog.dismiss());

        // Permitir cancelar tocando fuera (opcional) o agregar botón cancelar
        dialog.setCanceledOnTouchOutside(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    // --- MÉTODO CORE DE ENVÍO (PROTEGIDO CONTRA NULL) ---
    private void enviarDatosAlServidor(String uid, String email, String phone, String desc,
                                       String cat, String latStr, String lonStr, String sq, String sa,
                                       String statusReporte, // <--- Acepta el String aquí
                                       AlertDialog dialog, @Nullable File archivoAEnviar) {

        // 1. Convertimos todo a RequestBody (Texto)
        RequestBody uidPart = RequestBody.create(MediaType.parse("text/plain"), uid);
        RequestBody emailPart = RequestBody.create(MediaType.parse("text/plain"), email);
        RequestBody phonePart = RequestBody.create(MediaType.parse("text/plain"), phone);
        RequestBody descPart = RequestBody.create(MediaType.parse("text/plain"), desc);
        RequestBody catPart = RequestBody.create(MediaType.parse("text/plain"), cat);
        RequestBody latPart = RequestBody.create(MediaType.parse("text/plain"), latStr);
        RequestBody lonPart = RequestBody.create(MediaType.parse("text/plain"), lonStr);
        RequestBody sqPart = RequestBody.create(MediaType.parse("text/plain"), sq);
        RequestBody saPart = RequestBody.create(MediaType.parse("text/plain"), sa);

        // --- Convertimos el status (FOUND/LOST) para enviarlo ---
        RequestBody statusPart = RequestBody.create(MediaType.parse("text/plain"), statusReporte);

        MultipartBody.Part body = null;
        if (archivoAEnviar != null) {
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), archivoAEnviar);
            body = MultipartBody.Part.createFormData("foto", archivoAEnviar.getName(), reqFile);
        }

        // Llamada a Retrofit (¡Asegúrate de que statusPart esté aquí adentro!)
        apiService.enviarReporte(uidPart, emailPart, phonePart, descPart, catPart, latPart, lonPart, sqPart, saPart, statusPart, body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "¡Reporte enviado!", Toast.LENGTH_LONG).show();
                            dialog.dismiss();

                            // --- CORRECCIÓN CLAVE: PINTAR PIN MANUALMENTE ---
                            try {
                                double lat = Double.parseDouble(latStr);
                                double lon = Double.parseDouble(lonStr);
                                LatLng pos = new LatLng(lat, lon);

                                if (mMap != null) {
                                    mMap.addMarker(new MarkerOptions()
                                            .position(pos)
                                            .title(desc));
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16));
                                }
                            } catch (Exception e) {
                                Log.e("Mapa", "Error pintando pin manual", e);
                            }

                            // --- DESCARGAR LISTA Y MEDALLA CON RETRASO ---
                            new Handler().postDelayed(() -> {
                                cargarReportes();

                                // ¡AQUÍ ESTÁ LA LÍNEA NUEVA PARA EL RANGO EN TIEMPO REAL! 🏅
                                cargarMedallaUsuario(email);

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
                        if (errorMsg != null && (errorMsg.contains("unexpected end of stream") || errorMsg.contains("closed"))) {
                            Toast.makeText(MainActivity.this, "¡Reporte enviado! (Stream cerrado)", Toast.LENGTH_LONG).show();
                            if (dialog != null) dialog.dismiss();

                            try {
                                double lat = Double.parseDouble(latStr);
                                double lon = Double.parseDouble(lonStr);
                                LatLng pos = new LatLng(lat, lon);
                                if (mMap != null) {
                                    mMap.addMarker(new MarkerOptions().position(pos).title(desc));
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16));
                                }
                            } catch (Exception e) {}

                            new Handler().postDelayed(() -> {
                                cargarReportes();

                                // ¡Y AQUÍ ESTÁ LA OTRA LÍNEA NUEVA! 🏅
                                cargarMedallaUsuario(email);

                            }, 2000);
                            return;
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
                btn.setText("PUBLICAR REPORTE");
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
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
        if (resultCode == RESULT_OK) {

            // CASO 1: Vengo de la CÁMARA 📸
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // photoFile ya estaba seteado antes de abrir la cámara, así que ya está listo.
                mostrarPreviewFoto();
            }

            // CASO 2: Vengo de la GALERÍA 🖼️
            else if (requestCode == REQUEST_GALLERY_SELECT && data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        // Convertimos la URI de galería a un Archivo físico que podamos enviar
                        photoFile = crearArchivoDesdeUri(selectedImageUri);
                        mostrarPreviewFoto();
                    } catch (IOException e) {
                        Toast.makeText(this, "Error al cargar imagen de galería", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private File crearArchivoDesdeUri(Uri uri) throws IOException {
        // Creamos un archivo temporal vacío donde guardaremos la copia
        File destino = crearArchivoImagen(); // Reusamos tu función existente

        // Abrimos el flujo de datos de la URI y lo copiamos al archivo destino
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(destino)) {

            byte[] buffer = new byte[4 * 1024]; // Buffer de 4KB
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        }
        return destino;
    }

    private void mostrarPreviewFoto() {
        if(imgPreviewRef != null && photoFile != null) {
            imgPreviewRef.setVisibility(View.VISIBLE);
            imgPreviewRef.setImageURI(Uri.fromFile(photoFile));
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

    // Agrega este metodo en MainActivity
    // 1. MÉTODO PRINCIPAL DEL POPUP
    private void mostrarPopUpDetalle(ReporteModelo reporte) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_detalle_reporte, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        // --- A. VINCULAR VISTAS ---
        ImageView img = view.findViewById(R.id.imgDetalle);
        TextView txtCat = view.findViewById(R.id.txtDetalleCat);
        TextView txtDesc = view.findViewById(R.id.txtDetalleDesc);
        TextView txtEmail = view.findViewById(R.id.txtDetalleEmail);
        TextView txtTel = view.findViewById(R.id.txtDetalleTel);
        TextView txtPregunta = view.findViewById(R.id.txtDetallePregunta);
        TextView txtRespuesta = view.findViewById(R.id.txtDetalleRespuesta);

        // Botones de acción
        Button btnCerrar = view.findViewById(R.id.btnCerrarDetalle);
        Button btnWhats = view.findViewById(R.id.btnContactarWhats);
        Button btnEliminar = view.findViewById(R.id.btnEliminarDetalle);

        // Sección de Comentarios
        RecyclerView recyclerComents = view.findViewById(R.id.recyclerComentarios);
        EditText etComentario = view.findViewById(R.id.etNuevoComentario);
        ImageButton btnEnviarCom = view.findViewById(R.id.btnEnviarComentario);

        // --- B. LLENAR DATOS BÁSICOS (Solo los públicos generales) ---
        txtDesc.setText(reporte.getDescription());
        String estadoVisual = "🔴 PERDIDO";
        if (reporte.getStatus() != null && reporte.getStatus().equals("FOUND")) {
            estadoVisual = "🟢 ENCONTRADO";
        }

        // Concatenamos el estatus con la categoría
        String catTexto = reporte.getCategory() != null ? reporte.getCategory() : "General";
        txtCat.setText(estadoVisual + "  |  " + catTexto);
        // La pregunta la dejamos visible siempre
        txtPregunta.setText("P: " + (reporte.getSecurityQuestion() != null ? reporte.getSecurityQuestion() : "N/A"));

        // --- C. CARGAR FOTO ---
        String rutaFoto = reporte.getPhotoUrl();
        if (rutaFoto != null && !rutaFoto.isEmpty()) {
            if(rutaFoto.startsWith("/")) rutaFoto = rutaFoto.substring(1);
            String fullUrl = BASE_URL + rutaFoto;

            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.stat_notify_error)
                    .into(img);
        }

        // --- D. LÓGICA DE PRIVACIDAD (TUYO vs OTROS) ---
        boolean esMio = currentUser != null && reporte.getUserId() != null &&
                reporte.getUserId().equals(currentUser.getId());

        if (esMio) {
            // CASO 1: ES TUYO 🟢 (Ves todo completo)
            txtEmail.setText(reporte.getEmail() != null ? reporte.getEmail() : "Sin Email");
            txtTel.setText("Tel: " + (reporte.getPhone() != null ? reporte.getPhone() : "N/A"));

            // Respuesta visible
            txtRespuesta.setVisibility(View.VISIBLE);
            txtRespuesta.setText("R: " + (reporte.getSecurityAnswer() != null ? reporte.getSecurityAnswer() : "N/A"));

            // Botones
            btnEliminar.setVisibility(View.VISIBLE);
            btnWhats.setVisibility(View.GONE);

            btnEliminar.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("¿Eliminar Reporte?")
                        .setMessage("Esta acción no se puede deshacer.")
                        .setPositiveButton("Sí, borrar", (d, w) -> {
                            apiService.borrarReporte(reporte.getId()).enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Reporte eliminado", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        cargarReportes();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    Toast.makeText(MainActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });

        } else {
            // CASO 2: ES DE OTRO 🔴 (Censuramos datos sensibles)

            // Usamos las funciones nuevas para ocultar info
            txtEmail.setText(enmascararEmail(reporte.getEmail()));
            txtTel.setText("Tel: " + enmascararTelefono(reporte.getPhone()));

            // Ocultamos la respuesta secreta
            txtRespuesta.setVisibility(View.GONE);

            // Botones
            btnEliminar.setVisibility(View.GONE);
            btnWhats.setVisibility(View.VISIBLE);

            btnWhats.setOnClickListener(v -> {
                String telefonoReal = reporte.getPhone(); // <--- Aquí usamos el REAL para que WhatsApp funcione
                String nombreObjeto = reporte.getDescription();
                if (telefonoReal != null && !telefonoReal.isEmpty()) {
                    abrirWhatsApp(telefonoReal, nombreObjeto);
                } else {
                    Toast.makeText(MainActivity.this, "Sin teléfono disponible", Toast.LENGTH_SHORT).show();
                }
            });
        }
        String correoAutor = reporte.getEmail();
        if (correoAutor != null && !correoAutor.isEmpty()) {
            apiService.obtenerTipoUsuario(correoAutor).enqueue(new Callback<UsuarioGamificacion>() {
                @Override
                public void onResponse(Call<UsuarioGamificacion> call, Response<UsuarioGamificacion> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String rango = response.body().getTipo();

                        // Le ponemos su emoji para que se vea bien pro
                        String emoji = "🔍"; // Buscador por defecto
                        if (rango.equalsIgnoreCase("SABUESO")) emoji = "🕵️‍♂️";
                        else if (rango.equalsIgnoreCase("PERDEDOR")) emoji = "🤦‍♂️";

                        // Le pegamos el rango al lado del correo (respetando si está censurado o no)
                        String textoActual = txtEmail.getText().toString();
                        txtEmail.setText(textoActual + "  |  " + emoji + " " + rango);
                    }
                }

                @Override
                public void onFailure(Call<UsuarioGamificacion> call, Throwable t) {
                    // Si falla el internet, simplemente dejamos el correo normal, no pasa nada.
                    Log.e("Gamificacion", "No se pudo cargar la medalla en el popup");
                }
            });
        }

        // --- E. LÓGICA DE COMENTARIOS (EL MURO) ---
        recyclerComents.setLayoutManager(new LinearLayoutManager(this));

        Runnable cargarComentarios = () -> {
            apiService.obtenerComentarios(reporte.getId()).enqueue(new Callback<List<ComentarioModelo>>() {
                @Override
                public void onResponse(Call<List<ComentarioModelo>> call, Response<List<ComentarioModelo>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        recyclerComents.setAdapter(new ComentariosAdapter(response.body()));
                    }
                }
                @Override public void onFailure(Call<List<ComentarioModelo>> call, Throwable t) {}
            });
        };

        cargarComentarios.run();

        btnEnviarCom.setOnClickListener(v -> {
            String texto = etComentario.getText().toString().trim();
            if (texto.isEmpty()) return;

            HashMap<String, Object> body = new HashMap<>();
            body.put("report_id", reporte.getId());
            body.put("user_name", currentUser != null ? currentUser.getDisplayName() : "Anónimo");
            body.put("text", texto);

            btnEnviarCom.setEnabled(false);

            apiService.enviarComentario(body).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    etComentario.setText("");
                    btnEnviarCom.setEnabled(true);
                    cargarComentarios.run();
                    Toast.makeText(MainActivity.this, "Comentario enviado", Toast.LENGTH_SHORT).show();
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    btnEnviarCom.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Error enviando comentario", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // --- F. CERRAR ---
        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();
    }

    // 2. MÉTODO AUXILIAR PARA ABRIR WHATSAPP
    private void abrirWhatsApp(String telefono, String objeto) {
        try {
            // Limpieza básica del número
            String numeroLimpio = telefono.replaceAll("\\s+", "").replaceAll("-", "");

            // Asumimos lada 52 (México) si no la trae
            if (!numeroLimpio.startsWith("+") && !numeroLimpio.startsWith("52")) {
                numeroLimpio = "52" + numeroLimpio;
            }
            // WhatsApp API requiere solo números, sin el +
            numeroLimpio = numeroLimpio.replace("+", "");

            String mensaje = "Hola, vi en LostNet que perdiste tu *" + objeto + "*. ¡Creo que yo lo encontré! ¿Dónde nos vemos?";
            String url = "https://api.whatsapp.com/send?phone=" + numeroLimpio + "&text=" + URLEncoder.encode(mensaje, "UTF-8");

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);

        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir WhatsApp (¿Está instalado?)", Toast.LENGTH_SHORT).show();
        }
    }


    private void cargarPuntosSeguros() {
        if (apiService == null) return;

        apiService.obtenerPuntosSeguros().enqueue(new Callback<List<PuntoSeguroModelo>>() {
            @Override
            public void onResponse(Call<List<PuntoSeguroModelo>> call, Response<List<PuntoSeguroModelo>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // 1. Guardamos la lista en memoria para usarla después
                    listaPuntosSeguros = response.body();

                    // 2. Los pintamos por primera vez
                    pintarPuntosSegurosEnMapa();
                }
            }
            @Override
            public void onFailure(Call<List<PuntoSeguroModelo>> call, Throwable t) {
                Log.e("MAPA", "❌ Error cargando puntos seguros");
            }
        });
    }

    // --- METODO AUXILIAR NUEVO ---
    // Este metodo se encargará de poner los pines verdes.
    // Lo separamos para poder llamarlo cuantas veces queramos.
    private void pintarPuntosSegurosEnMapa() {
        if (mMap == null || listaPuntosSeguros == null) return;

        for (PuntoSeguroModelo punto : listaPuntosSeguros) {
            LatLng pos = new LatLng(punto.getLat(), punto.getLon());
            mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Zona Segura: " + punto.getNombre())
                    .snippet(punto.getTipo())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))); // VERDE
        }
    }

    private String enmascararEmail(String email) {
        if (email == null || email.isEmpty()) return "Sin Email";
        if (!email.contains("@")) return email; // Si no tiene arroba, no lo tocamos

        String[] partes = email.split("@");
        String nombre = partes[0];
        String dominio = partes[1];

        // Si el nombre es largo (ej. "josimar"), dejamos "jos***"
        // Si es corto (ej. "yo"), dejamos "***"
        if (nombre.length() > 3) {
            return nombre.substring(0, 3) + "***@" + dominio;
        } else {
            return "***@" + dominio;
        }
    }

    private String enmascararTelefono(String phone) {
        if (phone == null || phone.isEmpty()) return "Sin Teléfono";
        // Dejamos los primeros 2 dígitos y los últimos 2 visibles
        if (phone.length() > 4) {
            return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
        }
        return "****";
    }

    private void cargarMedallaUsuario(String emailUsuario) {
        // Asegurarnos de que Retrofit esté listo
        if (apiService == null) {
            apiService = RetrofitClient.getApiService();
        }

        apiService.obtenerTipoUsuario(emailUsuario).enqueue(new Callback<UsuarioGamificacion>() {
            @Override
            public void onResponse(Call<UsuarioGamificacion> call, Response<UsuarioGamificacion> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // El servidor nos da el rango (Sabueso, Buscador, etc.)
                    String medalla = response.body().getTipo();

                    // ¡MAGIA! Lo inyectamos en el nuevo panel de perfil
                    View panel = findViewById(R.id.profilePanelContainer);
                    if (panel != null) {
                        TextView txtMedalla = panel.findViewById(R.id.txtPerfilBadge);
                        if (txtMedalla != null) {
                            String emoji = "🔍";
                            int backgroundId = R.drawable.bg_badge_buscador;

                            if (medalla.equalsIgnoreCase("SABUESO")) {
                                emoji = "🕵️‍♂️";
                                backgroundId = R.drawable.bg_badge_sabueso;
                            } else if (medalla.equalsIgnoreCase("PERDEDOR")) {
                                emoji = "🤦‍♂️";
                                backgroundId = R.drawable.bg_badge_perdedor;
                            }

                            txtMedalla.setText(emoji + " " + medalla);
                            txtMedalla.setBackgroundResource(backgroundId);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<UsuarioGamificacion> call, Throwable t) {
                Log.e("GAMIFICACION", "Error al cargar medalla: " + t.getMessage());
            }
        });
    }
}