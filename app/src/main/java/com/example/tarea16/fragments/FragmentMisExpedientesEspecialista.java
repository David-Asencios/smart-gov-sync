package com.example.tarea16.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.R;
import com.example.tarea16.activities.DerivacionFormActivity;
import com.example.tarea16.activities.MapsActivity;
import com.example.tarea16.adapter.DerivacionAdapter;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.FragmentBandejaBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.HojaRuta;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentMisExpedientesEspecialista extends Fragment {
    private FragmentBandejaBinding binding;
    private DerivacionAdapter adapter;
    private TokenManager tokenManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBandejaBinding.inflate(inflater, container, false);
        tokenManager = new TokenManager(requireContext());
        adapter = new DerivacionAdapter(true);
        adapter.setTextosAcciones(R.string.finalizar, R.string.derivar);
        adapter.setAcciones(this::confirmarFinalizacion,
                this::abrirDerivacion,
                item -> abrirMapa(item.latitud, item.longitud));
        binding.txtTituloBandeja.setText(R.string.menu_mis_expedientes);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        return binding.getRoot();
    }

    private void abrirDerivacion(HojaRuta item) {
        Intent intent = new Intent(requireContext(), DerivacionFormActivity.class);
        intent.putExtra("id_derivacion_origen", item.idDerivacion);
        intent.putExtra("id_documento", item.idDocumento);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void confirmarFinalizacion(HojaRuta item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.finish_confirm_title)
                .setMessage(R.string.finish_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.finalizar, (dialog, which) -> finalizar(item.idDerivacion))
                .show();
    }

    private void finalizar(int idDerivacion) {
        Context context = requireContext().getApplicationContext();
        String observacion = getString(R.string.finish_observation);
        executor.execute(() -> {
            int changed = AppDatabase.getInstance(context).hojaRutaDao()
                    .finalizarAtencion(idDerivacion, observacion, System.currentTimeMillis());
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                if (changed == 0) {
                    Toast.makeText(requireContext(), R.string.transition_not_allowed, Toast.LENGTH_SHORT).show();
                }
                cargar();
            });
        });
    }

    private void abrirMapa(double latitud, double longitud) {
        Intent intent = new Intent(requireContext(), MapsActivity.class);
        intent.putExtra("latitud", latitud);
        intent.putExtra("longitud", longitud);
        startActivity(intent);
    }

    private void cargar() {
        Context context = requireContext().getApplicationContext();
        int empleadoId = tokenManager.obtenerIdEmpleado();
        executor.execute(() -> {
            List<HojaRuta> items = AppDatabase.getInstance(context).hojaRutaDao()
                    .recibidasPorEspecialista(empleadoId);
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.txtResumen.setText("Activos: " + items.size());
                    binding.txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                    adapter.setItems(items);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
