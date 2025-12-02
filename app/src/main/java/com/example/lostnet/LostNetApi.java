package com.example.lostnet;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.DELETE; // Importar esto
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LostNetApi {

    @GET("/reportes")
    Call<List<ReporteModelo>> obtenerReportes();

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
            @Part("security_question") RequestBody secQuestion,
            @Part("secret_answer") RequestBody secAnswer,
            @Part MultipartBody.Part foto
    );

    @DELETE("/reportes/{id}")
    Call<ResponseBody> borrarReporte(@Path("id") String id);

    @GET("/mis-alertas")
    Call<List<AlertaModelo>> obtenerMisAlertas(@Query("email") String email);
}