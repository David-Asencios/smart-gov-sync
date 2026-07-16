package com.example.tarea16.fragments;

import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.TipoDocumento;

import java.util.ArrayList;
import java.util.List;

public class FragmentTiposDocumentos extends SimpleListFragment {
    @Override
    protected String descripcion() {
        return "Gestiona los tipos de documentos disponibles para nuevos registros. Los tipos inactivos se conservan para trazabilidad historica.";
    }

    @Override
    protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        List<SimpleTextAdapter.Item> items = new ArrayList<>();
        for (TipoDocumento item : db.tipoDocumentoDao().listar()) {
            items.add(new SimpleTextAdapter.Item(
                    item.nombreTipoDocumento,
                    "ID: " + item.idTipoDocumento + " / " + (item.deleted ? "Inactivo" : "Activo"),
                    item.deleted ? "INACTIVO" : estado(item.sincronizado)));
        }
        return items;
    }
}
