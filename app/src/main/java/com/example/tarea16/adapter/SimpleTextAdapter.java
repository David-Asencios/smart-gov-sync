package com.example.tarea16.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tarea16.R;

import java.util.ArrayList;
import java.util.List;

public class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.Holder> {
    public interface Listener {
        void onClick(Item item);
    }

    private final List<Item> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Item> data) {
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
        Item item = items.get(position);
        holder.titulo.setText(item.title);
        holder.detalle.setText(item.detail);
        SyncStatusText.applyStatic(holder.estado, holder.icono, item.status);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class Item {
        public final String title;
        public final String detail;
        public final String status;
        public final Object source;

        public Item(String title, String detail, String status) {
            this(title, detail, status, null);
        }

        public Item(String title, String detail, String status, Object source) {
            this.title = title;
            this.detail = detail;
            this.status = status;
            this.source = source;
        }
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
