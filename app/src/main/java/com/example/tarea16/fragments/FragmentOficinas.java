package com.example.tarea16.fragments;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.tarea16.R;
import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Oficina;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentOficinas extends SimpleListFragment {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected String descripcion() {
        return "";
    }

    @Override
    protected String primaryActionText() {
        return "Nueva oficina";
    }

    @Override
    protected void onPrimaryAction() {
        mostrarFormulario(null);
    }

    @Override
    protected void onItemSelected(SimpleTextAdapter.Item item) {
        if (!(item.source instanceof Oficina)) return;
        Oficina oficina = (Oficina) item.source;
        String estado = oficina.deleted ? "Habilitar" : "Deshabilitar";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(oficina.nombreUnidad)
                .setItems(new String[]{"Editar", estado}, (dialog, which) -> {
                    if (which == 0) mostrarFormulario(oficina);
                    else cambiarEstado(oficina);
                })
                .show();
    }

    @Override
    protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        List<SimpleTextAdapter.Item> items = new ArrayList<>();
        for (Oficina item : db.oficinaDao().listar()) {
            items.add(new SimpleTextAdapter.Item(
                    item.nombreUnidad,
                    safe(item.codigoOficina) + " / " + safe(item.siglasOficiales)
                            + " / " + (item.deleted ? "Inactiva" : "Activa"),
                    item.deleted ? "INACTIVO" : estado(item.sincronizado),
                    item));
        }
        return items;
    }

    private void mostrarFormulario(Oficina actual) {
        Context context = requireContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);

        EditText codigo = input(context, "Codigo de oficina");
        EditText siglas = input(context, "Siglas");
        EditText nombre = input(context, "Nombre de unidad");
        if (actual != null) {
            codigo.setText(actual.codigoOficina);
            siglas.setText(actual.siglasOficiales);
            nombre.setText(actual.nombreUnidad);
        }
        layout.addView(codigo);
        layout.addView(siglas);
        layout.addView(nombre);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(actual == null ? "Nueva oficina" : "Editar oficina")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.guardar, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String cod = codigo.getText().toString().trim();
                    String nom = nombre.getText().toString().trim();
                    if (cod.isEmpty() || nom.isEmpty()) {
                        Toast.makeText(context, "Codigo y nombre son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    guardar(actual, cod, siglas.getText().toString().trim(), nom);
                }));
        dialog.show();
    }

    private void guardar(Oficina actual, String codigo, String siglas, String nombre) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            Oficina item = actual == null ? new Oficina() : actual;
            item.codigoOficina = codigo;
            item.siglasOficiales = siglas;
            item.nombreUnidad = nombre;
            item.updatedAt = System.currentTimeMillis();
            item.sincronizado = false;
            item.syncStatus = "PENDING";
            item.syncError = null;
            if (actual == null) AppDatabase.getInstance(app).oficinaDao().insertar(item);
            else AppDatabase.getInstance(app).oficinaDao().actualizar(item);
            if (isAdded()) requireActivity().runOnUiThread(this::recargar);
        });
    }

    private void cambiarEstado(Oficina oficina) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            oficina.deleted = !oficina.deleted;
            oficina.updatedAt = System.currentTimeMillis();
            oficina.sincronizado = false;
            oficina.syncStatus = "PENDING";
            oficina.syncError = null;
            AppDatabase.getInstance(app).oficinaDao().actualizar(oficina);
            if (isAdded()) requireActivity().runOnUiThread(this::recargar);
        });
    }

    private EditText input(Context context, String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        return input;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
