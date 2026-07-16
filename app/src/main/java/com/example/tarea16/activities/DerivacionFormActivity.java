package com.example.tarea16.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tarea16.R;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.ActivityDerivacionFormBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.HojaRuta;
import com.example.tarea16.security.RoleManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DerivacionFormActivity extends AppCompatActivity {
    private ActivityDerivacionFormBinding binding;
    private FusedLocationProviderClient locationClient;
    private double latitud;
    private double longitud;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> permisoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!RoleManager.canCreateDerivaciones(new TokenManager(this).obtenerRol())) {
            Toast.makeText(this, R.string.role_action_denied, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding = ActivityDerivacionFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        permisoLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) capturarUbicacion();
        });
        binding.btnUbicacion.setOnClickListener(v -> pedirUbicacion());
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void pedirUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            capturarUbicacion();
        } else {
            permisoLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void capturarUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                latitud = location.getLatitude();
                longitud = location.getLongitude();
                binding.txtUbicacion.setText(latitud + ", " + longitud);
            }
        });
    }

    private void guardar() {
        String codigo = binding.txtCodigo.getText().toString().trim();
        int idDocumento = entero(binding.txtDocumento.getText().toString());
        int idEmpleado = entero(binding.txtEmpleado.getText().toString());
        int idOficina = entero(binding.txtOficina.getText().toString());
        String prioridad = binding.txtPrioridad.getText().toString().trim().isEmpty()
                ? "NORMAL" : binding.txtPrioridad.getText().toString().trim().toUpperCase(java.util.Locale.US);
        if (codigo.isEmpty()) {
            Toast.makeText(this, "El codigo de seguimiento es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idDocumento < 1 || idEmpleado < 1 || idOficina < 1) {
            Toast.makeText(this, "Documento, empleado y oficina deben ser validos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!prioridad.equals("BAJA") && !prioridad.equals("NORMAL") && !prioridad.equals("ALTA")) {
            Toast.makeText(this, "Prioridad invalida", Toast.LENGTH_SHORT).show();
            return;
        }
        HojaRuta item = new HojaRuta();
        item.codigoBarrasSeguimiento = codigo;
        item.idDocumento = idDocumento;
        item.idEmpleadoAsignado = idEmpleado;
        item.idOficinaProcedencia = idOficina;
        item.prioridadEnvio = prioridad;
        item.observacionesReceptor = binding.txtObservaciones.getText().toString();
        item.latitud = latitud;
        item.longitud = longitud;
        item.fechaHoraDespacho = System.currentTimeMillis();
        item.updatedAt = System.currentTimeMillis();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            if (db.documentoDao().existe(idDocumento) == 0
                    || db.personalDao().existe(idEmpleado) == 0
                    || db.oficinaDao().existe(idOficina) == 0) {
                runOnUiThread(() -> Toast.makeText(this, "Las referencias locales no existen", Toast.LENGTH_SHORT).show());
                return;
            }
            db.hojaRutaDao().insertar(item);
            runOnUiThread(this::finish);
        });
    }

    private int entero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception error) {
            return 0;
        }
    }
}
