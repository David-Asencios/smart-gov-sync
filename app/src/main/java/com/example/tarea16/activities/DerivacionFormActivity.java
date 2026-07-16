package com.example.tarea16.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tarea16.R;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.ActivityDerivacionFormBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.HojaRuta;
import com.example.tarea16.modelo.Oficina;
import com.example.tarea16.modelo.Personal;
import com.example.tarea16.security.RoleManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DerivacionFormActivity extends AppCompatActivity {
    private ActivityDerivacionFormBinding binding;
    private FusedLocationProviderClient locationClient;
    private double latitud;
    private double longitud;
    private boolean ubicacionCapturada;
    private int idDocumento;
    private int idDerivacionOrigen;
    private final List<Oficina> oficinas = new ArrayList<>();
    private final List<Personal> todosEspecialistas = new ArrayList<>();
    private final List<Personal> especialistas = new ArrayList<>();
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
        idDocumento = getIntent().getIntExtra("id_documento", 0);
        idDerivacionOrigen = getIntent().getIntExtra("id_derivacion_origen", 0);
        if (idDocumento < 1 || idDerivacionOrigen < 1) {
            Toast.makeText(this, "Selecciona primero un expediente recibido", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        binding.txtDocumento.setText(String.valueOf(idDocumento));
        binding.txtDocumento.setEnabled(false);
        binding.txtCodigo.setText("HR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.US));
        binding.txtCodigo.setEnabled(false);
        binding.txtPrioridad.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"NORMAL", "ALTA", "BAJA"}));
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        permisoLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) capturarUbicacion();
        });
        binding.btnUbicacion.setOnClickListener(v -> pedirUbicacion());
        binding.btnGuardar.setOnClickListener(v -> guardar());
        binding.txtOficina.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { filtrarEspecialistas(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { filtrarEspecialistas(); }
        });
        cargarCatalogos();
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
                ubicacionCapturada = true;
                binding.txtUbicacion.setText(latitud + ", " + longitud);
            }
        });
    }

    private void guardar() {
        String codigo = binding.txtCodigo.getText().toString().trim();
        if (oficinas.isEmpty() || especialistas.isEmpty()) { Toast.makeText(this, "No hay destinos disponibles", Toast.LENGTH_LONG).show(); return; }
        int idEmpleado = especialistas.get(binding.txtEmpleado.getSelectedItemPosition()).idEmpleado;
        int idOficina = oficinas.get(binding.txtOficina.getSelectedItemPosition()).idOficina;
        String prioridad = String.valueOf(binding.txtPrioridad.getSelectedItem());
        if (codigo.isEmpty()) {
            Toast.makeText(this, "El codigo de seguimiento es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idDocumento < 1 || idEmpleado < 1 || idOficina < 1) {
            Toast.makeText(this, "Documento, empleado y oficina deben ser validos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idEmpleado == new TokenManager(this).obtenerIdEmpleado()) { Toast.makeText(this, "Selecciona otro especialista", Toast.LENGTH_LONG).show(); return; }
        if (!ubicacionCapturada) { Toast.makeText(this, "Captura la ubicación antes de derivar", Toast.LENGTH_LONG).show(); return; }
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
            try {
                db.runInTransaction(() -> {
                    int changed = db.hojaRutaDao().finalizarAtencion(idDerivacionOrigen,
                            "Atención derivada a otro especialista", System.currentTimeMillis());
                    if (changed == 0) throw new IllegalStateException("La derivación ya cambió de estado");
                    db.hojaRutaDao().insertar(item);
                });
                runOnUiThread(this::finish);
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this, R.string.transition_not_allowed, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void cargarCatalogos() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Oficina> oficinasDb = db.oficinaDao().listar();
            List<Personal> personalDb = db.personalDao().listar();
            runOnUiThread(() -> {
                oficinas.clear(); todosEspecialistas.clear();
                for (Oficina item : oficinasDb) if (!item.deleted) oficinas.add(item);
                for (Personal item : personalDb) if (!item.deleted) todosEspecialistas.add(item);
                List<String> nombres = new ArrayList<>();
                for (Oficina item : oficinas) nombres.add(item.nombreUnidad);
                binding.txtOficina.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres));
                filtrarEspecialistas();
            });
        });
    }

    private void filtrarEspecialistas() {
        if (binding == null || oficinas.isEmpty()) return;
        int position = binding.txtOficina.getSelectedItemPosition();
        if (position < 0 || position >= oficinas.size()) return;
        especialistas.clear();
        int oficinaId = oficinas.get(position).idOficina;
        List<String> nombres = new ArrayList<>();
        for (Personal item : todosEspecialistas) if (item.idOficina == oficinaId && !item.deleted) {
            especialistas.add(item); nombres.add(item.nombreCompleto + " - " + item.cargo);
        }
        binding.txtEmpleado.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres));
    }

}
