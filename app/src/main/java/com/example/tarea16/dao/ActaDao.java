package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.ActaArchivamiento;

import java.util.List;

@Dao
public interface ActaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(ActaArchivamiento item);
    @Update
    void actualizar(ActaArchivamiento item);
    @Query("SELECT * FROM actas_archivamiento ORDER BY id_acta DESC")
    List<ActaArchivamiento> listar();
    @Query("SELECT * FROM actas_archivamiento WHERE sincronizado = 0")
    List<ActaArchivamiento> pendientes();
    @Query("UPDATE actas_archivamiento SET sincronizado = 1 WHERE id_acta = :id")
    void marcarSincronizado(int id);
}
