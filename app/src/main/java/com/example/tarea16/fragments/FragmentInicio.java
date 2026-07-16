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
            DashboardData data;
            switch (RoleManager.normalize(role)) {
                case RoleManager.ADMIN:
                    data = new DashboardData(
                            metric("Usuarios", db.usuarioDao().listar().size()),
                            metric("Oficinas activas", contarActivos(db.oficinaDao().listar())),
                            metric("Tipos doc.", contarActivos(db.tipoDocumentoDao().listar())),
                            metric("Expedientes", expedientes.size()),
                            "Estado general",
                            "En proceso: " + contarExpedientes(expedientes, "EN_PROCESO")
                                    + "\nFinalizados: " + contarExpedientes(expedientes, "CERRADO")
                                    + "\nArchivados: " + contarExpedientes(expedientes, "ARCHIVADO"));
                    break;
                case RoleManager.MESA_PARTES:
                    List<Expediente> expedientesMesa = usuarioSesionId > 0
                            ? db.expedienteDao().listarPorUsuario(usuarioSesionId)
                            : expedientes;
                    data = new DashboardData(
                            metric("Registrados hoy", contarHoyExpedientes(expedientesMesa)),
                            metric("Pend. sync", contarPendientes(expedientesMesa)),
                            metric("Documentos", db.documentoDao().listar().size()),
                            metric("Administrados", db.administradoDao().listar().size()),
                            "Ultimos registros",
                            ultimosExpedientes(expedientesMesa));
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
                    data = new DashboardData(
                            metric("Por recibir", pendientesEspecialista.size()),
                            metric("En proceso", recibidasEspecialista.size()),
                            metric("Finalizados hoy", contarHoyDerivaciones(finalizadasEspecialista)),
                            metric("Prioridad alta", contarPrioridad(pendientesEspecialista, "ALTA")),
                            "Carga actual",
                            detalleDerivaciones(pendientesEspecialista));
                    break;
                case RoleManager.ARCHIVO:
                    data = new DashboardData(
                            metric("Por archivar", db.hojaRutaDao().expedientesPorArchivar().size()),
                            metric("Archivados hoy", contarHoyActas(actas)),
                            metric("Almacenados", actas.size()),
                            metric("Ubicaciones", db.archivoFisicoDao().listar().size()),
                            "Movimiento de archivo",
                            "Actas registradas: " + actas.size()
                                    + "\nPendientes de sincronizacion: " + contarPendientesActas(actas));
                    break;
                default:
                    data = new DashboardData(
                            metric("0", 0), metric("0", 0), metric("0", 0), metric("0", 0),
                            "Sesion", "Inicia sesion con un rol valido.");
                    break;
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.txtTitulo.setText(titulo);
                        binding.txtSubtitulo.setText(subtitulo);
                        binding.txtMetric1Value.setText(data.metric1.value);
                        binding.txtMetric1Label.setText(data.metric1.label);
                        binding.txtMetric2Value.setText(data.metric2.value);
                        binding.txtMetric2Label.setText(data.metric2.label);
                        binding.txtMetric3Value.setText(data.metric3.value);
                        binding.txtMetric3Label.setText(data.metric3.label);
                        binding.txtMetric4Value.setText(data.metric4.value);
                        binding.txtMetric4Label.setText(data.metric4.label);
                        binding.txtSectionTitle.setText(data.sectionTitle);
                        binding.txtDetalle.setText(data.detail);
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
        if (items.isEmpty()) return "Sin expedientes registrados";
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(3, items.size());
        for (int i = 0; i < limit; i++) {
            Expediente item = items.get(i);
            if (i > 0) builder.append("\n");
            builder.append(item.nroExpedienteAnual)
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

    private int contarPendientesActas(List<ActaArchivamiento> items) {
        int total = 0;
        for (ActaArchivamiento item : items) {
            if (!item.sincronizado) total++;
        }
        return total;
    }

    private String detalleDerivaciones(List<HojaRuta> items) {
        if (items.isEmpty()) return "Sin derivaciones pendientes";
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(3, items.size());
        for (int i = 0; i < limit; i++) {
            HojaRuta item = items.get(i);
            if (i > 0) builder.append("\n");
            builder.append(item.codigoBarrasSeguimiento)
                    .append(" / ").append(item.prioridadEnvio);
        }
        return builder.toString();
    }

    private Metric metric(String label, int value) {
        return new Metric(label, String.valueOf(value));
    }

    private static class Metric {
        final String label;
        final String value;

        Metric(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private static class DashboardData {
        final Metric metric1;
        final Metric metric2;
        final Metric metric3;
        final Metric metric4;
        final String sectionTitle;
        final String detail;

        DashboardData(Metric metric1, Metric metric2, Metric metric3, Metric metric4,
                      String sectionTitle, String detail) {
            this.metric1 = metric1;
            this.metric2 = metric2;
            this.metric3 = metric3;
            this.metric4 = metric4;
            this.sectionTitle = sectionTitle;
            this.detail = detail;
        }
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
