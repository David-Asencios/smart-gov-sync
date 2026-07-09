package com.example.tarea16.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.activities.DocumentoFormActivity;
import com.example.tarea16.adapter.DocumentoAdapter;
import com.example.tarea16.databinding.FragmentDocumentosBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.DocumentoIngresado;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentDocumentos extends Fragment {
    private FragmentDocumentosBinding binding;
    private final DocumentoAdapter adapter = new DocumentoAdapter();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDocumentosBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.btnNuevo.setOnClickListener(v -> startActivity(new Intent(requireContext(), DocumentoFormActivity.class)));
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<DocumentoIngresado> items = db.documentoDao().listar();
            requireActivity().runOnUiThread(() -> adapter.setItems(items));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
