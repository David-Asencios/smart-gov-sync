package com.example.grupo_04_tarea_16;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.tarea16.R;
import com.example.tarea16.security.RoleManager;

import org.junit.Test;

public class RoleManagerTest {
    @Test
    public void rolAusenteODesconocidoNoObtienePermisos() {
        assertEquals(RoleManager.SIN_PERMISO, RoleManager.normalize(null));
        assertEquals(RoleManager.SIN_PERMISO, RoleManager.normalize("SUPERVISOR"));
        assertEquals(RoleManager.SIN_PERMISO, RoleManager.inferFromUsername("admin"));
        assertFalse(RoleManager.canSeeDestination(null, R.id.nav_sincronizacion));
        assertFalse(RoleManager.canCreateExpedientes("SUPERVISOR"));
    }

    @Test
    public void mesaDePartesSoloVeModulosPermitidos() {
        assertTrue(RoleManager.canSeeDestination(RoleManager.MESA_PARTES, R.id.nav_expedientes));
        assertTrue(RoleManager.canSeeDestination(RoleManager.MESA_PARTES, R.id.nav_documentos));
        assertTrue(RoleManager.canSeeDestination(RoleManager.MESA_PARTES, R.id.nav_sincronizacion));
        assertFalse(RoleManager.canSeeDestination(RoleManager.MESA_PARTES, R.id.nav_archivo));
    }

    @Test
    public void especialistaPuedeAtenderBandejaPeroNoArchivar() {
        assertTrue(RoleManager.canUpdateBandeja(RoleManager.ESPECIALISTA));
        assertTrue(RoleManager.canCreateDerivaciones(RoleManager.ESPECIALISTA));
        assertFalse(RoleManager.canManageArchivo(RoleManager.ESPECIALISTA));
    }
}
