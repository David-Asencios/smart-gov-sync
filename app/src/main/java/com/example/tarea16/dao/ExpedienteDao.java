package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.Expediente;

import java.util.List;

@Dao
public interface ExpedienteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(Expediente item);
    @Update
    void actualizar(Expediente item);
    @Query("SELECT * FROM expedientes_generales ORDER BY id_expediente DESC")
    List<Expediente> listar();
    @Query("SELECT * FROM expedientes_generales WHERE id_usuario_registro = :usuarioId ORDER BY id_expediente DESC")
    List<Expediente> listarPorUsuario(int usuarioId);
    @Query("SELECT * FROM expedientes_generales WHERE sincronizado = 0")
    List<Expediente> pendientes();
    @Query("SELECT COUNT(*) FROM expedientes_generales WHERE id_expediente = :id AND deleted = 0")
    int existe(int id);
    @Query("UPDATE expedientes_generales SET estado_global = 'ARCHIVADO', sincronizado = 0, "
            + "sync_status = 'PENDING', sync_error = NULL, updated_at = :fecha "
            + "WHERE id_expediente = (SELECT id_expediente FROM documentos_ingresados WHERE id_documento = :idDocumento)")
    void marcarArchivadoPorDocumento(int idDocumento, long fecha);
    @Query("UPDATE expedientes_generales SET sincronizado = 1 WHERE id_expediente = :id")
    void marcarSincronizado(int id);
}
