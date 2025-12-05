package com.example.lostnet;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LostNetApi {

    @GET("/reportes")
    Call<List<ReporteModelo>> obtenerReportes();

    // --- AGREGA ESTA LÍNEA DE @Headers ---
    // Esto le dice al servidor: "No mantengas la conexión viva, ciérrala al terminar".
    // Soluciona el "unexpected end of stream".
    @Headers("Connection: close")
    @Multipart
    @POST("/reportar")
    Call<ResponseBody> enviarReporte(
            @Part("user_id") RequestBody userId,
            @Part("email") RequestBody email,
            @Part("phone") RequestBody phone,
            @Part("description") RequestBody description,
            @Part("category") RequestBody category,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("security_question") RequestBody secQ,
            @Part("security_answer") RequestBody secA,
            @Part MultipartBody.Part foto
    );

    @GET("/mis-alertas")
    Call<List<AlertaModelo>> obtenerMisAlertas(@Query("email") String email);

    @DELETE("/reportes/{id}")
    Call<ResponseBody> borrarReporte(@Path("id") String id);
    @POST("/actualizar-ubicacion")
    Call<Void> actualizarUbicacion(@Body UbicacionRequest request);
}