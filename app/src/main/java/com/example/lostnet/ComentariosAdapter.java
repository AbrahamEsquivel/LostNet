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
 * Adaptador para mostrar la sección de comentarios dentro del detalle de un reporte.
 */
public class ComentariosAdapter extends RecyclerView.Adapter<ComentariosAdapter.ViewHolder> {

    private final List<ComentarioModelo> lista;

    public ComentariosAdapter(List<ComentarioModelo> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComentarioModelo comentario = lista.get(position);

        // El servidor devuelve timestamp en segundos; Java usa milisegundos.
        Date fecha = new Date(comentario.getTimestamp() * 1000L);
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

        holder.txtNombre.setText(comentario.getUserName() + " (" + formato.format(fecha) + ")");
        holder.txtMensaje.setText(comentario.getText());
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtNombre;
        final TextView txtMensaje;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre  = itemView.findViewById(android.R.id.text1);
            txtMensaje = itemView.findViewById(android.R.id.text2);
        }
    }
}