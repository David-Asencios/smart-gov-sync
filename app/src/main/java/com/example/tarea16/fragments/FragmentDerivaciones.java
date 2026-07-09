package com.example.tarea16.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.activities.DerivacionFormActivity;
import com.example.tarea16.activities.MapsActivity;
import com.example.tarea16.adapter.DerivacionAdapter;
import com.example.tarea16.databinding.FragmentDerivacionesBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.HojaRuta;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentDerivaciones extends Fragment {
    private FragmentDerivacionesBinding binding;
    private final DerivacionAdapter adapter = new DerivacionAdapter(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDerivacionesBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.btnNuevo.setOnClickListener(v -> startActivity(new Intent(requireContext(), DerivacionFormActivity.class)));
        adapter.setAcciones(null, null, item -> abrirMapa(item.latitud, item.longitud));
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void abrirMapa(double latitud, double longitud) {
        Intent intent = new Intent(requireContext(), MapsActivity.class);
        intent.putExtra("latitud", latitud);
        intent.putExtra("longitud", longitud);
        startActivity(intent);
    }

    private void cargar() {
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<HojaRuta> items = db.hojaRutaDao().listar();
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
