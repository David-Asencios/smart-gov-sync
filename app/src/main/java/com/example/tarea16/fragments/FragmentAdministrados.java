package com.example.tarea16.fragments;

import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Administrado;

import java.util.ArrayList;
import java.util.List;

public class FragmentAdministrados extends SimpleListFragment {
    @Override
    protected String descripcion() {
        return "Personas naturales o juridicas que presentan documentos. El registro integrado verifica DNI/RUC para evitar duplicidades.";
    }

    @Override
    protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        List<SimpleTextAdapter.Item> items = new ArrayList<>();
        for (Administrado item : db.administradoDao().listar()) {
            items.add(new SimpleTextAdapter.Item(
                    item.nombreRazonSocial,
                    "DNI/RUC: " + item.dniRuc + "  Telefono: " + item.telefono,
                    estado(item.sincronizado)));
        }
        return items;
    }
}
