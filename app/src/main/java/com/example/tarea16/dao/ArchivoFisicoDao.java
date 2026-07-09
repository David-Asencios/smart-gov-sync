package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.ArchivoFisico;

import java.util.List;

@Dao
public interface ArchivoFisicoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(ArchivoFisico item);
    @Update
    void actualizar(ArchivoFisico item);
    @Query("SELECT * FROM archivo_fisico_central ORDER BY id_ubicacion DESC")
    List<ArchivoFisico> listar();
    @Query("SELECT * FROM archivo_fisico_central WHERE sincronizado = 0")
    List<ArchivoFisico> pendientes();
    @Query("UPDATE archivo_fisico_central SET sincronizado = 1 WHERE id_ubicacion = :id")
    void marcarSincronizado(int id);
}
