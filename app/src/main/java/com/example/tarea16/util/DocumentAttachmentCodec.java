package com.example.tarea16.util;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class DocumentAttachmentCodec {
    public static final long MAX_BYTES = 5L * 1024L * 1024L;

    private DocumentAttachmentCodec() {}

    public static String copyToApp(Context context, Uri uri, String extension) throws Exception {
        File directory = new File(context.getFilesDir(), "documentos");
        if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("No se pudo crear el directorio");
        File target = File.createTempFile("adjunto_", extension, directory);
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(target)) {
            if (input == null) throw new IllegalArgumentException("Archivo no disponible");
            copyLimited(input, output);
        } catch (Exception error) {
            target.delete();
            throw error;
        }
        return target.getAbsolutePath();
    }

    public static String toDataUrl(String source, String mimeType) {
        if (source == null || source.trim().isEmpty() || source.startsWith("data:")) return source;
        try (FileInputStream input = new FileInputStream(source); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copyLimited(input, output);
            return "data:" + mimeType + ";base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } catch (Exception error) {
            throw new IllegalArgumentException("No se pudo preparar el archivo adjunto", error);
        }
    }

    private static void copyLimited(InputStream input, java.io.OutputStream output) throws Exception {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BYTES) throw new IllegalArgumentException("El archivo supera el máximo de 5 MB");
            output.write(buffer, 0, read);
        }
    }
}
