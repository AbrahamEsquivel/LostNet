package com.example.lostnet;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Servicio que maneja los mensajes push de Firebase Cloud Messaging (FCM).
 * Firebase muestra la notificación automáticamente cuando la app está en segundo plano.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        if (message.getNotification() != null) {
            Log.d(TAG, "Mensaje recibido: " + message.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Nuevo token FCM: " + token);
        // TODO: Enviar el token al servidor para habilitar notificaciones personalizadas.
    }
}