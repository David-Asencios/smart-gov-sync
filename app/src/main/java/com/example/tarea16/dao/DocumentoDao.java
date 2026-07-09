package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.DocumentoIngresado;

import java.util.List;

@Dao
public interface DocumentoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(DocumentoIngresado item);
    @Update
    void actualizar(DocumentoIngresado item);
    @Query("SELECT * FROM documentos_ingresados ORDER BY id_documento DESC")
    List<DocumentoIngresado> listar();
    @Query("SELECT * FROM documentos_ingresados WHERE sincronizado = 0")
    List<DocumentoIngresado> pendientes();
    @Query("UPDATE documentos_ingresados SET sincronizado = 1 WHERE id_documento = :id")
    void marcarSincronizado(int id);
}
