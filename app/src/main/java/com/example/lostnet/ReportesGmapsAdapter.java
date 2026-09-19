package com.example.lostnet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adapter para mostrar reportes en estilo Google Maps (bottom sheet).
 * Muestra una thumbnail cuadrada, descripción, categoría y estado.
 */
public class ReportesGmapsAdapter extends RecyclerView.Adapter<ReportesGmapsAdapter.ViewHolder> {

    public interface OnReporteClickListener {
        void onReporteClick(ReporteModelo reporte);
    }

    private final Context context;
    private final List<ReporteModelo> lista;
    private final OnReporteClickListener listener;
    private List<String> idsConNovedad; // IDs que deben mostrar el punto rojo
    private static final String BASE_URL = AppConstants.BASE_URL;

    public ReportesGmapsAdapter(Context context, List<ReporteModelo> lista, OnReporteClickListener listener) {
        this.context = context;
        this.lista = lista;
        this.listener = listener;
    }

    public void setIdsConNovedad(List<String> ids) {
        this.idsConNovedad = ids;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reporte_gmaps, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReporteModelo r = lista.get(position);

        // Descripción
        holder.txtDesc.setText(r.getDescription() != null ? r.getDescription() : "Sin descripción");

        // Categoría · Estado
        String cat = r.getCategory() != null ? r.getCategory() : "General";
        String status = "FOUND".equals(r.getStatus()) ? "Encontrado" : "Perdido";
        holder.txtCatEstado.setText(cat + " · " + status);

        // Badge de estado
        if ("FOUND".equals(r.getStatus())) {
            holder.txtEstadoBadge.setText("🟢 ENCONTRADO");
            holder.txtEstadoBadge.setTextColor(0xFF0D8A4E);
        } else {
            holder.txtEstadoBadge.setText("🔴 PERDIDO");
            holder.txtEstadoBadge.setTextColor(0xFFD93025);
        }

        // Imagen
        String photoUrl = r.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            if (photoUrl.startsWith("/")) photoUrl = photoUrl.substring(1);
            Glide.with(context)
                    .load(BASE_URL + photoUrl)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_menu_camera)
                    .centerCrop()
                    .into(holder.imgThumb);
        } else {
            holder.imgThumb.setImageResource(android.R.drawable.ic_menu_camera);
        }

        // Punto de notificación (novedades)
        if (idsConNovedad != null && idsConNovedad.contains(r.getId())) {
            holder.dot.setVisibility(View.VISIBLE);
        } else {
            holder.dot.setVisibility(View.GONE);
        }

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Al hacer click, podríamos limpiar la "novedad" localmente
                if (idsConNovedad != null) idsConNovedad.remove(r.getId());
                holder.dot.setVisibility(View.GONE);
                listener.onReporteClick(r);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        TextView txtDesc, txtCatEstado, txtEstadoBadge;
        View dot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgGmaps);
            txtDesc = itemView.findViewById(R.id.txtGmapsDesc);
            txtCatEstado = itemView.findViewById(R.id.txtGmapsCatEstado);
            txtEstadoBadge = itemView.findViewById(R.id.txtGmapsEstadoBadge);
            dot = itemView.findViewById(R.id.dotNotificacion);
        }
    }
}
