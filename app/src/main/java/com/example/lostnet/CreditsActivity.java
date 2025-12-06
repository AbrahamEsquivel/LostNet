package com.example.lostnet;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CreditsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);

        // Botón volver
        findViewById(R.id.btnBackCredits).setOnClickListener(v -> finish());
    }
}