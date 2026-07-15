package com.example.tarea16.sync;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.tarea16.db.AppDatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PushResponseProcessor {
    private static final Map<String, String> IDS = new HashMap<>();
    static {
        IDS.put("oficinas", "id_oficina");
        IDS.put("tipos_documentos", "id_tipo_documento");
        IDS.put("administrados", "id_administrado");
        IDS.put("personal_especialistas", "id_empleado");
        IDS.put("administrados_direcciones", "id_direccion");
        IDS.put("expedientes_generales", "id_expediente");
        IDS.put("documentos_ingresados", "id_documento");
        IDS.put("hojas_ruta_derivaciones", "id_derivacion");
        IDS.put("archivo_fisico_central", "id_ubicacion");
        IDS.put("actas_archivamiento", "id_acta");
    }

    private PushResponseProcessor() { }

    static void process(AppDatabase db, Map<String, Object> body) throws IOException {
        if (body == null || !(body.get("procesados") instanceof List)) {
            throw new IOException("Respuesta de sincronizacion vacia");
        }
        SupportSQLiteDatabase localDb = db.getOpenHelper().getWritableDatabase();
        localDb.beginTransaction();
        try {
            for (Object raw : (List<?>) body.get("procesados")) {
                if (!(raw instanceof Map)) continue;
                Map<?, ?> item = (Map<?, ?>) raw;
                String table = String.valueOf(item.get("tabla"));
                String idColumn = IDS.get(table);
                Map<?, ?> data = item.get("datos") instanceof Map ? (Map<?, ?>) item.get("datos") : null;
                Object remoteUuid = data == null ? item.get("remote_uuid") : data.get("remote_uuid");
                if (idColumn == null || remoteUuid == null) continue;

                String resolution = String.valueOf(item.get("resolucion"));
                ContentValues values = new ContentValues();
                if (resolution.startsWith("CONFLICTO")) {
                    values.put("sync_status", "CONFLICT");
                    values.put("sync_error", "El servidor contiene una version mas reciente");
                    values.put("sincronizado", false);
                } else {
                    values.put("sync_status", "SYNCED");
                    values.putNull("sync_error");
                    values.put("sincronizado", true);
                    if (data != null && data.get(idColumn) instanceof Number) {
                        values.put("server_id", ((Number) data.get(idColumn)).longValue());
                    }
                    if (data != null && data.get("version") instanceof Number) {
                        values.put("sync_version", ((Number) data.get("version")).longValue());
                    }
                }
                localDb.update(table, SQLiteDatabase.CONFLICT_ABORT, values,
                        "remote_uuid = ?", new Object[]{remoteUuid});
            }
            localDb.setTransactionSuccessful();
        } finally {
            localDb.endTransaction();
        }
    }
}
