package com.example.lostnet;
import com.google.gson.annotations.SerializedName;

public class ComentarioModelo {
    @SerializedName("user_name")
    private String userName;
    @SerializedName("text")
    private String text;
    @SerializedName("timestamp")
    private long timestamp;

    public String getUserName() { return userName; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
}