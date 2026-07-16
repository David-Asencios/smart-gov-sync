package com.example.tarea16.fragments;

import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Oficina;

import java.util.ArrayList;
import java.util.List;

public class FragmentOficinas extends SimpleListFragment {
    @Override
    protected String descripcion() {
        return "Administra las oficinas que participan en el flujo documentario. Las oficinas inactivas no deben usarse como destino en nuevas derivaciones.";
    }

    @Override
    protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        List<SimpleTextAdapter.Item> items = new ArrayList<>();
        for (Oficina item : db.oficinaDao().listar()) {
            items.add(new SimpleTextAdapter.Item(
                    item.nombreUnidad,
                    item.codigoOficina + " / " + item.siglasOficiales
                            + " / " + (item.deleted ? "Inactiva" : "Activa"),
                    item.deleted ? "INACTIVO" : estado(item.sincronizado)));
        }
        return items;
    }
}
