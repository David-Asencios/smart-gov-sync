package com.example.tarea16.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tarea16.R;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.ActivityExpedienteFormBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.security.RoleManager;
import com.example.tarea16.modelo.Expediente;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpedienteFormActivity extends AppCompatActivity {
    private ActivityExpedienteFormBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!RoleManager.canCreateExpedientes(new TokenManager(this).obtenerRol())) {
            Toast.makeText(this, R.string.role_action_denied, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        super.onCreate(savedInstanceState);
        binding = ActivityExpedienteFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void guardar() {
        String numero = binding.txtNumero.getText().toString().trim();
        String asunto = binding.txtAsunto.getText().toString().trim();
        String estado = binding.txtEstado.getText().toString().trim().isEmpty()
                ? "ABIERTO" : binding.txtEstado.getText().toString().trim().toUpperCase(java.util.Locale.US);
        if (numero.isEmpty() || asunto.isEmpty()) {
            Toast.makeText(this, "Numero y asunto son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!estado.equals("ABIERTO") && !estado.equals("EN_PROCESO")
                && !estado.equals("CERRADO") && !estado.equals("ARCHIVADO")) {
            Toast.makeText(this, "Estado de expediente invalido", Toast.LENGTH_SHORT).show();
            return;
        }
        Expediente item = new Expediente();
        item.nroExpedienteAnual = numero;
        item.asuntoGeneral = asunto;
        item.estadoGlobal = estado;
        item.fechaHoraApertura = System.currentTimeMillis();
        item.updatedAt = System.currentTimeMillis();
        executor.execute(() -> {
            AppDatabase.getInstance(this).expedienteDao().insertar(item);
            runOnUiThread(this::finish);
        });
    }
}
