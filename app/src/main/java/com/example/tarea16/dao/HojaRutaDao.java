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
    @Query("SELECT * FROM hojas_ruta_derivaciones ORDER BY id_derivacion DESC")
    List<HojaRuta> listar();
    @Query("SELECT * FROM hojas_ruta_derivaciones WHERE estado_derivacion = 'PENDIENTE' ORDER BY id_derivacion DESC")
    List<HojaRuta> pendientesBandeja();
    @Query("SELECT * FROM hojas_ruta_derivaciones WHERE sincronizado = 0")
    List<HojaRuta> pendientes();
    @Query("UPDATE hojas_ruta_derivaciones SET estado_derivacion = :estado, fecha_hora_recepcion = :fecha, updated_at = :fecha, sincronizado = 0 WHERE id_derivacion = :id")
    void cambiarEstado(int id, String estado, long fecha);
    @Query("UPDATE hojas_ruta_derivaciones SET sincronizado = 1 WHERE id_derivacion = :id")
    void marcarSincronizado(int id);
}
