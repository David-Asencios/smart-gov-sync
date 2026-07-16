package com.example.tarea16.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.R;
import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.databinding.FragmentSimpleListBinding;
import com.example.tarea16.db.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SimpleListFragment extends Fragment {
    private FragmentSimpleListBinding binding;
    private final SimpleTextAdapter adapter = new SimpleTextAdapter();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSimpleListBinding.inflate(inflater, container, false);
        String description = descripcion();
        binding.txtDescripcion.setText(description);
        binding.txtDescripcion.setVisibility(description == null || description.trim().isEmpty() ? View.GONE : View.VISIBLE);
        String action = primaryActionText();
        binding.btnPrimary.setText(action);
        binding.btnPrimary.setVisibility(action == null || action.trim().isEmpty() ? View.GONE : View.VISIBLE);
        binding.btnPrimary.setOnClickListener(v -> onPrimaryAction());
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        adapter.setListener(this::onItemSelected);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            List<SimpleTextAdapter.Item> items = cargarItems(AppDatabase.getInstance(context));
            if (items.isEmpty()) {
                items.add(new SimpleTextAdapter.Item("Sin registros", "Aun no hay datos sincronizados o registrados.", ""));
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) adapter.setItems(items);
                });
            }
        });
    }

    protected String estado(boolean sincronizado) {
        return getString(sincronizado ? R.string.sync_status_synced : R.string.sync_status_pending);
    }

    protected String primaryActionText() { return ""; }
    protected void onPrimaryAction() { }
    protected void onItemSelected(SimpleTextAdapter.Item item) { }
    protected abstract String descripcion();
    protected abstract List<SimpleTextAdapter.Item> cargarItems(AppDatabase db);

    protected void recargar() {
        cargar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
