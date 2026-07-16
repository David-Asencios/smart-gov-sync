package com.example.tarea16.fragments;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.tarea16.R;
import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.TipoDocumento;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentTiposDocumentos extends SimpleListFragment {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected String descripcion() {
        return "";
    }

    @Override
    protected String primaryActionText() {
        return "Nuevo tipo de documento";
    }

    @Override
    protected void onPrimaryAction() {
        mostrarFormulario(null);
    }

    @Override
    protected void onItemSelected(SimpleTextAdapter.Item item) {
        if (!(item.source instanceof TipoDocumento)) return;
        TipoDocumento tipo = (TipoDocumento) item.source;
        String estado = tipo.deleted ? "Habilitar" : "Deshabilitar";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(tipo.nombreTipoDocumento)
                .setItems(new String[]{"Editar", estado}, (dialog, which) -> {
                    if (which == 0) mostrarFormulario(tipo);
                    else cambiarEstado(tipo);
                })
                .show();
    }

    @Override
    protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        List<SimpleTextAdapter.Item> items = new ArrayList<>();
        for (TipoDocumento item : db.tipoDocumentoDao().listar()) {
            items.add(new SimpleTextAdapter.Item(
                    item.nombreTipoDocumento,
                    "ID: " + item.idTipoDocumento + " / " + (item.deleted ? "Inactivo" : "Activo"),
                    item.deleted ? "INACTIVO" : estado(item.sincronizado),
                    item));
        }
        return items;
    }

    private void mostrarFormulario(TipoDocumento actual) {
        Context context = requireContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);

        EditText nombre = new EditText(context);
        nombre.setHint("Nombre del tipo de documento");
        nombre.setSingleLine(true);
        nombre.setInputType(InputType.TYPE_CLASS_TEXT);
        if (actual != null) nombre.setText(actual.nombreTipoDocumento);
        layout.addView(nombre);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(actual == null ? "Nuevo tipo de documento" : "Editar tipo de documento")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.guardar, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = nombre.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    guardar(actual, value);
                }));
        dialog.show();
    }

    private void guardar(TipoDocumento actual, String nombre) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            TipoDocumento item = actual == null ? new TipoDocumento() : actual;
            item.nombreTipoDocumento = nombre;
            item.updatedAt = System.currentTimeMillis();
            item.sincronizado = false;
            item.syncStatus = "PENDING";
            item.syncError = null;
            if (actual == null) AppDatabase.getInstance(app).tipoDocumentoDao().insertar(item);
            else AppDatabase.getInstance(app).tipoDocumentoDao().actualizar(item);
            com.example.tarea16.sync.SyncScheduler.trigger(app);
            if (isAdded()) requireActivity().runOnUiThread(this::recargar);
        });
    }

    private void cambiarEstado(TipoDocumento tipo) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            tipo.deleted = !tipo.deleted;
            tipo.updatedAt = System.currentTimeMillis();
            tipo.sincronizado = false;
            tipo.syncStatus = "PENDING";
            tipo.syncError = null;
            AppDatabase.getInstance(app).tipoDocumentoDao().actualizar(tipo);
            com.example.tarea16.sync.SyncScheduler.trigger(app);
            if (isAdded()) requireActivity().runOnUiThread(this::recargar);
        });
    }
}
