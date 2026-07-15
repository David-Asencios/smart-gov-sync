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
        HojaRuta item = new HojaRuta();
        item.codigoBarrasSeguimiento = binding.txtCodigo.getText().toString();
        item.idDocumento = entero(binding.txtDocumento.getText().toString());
        item.idEmpleadoAsignado = entero(binding.txtEmpleado.getText().toString());
        item.idOficinaProcedencia = entero(binding.txtOficina.getText().toString());
        item.prioridadEnvio = binding.txtPrioridad.getText().toString().isEmpty() ? "NORMAL" : binding.txtPrioridad.getText().toString();
        item.observacionesReceptor = binding.txtObservaciones.getText().toString();
        item.latitud = latitud;
        item.longitud = longitud;
        item.fechaHoraDespacho = System.currentTimeMillis();
        item.updatedAt = System.currentTimeMillis();
        executor.execute(() -> {
            AppDatabase.getInstance(this).hojaRutaDao().insertar(item);
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
