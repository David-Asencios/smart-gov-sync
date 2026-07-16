package com.example.tarea16.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.R;
import com.example.tarea16.activities.MapsActivity;
import com.example.tarea16.adapter.DerivacionAdapter;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.FragmentBandejaBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.HojaRuta;
import com.example.tarea16.modelo.DocumentoIngresado;
import com.example.tarea16.util.AttachmentDownloader;
import com.example.tarea16.security.RoleManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentBandeja extends Fragment {
    private FragmentBandejaBinding binding;
    private DerivacionAdapter adapter;
    private TokenManager tokenManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBandejaBinding.inflate(inflater, container, false);
        tokenManager = new TokenManager(requireContext());
        adapter = new DerivacionAdapter(RoleManager.canUpdateBandeja(tokenManager.obtenerRol()));
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        adapter.setAcciones(this::confirmarRecepcion, this::solicitarMotivoRechazo,
                item -> abrirMapa(item.latitud, item.longitud));
        adapter.setDetalle(this::abrirAdjunto);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void confirmarRecepcion(HojaRuta item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.receive_confirm_title)
                .setMessage(R.string.receive_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_receive, (dialog, which) ->
                        cambiar(item.idDerivacion, "RECIBIDO", "Recepcion confirmada"))
                .show();
    }

    private void solicitarMotivoRechazo(HojaRuta item) {
        EditText reason = new EditText(requireContext());
        reason.setHint(R.string.reject_reason_hint);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        reason.setPadding(padding, padding / 2, padding, padding / 2);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reject_reason_title)
                .setView(reason)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_reject, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = reason.getText().toString().trim();
                    if (value.isEmpty()) {
                        reason.setError(getString(R.string.reject_reason_required));
                        return;
                    }
                    dialog.dismiss();
                    cambiar(item.idDerivacion, "RECHAZADO", value);
                }));
        dialog.show();
    }

    private void cambiar(int id, String estado, String observacion) {
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            int changed = AppDatabase.getInstance(context).hojaRutaDao()
                    .cambiarEstadoSeguro(id, estado, observacion, System.currentTimeMillis());
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                if (changed == 0) Toast.makeText(requireContext(), R.string.transition_not_allowed, Toast.LENGTH_SHORT).show();
                if (binding != null) cargar();
            });
        });
    }

    private void abrirMapa(double latitud, double longitud) {
        Intent intent = new Intent(requireContext(), MapsActivity.class);
        intent.putExtra("latitud", latitud);
        intent.putExtra("longitud", longitud);
        startActivity(intent);
    }

    private void abrirAdjunto(HojaRuta item) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            DocumentoIngresado documento = AppDatabase.getInstance(app).documentoDao().buscar(item.idDocumento);
            if (isAdded()) requireActivity().runOnUiThread(() -> AttachmentDownloader.open(requireContext(), documento));
        });
    }

    private void cargar() {
        Context context = requireContext().getApplicationContext();
        String role = tokenManager.obtenerRol();
        executor.execute(() -> {
            List<HojaRuta> items = AppDatabase.getInstance(context).hojaRutaDao().pendientesBandeja(
                    tokenManager.obtenerIdEmpleado(), tokenManager.obtenerIdOficina(),
                    RoleManager.ADMIN.equals(role), RoleManager.ARCHIVO.equals(role));
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.txtTituloBandeja.setText(R.string.menu_bandeja_entrada);
                    binding.txtResumen.setText("Pendientes: " + items.size()
                            + "   Alta prioridad: " + contarPrioridad(items, "ALTA"));
                    binding.txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                    adapter.setItems(items);
                }
            });
        });
    }

    private int contarPrioridad(List<HojaRuta> items, String prioridad) {
        int total = 0;
        for (HojaRuta item : items) {
            if (prioridad.equalsIgnoreCase(String.valueOf(item.prioridadEnvio))) total++;
        }
        return total;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
