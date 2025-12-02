package com.example.lostnet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlertasAdapter extends RecyclerView.Adapter<AlertasAdapter.ViewHolder> {

    private List<AlertaModelo> lista;

    public AlertasAdapter(List<AlertaModelo> lista) {
        this.lista = lista;
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
        AlertaModelo alerta = lista.get(position);
        holder.txtMensaje.setText(alerta.getMessage());
        // Aquí podrías formatear la fecha 'timestamp' si quisieras
        holder.txtFecha.setText("ID: " + alerta.getId());
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtMensaje, txtFecha;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMensaje = itemView.findViewById(R.id.txtMensajeAlerta);
            txtFecha = itemView.findViewById(R.id.txtFechaAlerta);
        }
    }
}