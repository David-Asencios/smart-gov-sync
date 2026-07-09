package com.example.tarea16.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.activities.MapsActivity;
import com.example.tarea16.adapter.DerivacionAdapter;
import com.example.tarea16.databinding.FragmentBandejaBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.HojaRuta;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentBandeja extends Fragment {
    private FragmentBandejaBinding binding;
    private final DerivacionAdapter adapter = new DerivacionAdapter(true);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBandejaBinding.inflate(inflater, container, false);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        adapter.setAcciones(item -> cambiar(item.idDerivacion, "RECIBIDO"), item -> cambiar(item.idDerivacion, "RECHAZADO"), item -> abrirMapa(item.latitud, item.longitud));
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void cambiar(int id, String estado) {
        executor.execute(() -> {
            AppDatabase.getInstance(requireContext()).hojaRutaDao().cambiarEstado(id, estado, System.currentTimeMillis());
            requireActivity().runOnUiThread(this::cargar);
        });
    }

    private void abrirMapa(double latitud, double longitud) {
        Intent intent = new Intent(requireContext(), MapsActivity.class);
        intent.putExtra("latitud", latitud);
        intent.putExtra("longitud", longitud);
        startActivity(intent);
    }

    private void cargar() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<HojaRuta> items = db.hojaRutaDao().pendientesBandeja();
            requireActivity().runOnUiThread(() -> adapter.setItems(items));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
