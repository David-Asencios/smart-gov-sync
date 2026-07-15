package com.example.tarea16.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tarea16.R;
import com.example.tarea16.modelo.Expediente;

import java.util.ArrayList;
import java.util.List;

public class ExpedienteAdapter extends RecyclerView.Adapter<ExpedienteAdapter.Holder> {
    private final List<Expediente> items = new ArrayList<>();

    public void setItems(List<Expediente> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expediente, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Expediente item = items.get(position);
        holder.titulo.setText(item.nroExpedienteAnual);
        holder.detalle.setText(item.asuntoGeneral);
        holder.estado.setText(holder.itemView.getContext().getString(item.sincronizado
                ? R.string.sync_status_synced
                : R.string.sync_status_pending));
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
