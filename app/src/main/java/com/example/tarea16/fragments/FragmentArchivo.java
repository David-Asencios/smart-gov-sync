package com.example.tarea16.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.adapter.ArchivoAdapter;
import com.example.tarea16.databinding.FragmentArchivoBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.ArchivoFisico;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentArchivo extends Fragment {
    private FragmentArchivoBinding binding;
    private final ArchivoAdapter adapter = new ArchivoAdapter();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentArchivoBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.btnNuevo.setOnClickListener(v -> crear());
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void crear() {
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            ArchivoFisico item = new ArchivoFisico();
            item.codigoAlmacen = "ALM-" + System.currentTimeMillis();
            item.nroPabellon = 1;
            item.nroEstante = 1;
            item.nroCajaFisica = 1;
            item.updatedAt = System.currentTimeMillis();
            AppDatabase.getInstance(context).archivoFisicoDao().insertar(item);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        cargar();
                    }
                });
            }
        });
    }

    private void cargar() {
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<ArchivoFisico> items = db.archivoFisicoDao().listar();
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
