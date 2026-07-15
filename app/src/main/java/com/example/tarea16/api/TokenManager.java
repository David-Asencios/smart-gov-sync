package com.example.tarea16.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.example.tarea16.security.RoleManager;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class TokenManager {
    private static final String PREFS = "smart_gov_secure_session";
    private static final String KEY_ALIAS = "smart_gov_session_key_v1";
    private final SharedPreferences preferences;

    public TokenManager(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean guardarSesion(LoginResponse response) {
        if (response == null || response.token == null || !RoleManager.isAllowed(response.rol)) return false;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] encrypted = cipher.doFinal(response.token.getBytes(StandardCharsets.UTF_8));
            preferences.edit()
                    .putString("token_cipher", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString("token_iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString("username", response.username)
                    .putString("role", RoleManager.normalize(response.rol))
                    .putInt("id_usuario", valueOrZero(response.idUsuario))
                    .putInt("id_empleado", valueOrZero(response.idEmpleado))
                    .putInt("id_oficina", valueOrZero(response.idOficina))
                    .putString("nombre_completo", response.nombreCompleto)
                    .putString("nombre_oficina", response.nombreOficina)
                    .apply();
            return true;
        } catch (Exception error) {
            limpiar();
            return false;
        }
    }

    public String obtenerToken() {
        try {
            String encrypted = preferences.getString("token_cipher", null);
            String iv = preferences.getString("token_iv", null);
            if (encrypted == null || iv == null) return null;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                    new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception error) {
            limpiar();
            return null;
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }

    public String obtenerUsuario() { return preferences.getString("username", "usuario"); }
    public String obtenerNombreCompleto() { return preferences.getString("nombre_completo", obtenerUsuario()); }
    public String obtenerNombreOficina() { return preferences.getString("nombre_oficina", "Sin oficina asignada"); }
    public String obtenerRol() { return RoleManager.normalize(preferences.getString("role", null)); }
    public int obtenerIdUsuario() { return preferences.getInt("id_usuario", 0); }
    public int obtenerIdEmpleado() { return preferences.getInt("id_empleado", 0); }
    public int obtenerIdOficina() { return preferences.getInt("id_oficina", 0); }

    public boolean sesionValida() {
        String token = obtenerToken();
        return token != null && RoleManager.isAllowed(obtenerRol()) && !jwtVencido(token);
    }

    public boolean tieneToken() { return sesionValida(); }

    private boolean jwtVencido(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return true;
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING), StandardCharsets.UTF_8);
            long expiresAt = new JSONObject(payload).optLong("exp", 0L) * 1000L;
            return expiresAt <= System.currentTimeMillis();
        } catch (Exception error) {
            return true;
        }
    }

    private int valueOrZero(Integer value) { return value == null ? 0 : value; }
    public void guardarUltimaSync(long value) { preferences.edit().putLong("ultima_sync", value).apply(); }
    public long obtenerUltimaSync() { return preferences.getLong("ultima_sync", 0); }
    public void limpiar() { preferences.edit().clear().apply(); }
}
