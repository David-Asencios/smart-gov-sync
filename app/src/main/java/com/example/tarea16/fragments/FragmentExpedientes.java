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
import com.example.tarea16.modelo.DocumentoIngresado;
import com.example.tarea16.modelo.HojaRuta;
import com.example.tarea16.security.RoleManager;
import com.example.tarea16.util.EvidencePhotoCodec;
import android.graphics.Bitmap;
import android.widget.ImageView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Date;
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
        adapter.setListener(this::mostrarDetalle);
        binding.btnNuevo.setVisibility(View.GONE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TokenManager session = new TokenManager(requireContext());
        String role = session.obtenerRol();
        int usuarioId = session.obtenerIdUsuario();
        AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
        androidx.lifecycle.LiveData<List<Expediente>> source =
                RoleManager.MESA_PARTES.equals(RoleManager.normalize(role)) && usuarioId > 0
                        ? db.expedienteDao().observarPorUsuario(usuarioId)
                        : db.expedienteDao().observar();
        source.observe(getViewLifecycleOwner(), this::mostrarLista);
    }

    private void mostrarLista(List<Expediente> items) {
        if (binding == null) return;
        int pendientes = contarPendientes(items);
        binding.txtResumen.setText("Total: " + items.size()
                + "   Pendientes sync: " + pendientes
                + "   Sincronizados: " + (items.size() - pendientes));
        binding.txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.setItems(items);
    }

    private void mostrarDetalle(Expediente item) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(app);
            List<DocumentoIngresado> documentos = db.documentoDao().listar();
            List<HojaRuta> derivaciones = db.hojaRutaDao().listar();
            int totalDocumentos = 0;
            String evidencia = null;
            StringBuilder recorrido = new StringBuilder();
            for (DocumentoIngresado documento : documentos) {
                if (documento.idExpediente != item.idExpediente) continue;
                totalDocumentos++;
                if (evidencia == null && documento.rutaFoto != null && !documento.rutaFoto.trim().isEmpty()) evidencia = documento.rutaFoto;
                for (HojaRuta derivacion : derivaciones) {
                    if (derivacion.idDocumento != documento.idDocumento) continue;
                    if (recorrido.length() > 0) recorrido.append("\n");
                    recorrido.append("• ").append(safe(derivacion.estadoDerivacion))
                            .append(" · oficina ").append(derivacion.idOficinaProcedencia)
                            .append(" · ").append(safe(derivacion.prioridadEnvio));
                }
            }
            int cantidad = totalDocumentos;
            String trazabilidad = recorrido.length() == 0 ? "Sin derivaciones registradas" : recorrido.toString();
            String foto = evidencia;
            if (isAdded()) requireActivity().runOnUiThread(() -> mostrarDetalle(item, cantidad, trazabilidad, foto));
        });
    }

    private void mostrarDetalle(Expediente item, int documentos, String trazabilidad, String evidencia) {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(item.nroExpedienteAnual)
                .setMessage("Estado: " + safe(item.estadoGlobal)
                        + "\nFecha: " + format.format(new Date(item.fechaHoraApertura))
                        + "\nAsunto: " + safe(item.asuntoGeneral)
                        + "\nDocumentos: " + documentos
                        + "\n\nTrazabilidad:\n" + trazabilidad
                        + "\n\nSincronización: " + (item.sincronizado ? "Sincronizado" : "Pendiente"))
                .setPositiveButton(android.R.string.ok, null);
        if (evidencia != null) dialog.setNeutralButton("Ver evidencia", (d, which) -> mostrarEvidencia(evidencia));
        dialog.show();
    }

    private void mostrarEvidencia(String source) {
        Bitmap bitmap = EvidencePhotoCodec.decode(source);
        if (bitmap == null) {
            android.widget.Toast.makeText(requireContext(), "No se pudo abrir la evidencia", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        ImageView image = new ImageView(requireContext());
        image.setAdjustViewBounds(true); image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setImageBitmap(bitmap);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        image.setPadding(padding, padding, padding, padding);
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Evidencia fotográfica")
                .setView(image).setPositiveButton(android.R.string.ok, null).show();
    }

    private int contarPendientes(List<Expediente> items) {
        int total = 0;
        for (Expediente item : items) {
            if (!item.sincronizado) total++;
        }
        return total;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
