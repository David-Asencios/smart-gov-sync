package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.HojaRuta;

import java.util.List;

@Dao
public interface HojaRutaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(HojaRuta item);
    @Update
    void actualizar(HojaRuta item);
    @Query("SELECT * FROM hojas_ruta_derivaciones WHERE deleted = 0 ORDER BY id_derivacion DESC")
    List<HojaRuta> listar();
    @Query("SELECT * FROM hojas_ruta_derivaciones "
            + "WHERE deleted = 0 AND estado_derivacion = 'PENDIENTE' "
            + "AND (:admin = 1 OR id_empleado_asignado = :empleadoId OR id_oficina_procedencia = :oficinaId "
            + "OR (:archivo = 1 AND id_oficina_procedencia = :oficinaId)) "
            + "ORDER BY CASE prioridad_envio WHEN 'ALTA' THEN 0 WHEN 'NORMAL' THEN 1 ELSE 2 END, fecha_hora_despacho ASC")
    List<HojaRuta> pendientesBandeja(int empleadoId, int oficinaId, boolean admin, boolean archivo);
    @Query("SELECT * FROM hojas_ruta_derivaciones "
            + "WHERE deleted = 0 AND estado_derivacion = 'RECIBIDO' "
            + "AND (id_empleado_asignado = :empleadoId OR id_oficina_procedencia = :oficinaId) "
            + "ORDER BY fecha_hora_recepcion DESC")
    List<HojaRuta> recibidasPorEspecialista(int empleadoId, int oficinaId);
    @Query("SELECT * FROM hojas_ruta_derivaciones "
            + "WHERE deleted = 0 AND estado_derivacion IN ('ARCHIVADO', 'FINALIZADO') "
            + "AND (id_empleado_asignado = :empleadoId OR id_oficina_procedencia = :oficinaId) "
            + "ORDER BY fecha_hora_recepcion DESC")
    List<HojaRuta> finalizadasPorEspecialista(int empleadoId, int oficinaId);
    @Query("SELECT h.* FROM hojas_ruta_derivaciones h "
            + "LEFT JOIN actas_archivamiento a ON a.id_derivacion = h.id_derivacion AND a.deleted = 0 "
            + "WHERE h.deleted = 0 AND h.estado_derivacion = 'FINALIZADO' AND a.id_acta IS NULL "
            + "ORDER BY h.updated_at DESC")
    List<HojaRuta> expedientesPorArchivar();
    @Query("SELECT * FROM hojas_ruta_derivaciones WHERE sincronizado = 0")
    List<HojaRuta> pendientes();
    @Query("UPDATE hojas_ruta_derivaciones SET estado_derivacion = :estado, "
            + "fecha_hora_recepcion = :fecha, observaciones_receptor = :observacion, "
            + "updated_at = :fecha, sincronizado = 0, sync_status = 'PENDING', sync_error = NULL "
            + "WHERE id_derivacion = :id AND estado_derivacion = 'PENDIENTE'")
    int cambiarEstadoSeguro(int id, String estado, String observacion, long fecha);
    @Query("UPDATE hojas_ruta_derivaciones SET estado_derivacion = 'FINALIZADO', "
            + "observaciones_receptor = :observacion, updated_at = :fecha, sincronizado = 0, "
            + "sync_status = 'PENDING', sync_error = NULL "
            + "WHERE id_derivacion = :id AND estado_derivacion = 'RECIBIDO'")
    int finalizarAtencion(int id, String observacion, long fecha);
    @Query("UPDATE hojas_ruta_derivaciones SET estado_derivacion = 'ARCHIVADO', "
            + "observaciones_receptor = :observacion, updated_at = :fecha, sincronizado = 0, "
            + "sync_status = 'PENDING', sync_error = NULL "
            + "WHERE id_derivacion = :id AND estado_derivacion = 'FINALIZADO'")
    int marcarArchivado(int id, String observacion, long fecha);
    @Query("UPDATE hojas_ruta_derivaciones SET sincronizado = 1, sync_status = 'SYNCED', sync_error = NULL WHERE id_derivacion = :id")
    void marcarSincronizado(int id);
}
