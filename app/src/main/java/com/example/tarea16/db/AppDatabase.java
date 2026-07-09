package com.example.tarea16.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.tarea16.dao.ActaDao;
import com.example.tarea16.dao.AdministradoDao;
import com.example.tarea16.dao.ArchivoFisicoDao;
import com.example.tarea16.dao.DireccionDao;
import com.example.tarea16.dao.DocumentoDao;
import com.example.tarea16.dao.ExpedienteDao;
import com.example.tarea16.dao.HojaRutaDao;
import com.example.tarea16.dao.OficinaDao;
import com.example.tarea16.dao.PersonalDao;
import com.example.tarea16.dao.TipoDocumentoDao;
import com.example.tarea16.dao.UsuarioDao;
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
import com.example.tarea16.modelo.Usuario;

@Database(entities = {Oficina.class, TipoDocumento.class, Administrado.class, Personal.class, Direccion.class, Expediente.class, DocumentoIngresado.class, HojaRuta.class, ArchivoFisico.class, ActaArchivamiento.class, Usuario.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

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
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "smart_gov_sync.db").build();
                }
            }
        }
        return instance;
    }
}
