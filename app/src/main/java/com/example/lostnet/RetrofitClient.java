package com.example.lostnet;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // ⚠️ ASEGÚRATE QUE ESTA IP SEA LA CORRECTA (La misma que en MainActivity)
    private static final String BASE_URL = "http://10.155.13.137:5000/";

    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {

            // Configuración de timeouts (igual que la que tenías en MainActivity)
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // Helper para obtener la API directamente (Opcional, pero muy útil)
    public static LostNetApi getApiService() {
        return getRetrofitInstance().create(LostNetApi.class);
    }
}