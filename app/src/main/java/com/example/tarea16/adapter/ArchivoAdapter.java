package com.example.tarea16.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tarea16.R;
import com.example.tarea16.modelo.ArchivoFisico;

import java.util.ArrayList;
import java.util.List;

public class ArchivoAdapter extends RecyclerView.Adapter<ArchivoAdapter.Holder> {
    private final List<ArchivoFisico> items = new ArrayList<>();

    public void setItems(List<ArchivoFisico> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archivo, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ArchivoFisico item = items.get(position);
        holder.titulo.setText(item.codigoAlmacen);
        holder.detalle.setText("Pabellon " + item.nroPabellon + " Estante " + item.nroEstante + " Caja " + item.nroCajaFisica);
        SyncStatusText.apply(holder.estado, holder.icono, item, item.sincronizado);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView titulo;
        TextView detalle;
        TextView estado;
        ImageView icono;

        Holder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.txtTitulo);
            detalle = itemView.findViewById(R.id.txtDetalle);
            estado = itemView.findViewById(R.id.txtEstadoSync);
            icono = itemView.findViewById(R.id.imgSyncStatus);
        }
    }
}
