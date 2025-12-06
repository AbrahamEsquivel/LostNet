package com.example.lostnet;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Botón volver
        findViewById(R.id.btnBackAbout).setOnClickListener(v -> finish());
    }
}