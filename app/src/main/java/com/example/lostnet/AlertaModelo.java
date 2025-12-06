package com.example.lostnet;

import com.google.gson.annotations.SerializedName;

public class AlertaModelo {
    @SerializedName("id")
    private String id;

    @SerializedName("message")
    private String message;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("read")
    private boolean read;

    // --- AGREGAR ESTOS CAMPOS NUEVOS ---
    @SerializedName("lat_objeto") // Debe coincidir con el JSON del servidor (T7_buzon)
    private double latObjeto;

    @SerializedName("lon_objeto")
    private double lonObjeto;
    // ----------------------------------

    // Getters
    public String getId() { return id; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }

    // Getters Nuevos
    public double getLatObjeto() { return latObjeto; }
    public double getLonObjeto() { return lonObjeto; }
}