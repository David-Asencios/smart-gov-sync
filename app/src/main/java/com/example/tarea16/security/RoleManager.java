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
        return R.id.nav_inicio;
    }

    public static boolean canSeeDestination(String role, int itemId) {
        String normalized = normalize(role);
        if (SIN_PERMISO.equals(normalized)) return false;
        if (itemId == R.id.nav_inicio) return true;
        if (itemId == R.id.nav_sincronizacion) return false;
        if (ADMIN.equals(normalized)) {
            return itemId == R.id.nav_admin_usuarios
                    || itemId == R.id.nav_admin_personal
                    || itemId == R.id.nav_admin_oficinas
                    || itemId == R.id.nav_admin_tipos_documentos
                    || itemId == R.id.nav_admin_consultar_expedientes;
        }
        if (MESA_PARTES.equals(normalized)) {
            return itemId == R.id.nav_mesa_administrados
                    || itemId == R.id.nav_mesa_registrar_expediente
                    || itemId == R.id.nav_mesa_expedientes_registrados;
        }
        if (ESPECIALISTA.equals(normalized)) {
            return itemId == R.id.nav_especialista_bandeja
                    || itemId == R.id.nav_especialista_mis_expedientes;
        }
        return ARCHIVO.equals(normalized)
                && (itemId == R.id.nav_archivo_por_archivar
                || itemId == R.id.nav_archivo_fisico
                || itemId == R.id.nav_archivo_historial);
    }

    public static boolean canCreateExpedientes(String role) {
        String normalized = normalize(role);
        return MESA_PARTES.equals(normalized);
    }

    public static boolean canCreateDocumentos(String role) { return canCreateExpedientes(role); }

    public static boolean canCreateDerivaciones(String role) {
        return false;
    }

    public static boolean canManageArchivo(String role) {
        String normalized = normalize(role);
        return ARCHIVO.equals(normalized);
    }

    public static boolean canUpdateBandeja(String role) {
        String normalized = normalize(role);
        return ESPECIALISTA.equals(normalized) || ARCHIVO.equals(normalized);
    }
}
