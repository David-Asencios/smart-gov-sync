package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.Oficina;

import java.util.List;

@Dao
public interface OficinaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(Oficina item);
    @Update
    void actualizar(Oficina item);
    @Query("SELECT * FROM oficinas ORDER BY id_oficina DESC")
    List<Oficina> listar();
    @Query("SELECT * FROM oficinas WHERE sincronizado = 0")
    List<Oficina> pendientes();
    @Query("UPDATE oficinas SET sincronizado = 1 WHERE id_oficina = :id")
    void marcarSincronizado(int id);
}
