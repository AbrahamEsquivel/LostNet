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

public class ReportesAdapter extends RecyclerView.Adapter<ReportesAdapter.ViewHolder> {

    private List<ReporteModelo> lista;
    private OnItemClickListener listener;
    private Context context;

    // Tu IP correcta del APP-SERVER
    private static final String BASE_URL = "http://10.155.13.137:5000";

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
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_reporte, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReporteModelo reporte = lista.get(position);

        // 1. Poner textos BÁSICOS
        holder.txtDesc.setText(reporte.getDescription());
        holder.txtStatus.setText("Estado: PERDIDO 🔴");
        holder.txtCategoria.setText("📂 " + (reporte.getCategory() != null ? reporte.getCategory() : "Otros"));

        // 2. NUEVO: Mostrar Pregunta y Respuesta de Seguridad
        // (Asegúrate que tu ReporteModelo tenga getSecurityQuestion() y getSecurityAnswer())
        String preg = reporte.getSecurityQuestion();
        String resp = reporte.getSecurityAnswer();

        holder.txtPregunta.setText("🔒 P: " + (preg != null ? preg : "N/A"));
        holder.txtRespuesta.setText("🔑 R: " + (resp != null ? resp : "N/A"));

        // 3. ID Oculto (o visible si quieres debug)
        holder.txtId.setText(reporte.getId());

        // 4. Cargar IMAGEN con Glide 📸
        String rutaFoto = reporte.getPhotoUrl();

        if (rutaFoto != null && !rutaFoto.isEmpty()) {
            // concatenamos: http://10.155... + /photos/img_...
            String fullUrl = BASE_URL + rutaFoto;

            Glide.with(context)
                    .load(fullUrl)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.stat_notify_error)
                    .into(holder.imgFoto);
        } else {
            // Imagen por defecto si no hay foto en el JSON
            holder.imgFoto.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // 5. Listener del botón
        holder.btnEliminar.setOnClickListener(v -> {
            listener.onEliminarClick(reporte.getId(), position);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDesc, txtStatus, txtId;
        TextView txtPregunta, txtRespuesta; // <--- NUEVOS CAMPOS
        ImageView imgFoto;
        TextView txtCategoria;
        Button btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDesc = itemView.findViewById(R.id.txtDescripcion);
            txtStatus = itemView.findViewById(R.id.txtEstado);
            txtId = itemView.findViewById(R.id.txtIdOculto);
            imgFoto = itemView.findViewById(R.id.imgReporte);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);

            // Vinculamos los nuevos TextViews del XML
            txtPregunta = itemView.findViewById(R.id.txtPregunta);
            txtRespuesta = itemView.findViewById(R.id.txtRespuesta);
            txtCategoria = itemView.findViewById(R.id.txtCategoria);
        }
    }
}