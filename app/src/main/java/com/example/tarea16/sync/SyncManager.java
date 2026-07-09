package com.example.tarea16.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.tarea16.api.ApiClient;
import com.example.tarea16.api.SyncRecord;
import com.example.tarea16.api.SyncRequest;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.ActaArchivamiento;
import com.example.tarea16.modelo.Administrado;
import com.example.tarea16.modelo.ArchivoFisico;
import com.example.tarea16.modelo.Direccion;
import com.example.tarea16.modelo.DocumentoIngresado;
import com.example.tarea16.modelo.Expediente;
import com.example.tarea16.modelo.HojaRuta;
import com.example.tarea16.modelo.Oficina;
import com.example.tarea16.modelo.Personal;
import com.example.tarea16.modelo.TipoDocumento;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class SyncManager {
    private final Context context;
    private final AppDatabase db;
    private final TokenManager tokenManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        db = AppDatabase.getInstance(context);
        tokenManager = new TokenManager(context);
    }

    public boolean hayConexion() {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager == null ? null : manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public void sincronizar(Runnable fin) {
        executor.execute(() -> {
            if (!hayConexion() || !tokenManager.tieneToken()) {
                if (fin != null) {
                    fin.run();
                }
                return;
            }
            try {
                push();
                pull();
            } catch (Exception ignored) {
            }
            if (fin != null) {
                fin.run();
            }
        });
    }

    private void push() throws Exception {
        List<SyncRecord> registros = new ArrayList<>();
        for (Oficina item : db.oficinaDao().pendientes()) registros.add(new SyncRecord("oficinas", oficina(item)));
        for (TipoDocumento item : db.tipoDocumentoDao().pendientes()) registros.add(new SyncRecord("tipos_documentos", tipoDocumento(item)));
        for (Administrado item : db.administradoDao().pendientes()) registros.add(new SyncRecord("administrados", administrado(item)));
        for (Personal item : db.personalDao().pendientes()) registros.add(new SyncRecord("personal_especialistas", personal(item)));
        for (Direccion item : db.direccionDao().pendientes()) registros.add(new SyncRecord("administrados_direcciones", direccion(item)));
        for (Expediente item : db.expedienteDao().pendientes()) registros.add(new SyncRecord("expedientes_generales", expediente(item)));
        for (DocumentoIngresado item : db.documentoDao().pendientes()) registros.add(new SyncRecord("documentos_ingresados", documento(item)));
        for (HojaRuta item : db.hojaRutaDao().pendientes()) registros.add(new SyncRecord("hojas_ruta_derivaciones", hojaRuta(item)));
        for (ArchivoFisico item : db.archivoFisicoDao().pendientes()) registros.add(new SyncRecord("archivo_fisico_central", archivo(item)));
        for (ActaArchivamiento item : db.actaDao().pendientes()) registros.add(new SyncRecord("actas_archivamiento", acta(item)));
        if (registros.isEmpty()) {
            return;
        }
        Response<Map<String, Object>> response = ApiClient.getService().syncData("Bearer " + tokenManager.obtenerToken(), new SyncRequest(registros)).execute();
        if (response.isSuccessful()) {
            for (Oficina item : db.oficinaDao().pendientes()) db.oficinaDao().marcarSincronizado(item.idOficina);
            for (TipoDocumento item : db.tipoDocumentoDao().pendientes()) db.tipoDocumentoDao().marcarSincronizado(item.idTipoDocumento);
            for (Administrado item : db.administradoDao().pendientes()) db.administradoDao().marcarSincronizado(item.idAdministrado);
            for (Personal item : db.personalDao().pendientes()) db.personalDao().marcarSincronizado(item.idEmpleado);
            for (Direccion item : db.direccionDao().pendientes()) db.direccionDao().marcarSincronizado(item.idDireccion);
            for (Expediente item : db.expedienteDao().pendientes()) db.expedienteDao().marcarSincronizado(item.idExpediente);
            for (DocumentoIngresado item : db.documentoDao().pendientes()) db.documentoDao().marcarSincronizado(item.idDocumento);
            for (HojaRuta item : db.hojaRutaDao().pendientes()) db.hojaRutaDao().marcarSincronizado(item.idDerivacion);
            for (ArchivoFisico item : db.archivoFisicoDao().pendientes()) db.archivoFisicoDao().marcarSincronizado(item.idUbicacion);
            for (ActaArchivamiento item : db.actaDao().pendientes()) db.actaDao().marcarSincronizado(item.idActa);
        }
    }

    private void pull() throws Exception {
        Response<Map<String, Object>> response = ApiClient.getService().sincronizacion("Bearer " + tokenManager.obtenerToken(), tokenManager.obtenerUltimaSync()).execute();
        if (response.isSuccessful()) {
            tokenManager.guardarUltimaSync(System.currentTimeMillis());
        }
    }

    private Map<String, Object> base(boolean sincronizado, long updatedAt) {
        Map<String, Object> map = new HashMap<>();
        map.put("updated_at", updatedAt);
        return map;
    }

    private Map<String, Object> oficina(Oficina item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idOficina > 0) map.put("id_oficina", item.idOficina);
        map.put("codigo_oficina", item.codigoOficina);
        map.put("siglas_oficiales", item.siglasOficiales);
        map.put("nombre_unidad", item.nombreUnidad);
        return map;
    }

    private Map<String, Object> tipoDocumento(TipoDocumento item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idTipoDocumento > 0) map.put("id_tipo_documento", item.idTipoDocumento);
        map.put("nombre_tipo_documento", item.nombreTipoDocumento);
        return map;
    }

    private Map<String, Object> administrado(Administrado item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idAdministrado > 0) map.put("id_administrado", item.idAdministrado);
        map.put("codigo_administrado", item.codigoAdministrado);
        map.put("dni_ruc", item.dniRuc);
        map.put("nombre_razon_social", item.nombreRazonSocial);
        map.put("telefono", item.telefono);
        map.put("correo_notificaciones", item.correoNotificaciones);
        return map;
    }

    private Map<String, Object> personal(Personal item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idEmpleado > 0) map.put("id_empleado", item.idEmpleado);
        map.put("codigo_empleado", item.codigoEmpleado);
        map.put("nombre_completo", item.nombreCompleto);
        map.put("cargo", item.cargo);
        map.put("id_oficina", item.idOficina);
        return map;
    }

    private Map<String, Object> direccion(Direccion item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idDireccion > 0) map.put("id_direccion", item.idDireccion);
        map.put("id_administrado", item.idAdministrado);
        map.put("tipo_inmueble", item.tipoInmueble);
        map.put("calle", item.calle);
        map.put("numero", item.numero);
        map.put("comuna_distrito", item.comunaDistrito);
        map.put("ciudad", item.ciudad);
        return map;
    }

    private Map<String, Object> expediente(Expediente item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idExpediente > 0) map.put("id_expediente", item.idExpediente);
        map.put("nro_expediente_anual", item.nroExpedienteAnual);
        map.put("fecha_hora_apertura", item.fechaHoraApertura);
        map.put("asunto_general", item.asuntoGeneral);
        map.put("estado_global", item.estadoGlobal);
        return map;
    }

    private Map<String, Object> documento(DocumentoIngresado item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idDocumento > 0) map.put("id_documento", item.idDocumento);
        map.put("nro_documento_unico", item.nroDocumentoUnico);
        map.put("id_expediente", item.idExpediente);
        map.put("id_tipo_documento", item.idTipoDocumento);
        map.put("id_administrado", item.idAdministrado);
        map.put("cantidad_folios", item.cantidadFolios);
        map.put("fecha_hora_recepcion", item.fechaHoraRecepcion);
        map.put("ruta_foto", item.rutaFoto);
        return map;
    }

    private Map<String, Object> hojaRuta(HojaRuta item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idDerivacion > 0) map.put("id_derivacion", item.idDerivacion);
        map.put("codigo_barras_seguimiento", item.codigoBarrasSeguimiento);
        map.put("id_documento", item.idDocumento);
        map.put("id_empleado_asignado", item.idEmpleadoAsignado);
        map.put("id_oficina_procedencia", item.idOficinaProcedencia);
        map.put("fecha_hora_despacho", item.fechaHoraDespacho);
        map.put("prioridad_envio", item.prioridadEnvio);
        map.put("fecha_hora_recepcion", item.fechaHoraRecepcion);
        map.put("observaciones_receptor", item.observacionesReceptor);
        map.put("estado_derivacion", item.estadoDerivacion);
        map.put("latitud", item.latitud);
        map.put("longitud", item.longitud);
        return map;
    }

    private Map<String, Object> archivo(ArchivoFisico item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idUbicacion > 0) map.put("id_ubicacion", item.idUbicacion);
        map.put("codigo_almacen", item.codigoAlmacen);
        map.put("nro_pabellon", item.nroPabellon);
        map.put("nro_estante", item.nroEstante);
        map.put("nro_caja_fisica", item.nroCajaFisica);
        return map;
    }

    private Map<String, Object> acta(ActaArchivamiento item) {
        Map<String, Object> map = base(item.sincronizado, item.updatedAt);
        if (item.idActa > 0) map.put("id_acta", item.idActa);
        map.put("nro_acta_unico", item.nroActaUnico);
        map.put("id_derivacion", item.idDerivacion);
        map.put("id_ubicacion_archivo", item.idUbicacionArchivo);
        map.put("fecha_hora_guardado", item.fechaHoraGuardado);
        map.put("costo_digitalizacion", item.costoDigitalizacion);
        map.put("costo_arancel_custodia", item.costoArancelCustodia);
        map.put("costo_final_procesamiento", item.costoFinalProcesamiento);
        return map;
    }
}
