package com.example.lostnet;

public class UbicacionRequest {
    private String email;
    private double lat;
    private double lon;

    public UbicacionRequest(String email, double lat, double lon) {
        this.email = email;
        this.lat = lat;
        this.lon = lon;
    }

    // Getters y Setters (opcionales si usas Gson directo, pero buena práctica)
}