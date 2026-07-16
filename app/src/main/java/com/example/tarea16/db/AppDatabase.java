package com.example.tarea16.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.tarea16.dao.*;
import com.example.tarea16.modelo.*;

@Database(entities = {Oficina.class, TipoDocumento.class, Administrado.class,
        Personal.class, Direccion.class, Expediente.class, DocumentoIngresado.class,
        HojaRuta.class, ArchivoFisico.class, ActaArchivamiento.class, Usuario.class},
        version = 5, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            migrateTable(database, "oficinas",
                    "CREATE TABLE IF NOT EXISTS oficinas (`id_oficina` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_oficina` TEXT, `siglas_oficiales` TEXT, `nombre_unidad` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_oficina, codigo_oficina, siglas_oficiales, nombre_unidad, sincronizado, updated_at",
                    "id_oficina, codigo_oficina, siglas_oficiales, nombre_unidad, sincronizado, updated_at");
            migrateTable(database, "tipos_documentos",
                    "CREATE TABLE IF NOT EXISTS tipos_documentos (`id_tipo_documento` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre_tipo_documento` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_tipo_documento, nombre_tipo_documento, sincronizado, updated_at",
                    "id_tipo_documento, nombre_tipo_documento, sincronizado, updated_at");
            migrateTable(database, "administrados",
                    "CREATE TABLE IF NOT EXISTS administrados (`id_administrado` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_administrado` TEXT, `dni_ruc` TEXT, `nombre_razon_social` TEXT, `telefono` TEXT, `correo_notificaciones` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_administrado, codigo_administrado, dni_ruc, nombre_razon_social, telefono, correo_notificaciones, sincronizado, updated_at",
                    "id_administrado, codigo_administrado, dni_ruc, nombre_razon_social, telefono, correo_notificaciones, sincronizado, updated_at");
            migrateTable(database, "personal_especialistas",
                    "CREATE TABLE IF NOT EXISTS personal_especialistas (`id_empleado` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_empleado` TEXT, `nombre_completo` TEXT, `cargo` TEXT, `id_oficina` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_empleado, codigo_empleado, nombre_completo, cargo, id_oficina, sincronizado, updated_at",
                    "id_empleado, codigo_empleado, nombre_completo, cargo, id_oficina, sincronizado, updated_at");
            migrateTable(database, "administrados_direcciones",
                    "CREATE TABLE IF NOT EXISTS administrados_direcciones (`id_direccion` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_administrado` INTEGER NOT NULL, `tipo_inmueble` TEXT, `calle` TEXT, `numero` TEXT, `comuna_distrito` TEXT, `ciudad` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_direccion, id_administrado, tipo_inmueble, calle, numero, comuna_distrito, ciudad, sincronizado, updated_at",
                    "id_direccion, id_administrado, tipo_inmueble, calle, numero, comuna_distrito, ciudad, sincronizado, updated_at");
            migrateTable(database, "expedientes_generales",
                    "CREATE TABLE IF NOT EXISTS expedientes_generales (`id_expediente` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nro_expediente_anual` TEXT, `fecha_hora_apertura` INTEGER NOT NULL, `asunto_general` TEXT, `estado_global` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_expediente, nro_expediente_anual, fecha_hora_apertura, asunto_general, estado_global, sincronizado, updated_at",
                    "id_expediente, nro_expediente_anual, fecha_hora_apertura, asunto_general, estado_global, sincronizado, updated_at");
            migrateTable(database, "documentos_ingresados",
                    "CREATE TABLE IF NOT EXISTS documentos_ingresados (`id_documento` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nro_documento_unico` TEXT, `id_expediente` INTEGER NOT NULL, `id_tipo_documento` INTEGER NOT NULL, `id_administrado` INTEGER NOT NULL, `cantidad_folios` INTEGER NOT NULL, `fecha_hora_recepcion` INTEGER NOT NULL, `ruta_foto` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_documento, nro_documento_unico, id_expediente, id_tipo_documento, id_administrado, cantidad_folios, fecha_hora_recepcion, ruta_foto, sincronizado, updated_at",
                    "id_documento, nro_documento_unico, id_expediente, id_tipo_documento, id_administrado, cantidad_folios, fecha_hora_recepcion, ruta_foto, sincronizado, updated_at");
            migrateTable(database, "hojas_ruta_derivaciones",
                    "CREATE TABLE IF NOT EXISTS hojas_ruta_derivaciones (`id_derivacion` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_barras_seguimiento` TEXT, `id_documento` INTEGER NOT NULL, `id_empleado_asignado` INTEGER NOT NULL, `id_oficina_procedencia` INTEGER NOT NULL, `fecha_hora_despacho` INTEGER NOT NULL, `prioridad_envio` TEXT, `fecha_hora_recepcion` INTEGER NOT NULL, `observaciones_receptor` TEXT, `estado_derivacion` TEXT, `latitud` REAL NOT NULL, `longitud` REAL NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_derivacion, codigo_barras_seguimiento, id_documento, id_empleado_asignado, id_oficina_procedencia, fecha_hora_despacho, prioridad_envio, fecha_hora_recepcion, observaciones_receptor, estado_derivacion, latitud, longitud, sincronizado, updated_at",
                    "id_derivacion, codigo_barras_seguimiento, id_documento, id_empleado_asignado, id_oficina_procedencia, fecha_hora_despacho, prioridad_envio, fecha_hora_recepcion, observaciones_receptor, estado_derivacion, latitud, longitud, sincronizado, updated_at");
            migrateTable(database, "archivo_fisico_central",
                    "CREATE TABLE IF NOT EXISTS archivo_fisico_central (`id_ubicacion` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_almacen` TEXT, `nro_pabellon` INTEGER NOT NULL, `nro_estante` INTEGER NOT NULL, `nro_caja_fisica` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_ubicacion, codigo_almacen, nro_pabellon, nro_estante, nro_caja_fisica, sincronizado, updated_at",
                    "id_ubicacion, codigo_almacen, nro_pabellon, nro_estante, nro_caja_fisica, sincronizado, updated_at");
            migrateTable(database, "actas_archivamiento",
                    "CREATE TABLE IF NOT EXISTS actas_archivamiento (`id_acta` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nro_acta_unico` TEXT, `id_derivacion` INTEGER NOT NULL, `id_ubicacion_archivo` INTEGER NOT NULL, `fecha_hora_guardado` INTEGER NOT NULL, `costo_digitalizacion` REAL NOT NULL, `costo_arancel_custodia` REAL NOT NULL, `costo_final_procesamiento` REAL NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_acta, nro_acta_unico, id_derivacion, id_ubicacion_archivo, fecha_hora_guardado, costo_digitalizacion, costo_arancel_custodia, costo_final_procesamiento, sincronizado, updated_at",
                    "id_acta, nro_acta_unico, id_derivacion, id_ubicacion_archivo, fecha_hora_guardado, costo_digitalizacion, costo_arancel_custodia, costo_final_procesamiento, sincronizado, updated_at");
            migrateTable(database, "usuario",
                    "CREATE TABLE IF NOT EXISTS usuario (`id_usuario` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT, `password_hash` TEXT, `id_empleado` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_usuario, username, password_hash, id_empleado, sincronizado, updated_at",
                    "id_usuario, username, password_hash, id_empleado, sincronizado, updated_at");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            rebuildTable(database, "oficinas",
                    "CREATE TABLE IF NOT EXISTS oficinas (`id_oficina` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_oficina` TEXT, `siglas_oficiales` TEXT, `nombre_unidad` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_oficina, codigo_oficina, siglas_oficiales, nombre_unidad, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "tipos_documentos",
                    "CREATE TABLE IF NOT EXISTS tipos_documentos (`id_tipo_documento` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre_tipo_documento` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_tipo_documento, nombre_tipo_documento, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "administrados",
                    "CREATE TABLE IF NOT EXISTS administrados (`id_administrado` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_administrado` TEXT, `dni_ruc` TEXT, `nombre_razon_social` TEXT, `telefono` TEXT, `correo_notificaciones` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_administrado, codigo_administrado, dni_ruc, nombre_razon_social, telefono, correo_notificaciones, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "personal_especialistas",
                    "CREATE TABLE IF NOT EXISTS personal_especialistas (`id_empleado` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_empleado` TEXT, `nombre_completo` TEXT, `cargo` TEXT, `id_oficina` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_empleado, codigo_empleado, nombre_completo, cargo, id_oficina, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "administrados_direcciones",
                    "CREATE TABLE IF NOT EXISTS administrados_direcciones (`id_direccion` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_administrado` INTEGER NOT NULL, `tipo_inmueble` TEXT, `calle` TEXT, `numero` TEXT, `comuna_distrito` TEXT, `ciudad` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_direccion, id_administrado, tipo_inmueble, calle, numero, comuna_distrito, ciudad, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "expedientes_generales",
                    "CREATE TABLE IF NOT EXISTS expedientes_generales (`id_expediente` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nro_expediente_anual` TEXT, `fecha_hora_apertura` INTEGER NOT NULL, `asunto_general` TEXT, `estado_global` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_expediente, nro_expediente_anual, fecha_hora_apertura, asunto_general, estado_global, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "documentos_ingresados",
                    "CREATE TABLE IF NOT EXISTS documentos_ingresados (`id_documento` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nro_documento_unico` TEXT, `id_expediente` INTEGER NOT NULL, `id_tipo_documento` INTEGER NOT NULL, `id_administrado` INTEGER NOT NULL, `cantidad_folios` INTEGER NOT NULL, `fecha_hora_recepcion` INTEGER NOT NULL, `ruta_foto` TEXT, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_documento, nro_documento_unico, id_expediente, id_tipo_documento, id_administrado, cantidad_folios, fecha_hora_recepcion, ruta_foto, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "hojas_ruta_derivaciones",
                    "CREATE TABLE IF NOT EXISTS hojas_ruta_derivaciones (`id_derivacion` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_barras_seguimiento` TEXT, `id_documento` INTEGER NOT NULL, `id_empleado_asignado` INTEGER NOT NULL, `id_oficina_procedencia` INTEGER NOT NULL, `fecha_hora_despacho` INTEGER NOT NULL, `prioridad_envio` TEXT, `fecha_hora_recepcion` INTEGER NOT NULL, `observaciones_receptor` TEXT, `estado_derivacion` TEXT, `latitud` REAL NOT NULL, `longitud` REAL NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_derivacion, codigo_barras_seguimiento, id_documento, id_empleado_asignado, id_oficina_procedencia, fecha_hora_despacho, prioridad_envio, fecha_hora_recepcion, observaciones_receptor, estado_derivacion, latitud, longitud, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "archivo_fisico_central",
                    "CREATE TABLE IF NOT EXISTS archivo_fisico_central (`id_ubicacion` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo_almacen` TEXT, `nro_pabellon` INTEGER NOT NULL, `nro_estante` INTEGER NOT NULL, `nro_caja_fisica` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_ubicacion, codigo_almacen, nro_pabellon, nro_estante, nro_caja_fisica, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "actas_archivamiento",
                    "CREATE TABLE IF NOT EXISTS actas_archivamiento (`id_acta` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nro_acta_unico` TEXT, `id_derivacion` INTEGER NOT NULL, `id_ubicacion_archivo` INTEGER NOT NULL, `fecha_hora_guardado` INTEGER NOT NULL, `costo_digitalizacion` REAL NOT NULL, `costo_arancel_custodia` REAL NOT NULL, `costo_final_procesamiento` REAL NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_acta, nro_acta_unico, id_derivacion, id_ubicacion_archivo, fecha_hora_guardado, costo_digitalizacion, costo_arancel_custodia, costo_final_procesamiento, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
            rebuildTable(database, "usuario",
                    "CREATE TABLE IF NOT EXISTS usuario (`id_usuario` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT, `password_hash` TEXT, `id_empleado` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `remote_uuid` TEXT, `server_id` INTEGER, `sync_status` TEXT, `sync_error` TEXT, `deleted` INTEGER NOT NULL, `sync_version` INTEGER NOT NULL)",
                    "id_usuario, username, password_hash, id_empleado, sincronizado, updated_at, remote_uuid, server_id, sync_status, sync_error, deleted, sync_version");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE usuario ADD COLUMN rol TEXT DEFAULT 'MESA_PARTES'");
            database.execSQL("ALTER TABLE usuario ADD COLUMN activo INTEGER NOT NULL DEFAULT 1");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE expedientes_generales ADD COLUMN id_usuario_registro INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static void migrateTable(SupportSQLiteDatabase database, String table, String createSql,
                                     String targetColumns, String sourceColumns) {
        String oldTable = table + "_old";
        String uuidSql = "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || "
                + "substr(hex(randomblob(2)), 2) || '-8' || substr(hex(randomblob(2)), 2) || '-' "
                + "|| hex(randomblob(6)))";
        database.execSQL("ALTER TABLE " + table + " RENAME TO " + oldTable);
        database.execSQL(createSql);
        database.execSQL("INSERT INTO " + table + " (" + targetColumns
                + ", remote_uuid, server_id, sync_status, sync_error, deleted, sync_version) SELECT "
                + sourceColumns + ", " + uuidSql + ", NULL, 'PENDING', NULL, 0, 0 FROM " + oldTable);
        database.execSQL("DROP TABLE " + oldTable);
    }

    private static void rebuildTable(SupportSQLiteDatabase database, String table, String createSql,
                                     String columns) {
        String oldTable = table + "_old_v2";
        database.execSQL("ALTER TABLE " + table + " RENAME TO " + oldTable);
        database.execSQL(createSql);
        database.execSQL("INSERT INTO " + table + " (" + columns + ") SELECT " + columns + " FROM " + oldTable);
        database.execSQL("DROP TABLE " + oldTable);
    }

    public abstract OficinaDao oficinaDao();
    public abstract TipoDocumentoDao tipoDocumentoDao();
    public abstract AdministradoDao administradoDao();
    public abstract PersonalDao personalDao();
    public abstract DireccionDao direccionDao();
    public abstract ExpedienteDao expedienteDao();
    public abstract DocumentoDao documentoDao();
    public abstract HojaRutaDao hojaRutaDao();
    public abstract ArchivoFisicoDao archivoFisicoDao();
    public abstract ActaDao actaDao();
    public abstract UsuarioDao usuarioDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "smart_gov_sync.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .build();
                }
            }
        }
        return instance;
    }
}
