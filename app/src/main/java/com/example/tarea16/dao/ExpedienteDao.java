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
    @Query("SELECT * FROM expedientes_generales WHERE sincronizado = 0")
    List<Expediente> pendientes();
    @Query("UPDATE expedientes_generales SET sincronizado = 1 WHERE id_expediente = :id")
    void marcarSincronizado(int id);
}
