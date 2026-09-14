package com.example.lostnet;

import com.google.gson.annotations.SerializedName;

public class UsuarioGamificacion {

    // SerializedName le dice a Retrofit que busque la llave "tipo" en el JSON de Python
    @SerializedName("tipo")
    private String tipo;

    // Constructor vacío (necesario para Firebase/Retrofit)
    public UsuarioGamificacion() {
    }

    // Getters y Setters
    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}