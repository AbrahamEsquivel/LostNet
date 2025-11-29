package com.example.lostnet;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.util.Log;

import androidx.annotation.NonNull;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        // Aquí podrías mostrar una notificación personalizada
        // Pero Firebase la muestra automática si la app está en segundo plano
        Log.d("LostNet", "Mensaje recibido: " + message.getNotification().getBody());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("LostNet", "Nuevo Token: " + token);
        // Aquí deberías guardar el token localmente para mandarlo al login
    }
}