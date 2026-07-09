package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.Administrado;

import java.util.List;

@Dao
public interface AdministradoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(Administrado item);
    @Update
    void actualizar(Administrado item);
    @Query("SELECT * FROM administrados ORDER BY id_administrado DESC")
    List<Administrado> listar();
    @Query("SELECT * FROM administrados WHERE sincronizado = 0")
    List<Administrado> pendientes();
    @Query("UPDATE administrados SET sincronizado = 1 WHERE id_administrado = :id")
    void marcarSincronizado(int id);
}
