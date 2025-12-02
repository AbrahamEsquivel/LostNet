package com.example.lostnet;

import com.google.gson.annotations.SerializedName;

public class AlertaModelo {
    @SerializedName("id")
    private String id;

    @SerializedName("message")
    private String message; // "🚨 ALERTA: Se perdió..."

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("read")
    private boolean read;

    // Getters
    public String getId() { return id; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
}