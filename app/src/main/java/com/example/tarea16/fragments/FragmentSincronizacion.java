package com.example.tarea16.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.tarea16.R;
import com.example.tarea16.databinding.FragmentSincronizacionBinding;
import com.example.tarea16.sync.SyncManager;

public class FragmentSincronizacion extends Fragment {
    private FragmentSincronizacionBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSincronizacionBinding.inflate(inflater, container, false);
        SyncManager syncManager = new SyncManager(requireContext());
        binding.txtEstado.setText(syncManager.hayConexion()
                ? R.string.sync_connected
                : R.string.sync_disconnected);
        binding.progressSincronizacion.setVisibility(View.GONE);
        binding.btnSincronizar.setOnClickListener(v -> {
            binding.btnSincronizar.setEnabled(false);
            binding.progressSincronizacion.setVisibility(View.VISIBLE);
            binding.txtEstado.setText(R.string.sync_in_progress);
            binding.txtDetalle.setText(R.string.sync_wait_message);
            syncManager.sincronizar(result -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.progressSincronizacion.setVisibility(View.GONE);
                            binding.btnSincronizar.setEnabled(true);
                            binding.txtEstado.setText(result.mensaje);
                            binding.txtDetalle.setText(result.exitoso
                                    ? R.string.sync_success_detail
                                    : R.string.sync_error_detail);
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
