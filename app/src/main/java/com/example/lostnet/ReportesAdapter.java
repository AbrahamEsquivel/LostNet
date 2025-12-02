package com.example.lostnet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.ViewHolder> {

    private List<ReporteModelo> lista;
    private OnItemClickListener listener;

    // Interfaz para comunicar el click a la Activity
    public interface OnItemClickListener {
        void onEliminarClick(String idReporte, int position);
    }

    public ReportesAdapter(List<ReporteModelo> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reporte, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReporteModelo reporte = lista.get(position);
        holder.txtDesc.setText(reporte.getDescription());
        holder.txtStatus.setText("ID: " + reporte.getId()); // O mostrar estado

        holder.btnEliminar.setOnClickListener(v -> {
            listener.onEliminarClick(reporte.getId(), position);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDesc, txtStatus;
        Button btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDesc = itemView.findViewById(R.id.txtDescripcion);
            txtStatus = itemView.findViewById(R.id.txtEstado);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}