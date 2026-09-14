package com.example.lostnet;

/** Cuerpo de la petición para actualizar la ubicación del usuario en el servidor. */
public class UbicacionRequest {

    private final String email;
    private final double lat;
    private final double lon;

    public UbicacionRequest(String email, double lat, double lon) {
        this.email = email;
        this.lat   = lat;
        this.lon   = lon;
    }

    public String getEmail() { return email; }
    public double getLat()   { return lat; }
    public double getLon()   { return lon; }
}