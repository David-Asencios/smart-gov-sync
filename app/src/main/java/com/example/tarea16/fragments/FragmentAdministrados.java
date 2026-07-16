package com.example.tarea16.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.databinding.FragmentAdministradosBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Administrado;
import com.example.tarea16.modelo.Direccion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentAdministrados extends Fragment {
    private FragmentAdministradosBinding binding;
    private final SimpleTextAdapter adapter = new SimpleTextAdapter();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Administrado editando;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdministradosBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        adapter.setListener(this::seleccionar);
        binding.btnNuevo.setOnClickListener(v -> mostrarFormulario(null));
        binding.btnCancelar.setOnClickListener(v -> ocultarFormulario());
        binding.btnGuardar.setOnClickListener(v -> validarYGuardar());
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void seleccionar(SimpleTextAdapter.Item item) {
        if (!(item.source instanceof Administrado)) return;
        Administrado administrado = (Administrado) item.source;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(administrado.nombreRazonSocial)
                .setItems(new String[]{"Editar", "Ver detalle"}, (dialog, which) -> {
                    if (which == 0) mostrarFormulario(administrado);
                    else mostrarDetalle(administrado);
                })
                .show();
    }

    private void mostrarFormulario(Administrado actual) {
        editando = actual;
        limpiarFormulario();
        binding.txtFormTitle.setText(actual == null ? "Nuevo administrado" : "Editar administrado");
        if (actual != null) {
            binding.txtDniRuc.setText(actual.dniRuc);
            binding.txtNombre.setText(actual.nombreRazonSocial);
            binding.txtTelefono.setText(actual.telefono);
            binding.txtCorreo.setText(actual.correoNotificaciones);
            executor.execute(() -> {
                Direccion direccion = AppDatabase.getInstance(requireContext().getApplicationContext())
                        .direccionDao().buscarPorAdministrado(actual.idAdministrado);
                if (isAdded()) requireActivity().runOnUiThread(() -> cargarDireccion(direccion));
            });
        }
        binding.formContainer.setVisibility(View.VISIBLE);
        binding.recycler.setVisibility(View.GONE);
        binding.txtEmpty.setVisibility(View.GONE);
        binding.btnNuevo.setVisibility(View.GONE);
    }

    private void ocultarFormulario() {
        editando = null;
        limpiarFormulario();
        binding.formContainer.setVisibility(View.GONE);
        binding.btnNuevo.setVisibility(View.VISIBLE);
        cargar();
    }

    private void validarYGuardar() {
        String dniRuc = text(binding.txtDniRuc);
        String nombre = text(binding.txtNombre);
        if (dniRuc.isEmpty()) {
            binding.txtDniRuc.setError("Obligatorio");
            return;
        }
        if (nombre.isEmpty()) {
            binding.txtNombre.setError("Obligatorio");
            return;
        }
        guardar(editando, dniRuc, nombre, text(binding.txtTelefono), text(binding.txtCorreo),
                text(binding.txtCalle), text(binding.txtNumero), text(binding.txtDistrito), text(binding.txtCiudad));
    }

    private void guardar(Administrado actual, String dniRuc, String nombre, String telefono,
                         String correo, String calle, String numero, String distrito, String ciudad) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(app);
            long now = System.currentTimeMillis();
            Administrado existente = db.administradoDao().buscarPorDniRuc(dniRuc);
            if (actual == null && existente != null) {
                mostrarToast("Ya existe un administrado con ese DNI/RUC");
                return;
            }
            if (actual != null && existente != null && existente.idAdministrado != actual.idAdministrado) {
                mostrarToast("Ese DNI/RUC pertenece a otro administrado");
                return;
            }

            db.runInTransaction(() -> {
                Administrado item = actual == null ? new Administrado() : actual;
                item.codigoAdministrado = item.codigoAdministrado == null || item.codigoAdministrado.trim().isEmpty()
                        ? "ADM-" + now
                        : item.codigoAdministrado;
                item.dniRuc = dniRuc;
                item.nombreRazonSocial = nombre;
                item.telefono = telefono;
                item.correoNotificaciones = correo;
                item.updatedAt = now;
                item.sincronizado = false;
                item.syncStatus = "PENDING";
                item.syncError = null;
                long administradoId = actual == null ? db.administradoDao().insertar(item) : item.idAdministrado;
                if (actual != null) db.administradoDao().actualizar(item);

                Direccion direccion = db.direccionDao().buscarPorAdministrado((int) administradoId);
                if (direccion == null) {
                    direccion = new Direccion();
                    direccion.idAdministrado = (int) administradoId;
                    direccion.tipoInmueble = "DOMICILIO";
                }
                direccion.calle = calle;
                direccion.numero = numero;
                direccion.comunaDistrito = distrito;
                direccion.ciudad = ciudad;
                direccion.updatedAt = now;
                direccion.sincronizado = false;
                direccion.syncStatus = "PENDING";
                direccion.syncError = null;
                if (direccion.idDireccion == 0) db.direccionDao().insertar(direccion);
                else db.direccionDao().actualizar(direccion);
            });

            if (isAdded()) requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Administrado guardado", Toast.LENGTH_SHORT).show();
                ocultarFormulario();
            });
        });
    }

    private void cargar() {
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<SimpleTextAdapter.Item> items = new ArrayList<>();
            for (Administrado item : db.administradoDao().listar()) {
                Direccion direccion = db.direccionDao().buscarPorAdministrado(item.idAdministrado);
                String detalle = "DNI/RUC: " + safe(item.dniRuc)
                        + "  Telefono: " + safe(item.telefono)
                        + "\n" + direccionTexto(direccion);
                items.add(new SimpleTextAdapter.Item(item.nombreRazonSocial, detalle,
                        item.sincronizado ? "SINCRONIZADO" : "PENDIENTE", item));
            }
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                if (binding == null || binding.formContainer.getVisibility() == View.VISIBLE) return;
                binding.txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.setItems(items);
            });
        });
    }

    private void mostrarDetalle(Administrado administrado) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            Direccion direccion = AppDatabase.getInstance(app).direccionDao()
                    .buscarPorAdministrado(administrado.idAdministrado);
            if (isAdded()) requireActivity().runOnUiThread(() ->
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(administrado.nombreRazonSocial)
                            .setMessage("DNI/RUC: " + safe(administrado.dniRuc)
                                    + "\nTelefono: " + safe(administrado.telefono)
                                    + "\nCorreo: " + safe(administrado.correoNotificaciones)
                                    + "\nDireccion: " + direccionTexto(direccion))
                            .setPositiveButton(android.R.string.ok, null)
                            .show());
        });
    }

    private void cargarDireccion(Direccion direccion) {
        if (direccion == null || binding == null) return;
        binding.txtCalle.setText(direccion.calle);
        binding.txtNumero.setText(direccion.numero);
        binding.txtDistrito.setText(direccion.comunaDistrito);
        binding.txtCiudad.setText(direccion.ciudad);
    }

    private void limpiarFormulario() {
        if (binding == null) return;
        binding.txtDniRuc.setText("");
        binding.txtNombre.setText("");
        binding.txtTelefono.setText("");
        binding.txtCorreo.setText("");
        binding.txtCalle.setText("");
        binding.txtNumero.setText("");
        binding.txtDistrito.setText("");
        binding.txtCiudad.setText("");
    }

    private void mostrarToast(String message) {
        if (isAdded()) requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }

    private String text(android.widget.TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    private String direccionTexto(Direccion direccion) {
        if (direccion == null) return "Sin direccion";
        return safe(direccion.calle) + " " + safe(direccion.numero)
                + ", " + safe(direccion.comunaDistrito)
                + ", " + safe(direccion.ciudad);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
