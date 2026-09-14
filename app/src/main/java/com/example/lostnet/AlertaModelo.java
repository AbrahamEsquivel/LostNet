package com.example.lostnet;

import com.google.gson.annotations.SerializedName;

/** Representa una alerta de proximidad enviada al usuario cuando hay un reporte cercano. */
public class AlertaModelo {

    @SerializedName("id")
    private String id;

    @SerializedName("message")
    private String message;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("read")
    private boolean read;

    @SerializedName("lat_objeto")
    private double latObjeto;

    @SerializedName("lon_objeto")
    private double lonObjeto;

    // --- Getters ---

    public String getId()        { return id; }
    public String getMessage()   { return message; }
    public long getTimestamp()   { return timestamp; }
    public boolean isRead()      { return read; }
    public double getLatObjeto() { return latObjeto; }
    public double getLonObjeto() { return lonObjeto; }
}