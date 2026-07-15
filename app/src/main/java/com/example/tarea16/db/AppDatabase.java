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
        version = 2, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;
    private static final String[] TABLES = {
            "oficinas", "tipos_documentos", "administrados", "personal_especialistas",
            "administrados_direcciones", "expedientes_generales", "documentos_ingresados",
            "hojas_ruta_derivaciones", "archivo_fisico_central", "actas_archivamiento", "usuario"
    };

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            for (String table : TABLES) {
                database.execSQL("ALTER TABLE " + table + " ADD COLUMN remote_uuid TEXT");
                database.execSQL("ALTER TABLE " + table + " ADD COLUMN server_id INTEGER");
                database.execSQL("ALTER TABLE " + table + " ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING'");
                database.execSQL("ALTER TABLE " + table + " ADD COLUMN sync_error TEXT");
                database.execSQL("ALTER TABLE " + table + " ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0");
                database.execSQL("ALTER TABLE " + table + " ADD COLUMN sync_version INTEGER NOT NULL DEFAULT 0");
                database.execSQL("UPDATE " + table + " SET remote_uuid = lower(hex(randomblob(4)) || '-' || "
                        + "hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-8' || "
                        + "substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6))) "
                        + "WHERE remote_uuid IS NULL");
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_" + table
                        + "_remote_uuid ON " + table + "(remote_uuid)");
            }
        }
    };

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
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
