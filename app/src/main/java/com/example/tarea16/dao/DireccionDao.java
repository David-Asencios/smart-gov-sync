package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.Direccion;

import java.util.List;

@Dao
public interface DireccionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(Direccion item);
    @Update
    void actualizar(Direccion item);
    @Query("SELECT * FROM administrados_direcciones ORDER BY id_direccion DESC")
    List<Direccion> listar();
    @Query("SELECT * FROM administrados_direcciones WHERE sincronizado = 0")
    List<Direccion> pendientes();
    @Query("UPDATE administrados_direcciones SET sincronizado = 1 WHERE id_direccion = :id")
    void marcarSincronizado(int id);
}
