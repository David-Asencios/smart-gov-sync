package com.example.tarea16.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
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
import com.example.tarea16.modelo.DocumentoIngresado;
import com.example.tarea16.util.AttachmentDownloader;
import com.example.tarea16.sync.SyncScheduler;
import com.example.tarea16.sync.SyncManager;
import com.example.tarea16.api.ApiClient;
import com.example.tarea16.api.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        adapter.setDetalle(this::abrirAdjunto);
        binding.txtTituloBandeja.setText(R.string.menu_expedientes_archivar);
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
        View layout = LayoutInflater.from(context).inflate(R.layout.dialog_archivamiento, null, false);
        EditText codigo = layout.findViewById(R.id.txtCodigoArchivo);
        codigo.setText("ARCH-" + System.currentTimeMillis());
        EditText pabellon = layout.findViewById(R.id.txtPabellon);
        EditText estante = layout.findViewById(R.id.txtEstante);
        EditText caja = layout.findViewById(R.id.txtCaja);
        EditText digitalizacion = layout.findViewById(R.id.txtCostoDigitalizacion);
        EditText custodia = layout.findViewById(R.id.txtCostoCustodia);
        TextView totalView = layout.findViewById(R.id.txtCostoTotal);
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                Double d = decimal(digitalizacion), c = decimal(custodia);
                totalView.setText(String.format(Locale.US, "Total: %.2f", (d == null ? 0 : d) + (c == null ? 0 : c)));
            }
        };
        digitalizacion.addTextChangedListener(watcher);
        custodia.addTextChangedListener(watcher);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.archive_confirm_title)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Revisar y archivar", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String codigoArchivo = codigo.getText().toString().trim();
                    Integer nroPabellon = numero(pabellon);
                    Integer nroEstante = numero(estante);
                    Integer nroCaja = numero(caja);
                    Double costoDigitalizacion = decimal(digitalizacion);
                    Double costoCustodia = decimal(custodia);
                    if (codigoArchivo.isEmpty() || !positivo(nroPabellon) || !positivo(nroEstante) || !positivo(nroCaja)
                            || costoDigitalizacion == null || costoCustodia == null) {
                        if (!positivo(nroPabellon)) pabellon.setError("Ingresa un número mayor a cero");
                        if (!positivo(nroEstante)) estante.setError("Ingresa un número mayor a cero");
                        if (!positivo(nroCaja)) caja.setError("Ingresa un número mayor a cero");
                        if (costoDigitalizacion == null) digitalizacion.setError("Ingresa un costo válido");
                        if (costoCustodia == null) custodia.setError("Ingresa un costo válido");
                        return;
                    }
                    double total = costoDigitalizacion + costoCustodia;
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("Confirmar archivamiento")
                            .setMessage("Código: " + codigoArchivo
                                    + "\nPabellón: " + nroPabellon
                                    + "\nEstante: " + nroEstante
                                    + "\nCaja: " + nroCaja
                                    + "\nDigitalización: " + String.format(Locale.US, "%.2f", costoDigitalizacion)
                                    + "\nCustodia: " + String.format(Locale.US, "%.2f", costoCustodia)
                                    + "\nTotal: " + String.format(Locale.US, "%.2f", total))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.archivar, (confirm, selected) -> {
                                dialog.dismiss();
                                archivar(item, codigoArchivo, nroPabellon, nroEstante, nroCaja,
                                        costoDigitalizacion, costoCustodia);
                            }).show();
                }));
        dialog.show();
    }

    private Double decimal(EditText input) {
        try { double value = Double.parseDouble(input.getText().toString().trim()); return value < 0 ? null : value; }
        catch (Exception error) { return null; }
    }

    private boolean positivo(Integer value) { return value != null && value > 0; }

    private Integer numero(EditText input) {
        try {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) return null;
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private void archivar(HojaRuta item, String codigo, int pabellon, int estante, int caja,
                          double costoDigitalizacion, double costoCustodia) {
        if (item.remoteUuid == null) {
            Toast.makeText(requireContext(), "La derivación aún no está sincronizada", Toast.LENGTH_LONG).show();
            return;
        }
        Map<String, Object> request = new HashMap<>();
        request.put("id_derivacion_remote_uuid", item.remoteUuid);
        request.put("codigo_almacen", codigo);
        request.put("nro_pabellon", pabellon);
        request.put("nro_estante", estante);
        request.put("nro_caja_fisica", caja);
        request.put("costo_digitalizacion", costoDigitalizacion);
        request.put("costo_arancel_custodia", costoCustodia);
        String authorization = "Bearer " + new TokenManager(requireContext()).obtenerToken();
        ApiClient.getService().archivar(authorization, request).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful()) {
                    String detail = "No se pudo archivar (" + response.code() + ")";
                    try { if (response.errorBody() != null) detail += ": " + response.errorBody().string(); }
                    catch (Exception ignored) { }
                    Toast.makeText(requireContext(), detail, Toast.LENGTH_LONG).show();
                    return;
                }
                Context app = requireContext().getApplicationContext();
                new SyncManager(app).sincronizar(result -> {
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.archive_done, Toast.LENGTH_SHORT).show();
                        cargar();
                    });
                });
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable error) {
                if (isAdded()) Toast.makeText(requireContext(), "Sin conexión: el archivamiento no fue aplicado", Toast.LENGTH_LONG).show();
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

    private void abrirAdjunto(HojaRuta item) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            DocumentoIngresado documento = AppDatabase.getInstance(app).documentoDao().buscar(item.idDocumento);
            if (isAdded()) requireActivity().runOnUiThread(() -> AttachmentDownloader.open(requireContext(), documento));
        });
    }

    private void cargar() {
        Context context = requireContext().getApplicationContext();
        executor.execute(() -> {
            List<HojaRuta> items = AppDatabase.getInstance(context).hojaRutaDao().expedientesPorArchivar();
            if (isAdded()) requireActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.txtResumen.setText("Pendientes de archivamiento: " + items.size());
                    binding.txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                    adapter.setItems(items);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
