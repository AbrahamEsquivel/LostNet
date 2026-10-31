package com.example.lostnet;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton que provee la instancia de Retrofit y el servicio de API.
 * Garantiza que solo exista una instancia de cada uno durante el ciclo de vida de la app.
 */
public final class RetrofitClient {

    private static Retrofit retrofitInstance = null;
    private static LostNetApi apiServiceInstance = null;

    private RetrofitClient() {
        // Clase utilitaria — no instanciar
    }

    /**
     * Devuelve la instancia singleton de Retrofit configurada con la URL base de LostNet.
     */
    public static Retrofit getClient() {
        if (retrofitInstance == null) {
            retrofitInstance = construirRetrofit(AppConstants.API_BASE_URL);
        }
        return retrofitInstance;
    }

    /** Construye un cliente Retrofit para la URL indicada (usado también por las pruebas). */
    static Retrofit construirRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /** Crea el servicio de API apuntando a la URL indicada (usado por las pruebas de integración). */
    static LostNetApi crearApi(String baseUrl) {
        return construirRetrofit(baseUrl).create(LostNetApi.class);
    }

    /**
     * Devuelve la instancia singleton del servicio de API generado por Retrofit.
     */
    public static LostNetApi getApiService() {
        if (apiServiceInstance == null) {
            apiServiceInstance = getClient().create(LostNetApi.class);
        }
        return apiServiceInstance;
    }
}