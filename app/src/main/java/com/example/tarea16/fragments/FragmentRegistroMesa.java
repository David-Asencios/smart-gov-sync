package com.example.tarea16.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.tarea16.modelo.Direccion;
import com.example.tarea16.modelo.DocumentoIngresado;
import com.example.tarea16.modelo.Expediente;
import com.example.tarea16.modelo.HojaRuta;
import com.example.tarea16.security.RoleManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentRegistroMesa extends Fragment {
    private FragmentRegistroMesaBinding binding;
    private TokenManager tokenManager;
    private FusedLocationProviderClient locationClient;
    private ActivityResultLauncher<String> permisoUbicacionLauncher;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
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
        binding.btnUbicacion.setOnClickListener(v -> pedirUbicacion());
        binding.btnGuardarRegistro.setOnClickListener(v -> guardarRegistroCompleto());
        return binding.getRoot();
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
            binding.txtUbicacion.setText(latitud + ", " + longitud);
        });
    }

    private void guardarRegistroCompleto() {
        if (!RoleManager.MESA_PARTES.equals(RoleManager.normalize(tokenManager.obtenerRol()))) {
            Toast.makeText(requireContext(), R.string.role_action_denied, Toast.LENGTH_SHORT).show();
            return;
        }

        String dniRuc = texto(binding.txtDniRuc.getText());
        String nombre = texto(binding.txtNombreAdministrado.getText());
        String telefono = texto(binding.txtTelefono.getText());
        String correo = texto(binding.txtCorreo.getText());
        String calle = texto(binding.txtCalle.getText());
        String numeroDireccion = texto(binding.txtNumeroDireccion.getText());
        String distritoCiudad = texto(binding.txtDistritoCiudad.getText());
        String numeroDocumento = texto(binding.txtNumeroDocumento.getText());
        int idTipoDocumento = entero(texto(binding.txtTipoDocumento.getText()));
        int folios = entero(texto(binding.txtFolios.getText()));
        String asunto = texto(binding.txtAsunto.getText());
        int idOficinaDestino = entero(texto(binding.txtOficinaDestino.getText()));
        int idEspecialista = entero(texto(binding.txtEspecialistaDestino.getText()));
        String prioridad = texto(binding.txtPrioridad.getText()).toUpperCase(Locale.US);

        if (dniRuc.isEmpty() || nombre.isEmpty()) {
            Toast.makeText(requireContext(), "DNI/RUC y nombre del administrado son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
        if (numeroDocumento.isEmpty() || asunto.isEmpty()) {
            Toast.makeText(requireContext(), "Numero de documento y asunto son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idTipoDocumento < 1 || folios < 1 || idOficinaDestino < 1 || idEspecialista < 1) {
            Toast.makeText(requireContext(), "Tipo, folios, oficina y especialista deben ser validos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!prioridad.equals("BAJA") && !prioridad.equals("NORMAL") && !prioridad.equals("ALTA")) {
            Toast.makeText(requireContext(), "Prioridad invalida. Usa BAJA, NORMAL o ALTA", Toast.LENGTH_SHORT).show();
            return;
        }

        Context context = requireContext().getApplicationContext();
        int usuarioId = tokenManager.obtenerIdUsuario();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db.tipoDocumentoDao().existe(idTipoDocumento) == 0
                    || db.oficinaDao().existe(idOficinaDestino) == 0
                    || db.personalDao().existe(idEspecialista) == 0) {
                mostrar("Tipo de documento, oficina o especialista no existen o estan inactivos");
                return;
            }

            try {
                db.runInTransaction(() -> {
                    long ahora = System.currentTimeMillis();
                    Administrado administrado = db.administradoDao().buscarPorDniRuc(dniRuc);
                    int idAdministrado;
                    if (administrado == null) {
                        administrado = new Administrado();
                        administrado.codigoAdministrado = "ADM-" + dniRuc;
                        administrado.dniRuc = dniRuc;
                        administrado.nombreRazonSocial = nombre;
                        administrado.telefono = telefono;
                        administrado.correoNotificaciones = correo;
                        administrado.updatedAt = ahora;
                        idAdministrado = (int) db.administradoDao().insertar(administrado);
                    } else {
                        administrado.nombreRazonSocial = nombre;
                        administrado.telefono = telefono;
                        administrado.correoNotificaciones = correo;
                        administrado.sincronizado = false;
                        administrado.syncStatus = "PENDING";
                        administrado.syncError = null;
                        administrado.updatedAt = ahora;
                        db.administradoDao().actualizar(administrado);
                        idAdministrado = administrado.idAdministrado;
                    }

                    guardarDireccionSiCorresponde(db, idAdministrado, ahora, calle, numeroDireccion, distritoCiudad);

                    Expediente expediente = new Expediente();
                    expediente.nroExpedienteAnual = generarNumeroExpediente(ahora);
                    expediente.fechaHoraApertura = ahora;
                    expediente.asuntoGeneral = asunto;
                    expediente.estadoGlobal = "ABIERTO";
                    expediente.idUsuarioRegistro = usuarioId;
                    expediente.updatedAt = ahora;
                    int idExpediente = (int) db.expedienteDao().insertar(expediente);

                    DocumentoIngresado documento = new DocumentoIngresado();
                    documento.nroDocumentoUnico = numeroDocumento;
                    documento.idExpediente = idExpediente;
                    documento.idTipoDocumento = idTipoDocumento;
                    documento.idAdministrado = idAdministrado;
                    documento.cantidadFolios = folios;
                    documento.fechaHoraRecepcion = ahora;
                    documento.updatedAt = ahora;
                    int idDocumento = (int) db.documentoDao().insertar(documento);

                    HojaRuta hojaRuta = new HojaRuta();
                    hojaRuta.codigoBarrasSeguimiento = "HR-" + ahora;
                    hojaRuta.idDocumento = idDocumento;
                    hojaRuta.idEmpleadoAsignado = idEspecialista;
                    hojaRuta.idOficinaProcedencia = idOficinaDestino;
                    hojaRuta.fechaHoraDespacho = ahora;
                    hojaRuta.prioridadEnvio = prioridad;
                    hojaRuta.estadoDerivacion = "PENDIENTE";
                    hojaRuta.latitud = latitud;
                    hojaRuta.longitud = longitud;
                    hojaRuta.updatedAt = ahora;
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

    private void guardarDireccionSiCorresponde(AppDatabase db, int idAdministrado, long ahora,
                                               String calle, String numero, String distritoCiudad) {
        if (calle.isEmpty() && numero.isEmpty() && distritoCiudad.isEmpty()) return;
        Direccion direccion = new Direccion();
        direccion.idAdministrado = idAdministrado;
        direccion.tipoInmueble = "DOMICILIO";
        direccion.calle = calle;
        direccion.numero = numero;
        direccion.comunaDistrito = distritoCiudad;
        direccion.ciudad = distritoCiudad;
        direccion.updatedAt = ahora;
        db.direccionDao().insertar(direccion);
    }

    private void limpiarFormulario() {
        binding.txtDniRuc.setText("");
        binding.txtNombreAdministrado.setText("");
        binding.txtTelefono.setText("");
        binding.txtCorreo.setText("");
        binding.txtCalle.setText("");
        binding.txtNumeroDireccion.setText("");
        binding.txtDistritoCiudad.setText("");
        binding.txtNumeroDocumento.setText("");
        binding.txtTipoDocumento.setText("");
        binding.txtFolios.setText("");
        binding.txtAsunto.setText("");
        binding.txtOficinaDestino.setText("");
        binding.txtEspecialistaDestino.setText("");
        binding.txtPrioridad.setText("NORMAL");
        binding.txtUbicacion.setText(R.string.map_location_missing);
        latitud = 0;
        longitud = 0;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
