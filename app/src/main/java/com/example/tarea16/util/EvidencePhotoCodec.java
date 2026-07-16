package com.example.tarea16.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public final class EvidencePhotoCodec {
    private static final String PREFIX = "data:image/jpeg;base64,";
    private EvidencePhotoCodec() { }

    public static String toDataUrl(String source) {
        if (source == null || source.trim().isEmpty() || source.startsWith("data:image/")) return source;
        Bitmap original = BitmapFactory.decodeFile(source);
        if (original == null) return null;
        int max = 1600;
        float scale = Math.min(1f, max / (float) Math.max(original.getWidth(), original.getHeight()));
        Bitmap output = scale < 1f ? Bitmap.createScaledBitmap(original,
                Math.round(original.getWidth() * scale), Math.round(original.getHeight() * scale), true) : original;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        output.compress(Bitmap.CompressFormat.JPEG, 75, bytes);
        if (output != original) output.recycle();
        original.recycle();
        return PREFIX + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP);
    }

    public static Bitmap decode(String source) {
        if (source == null || source.trim().isEmpty()) return null;
        if (!source.startsWith("data:image/")) return BitmapFactory.decodeFile(source);
        int comma = source.indexOf(',');
        if (comma < 0) return null;
        try {
            byte[] bytes = Base64.decode(source.substring(comma + 1), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException error) { return null; }
    }
}
