package com.example.tarea16.sync;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class PullRelationResolver {
    private static final Map<String, Map<String, String[]>> RELATIONS = new HashMap<>();
    static {
        add("personal_especialistas", "id_oficina", "oficinas", "id_oficina");
        add("administrados_direcciones", "id_administrado", "administrados", "id_administrado");
        add("documentos_ingresados", "id_expediente", "expedientes_generales", "id_expediente");
        add("documentos_ingresados", "id_tipo_documento", "tipos_documentos", "id_tipo_documento");
        add("documentos_ingresados", "id_administrado", "administrados", "id_administrado");
        add("hojas_ruta_derivaciones", "id_documento", "documentos_ingresados", "id_documento");
        add("hojas_ruta_derivaciones", "id_empleado_asignado", "personal_especialistas", "id_empleado");
        add("hojas_ruta_derivaciones", "id_oficina_procedencia", "oficinas", "id_oficina");
        add("actas_archivamiento", "id_derivacion", "hojas_ruta_derivaciones", "id_derivacion");
        add("actas_archivamiento", "id_ubicacion_archivo", "archivo_fisico_central", "id_ubicacion");
    }

    private PullRelationResolver() { }

    private static void add(String table, String field, String targetTable, String targetId) {
        RELATIONS.computeIfAbsent(table, ignored -> new LinkedHashMap<>())
                .put(field, new String[]{targetTable, targetId});
    }

    static boolean resolve(SupportSQLiteDatabase db, String table, ContentValues values) {
        Map<String, String[]> relations = RELATIONS.get(table);
        if (relations == null) return true;
        for (Map.Entry<String, String[]> relation : relations.entrySet()) {
            Long serverId = values.getAsLong(relation.getKey());
            if (serverId == null) continue;
            String[] target = relation.getValue();
            Cursor cursor = db.query("SELECT " + target[1] + " FROM " + target[0]
                    + " WHERE server_id = ? LIMIT 1", new Object[]{serverId});
            try {
                if (!cursor.moveToFirst()) return false;
                values.put(relation.getKey(), cursor.getLong(0));
            } finally {
                cursor.close();
            }
        }
        return true;
    }
}
