package com.example.tarea16.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
    private ActivityResultLauncher<String> permisoCamaraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentoFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && rutaFoto != null) {
                binding.txtFoto.setText(rutaFoto);
            }
        });
        permisoCamaraLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                abrirCamara();
            } else {
                Toast.makeText(this, "Permiso de camara denegado", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnFoto.setOnClickListener(v -> fotografiar());
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void fotografiar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        try {
            File directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (directorio == null) {
                Toast.makeText(this, "No se pudo preparar el almacenamiento", Toast.LENGTH_SHORT).show();
                return;
            }
            File archivo = File.createTempFile("documento_", ".jpg", directorio);
            rutaFoto = archivo.getAbsolutePath();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newUri(getContentResolver(), "documento", uri));
            if (intent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "No hay aplicacion de camara disponible", Toast.LENGTH_SHORT).show();
                return;
            }
            cameraLauncher.launch(intent);
        } catch (IOException | IllegalArgumentException | ActivityNotFoundException error) {
            Toast.makeText(this, "No se pudo abrir la camara", Toast.LENGTH_SHORT).show();
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
