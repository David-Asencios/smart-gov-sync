package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.ActaArchivamiento;
import com.example.tarea16.modelo.ArchivoResumen;

import java.util.List;

@Dao
public interface ActaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(ActaArchivamiento item);
    @Update
    void actualizar(ActaArchivamiento item);
    @Query("SELECT * FROM actas_archivamiento ORDER BY id_acta DESC")
    List<ActaArchivamiento> listar();
    @Query("SELECT a.id_acta, a.nro_acta_unico, a.fecha_hora_guardado, a.id_derivacion, "
            + "e.nro_expediente_anual, e.asunto_general, u.codigo_almacen, u.nro_pabellon, "
            + "u.nro_estante, u.nro_caja_fisica, a.sincronizado, a.sync_status, "
            + "a.costo_digitalizacion, a.costo_arancel_custodia, a.costo_final_procesamiento "
            + "FROM actas_archivamiento a "
            + "INNER JOIN archivo_fisico_central u ON u.id_ubicacion = a.id_ubicacion_archivo "
            + "INNER JOIN hojas_ruta_derivaciones h ON h.id_derivacion = a.id_derivacion "
            + "INNER JOIN documentos_ingresados d ON d.id_documento = h.id_documento "
            + "INNER JOIN expedientes_generales e ON e.id_expediente = d.id_expediente "
            + "WHERE a.deleted = 0 "
            + "ORDER BY a.fecha_hora_guardado DESC")
    List<ArchivoResumen> listarResumen();
    @Query("SELECT * FROM actas_archivamiento WHERE sincronizado = 0")
    List<ActaArchivamiento> pendientes();
    @Query("UPDATE actas_archivamiento SET sincronizado = 1 WHERE id_acta = :id")
    void marcarSincronizado(int id);
}
