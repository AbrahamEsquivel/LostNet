package com.example.lostnet;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface LostNetApi {

    // Endpoint para LEER (Mapa)
    @GET("/reportes")
    Call<List<ReporteModelo>> obtenerReportes();

    // Endpoint para ESCRIBIR (Formulario Complejo)
    @Multipart
    @POST("/reportar")
    Call<Object> enviarReporteCompleto(
            @Part("user_id") RequestBody userId,
            @Part("email") RequestBody email,
            @Part("phone") RequestBody phone,
            @Part("description") RequestBody description,
            @Part("category") RequestBody category,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("security_question") RequestBody question,
            @Part("secret_answer") RequestBody answer,
            @Part MultipartBody.Part foto // <--- La foto
    );
}