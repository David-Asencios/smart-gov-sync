package com.example.tarea16.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.tarea16.api.ApiClient;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.modelo.DocumentoIngresado;

import java.io.File;
import java.io.FileOutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class AttachmentDownloader {
    private AttachmentDownloader() {}

    public static void open(Context source, DocumentoIngresado document) {
        Context context = source.getApplicationContext();
        if (document == null || document.serverId == null || document.nombreAdjunto == null) {
            Toast.makeText(source, "El documento no tiene un adjunto disponible", Toast.LENGTH_LONG).show();
            return;
        }
        String authorization = "Bearer " + new TokenManager(context).obtenerToken();
        ApiClient.getService().descargarAdjunto(authorization, document.serverId).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(context, "No se pudo descargar el adjunto (" + response.code() + ")", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    File directory = new File(context.getCacheDir(), "adjuntos");
                    if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException();
                    String safeName = document.nombreAdjunto.replaceAll("[^a-zA-Z0-9._-]", "_");
                    File target = new File(directory, System.currentTimeMillis() + "_" + safeName);
                    try (ResponseBody body = response.body(); FileOutputStream output = new FileOutputStream(target)) {
                        byte[] buffer = new byte[8192]; int read;
                        java.io.InputStream input = body.byteStream();
                        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                    }
                    Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", target);
                    Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, document.tipoMimeAdjunto)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception error) {
                    Toast.makeText(context, "No hay una aplicación compatible para abrir el adjunto", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable error) {
                Toast.makeText(context, "No se pudo conectar para descargar el adjunto", Toast.LENGTH_LONG).show();
            }
        });
    }
}
