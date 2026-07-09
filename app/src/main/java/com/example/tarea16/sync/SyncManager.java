package com.example.tarea16.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class SyncManager {
    private final Context context;
    private final AppDatabase db;
    private final TokenManager tokenManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<TableConfig> tables = Arrays.asList(
            new TableConfig("oficinas", "id_oficina"),
            new TableConfig("tipos_documentos", "id_tipo_documento"),
            new TableConfig("administrados", "id_administrado"),
            new TableConfig("personal_especialistas", "id_empleado"),
            new TableConfig("administrados_direcciones", "id_direccion"),
            new TableConfig("expedientes_generales", "id_expediente"),
            new TableConfig("documentos_ingresados", "id_documento"),
            new TableConfig("hojas_ruta_derivaciones", "id_derivacion"),
            new TableConfig("archivo_fisico_central", "id_ubicacion"),
            new TableConfig("actas_archivamiento", "id_acta")
    );

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
        if (!response.isSuccessful() || response.body() == null) {
            return;
        }
        Object data = response.body().get("data");
        if (!(data instanceof Map)) {
            return;
        }
        Map<?, ?> tablesData = (Map<?, ?>) data;
        androidx.sqlite.db.SupportSQLiteDatabase localDb = db.getOpenHelper().getWritableDatabase();
        localDb.beginTransaction();
        try {
            for (TableConfig table : tables) {
                Object rows = tablesData.get(table.name);
                if (rows instanceof List) {
                    for (Object row : (List<?>) rows) {
                        if (row instanceof Map) {
                            upsert(localDb, table, (Map<?, ?>) row);
                        }
                    }
                }
            }
            localDb.setTransactionSuccessful();
        } finally {
            localDb.endTransaction();
        }
        Object timestamp = response.body().get("timestamp");
        long ultimaSync = timestamp instanceof Number ? ((Number) timestamp).longValue() : System.currentTimeMillis();
        tokenManager.guardarUltimaSync(ultimaSync);
    }

    private void upsert(androidx.sqlite.db.SupportSQLiteDatabase localDb, TableConfig table, Map<?, ?> row) {
        Object id = row.get(table.id);
        if (id == null) {
            return;
        }
        ContentValues insertValues = contentValues(row, table.id, true);
        ContentValues updateValues = contentValues(row, table.id, false);
        updateValues.put("sincronizado", true);
        insertValues.put("sincronizado", true);
        boolean exists = existe(localDb, table, id);
        if (exists) {
            localDb.update(table.name, SQLiteDatabase.CONFLICT_REPLACE, updateValues, table.id + " = ?", new Object[]{id});
        } else {
            localDb.insert(table.name, SQLiteDatabase.CONFLICT_REPLACE, insertValues);
        }
    }

    private boolean existe(androidx.sqlite.db.SupportSQLiteDatabase localDb, TableConfig table, Object id) {
        Cursor cursor = localDb.query("SELECT 1 FROM " + table.name + " WHERE " + table.id + " = ? LIMIT 1", new Object[]{id});
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    private ContentValues contentValues(Map<?, ?> row, String idColumn, boolean includeId) {
        ContentValues values = new ContentValues();
        for (Map.Entry<?, ?> entry : row.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                continue;
            }
            String key = (String) entry.getKey();
            if (!includeId && key.equals(idColumn)) {
                continue;
            }
            putValue(values, key, normalizarValor(key, entry.getValue()));
        }
        return values;
    }

    private Object normalizarValor(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (key.startsWith("fecha_hora") && value instanceof String) {
            long parsed = parseFecha((String) value);
            return parsed > 0 ? parsed : value;
        }
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (Math.floor(number) == number) {
                return ((Number) value).longValue();
            }
        }
        return value;
    }

    private long parseFecha(String value) {
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = format.parse(value);
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private void putValue(ContentValues values, String key, Object value) {
        if (value == null) {
            values.putNull(key);
        } else if (value instanceof Boolean) {
            values.put(key, (Boolean) value);
        } else if (value instanceof Integer) {
            values.put(key, (Integer) value);
        } else if (value instanceof Long) {
            values.put(key, (Long) value);
        } else if (value instanceof Float) {
            values.put(key, (Float) value);
        } else if (value instanceof Double) {
            values.put(key, (Double) value);
        } else {
            values.put(key, String.valueOf(value));
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

    private static class TableConfig {
        final String name;
        final String id;

        TableConfig(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }
}
