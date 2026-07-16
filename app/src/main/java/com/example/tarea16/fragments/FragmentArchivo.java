package com.example.tarea16.fragments;

import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.ArchivoResumen;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FragmentArchivo extends SimpleListFragment {
    @Override
    protected String descripcion() {
        return getString(com.example.tarea16.R.string.archivo_fisico_description);
    }

    @Override
    protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        List<SimpleTextAdapter.Item> items = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (ArchivoResumen item : db.actaDao().listarResumen()) {
            String titulo = safe(item.nroExpedienteAnual, item.nroActaUnico);
            String detalle = "Archivo: " + item.codigoAlmacen
                    + "\nUbicacion: Pabellon/Nivel " + item.nroPabellon
                    + "  Estante " + item.nroEstante
                    + "  Caja " + item.nroCajaFisica
                    + "\nFecha: " + format.format(new Date(item.fechaHoraGuardado))
                    + "\nAsunto: " + safe(item.asuntoGeneral, "Sin asunto");
            items.add(new SimpleTextAdapter.Item(titulo, detalle, estado(item.sincronizado)));
        }
        return items;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
