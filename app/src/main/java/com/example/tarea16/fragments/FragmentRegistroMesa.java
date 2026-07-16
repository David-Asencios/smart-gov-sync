package com.example.tarea16.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.tarea16.R;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.FragmentRegistroMesaBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Administrado;
import com.example.tarea16.modelo.DocumentoIngresado;
import com.example.tarea16.modelo.Expediente;
import com.example.tarea16.modelo.HojaRuta;
import com.example.tarea16.modelo.Oficina;
import com.example.tarea16.modelo.Personal;
import com.example.tarea16.modelo.TipoDocumento;
import com.example.tarea16.security.RoleManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentRegistroMesa extends Fragment {
    private FragmentRegistroMesaBinding binding;
    private TokenManager tokenManager;
    private FusedLocationProviderClient locationClient;
    private ActivityResultLauncher<String> permisoUbicacionLauncher;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Administrado> administrados = new ArrayList<>();
    private final List<TipoDocumento> tiposDocumento = new ArrayList<>();
    private final List<Oficina> oficinas = new ArrayList<>();
    private final List<Personal> especialistas = new ArrayList<>();
    private double latitud;
    private double longitud;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegistroMesaBinding.inflate(inflater, container, false);
        tokenManager = new TokenManager(requireContext());
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        permisoUbicacionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) capturarUbicacion();
            else Toast.makeText(requireContext(), "Permiso de ubicacion denegado", Toast.LENGTH_SHORT).show();
        });
        configurarFormularioInicial();
        binding.btnUbicacion.setOnClickListener(v -> pedirUbicacion());
        binding.btnGuardarRegistro.setOnClickListener(v -> guardarRegistroCompleto());
        binding.spinnerAdministrado.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                actualizarDetalleAdministrado();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                actualizarDetalleAdministrado();
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        sugerirNumeroExpediente();
        cargarCatalogos();
    }

    private void configurarFormularioInicial() {
        setSpinner(binding.spinnerPrioridad, new String[]{"NORMAL", "ALTA", "BAJA"});
        binding.txtUbicacion.setText(R.string.map_location_missing);
        sugerirNumeroExpediente();
    }

    private void cargarCatalogos() {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(app);
            List<Administrado> administradosDb = db.administradoDao().listar();
            List<TipoDocumento> tiposDb = db.tipoDocumentoDao().listar();
            List<Oficina> oficinasDb = db.oficinaDao().listar();
            List<Personal> especialistasDb = db.personalDao().listar();
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                administrados.clear();
                administrados.addAll(administradosDb);
                tiposDocumento.clear();
                tiposDocumento.addAll(tiposDb);
                oficinas.clear();
                oficinas.addAll(oficinasDb);
                especialistas.clear();
                especialistas.addAll(especialistasDb);

                setSpinner(binding.spinnerAdministrado, nombresAdministrados(administrados));
                setSpinner(binding.spinnerTipoDocumento, nombresTipos(tiposDocumento));
                setSpinner(binding.spinnerOficinaDestino, nombresOficinas(oficinas));
                setSpinner(binding.spinnerEspecialistaDestino, nombresEspecialistas(especialistas));
                actualizarDetalleAdministrado();
            });
        });
    }

    private void pedirUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            capturarUbicacion();
        } else {
            permisoUbicacionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void capturarUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (binding == null) return;
            if (location == null) {
                Toast.makeText(requireContext(), "No se pudo obtener la ubicacion actual", Toast.LENGTH_SHORT).show();
                return;
            }
            latitud = location.getLatitude();
            longitud = location.getLongitude();
            binding.txtUbicacion.setText(String.format(Locale.US, "%.6f, %.6f", latitud, longitud));
        });
    }

    private void guardarRegistroCompleto() {
        if (!RoleManager.MESA_PARTES.equals(RoleManager.normalize(tokenManager.obtenerRol()))) {
            Toast.makeText(requireContext(), R.string.role_action_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        if (administrados.isEmpty()) {
            Toast.makeText(requireContext(), "Primero registra un administrado", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tiposDocumento.isEmpty() || oficinas.isEmpty() || especialistas.isEmpty()) {
            Toast.makeText(requireContext(), "Faltan tipos de documento, oficinas o especialistas", Toast.LENGTH_SHORT).show();
            return;
        }

        Administrado administrado = administrados.get(binding.spinnerAdministrado.getSelectedItemPosition());
        TipoDocumento tipoDocumento = tiposDocumento.get(binding.spinnerTipoDocumento.getSelectedItemPosition());
        Oficina oficinaDestino = oficinas.get(binding.spinnerOficinaDestino.getSelectedItemPosition());
        Personal especialista = especialistas.get(binding.spinnerEspecialistaDestino.getSelectedItemPosition());
        String numeroExpediente = texto(binding.txtNumeroExpediente.getText());
        String numeroDocumento = texto(binding.txtNumeroDocumento.getText());
        int folios = entero(texto(binding.txtFolios.getText()));
        String asunto = texto(binding.txtAsunto.getText());
        String prioridad = String.valueOf(binding.spinnerPrioridad.getSelectedItem()).toUpperCase(Locale.US);

        if (numeroExpediente.isEmpty() || numeroDocumento.isEmpty() || asunto.isEmpty()) {
            Toast.makeText(requireContext(), "Numero de expediente, documento y asunto son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
        if (folios < 1) {
            Toast.makeText(requireContext(), "La cantidad de folios debe ser mayor a cero", Toast.LENGTH_SHORT).show();
            return;
        }

        Context context = requireContext().getApplicationContext();
        int usuarioId = tokenManager.obtenerIdUsuario();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            try {
                db.runInTransaction(() -> {
                    long ahora = System.currentTimeMillis();

                    Expediente expediente = new Expediente();
                    expediente.nroExpedienteAnual = numeroExpediente;
                    expediente.fechaHoraApertura = ahora;
                    expediente.asuntoGeneral = asunto;
                    expediente.estadoGlobal = "ABIERTO";
                    expediente.idUsuarioRegistro = usuarioId;
                    expediente.updatedAt = ahora;
                    expediente.sincronizado = false;
                    expediente.syncStatus = "PENDING";
                    int idExpediente = (int) db.expedienteDao().insertar(expediente);

                    DocumentoIngresado documento = new DocumentoIngresado();
                    documento.nroDocumentoUnico = numeroDocumento;
                    documento.idExpediente = idExpediente;
                    documento.idTipoDocumento = tipoDocumento.idTipoDocumento;
                    documento.idAdministrado = administrado.idAdministrado;
                    documento.cantidadFolios = folios;
                    documento.fechaHoraRecepcion = ahora;
                    documento.updatedAt = ahora;
                    documento.sincronizado = false;
                    documento.syncStatus = "PENDING";
                    int idDocumento = (int) db.documentoDao().insertar(documento);

                    HojaRuta hojaRuta = new HojaRuta();
                    hojaRuta.codigoBarrasSeguimiento = "HR-" + ahora;
                    hojaRuta.idDocumento = idDocumento;
                    hojaRuta.idEmpleadoAsignado = especialista.idEmpleado;
                    hojaRuta.idOficinaProcedencia = oficinaDestino.idOficina;
                    hojaRuta.fechaHoraDespacho = ahora;
                    hojaRuta.prioridadEnvio = prioridad;
                    hojaRuta.estadoDerivacion = "PENDIENTE";
                    hojaRuta.latitud = latitud;
                    hojaRuta.longitud = longitud;
                    hojaRuta.updatedAt = ahora;
                    hojaRuta.sincronizado = false;
                    hojaRuta.syncStatus = "PENDING";
                    db.hojaRutaDao().insertar(hojaRuta);
                });
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Expediente registrado y derivado", Toast.LENGTH_LONG).show();
                    limpiarFormulario();
                });
            } catch (Exception error) {
                mostrar("No se pudo registrar el expediente completo");
            }
        });
    }

    private void limpiarFormulario() {
        binding.txtNumeroDocumento.setText("");
        binding.txtFolios.setText("");
        binding.txtAsunto.setText("");
        binding.spinnerPrioridad.setSelection(0);
        binding.txtUbicacion.setText(R.string.map_location_missing);
        latitud = 0;
        longitud = 0;
        sugerirNumeroExpediente();
    }

    private void sugerirNumeroExpediente() {
        if (binding != null) binding.txtNumeroExpediente.setText(generarNumeroExpediente(System.currentTimeMillis()));
    }

    private void actualizarDetalleAdministrado() {
        if (binding == null || administrados.isEmpty()) {
            if (binding != null) binding.txtAdministradoDetalle.setText("Sin administrados disponibles");
            return;
        }
        Administrado item = administrados.get(binding.spinnerAdministrado.getSelectedItemPosition());
        binding.txtAdministradoDetalle.setText("DNI/RUC: " + safe(item.dniRuc)
                + "\nTelefono: " + safe(item.telefono)
                + "\nCorreo: " + safe(item.correoNotificaciones));
    }

    private void setSpinner(Spinner spinner, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private List<String> nombresAdministrados(List<Administrado> items) {
        List<String> values = new ArrayList<>();
        for (Administrado item : items) values.add(safe(item.nombreRazonSocial) + " - " + safe(item.dniRuc));
        if (values.isEmpty()) values.add("Sin administrados");
        return values;
    }

    private List<String> nombresTipos(List<TipoDocumento> items) {
        List<String> values = new ArrayList<>();
        for (TipoDocumento item : items) values.add(safe(item.nombreTipoDocumento));
        if (values.isEmpty()) values.add("Sin tipos de documento");
        return values;
    }

    private List<String> nombresOficinas(List<Oficina> items) {
        List<String> values = new ArrayList<>();
        for (Oficina item : items) values.add(safe(item.nombreUnidad));
        if (values.isEmpty()) values.add("Sin oficinas");
        return values;
    }

    private List<String> nombresEspecialistas(List<Personal> items) {
        List<String> values = new ArrayList<>();
        for (Personal item : items) values.add(safe(item.nombreCompleto) + " - " + safe(item.cargo));
        if (values.isEmpty()) values.add("Sin especialistas");
        return values;
    }

    private void mostrar(String mensaje) {
        if (isAdded()) requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show());
    }

    private String generarNumeroExpediente(long timestamp) {
        return "EXP-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date(timestamp));
    }

    private String texto(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int entero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception error) {
            return 0;
        }
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
