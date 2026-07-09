package com.example.tarea16.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.tarea16.databinding.ActivityDocumentoFormBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.DocumentoIngresado;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DocumentoFormActivity extends AppCompatActivity {
    private ActivityDocumentoFormBinding binding;
    private String rutaFoto;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentoFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> binding.txtFoto.setText(rutaFoto == null ? "" : rutaFoto));
        binding.btnFoto.setOnClickListener(v -> fotografiar());
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void fotografiar() {
        try {
            File archivo = File.createTempFile("documento_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            rutaFoto = archivo.getAbsolutePath();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            cameraLauncher.launch(intent);
        } catch (IOException ignored) {
        }
    }

    private void guardar() {
        DocumentoIngresado item = new DocumentoIngresado();
        item.nroDocumentoUnico = binding.txtNumero.getText().toString();
        item.idExpediente = entero(binding.txtExpediente.getText().toString());
        item.idTipoDocumento = entero(binding.txtTipo.getText().toString());
        item.idAdministrado = entero(binding.txtAdministrado.getText().toString());
        item.cantidadFolios = entero(binding.txtFolios.getText().toString());
        item.rutaFoto = rutaFoto;
        item.fechaHoraRecepcion = System.currentTimeMillis();
        item.updatedAt = System.currentTimeMillis();
        executor.execute(() -> {
            AppDatabase.getInstance(this).documentoDao().insertar(item);
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
