package com.example.lostnet;

/**
 * Constantes globales de la aplicación.
 * Centraliza valores que se usan en múltiples clases para evitar duplicación.
 */
public final class AppConstants {

    /** URL base del servidor LostNet (para archivos estáticos). */
    public static final String BASE_URL = "http://10.147.20.62:5000/";

    /** URL base de la API. */
    public static final String API_BASE_URL = "http://10.147.20.62:5000/api/";

    /** Nombre del archivo de SharedPreferences de la app. */
    public static final String PREFS_NAME = "LostNetPrefs";

    /** Clave para el email del usuario en SharedPreferences. */
    public static final String PREF_KEY_EMAIL = "email";

    // Códigos de request para startActivityForResult
    public static final int RC_SIGN_IN = 9001;
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int REQUEST_GALLERY_SELECT = 2;
    public static final int REQUEST_CAMERA_PERMISSION = 100;
    public static final int REQUEST_LOCATION_PERMISSION = 999;

    // Estados del reporte
    public static final String STATUS_LOST = "LOST";
    public static final String STATUS_FOUND = "FOUND";

    // Calidad de compresión de imagen (0–100)
    public static final int IMAGE_QUALITY = 60;

    private AppConstants() {
        // Clase utilitaria — no instanciar
    }
}
