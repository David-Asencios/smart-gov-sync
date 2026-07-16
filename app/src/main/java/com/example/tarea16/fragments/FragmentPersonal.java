package com.example.tarea16.fragments;

import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.tarea16.R;
import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Oficina;
import com.example.tarea16.modelo.Personal;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentPersonal extends SimpleListFragment {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override protected String descripcion() { return "Registra trabajadores y asígnalos a una oficina antes de crear sus cuentas."; }
    @Override protected String primaryActionText() { return "Nuevo trabajador"; }
    @Override protected void onPrimaryAction() { mostrarFormulario(null); }

    @Override protected void onItemSelected(SimpleTextAdapter.Item selected) {
        if (!(selected.source instanceof Personal)) return;
        Personal item = (Personal) selected.source;
        String estado = item.deleted ? "Habilitar" : "Deshabilitar";
        new MaterialAlertDialogBuilder(requireContext()).setTitle(item.nombreCompleto)
                .setItems(new String[]{"Editar", estado}, (dialog, which) -> {
                    if (which == 0) mostrarFormulario(item); else cambiarEstado(item);
                }).show();
    }

    @Override protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        List<SimpleTextAdapter.Item> result = new ArrayList<>();
        for (Personal item : db.personalDao().listar()) {
            result.add(new SimpleTextAdapter.Item(item.nombreCompleto,
                    valor(item.codigoEmpleado) + " / " + valor(item.cargo) + " / Oficina " + item.idOficina,
                    item.deleted ? "INACTIVO" : estado(item.sincronizado), item));
        }
        return result;
    }

    private void mostrarFormulario(Personal actual) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            List<Oficina> oficinas = AppDatabase.getInstance(app).oficinaDao().listar();
            oficinas.removeIf(item -> item.deleted);
            if (isAdded()) requireActivity().runOnUiThread(() -> formulario(actual, oficinas));
        });
    }

    private void formulario(Personal actual, List<Oficina> oficinas) {
        if (oficinas.isEmpty()) {
            Toast.makeText(requireContext(), "Primero registra una oficina activa", Toast.LENGTH_LONG).show();
            return;
        }
        Context context = requireContext();
        LinearLayout layout = new LinearLayout(context); layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);
        EditText codigo = input(context, "Código de empleado");
        EditText nombre = input(context, "Nombre completo");
        EditText cargo = input(context, "Cargo");
        Spinner oficina = new Spinner(context);
        List<String> nombres = new ArrayList<>();
        for (Oficina item : oficinas) nombres.add(item.nombreUnidad);
        oficina.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, nombres));
        if (actual != null) {
            codigo.setText(actual.codigoEmpleado); nombre.setText(actual.nombreCompleto); cargo.setText(actual.cargo);
            for (int i = 0; i < oficinas.size(); i++) if (oficinas.get(i).idOficina == actual.idOficina) oficina.setSelection(i);
        }
        layout.addView(codigo); layout.addView(nombre); layout.addView(cargo);
        layout.addView(oficina, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(actual == null ? "Nuevo trabajador" : "Editar trabajador").setView(layout)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.guardar, null).create();
        dialog.setOnShowListener(x -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String cod = codigo.getText().toString().trim(), nom = nombre.getText().toString().trim();
            if (cod.isEmpty() || nom.isEmpty()) { Toast.makeText(context, "Código y nombre son obligatorios", Toast.LENGTH_SHORT).show(); return; }
            dialog.dismiss(); guardar(actual, cod, nom, cargo.getText().toString().trim(), oficinas.get(oficina.getSelectedItemPosition()).idOficina);
        }));
        dialog.show();
    }

    private void guardar(Personal actual, String codigo, String nombre, String cargo, int oficinaId) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            Personal item = actual == null ? new Personal() : actual;
            item.codigoEmpleado = codigo; item.nombreCompleto = nombre; item.cargo = cargo; item.idOficina = oficinaId;
            item.updatedAt = System.currentTimeMillis(); item.sincronizado = false; item.syncStatus = "PENDING"; item.syncError = null;
            if (actual == null) AppDatabase.getInstance(app).personalDao().insertar(item); else AppDatabase.getInstance(app).personalDao().actualizar(item);
            if (isAdded()) requireActivity().runOnUiThread(this::recargar);
        });
    }

    private void cambiarEstado(Personal item) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            item.deleted = !item.deleted; item.updatedAt = System.currentTimeMillis(); item.sincronizado = false;
            item.syncStatus = "PENDING"; item.syncError = null; AppDatabase.getInstance(app).personalDao().actualizar(item);
            if (isAdded()) requireActivity().runOnUiThread(this::recargar);
        });
    }

    private EditText input(Context context, String hint) { EditText v = new EditText(context); v.setHint(hint); v.setSingleLine(); v.setInputType(InputType.TYPE_CLASS_TEXT); return v; }
    private String valor(String value) { return value == null || value.trim().isEmpty() ? "-" : value; }
}
