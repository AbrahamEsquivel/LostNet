package com.example.lostnet;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Pantalla de información general sobre la aplicación LostNet. */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.btnBackAbout).setOnClickListener(v -> finish());
    }
}