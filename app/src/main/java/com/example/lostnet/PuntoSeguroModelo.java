package com.example.lostnet;

import com.google.gson.annotations.SerializedName;

public class PuntoSeguroModelo {
    private int id;

    @SerializedName("nombre") // Así se llama en el JSON del server
    private String nombre;

    @SerializedName("lat")
    private double lat;

    @SerializedName("lon")
    private double lon;

    @SerializedName("tipo")
    private String tipo; // Ej: "Oficial", "Público"

    // Getters
    public String getNombre() { return nombre; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getTipo() { return tipo; }
}