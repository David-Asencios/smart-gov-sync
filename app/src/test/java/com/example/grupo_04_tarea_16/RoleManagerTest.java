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
        assertFalse(RoleManager.canSeeDestination(null, R.id.nav_inicio));
        assertFalse(RoleManager.canCreateExpedientes("SUPERVISOR"));
    }

    @Test
    public void mesaDePartesSoloVeModulosPermitidos() {
        assertTrue(RoleManager.canSeeDestination(RoleManager.MESA_PARTES, R.id.nav_inicio));
        assertTrue(RoleManager.canSeeDestination(RoleManager.MESA_PARTES, R.id.nav_mesa_administrados));
        assertTrue(RoleManager.canSeeDestination(RoleManager.MESA_PARTES, R.id.nav_mesa_registrar_expediente));
        assertTrue(RoleManager.canSeeDestination(RoleManager.MESA_PARTES, R.id.nav_mesa_expedientes_registrados));
        assertFalse(RoleManager.canSeeDestination(RoleManager.MESA_PARTES, R.id.nav_archivo_fisico));
    }

    @Test
    public void especialistaPuedeAtenderBandejaPeroNoArchivar() {
        assertTrue(RoleManager.canUpdateBandeja(RoleManager.ESPECIALISTA));
        assertTrue(RoleManager.canCreateDerivaciones(RoleManager.ESPECIALISTA));
        assertFalse(RoleManager.canManageArchivo(RoleManager.ESPECIALISTA));
    }

    @Test
    public void administradorSoloConfiguraYConsulta() {
        assertTrue(RoleManager.canSeeDestination(RoleManager.ADMIN, R.id.nav_admin_usuarios));
        assertTrue(RoleManager.canSeeDestination(RoleManager.ADMIN, R.id.nav_admin_consultar_expedientes));
        assertFalse(RoleManager.canCreateExpedientes(RoleManager.ADMIN));
        assertFalse(RoleManager.canCreateDocumentos(RoleManager.ADMIN));
        assertFalse(RoleManager.canCreateDerivaciones(RoleManager.ADMIN));
        assertFalse(RoleManager.canManageArchivo(RoleManager.ADMIN));
    }
}
