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

public class AlertasAdapter extends RecyclerView.Adapter<AlertasAdapter.ViewHolder> {

    private List<AlertaModelo> listaAlertas;

    public AlertasAdapter(List<AlertaModelo> listaAlertas) {
        this.listaAlertas = listaAlertas;
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

        // 1. Asignar el mensaje principal
        holder.txtMensaje.setText(alerta.getMessage());

        // 2. Convertir el timestamp (números) a fecha legible
        Date date = new Date(alerta.getTimestamp() * 1000L); // x1000 porque Python usa segundos y Java milisegundos
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.txtFecha.setText("Fecha: " + sdf.format(date));
    }

    @Override
    public int getItemCount() {
        return listaAlertas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtMensaje, txtFecha;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Estos IDs deben coincidir con tu item_alerta.xml
            txtMensaje = itemView.findViewById(R.id.txtMensajeAlerta);
            txtFecha = itemView.findViewById(R.id.txtFechaAlerta);
        }
    }
}