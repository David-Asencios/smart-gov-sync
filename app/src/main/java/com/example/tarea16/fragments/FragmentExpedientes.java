package com.example.tarea16.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.api.TokenManager;
import com.example.tarea16.adapter.ExpedienteAdapter;
import com.example.tarea16.databinding.FragmentExpedientesBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Expediente;
import com.example.tarea16.security.RoleManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentExpedientes extends Fragment {
    private FragmentExpedientesBinding binding;
    private final ExpedienteAdapter adapter = new ExpedienteAdapter();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentExpedientesBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.btnNuevo.setVisibility(View.GONE);
        String role = new TokenManager(requireContext()).obtenerRol();
        if (RoleManager.ADMIN.equals(RoleManager.normalize(role))) {
            binding.txtResumen.setText("Consulta general de expedientes. Esta pantalla es solo informativa: permite supervisar estados y trazabilidad sin modificar el flujo operativo.");
        } else {
            binding.txtResumen.setText("Expedientes registrados disponibles en el dispositivo y sincronizados con el servidor.");
        }
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        Context context = requireContext().getApplicationContext();
        TokenManager session = new TokenManager(requireContext());
        String role = session.obtenerRol();
        int usuarioId = session.obtenerIdUsuario();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Expediente> items = RoleManager.MESA_PARTES.equals(RoleManager.normalize(role)) && usuarioId > 0
                    ? db.expedienteDao().listarPorUsuario(usuarioId)
                    : db.expedienteDao().listar();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        adapter.setItems(items);
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
