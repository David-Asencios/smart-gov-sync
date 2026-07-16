package com.example.tarea16.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.FragmentInicioBinding;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.ActaArchivamiento;
import com.example.tarea16.modelo.Expediente;
import com.example.tarea16.modelo.HojaRuta;
import com.example.tarea16.security.RoleManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentInicio extends Fragment {
    private FragmentInicioBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentInicioBinding.inflate(inflater, container, false);
        cargar();
        return binding.getRoot();
    }

    private void cargar() {
        Context context = requireContext().getApplicationContext();
        TokenManager session = new TokenManager(requireContext());
        String role = session.obtenerRol();
        int usuarioSesionId = session.obtenerIdUsuario();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<Expediente> expedientes = db.expedienteDao().listar();
            List<HojaRuta> derivaciones = db.hojaRutaDao().listar();
            List<ActaArchivamiento> actas = db.actaDao().listar();
            String titulo = "Inicio";
            String subtitulo = RoleManager.displayName(role);
            String resumen;
            String acciones;
            switch (RoleManager.normalize(role)) {
                case RoleManager.ADMIN:
                    resumen = "Usuarios registrados: " + db.usuarioDao().listar().size()
                            + "\nOficinas activas: " + contarActivos(db.oficinaDao().listar())
                            + "\nTipos de documentos activos: " + contarActivos(db.tipoDocumentoDao().listar())
                            + "\nExpedientes registrados: " + expedientes.size()
                            + "\nExpedientes en proceso: " + contarExpedientes(expedientes, "EN_PROCESO")
                            + "\nExpedientes finalizados: " + contarExpedientes(expedientes, "CERRADO")
                            + "\nExpedientes archivados: " + contarExpedientes(expedientes, "ARCHIVADO");
                    acciones = "Panel informativo. El administrador configura usuarios, oficinas y tipos de documentos; no registra documentos, no deriva expedientes, no cambia estados y no archiva.";
                    break;
                case RoleManager.MESA_PARTES:
                    List<Expediente> expedientesMesa = usuarioSesionId > 0
                            ? db.expedienteDao().listarPorUsuario(usuarioSesionId)
                            : expedientes;
                    resumen = "Expedientes registrados hoy: " + contarHoyExpedientes(expedientesMesa)
                            + "\nExpedientes pendientes de sincronizacion: " + contarPendientes(expedientesMesa)
                            + "\nDocumentos ingresados: " + db.documentoDao().listar().size()
                            + "\nAdministrados registrados: " + db.administradoDao().listar().size()
                            + "\nDerivaciones iniciales pendientes: " + contarDerivaciones(derivaciones, "PENDIENTE")
                            + ultimosExpedientes(expedientesMesa);
                    acciones = "Accesos frecuentes: Administrados, Registrar Expediente y Expedientes Registrados.";
                    break;
                case RoleManager.ESPECIALISTA:
                    int empleadoId = session.obtenerIdEmpleado();
                    int oficinaId = session.obtenerIdOficina();
                    List<HojaRuta> pendientesEspecialista = db.hojaRutaDao()
                            .pendientesBandeja(empleadoId, oficinaId, false, false);
                    List<HojaRuta> recibidasEspecialista = db.hojaRutaDao()
                            .recibidasPorEspecialista(empleadoId, oficinaId);
                    List<HojaRuta> finalizadasEspecialista = db.hojaRutaDao()
                            .finalizadasPorEspecialista(empleadoId, oficinaId);
                    resumen = "Expedientes pendientes de recepcion: " + pendientesEspecialista.size()
                            + "\nExpedientes en proceso: " + recibidasEspecialista.size()
                            + "\nFinalizados hoy: " + contarHoyDerivaciones(finalizadasEspecialista)
                            + "\nPrioridad alta pendiente: " + contarPrioridad(pendientesEspecialista, "ALTA");
                    acciones = "Accesos: Bandeja de entrada para aceptar o rechazar, y Mis expedientes para continuar la atencion.";
                    break;
                case RoleManager.ARCHIVO:
                    resumen = "Expedientes por archivar: " + db.hojaRutaDao().expedientesPorArchivar().size()
                            + "\nArchivados hoy: " + contarHoyActas(actas)
                            + "\nExpedientes almacenados: " + actas.size()
                            + "\nUbicaciones fisicas registradas: " + db.archivoFisicoDao().listar().size()
                            + "\nHistorial de archivamiento: " + actas.size();
                    acciones = "Accesos: Expedientes por archivar, archivo fisico e historial.";
                    break;
                default:
                    resumen = "No hay informacion disponible para este rol.";
                    acciones = "Inicia sesion con un rol valido.";
                    break;
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.txtTitulo.setText(titulo);
                        binding.txtSubtitulo.setText(subtitulo);
                        binding.txtResumen.setText(resumen);
                        binding.txtAcciones.setText(acciones);
                    }
                });
            }
        });
    }

    private int contarExpedientes(List<Expediente> items, String estado) {
        int total = 0;
        for (Expediente item : items) {
            if (estado.equalsIgnoreCase(String.valueOf(item.estadoGlobal))) total++;
        }
        return total;
    }

    private int contarDerivaciones(List<HojaRuta> items, String estado) {
        int total = 0;
        for (HojaRuta item : items) {
            if (estado.equalsIgnoreCase(String.valueOf(item.estadoDerivacion))) total++;
        }
        return total;
    }

    private int contarActivos(List<? extends com.example.tarea16.modelo.SyncEntity> items) {
        int total = 0;
        for (com.example.tarea16.modelo.SyncEntity item : items) {
            if (!item.deleted) total++;
        }
        return total;
    }

    private int contarHoyExpedientes(List<Expediente> items) {
        long inicio = inicioHoy();
        int total = 0;
        for (Expediente item : items) {
            if (item.fechaHoraApertura >= inicio) total++;
        }
        return total;
    }

    private int contarPendientes(List<Expediente> items) {
        int total = 0;
        for (Expediente item : items) {
            if (!item.sincronizado) total++;
        }
        return total;
    }

    private String ultimosExpedientes(List<Expediente> items) {
        if (items.isEmpty()) return "\nUltimos registros: sin expedientes registrados";
        StringBuilder builder = new StringBuilder("\nUltimos registros:");
        int limit = Math.min(3, items.size());
        for (int i = 0; i < limit; i++) {
            Expediente item = items.get(i);
            builder.append("\n- ").append(item.nroExpedienteAnual)
                    .append(" / ").append(item.estadoGlobal);
        }
        return builder.toString();
    }

    private int contarHoyActas(List<ActaArchivamiento> items) {
        long inicio = inicioHoy();
        int total = 0;
        for (ActaArchivamiento item : items) {
            if (item.fechaHoraGuardado >= inicio) total++;
        }
        return total;
    }

    private int contarHoyDerivaciones(List<HojaRuta> items) {
        long inicio = inicioHoy();
        int total = 0;
        for (HojaRuta item : items) {
            if (item.updatedAt >= inicio) total++;
        }
        return total;
    }

    private int contarPrioridad(List<HojaRuta> items, String prioridad) {
        int total = 0;
        for (HojaRuta item : items) {
            if (prioridad.equalsIgnoreCase(String.valueOf(item.prioridadEnvio))) total++;
        }
        return total;
    }

    private long inicioHoy() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
