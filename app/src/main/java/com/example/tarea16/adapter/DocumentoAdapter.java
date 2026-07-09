package com.example.tarea16.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tarea16.R;
import com.example.tarea16.modelo.DocumentoIngresado;

import java.util.ArrayList;
import java.util.List;

public class DocumentoAdapter extends RecyclerView.Adapter<DocumentoAdapter.Holder> {
    private final List<DocumentoIngresado> items = new ArrayList<>();

    public void setItems(List<DocumentoIngresado> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_documento, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DocumentoIngresado item = items.get(position);
        holder.titulo.setText(item.nroDocumentoUnico);
        holder.detalle.setText("Folios: " + item.cantidadFolios);
        holder.estado.setText(item.sincronizado ? "SINCRONIZADO" : "PENDIENTE");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView titulo;
        TextView detalle;
        TextView estado;

        Holder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.txtTitulo);
            detalle = itemView.findViewById(R.id.txtDetalle);
            estado = itemView.findViewById(R.id.txtEstadoSync);
        }
    }
}
