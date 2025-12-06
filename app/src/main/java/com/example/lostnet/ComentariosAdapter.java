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

public class ComentariosAdapter extends RecyclerView.Adapter<ComentariosAdapter.ViewHolder> {

    private List<ComentarioModelo> lista;

    public ComentariosAdapter(List<ComentarioModelo> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Usaremos un layout simple de Android para no crear otro XML
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComentarioModelo c = lista.get(position);

        // Convertir fecha
        Date date = new Date(c.getTimestamp() * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

        holder.txtNombre.setText(c.getUserName() + " (" + sdf.format(date) + ")");
        holder.txtMensaje.setText(c.getText());
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtMensaje;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(android.R.id.text1); // ID nativo de Android
            txtMensaje = itemView.findViewById(android.R.id.text2);
        }
    }
}