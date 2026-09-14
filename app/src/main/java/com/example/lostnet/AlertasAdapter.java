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

/**
 * Adaptador para mostrar la lista de alertas de proximidad en un RecyclerView.
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

        holder.itemView.setOnClickListener(v -> listener.onAlertaClick(alerta));
    }

    @Override
    public int getItemCount() {
        return listaAlertas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtMensaje;
        final TextView txtFecha;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMensaje = itemView.findViewById(R.id.txtMensajeAlerta);
            txtFecha   = itemView.findViewById(R.id.txtFechaAlerta);
        }
    }
}