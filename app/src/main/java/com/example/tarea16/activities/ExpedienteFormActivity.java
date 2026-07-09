package com.example.tarea16.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tarea16.databinding.ActivityExpedienteFormBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Expediente;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpedienteFormActivity extends AppCompatActivity {
    private ActivityExpedienteFormBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExpedienteFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void guardar() {
        Expediente item = new Expediente();
        item.nroExpedienteAnual = binding.txtNumero.getText().toString();
        item.asuntoGeneral = binding.txtAsunto.getText().toString();
        item.estadoGlobal = binding.txtEstado.getText().toString().isEmpty() ? "ABIERTO" : binding.txtEstado.getText().toString();
        item.fechaHoraApertura = System.currentTimeMillis();
        item.updatedAt = System.currentTimeMillis();
        executor.execute(() -> {
            AppDatabase.getInstance(this).expedienteDao().insertar(item);
            runOnUiThread(this::finish);
        });
    }
}
