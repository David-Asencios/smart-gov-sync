package com.example.tarea16.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tarea16.R;
import com.example.tarea16.modelo.HojaRuta;

import java.util.ArrayList;
import java.util.List;

public class DerivacionAdapter extends RecyclerView.Adapter<DerivacionAdapter.Holder> {
    public interface Accion {
        void ejecutar(HojaRuta item);
    }

    private final List<HojaRuta> items = new ArrayList<>();
    private Accion recibido;
    private Accion rechazado;
    private Accion mapa;
    private Accion detalle;
    private boolean mostrarAcciones;
    private boolean mostrarAccionSecundaria = true;
    private int textoAccionPrimaria = R.string.recibido;
    private int textoAccionSecundaria = R.string.rechazado;

    public DerivacionAdapter(boolean mostrarAcciones) {
        this.mostrarAcciones = mostrarAcciones;
    }

    public void setAcciones(Accion recibido, Accion rechazado, Accion mapa) {
        this.recibido = recibido;
        this.rechazado = rechazado;
        this.mapa = mapa;
    }

    public void setTextosAcciones(int primaria, int secundaria) {
        textoAccionPrimaria = primaria;
        textoAccionSecundaria = secundaria;
    }

    public void setDetalle(Accion detalle) {
        this.detalle = detalle;
    }

    public void setMostrarAccionSecundaria(boolean mostrar) {
        mostrarAccionSecundaria = mostrar;
    }

    public void setItems(List<HojaRuta> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_derivacion, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HojaRuta item = items.get(position);
        holder.titulo.setText(item.codigoBarrasSeguimiento);
        holder.detalle.setText(item.estadoDerivacion + "  " + item.prioridadEnvio);
        SyncStatusText.apply(holder.estado, holder.icono, item, item.sincronizado);
        holder.recibido.setVisibility(mostrarAcciones ? View.VISIBLE : View.GONE);
        holder.rechazado.setVisibility(mostrarAcciones && mostrarAccionSecundaria ? View.VISIBLE : View.GONE);
        holder.recibido.setText(textoAccionPrimaria);
        holder.rechazado.setText(textoAccionSecundaria);
        holder.recibido.setOnClickListener(v -> {
            if (recibido != null) recibido.ejecutar(item);
        });
        holder.rechazado.setOnClickListener(v -> {
            if (rechazado != null) rechazado.ejecutar(item);
        });
        holder.mapa.setOnClickListener(v -> {
            if (mapa != null) mapa.ejecutar(item);
        });
        holder.itemView.setOnClickListener(v -> {
            if (detalle != null) detalle.ejecutar(item);
        });
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
        Button recibido;
        Button rechazado;
        Button mapa;

        Holder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.txtTitulo);
            detalle = itemView.findViewById(R.id.txtDetalle);
            estado = itemView.findViewById(R.id.txtEstadoSync);
            icono = itemView.findViewById(R.id.imgSyncStatus);
            recibido = itemView.findViewById(R.id.btnRecibido);
            rechazado = itemView.findViewById(R.id.btnRechazado);
            mapa = itemView.findViewById(R.id.btnMapa);
        }
    }
}
