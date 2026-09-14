package com.example.lostnet;

import com.google.gson.annotations.SerializedName;

/** Representa un reporte de objeto perdido o encontrado en el sistema LostNet. */
public class ReporteModelo {

    @SerializedName("id")
    private String id;

    @SerializedName("desc_short")
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

    @SerializedName("user_id")
    private String userId;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("category")
    private String category;

    // --- Getters ---

    public String getId()               { return id; }
    public String getDescription()      { return description; }
    public double getLatitude()         { return latitude; }
    public double getLongitude()        { return longitude; }
    public String getSecurityQuestion() { return securityQuestion; }
    public String getSecurityAnswer()   { return securityAnswer; }
    public String getStatus()           { return status; }
    public String getPhotoUrl()         { return photoUrl; }
    public String getUserId()           { return userId; }
    public String getEmail()            { return email; }
    public String getPhone()            { return phone; }
    public String getCategory()         { return category; }
}