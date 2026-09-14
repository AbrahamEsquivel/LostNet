package com.example.lostnet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adaptador para mostrar la lista de reportes propios del usuario.
 */
public class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.ViewHolder> {

    /** Callbacks de interacción con cada ítem de la lista. */
    public interface OnItemClickListener {
        void onEliminarClick(String idReporte, int position);
        void onItemClick(ReporteModelo reporte);
    }

    private final List<ReporteModelo> lista;
    private final OnItemClickListener listener;
    private final Context context;

    public ReportesAdapter(Context context, List<ReporteModelo> lista, OnItemClickListener listener) {
        this.context  = context;
        this.lista    = lista;
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

        holder.txtDescripcion.setText(reporte.getDescription());
        holder.txtEstado.setText("Estado: PERDIDO 🔴");
        holder.txtCategoria.setText("📂 " + (reporte.getCategory() != null ? reporte.getCategory() : "Otros"));

        String pregunta = reporte.getSecurityQuestion();
        String respuesta = reporte.getSecurityAnswer();
        holder.txtPregunta.setText("🔒 P: " + (pregunta != null ? pregunta : "N/A"));
        holder.txtRespuesta.setText("🔑 R: " + (respuesta != null ? respuesta : "N/A"));

        holder.txtIdOculto.setText(reporte.getId());

        cargarImagen(holder.imgReporte, reporte.getPhotoUrl());

        holder.btnEliminar.setOnClickListener(v -> listener.onEliminarClick(reporte.getId(), position));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(reporte));
    }

    private void cargarImagen(ImageView imageView, String rutaFoto) {
        if (rutaFoto != null && !rutaFoto.isEmpty()) {
            String urlCompleta = AppConstants.BASE_URL + rutaFoto;
            Glide.with(context)
                    .load(urlCompleta)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.stat_notify_error)
                    .into(imageView);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtDescripcion;
        final TextView txtEstado;
        final TextView txtIdOculto;
        final TextView txtPregunta;
        final TextView txtRespuesta;
        final TextView txtCategoria;
        final ImageView imgReporte;
        final Button btnEliminar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcion);
            txtEstado      = itemView.findViewById(R.id.txtEstado);
            txtIdOculto    = itemView.findViewById(R.id.txtIdOculto);
            imgReporte     = itemView.findViewById(R.id.imgReporte);
            btnEliminar    = itemView.findViewById(R.id.btnEliminar);
            txtPregunta    = itemView.findViewById(R.id.txtPregunta);
            txtRespuesta   = itemView.findViewById(R.id.txtRespuesta);
            txtCategoria   = itemView.findViewById(R.id.txtCategoria);
        }
    }
}