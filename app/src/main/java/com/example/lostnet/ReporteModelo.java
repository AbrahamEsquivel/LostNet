package com.example.lostnet; // Asegúrate que sea tu paquete

import com.google.gson.annotations.SerializedName;

public class ReporteModelo {
    @SerializedName("id")
    private String id;
    @SerializedName("desc")
    private String description;
    @SerializedName("lat")
    private double latitude;
    @SerializedName("lon")
    private double longitude;
    @SerializedName("security_question")
    private String securityQuestion;
    @SerializedName("security_answer")
    private String securityAnswer;
    @SerializedName("status")
    private String status;
    @SerializedName("photo_url")
    private String photoUrl;

    // --- NUEVO CAMPO NECESARIO ---
    @SerializedName("user_id")
    private String userId;

    private String category;
    private String email; // Asegúrate que el JSON trae este campo si lo quieres mostrar
    private String phone; // Lo mismo con el teléfono



    // Getters
    public String getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public String getDescription() { return description; }

    public String getSecurityQuestion() { return securityQuestion; }
    public String getSecurityAnswer() { return securityAnswer; }
    public String getPhotoUrl() { return photoUrl; }
    public String getUserId() { return userId; } // Getter nuevo
    public String getCategory() { return category; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}