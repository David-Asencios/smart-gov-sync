package com.example.tarea16.fragments;

import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.ArchivoResumen;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FragmentHistorialArchivamiento extends SimpleListFragment {
    @Override
    protected String descripcion() {
        return "";
    }

    @Override
    protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        List<SimpleTextAdapter.Item> items = new ArrayList<>();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (ArchivoResumen item : db.actaDao().listarResumen()) {
            items.add(new SimpleTextAdapter.Item(
                    item.nroActaUnico,
                    "Expediente: " + item.nroExpedienteAnual
                            + "\nFecha: " + format.format(new Date(item.fechaHoraGuardado))
                            + "\nUbicacion: " + item.codigoAlmacen
                            + " / Pabellon " + item.nroPabellon
                            + " / Estante " + item.nroEstante
                            + " / Caja " + item.nroCajaFisica
                            + "\nCostos: digitalización " + String.format(java.util.Locale.US, "%.2f", item.costoDigitalizacion)
                            + " + custodia " + String.format(java.util.Locale.US, "%.2f", item.costoArancelCustodia)
                            + " = " + String.format(java.util.Locale.US, "%.2f", item.costoFinalProcesamiento)
                            + "\nDerivacion ID: " + item.idDerivacion,
                    estado(item.sincronizado)));
        }
        return items;
    }
}
