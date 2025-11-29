package com.example.lostnet;

public class ReporteModelo {
    String user_id;
    String email;
    String phone;
    String description;
    double latitude;
    double longitude;
    String fcm_token;

    public ReporteModelo(String uid, String mail, String cel, String desc, double lat, double lon,String token) {
        this.user_id = uid;
        this.email = mail;
        this.phone = cel;
        this.description = desc;
        this.latitude = lat;
        this.longitude = lon;
        this.fcm_token = token;
    }
}
