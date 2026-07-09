package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.TipoDocumento;

import java.util.List;

@Dao
public interface TipoDocumentoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(TipoDocumento item);
    @Update
    void actualizar(TipoDocumento item);
    @Query("SELECT * FROM tipos_documentos ORDER BY id_tipo_documento DESC")
    List<TipoDocumento> listar();
    @Query("SELECT * FROM tipos_documentos WHERE sincronizado = 0")
    List<TipoDocumento> pendientes();
    @Query("UPDATE tipos_documentos SET sincronizado = 1 WHERE id_tipo_documento = :id")
    void marcarSincronizado(int id);
}
