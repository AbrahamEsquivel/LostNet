package com.example.lostnet;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LostNetApi {
    @POST("/reportar")
    Call<Object> enviarReporte(@Body ReporteModelo reporte);
}