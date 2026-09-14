package com.example.lostnet;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Pantalla de créditos del equipo de desarrollo de LostNet. */
public class CreditsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);

        findViewById(R.id.btnBackCredits).setOnClickListener(v -> finish());
    }
}