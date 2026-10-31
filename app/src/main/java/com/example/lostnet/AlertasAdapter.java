package com.example.lostnet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador para mostrar la lista de alertas de proximidad en un RecyclerView.
 * Muestra el mensaje, fecha y tiempo restante antes de que el reporte expire.
 */
public class AlertasAdapter extends RecyclerView.Adapter<AlertasAdapter.ViewHolder> {

    /** Callback para manejar el clic en una alerta. */
    public interface OnAlertaClickListener {
        void onAlertaClick(AlertaModelo alerta);
    }

    private final List<AlertaModelo> listaAlertas;
    private final OnAlertaClickListener listener;

    public AlertasAdapter(List<AlertaModelo> listaAlertas, OnAlertaClickListener listener) {
        this.listaAlertas = listaAlertas;
        this.listener     = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alerta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertaModelo alerta = listaAlertas.get(position);

        holder.txtMensaje.setText(alerta.getMessage());

        // El servidor devuelve timestamp en segundos; Java usa milisegundos.
        Date fecha = new Date(alerta.getTimestamp() * 1000L);
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.txtFecha.setText("Fecha: " + formato.format(fecha));

        // --- Tiempo restante del reporte referenciado ---
        if (holder.txtTiempoExpiry != null) {
            long ahoraSegundos = System.currentTimeMillis() / 1000L;
            long segundosRestantes = alerta.getSecondsUntilExpiry();

            // Fallback: calcular desde expires_at_epoch si secondsUntilExpiry no viene
            if (segundosRestantes <= 0 && alerta.getExpiresAtEpoch() > 0) {
                segundosRestantes = alerta.getExpiresAtEpoch() - ahoraSegundos;
            }

            if (segundosRestantes > 0) {
                holder.txtTiempoExpiry.setText(formatearTiempoRestante(segundosRestantes));
                holder.txtTiempoExpiry.setVisibility(View.VISIBLE);
            } else {
                holder.txtTiempoExpiry.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onAlertaClick(alerta));
    }

    @Override
    public int getItemCount() {
        return listaAlertas.size();
    }

    /** Convierte segundos restantes en texto legible. */
    private String formatearTiempoRestante(long segundos) {
        return LostNetUtils.textoExpiracionAlerta(segundos);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtMensaje;
        final TextView txtFecha;
        final TextView txtTiempoExpiry;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMensaje      = itemView.findViewById(R.id.txtMensajeAlerta);
            txtFecha        = itemView.findViewById(R.id.txtFechaAlerta);
            txtTiempoExpiry = itemView.findViewById(R.id.txtTiempoExpiryAlerta);
        }
    }
}
