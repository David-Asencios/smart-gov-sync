package com.example.tarea16.api;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private final SharedPreferences preferences;

    public TokenManager(Context context) {
        preferences = context.getSharedPreferences("smart_gov_token", Context.MODE_PRIVATE);
    }

    public void guardarToken(String token) {
        preferences.edit().putString("token", token).apply();
    }

    public String obtenerToken() {
        return preferences.getString("token", null);
    }

    public boolean tieneToken() {
        String token = obtenerToken();
        return token != null && !token.isEmpty();
    }

    public void guardarUltimaSync(long value) {
        preferences.edit().putLong("ultima_sync", value).apply();
    }

    public long obtenerUltimaSync() {
        return preferences.getLong("ultima_sync", 0);
    }

    public void limpiar() {
        preferences.edit().clear().apply();
    }
}
