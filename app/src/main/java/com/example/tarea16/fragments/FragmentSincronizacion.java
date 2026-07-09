package com.example.tarea16.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.tarea16.databinding.FragmentSincronizacionBinding;
import com.example.tarea16.sync.SyncManager;

public class FragmentSincronizacion extends Fragment {
    private FragmentSincronizacionBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSincronizacionBinding.inflate(inflater, container, false);
        SyncManager syncManager = new SyncManager(requireContext());
        binding.txtEstado.setText(syncManager.hayConexion() ? "CON CONEXION" : "SIN CONEXION");
        binding.btnSincronizar.setOnClickListener(v -> {
            binding.txtEstado.setText("SINCRONIZANDO");
            syncManager.sincronizar(() -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.txtEstado.setText("SINCRONIZACION FINALIZADA");
                        }
                    });
                }
            });
        });
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
