package com.example.lostnet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interfaz de la API REST de LostNet.
 * Retrofit genera la implementación automáticamente en tiempo de ejecución.
 */
public interface LostNetApi {

    @GET("reportes")
    Call<List<ReporteModelo>> obtenerReportes();

    @GET("puntos-seguros")
    Call<List<PuntoSeguroModelo>> obtenerPuntosSeguros();

    @GET("comentarios")
    Call<List<ComentarioModelo>> obtenerComentarios(@Query("report_id") String reportId);

    @POST("comentar")
    Call<Void> enviarComentario(@Body HashMap<String, Object> body);

    @Headers("Connection: close")
    @Multipart
    @POST("reportar")
    Call<ResponseBody> enviarReporte(
            @Part("user_id")          RequestBody userId,
            @Part("email")            RequestBody email,
            @Part("phone")            RequestBody phone,
            @Part("description")      RequestBody description,
            @Part("category")         RequestBody category,
            @Part("latitude")         RequestBody latitude,
            @Part("longitude")        RequestBody longitude,
            @Part("security_question") RequestBody securityQuestion,
            @Part("security_answer")   RequestBody securityAnswer,
            @Part("status")           RequestBody status,
            @Part MultipartBody.Part  foto
    );

    @DELETE("reportes/{id}")
    Call<ResponseBody> borrarReporte(@Path("id") String id);

    @POST("actualizar-ubicacion")
    Call<Void> actualizarUbicacion(@Body UbicacionRequest request);

    @GET("mis-alertas")
    Call<List<AlertaModelo>> obtenerAlertas(@Query("email") String email);

    @GET("usuario/tipo")
    Call<UsuarioGamificacion> obtenerTipoUsuario(@Query("email") String email);
}