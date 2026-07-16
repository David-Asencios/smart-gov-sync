package com.example.tarea16.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tarea16.R;
import com.example.tarea16.activities.MapsActivity;
import com.example.tarea16.adapter.DerivacionAdapter;
import com.example.tarea16.databinding.FragmentBandejaBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.ActaArchivamiento;
import com.example.tarea16.modelo.ArchivoFisico;
import com.example.tarea16.modelo.HojaRuta;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentExpedientesPorArchivar extends Fragment {
    private FragmentBandejaBinding binding;
    private DerivacionAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBandejaBinding.inflate(inflater, container, false);
        adapter = new DerivacionAdapter(true);
        adapter.setTextosAcciones(R.string.archivar, R.string.ver_detalle);
        adapter.setAcciones(this::mostrarDialogoArchivamiento, this::mostrarDetalle,
                item -> abrirMapa(item.latitud, item.longitud));
        binding.txtTituloBandeja.setText(R.string.menu_expedientes_archivar);
        binding.txtResumen.setText(R.string.archivo_por_archivar_description);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void mostrarDialogoArchivamiento(HojaRuta item) {
        Context context = requireContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);

        EditText codigo = campoTexto(context, R.string.codigo_archivo);
        codigo.setText("ARCH-" + System.currentTimeMillis());
        EditText pabellon = campoNumero(context, R.string.pabellon_nivel);
        EditText estante = campoNumero(context, R.string.estante);
        EditText caja = campoNumero(context, R.string.caja);
        layout.addView(codigo);
        layout.addView(pabellon);
        layout.addView(estante);
        layout.addView(caja);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.archive_confirm_title)
                .setMessage(R.string.archive_confirm_message)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.archivar, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String codigoArchivo = codigo.getText().toString().trim();
                    Integer nroPabellon = numero(pabellon);
                    Integer nroEstante = numero(estante);
                    Integer nroCaja = numero(caja);
                    if (codigoArchivo.isEmpty() || nroPabellon == null || nroEstante == null || nroCaja == null) {
                        Toast.makeText(context, R.string.archive_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    archivar(item, codigoArchivo, nroPabellon, nroEstante, nroCaja);
                }));
        dialog.show();
    }

    private EditText campoTexto(Context context, int hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setSingleLine(true);
        return input;
    }

    private EditText campoNumero(Context context, int hint) {
        EditText input = campoTexto(context, hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        return input;
    }

    private Integer numero(EditText input) {
        try {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) return null;
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private void archivar(HojaRuta item, String codigo, int pabellon, int estante, int caja) {
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                db.runInTransaction(() -> {
                    ArchivoFisico ubicacion = new ArchivoFisico();
                    ubicacion.codigoAlmacen = codigo;
                    ubicacion.nroPabellon = pabellon;
                    ubicacion.nroEstante = estante;
                    ubicacion.nroCajaFisica = caja;
                    ubicacion.updatedAt = now;
                    long idUbicacion = db.archivoFisicoDao().insertar(ubicacion);

                    ActaArchivamiento acta = new ActaArchivamiento();
                    acta.nroActaUnico = "ACT-" + now;
                    acta.idDerivacion = item.idDerivacion;
                    acta.idUbicacionArchivo = (int) idUbicacion;
                    acta.fechaHoraGuardado = now;
                    acta.updatedAt = now;
                    db.actaDao().insertar(acta);

                    int changed = db.hojaRutaDao().marcarArchivado(
                            item.idDerivacion, "Archivamiento fisico registrado", now);
                    if (changed == 0) {
                        throw new IllegalStateException("transition_not_allowed");
                    }
                    db.expedienteDao().marcarArchivadoPorDocumento(item.idDocumento, now);
                });
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.archive_done, Toast.LENGTH_SHORT).show();
                        cargar();
                    });
                }
            } catch (Exception error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), R.string.transition_not_allowed, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void mostrarDetalle(HojaRuta item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.codigoBarrasSeguimiento)
                .setMessage("Estado: " + item.estadoDerivacion
                        + "\nPrioridad: " + item.prioridadEnvio
                        + "\nDocumento ID: " + item.idDocumento
                        + "\nFinalizado: " + item.updatedAt)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
            List<HojaRuta> items = AppDatabase.getInstance(context).hojaRutaDao().expedientesPorArchivar();
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                if (binding != null) adapter.setItems(items);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
