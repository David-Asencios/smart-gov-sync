package com.example.tarea16.security;

import com.example.tarea16.R;

import java.util.Locale;

public final class RoleManager {
    public static final String ADMIN = "ADMIN";
    public static final String MESA_PARTES = "MESA_PARTES";
    public static final String ESPECIALISTA = "ESPECIALISTA";
    public static final String ARCHIVO = "ARCHIVO";
    public static final String SIN_PERMISO = "SIN_PERMISO";

    private RoleManager() { }

    public static String normalize(String role) {
        if (role == null || role.trim().isEmpty()) return SIN_PERMISO;
        String value = role.trim().toUpperCase(Locale.US);
        if ("MESA".equals(value) || "MESA_DE_PARTES".equals(value)) return MESA_PARTES;
        if ("PERSONAL".equals(value)) return ESPECIALISTA;
        if (ADMIN.equals(value) || MESA_PARTES.equals(value)
                || ESPECIALISTA.equals(value) || ARCHIVO.equals(value)) return value;
        return SIN_PERMISO;
    }

    public static boolean isAllowed(String role) {
        return !SIN_PERMISO.equals(normalize(role));
    }

    /** Los roles deben provenir del servidor/base de datos, nunca del username. */
    @Deprecated
    public static String inferFromUsername(String ignoredUsername) {
        return SIN_PERMISO;
    }

    public static String displayName(String role) {
        switch (normalize(role)) {
            case ADMIN: return "Administrador";
            case MESA_PARTES: return "Mesa de Partes";
            case ESPECIALISTA: return "Especialista";
            case ARCHIVO: return "Archivo Central";
            default: return "Sin permisos";
        }
    }

    public static int homeDestination(String role) {
        switch (normalize(role)) {
            case ESPECIALISTA: return R.id.nav_bandeja;
            case ARCHIVO: return R.id.nav_archivo;
            case ADMIN:
            case MESA_PARTES: return R.id.nav_expedientes;
            default: return R.id.nav_sincronizacion;
        }
    }

    public static boolean canSeeDestination(String role, int itemId) {
        String normalized = normalize(role);
        if (SIN_PERMISO.equals(normalized)) return false;
        if (itemId == R.id.nav_sincronizacion) return true;
        if (ADMIN.equals(normalized)) return true;
        if (MESA_PARTES.equals(normalized)) {
            return itemId == R.id.nav_expedientes || itemId == R.id.nav_documentos;
        }
        if (ESPECIALISTA.equals(normalized)) {
            return itemId == R.id.nav_bandeja || itemId == R.id.nav_derivaciones;
        }
        return ARCHIVO.equals(normalized)
                && (itemId == R.id.nav_bandeja || itemId == R.id.nav_archivo);
    }

    public static boolean canCreateExpedientes(String role) {
        String normalized = normalize(role);
        return ADMIN.equals(normalized) || MESA_PARTES.equals(normalized);
    }

    public static boolean canCreateDocumentos(String role) { return canCreateExpedientes(role); }

    public static boolean canCreateDerivaciones(String role) {
        String normalized = normalize(role);
        return ADMIN.equals(normalized) || ESPECIALISTA.equals(normalized);
    }

    public static boolean canManageArchivo(String role) {
        String normalized = normalize(role);
        return ADMIN.equals(normalized) || ARCHIVO.equals(normalized);
    }

    public static boolean canUpdateBandeja(String role) {
        String normalized = normalize(role);
        return ADMIN.equals(normalized) || ESPECIALISTA.equals(normalized) || ARCHIVO.equals(normalized);
    }
}
