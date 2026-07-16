package com.example.tarea16.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.database.Cursor;
import android.location.LocationManager;
import android.provider.OpenableColumns;
import android.os.Environment;
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
import androidx.core.content.FileProvider;
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
import com.example.tarea16.util.DocumentAttachmentCodec;
import com.example.tarea16.sync.SyncScheduler;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import java.io.File;
import java.io.IOException;

public class FragmentRegistroMesa extends Fragment {
    private FragmentRegistroMesaBinding binding;
    private TokenManager tokenManager;
    private FusedLocationProviderClient locationClient;
    private ActivityResultLauncher<String> permisoUbicacionLauncher;
    private ActivityResultLauncher<String> permisoCamaraLauncher;
    private ActivityResultLauncher<Uri> fotoLauncher;
    private ActivityResultLauncher<String[]> adjuntoLauncher;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Administrado> administrados = new ArrayList<>();
    private final List<TipoDocumento> tiposDocumento = new ArrayList<>();
    private final List<Oficina> oficinas = new ArrayList<>();
    private final List<Personal> especialistas = new ArrayList<>();
    private final List<Personal> todosEspecialistas = new ArrayList<>();
    private double latitud;
    private double longitud;
    private boolean ubicacionCapturada;
    private String rutaFoto;
    private Uri fotoUri;
    private String rutaAdjunto;
    private String nombreAdjunto;
    private String tipoMimeAdjunto;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegistroMesaBinding.inflate(inflater, container, false);
        tokenManager = new TokenManager(requireContext());
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        permisoUbicacionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) capturarUbicacion();
            else Toast.makeText(requireContext(), "Permiso de ubicacion denegado", Toast.LENGTH_SHORT).show();
        });
        fotoLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && fotoUri != null && binding != null) {
                binding.imgDocumentoPreview.setImageURI(fotoUri);
                binding.imgDocumentoPreview.setVisibility(View.VISIBLE);
            } else {
                if (rutaFoto != null) new File(rutaFoto).delete();
                rutaFoto = null; fotoUri = null;
            }
        });
        permisoCamaraLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) abrirCamara(); else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
        });
        adjuntoLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::procesarAdjunto);
        configurarFormularioInicial();
        binding.btnUbicacion.setOnClickListener(v -> pedirUbicacion());
        binding.btnFotoDocumento.setOnClickListener(v -> fotografiarDocumento());
        binding.btnAdjuntarDocumento.setOnClickListener(v -> adjuntoLauncher.launch(new String[]{"application/pdf", "image/jpeg", "image/png"}));
        binding.btnGenerarCodigoDocumento.setOnClickListener(v -> sugerirCodigoDocumento());
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
        binding.spinnerOficinaDestino.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { actualizarEspecialistas(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { actualizarEspecialistas(); }
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
        sugerirCodigoDocumento();
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
                for (Administrado item : administradosDb) if (!item.deleted) administrados.add(item);
                tiposDocumento.clear();
                for (TipoDocumento item : tiposDb) if (!item.deleted) tiposDocumento.add(item);
                todosEspecialistas.clear();
                for (Personal item : especialistasDb) if (!item.deleted) todosEspecialistas.add(item);
                oficinas.clear();
                for (Oficina item : oficinasDb) {
                    if (!item.deleted && tieneEspecialistaActivo(item.idOficina)) oficinas.add(item);
                }

                setSpinner(binding.spinnerAdministrado, nombresAdministrados(administrados));
                setSpinner(binding.spinnerTipoDocumento, nombresTipos(tiposDocumento));
                setSpinner(binding.spinnerOficinaDestino, nombresOficinas(oficinas));
                actualizarEspecialistas();
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
        LocationManager manager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (manager == null || !manager.isLocationEnabled()) {
            ubicacionCapturada = false;
            Toast.makeText(requireContext(), "Activa la ubicación del dispositivo e inténtalo nuevamente", Toast.LENGTH_LONG).show();
            return;
        }
        binding.btnUbicacion.setEnabled(false);
        binding.txtUbicacion.setText("Obteniendo ubicación actual…");
        CancellationTokenSource cancellation = new CancellationTokenSource();
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.getToken()).addOnSuccessListener(location -> {
            if (binding == null) return;
            binding.btnUbicacion.setEnabled(true);
            if (location == null) {
                ubicacionCapturada = false;
                binding.txtUbicacion.setText(R.string.map_location_missing);
                Toast.makeText(requireContext(), "No se obtuvo una señal GPS. Sal a un lugar abierto e inténtalo nuevamente", Toast.LENGTH_LONG).show();
                return;
            }
            latitud = location.getLatitude();
            longitud = location.getLongitude();
            ubicacionCapturada = true;
            binding.txtUbicacion.setText(String.format(Locale.US, "%.6f, %.6f · precisión %.0f m",
                    latitud, longitud, location.getAccuracy()));
        }).addOnFailureListener(error -> {
            if (binding == null) return;
            binding.btnUbicacion.setEnabled(true);
            ubicacionCapturada = false;
            binding.txtUbicacion.setText(R.string.map_location_missing);
            Toast.makeText(requireContext(), "No se pudo consultar la ubicación actual", Toast.LENGTH_LONG).show();
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
        if (!ubicacionCapturada) {
            Toast.makeText(requireContext(), "Captura la ubicación antes de registrar la derivación", Toast.LENGTH_LONG).show();
            return;
        }
        if (rutaFoto == null) {
            Toast.makeText(requireContext(), "Fotografía el documento antes de registrarlo", Toast.LENGTH_LONG).show();
            return;
        }

        Context context = requireContext().getApplicationContext();
        int usuarioId = tokenManager.obtenerIdUsuario();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            try {
                if (db.expedienteDao().existeNumero(numeroExpediente) > 0) { mostrar("El número de expediente ya existe"); return; }
                if (db.documentoDao().existeNumero(numeroDocumento) > 0) { mostrar("El número de documento ya existe"); return; }
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
                    documento.rutaFoto = rutaFoto;
                    documento.rutaAdjunto = rutaAdjunto;
                    documento.nombreAdjunto = nombreAdjunto;
                    documento.tipoMimeAdjunto = tipoMimeAdjunto;
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
                    new com.example.tarea16.sync.SyncManager(requireContext().getApplicationContext()).sincronizar(result -> {
                        if (!result.exitoso) SyncScheduler.enqueueNow(requireContext().getApplicationContext());
                    });
                });
            } catch (Exception error) {
                mostrar("No se pudo registrar el expediente completo");
            }
        });
    }

    private void limpiarFormulario() {
        binding.txtFolios.setText("");
        binding.txtAsunto.setText("");
        binding.spinnerPrioridad.setSelection(0);
        binding.txtUbicacion.setText(R.string.map_location_missing);
        latitud = 0;
        longitud = 0;
        ubicacionCapturada = false;
        rutaFoto = null;
        fotoUri = null;
        rutaAdjunto = null;
        nombreAdjunto = null;
        tipoMimeAdjunto = null;
        binding.txtAdjuntoDocumento.setText(R.string.sin_adjunto);
        binding.imgDocumentoPreview.setImageDrawable(null);
        binding.imgDocumentoPreview.setVisibility(View.GONE);
        sugerirNumeroExpediente();
        sugerirCodigoDocumento();
    }

    private void sugerirNumeroExpediente() {
        if (binding != null) binding.txtNumeroExpediente.setText(generarNumeroExpediente(System.currentTimeMillis()));
    }

    private void sugerirCodigoDocumento() {
        if (binding != null) binding.txtNumeroDocumento.setText(generarCodigoDocumento(System.currentTimeMillis()));
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

    private boolean tieneEspecialistaActivo(int oficinaId) {
        for (Personal item : todosEspecialistas) {
            if (!item.deleted && item.idOficina == oficinaId) return true;
        }
        return false;
    }

    private List<String> nombresEspecialistas(List<Personal> items) {
        List<String> values = new ArrayList<>();
        for (Personal item : items) values.add(safe(item.nombreCompleto) + " - " + safe(item.cargo));
        if (values.isEmpty()) values.add("Sin especialistas");
        return values;
    }

    private void actualizarEspecialistas() {
        if (binding == null || oficinas.isEmpty()) return;
        int position = binding.spinnerOficinaDestino.getSelectedItemPosition();
        if (position < 0 || position >= oficinas.size()) return;
        int oficinaId = oficinas.get(position).idOficina;
        List<Personal> filtrados = new ArrayList<>();
        for (Personal item : todosEspecialistas) if (item.idOficina == oficinaId && !item.deleted) filtrados.add(item);
        especialistas.clear();
        especialistas.addAll(filtrados);
        setSpinner(binding.spinnerEspecialistaDestino, nombresEspecialistas(especialistas));
    }

    private void fotografiarDocumento() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) abrirCamara();
        else permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
    }

    private void procesarAdjunto(Uri uri) {
        if (uri == null || binding == null) return;
        String mime = requireContext().getContentResolver().getType(uri);
        if (!("application/pdf".equals(mime) || "image/jpeg".equals(mime) || "image/png".equals(mime))) {
            Toast.makeText(requireContext(), "Solo se permiten archivos PDF, JPG o PNG", Toast.LENGTH_LONG).show();
            return;
        }
        String name = nombreArchivo(uri);
        String extension = "application/pdf".equals(mime) ? ".pdf" : ("image/png".equals(mime) ? ".png" : ".jpg");
        try {
            String copied = DocumentAttachmentCodec.copyToApp(requireContext(), uri, extension);
            if (rutaAdjunto != null) new File(rutaAdjunto).delete();
            rutaAdjunto = copied;
            nombreAdjunto = name;
            tipoMimeAdjunto = mime;
            binding.txtAdjuntoDocumento.setText(name + " · máximo 5 MB");
        } catch (Exception error) {
            Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String nombreArchivo(Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        }
        return "documento_adjunto";
    }

    private void abrirCamara() {
        try {
            File directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (directory == null) throw new IOException("Directorio no disponible");
            File photo = File.createTempFile("documento_", ".jpg", directory);
            rutaFoto = photo.getAbsolutePath();
            fotoUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", photo);
            fotoLauncher.launch(fotoUri);
        } catch (IOException | IllegalArgumentException | SecurityException error) {
            rutaFoto = null; fotoUri = null;
            Toast.makeText(requireContext(), "No se pudo abrir la cámara", Toast.LENGTH_LONG).show();
        }
    }

    private void mostrar(String mensaje) {
        if (isAdded()) requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show());
    }

    private String generarNumeroExpediente(long timestamp) {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.US);
        return "EXP-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date(timestamp)) + "-" + suffix;
    }

    private String generarCodigoDocumento(long timestamp) {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.US);
        return "DOC-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date(timestamp)) + "-" + suffix;
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
