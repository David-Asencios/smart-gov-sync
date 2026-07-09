package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.tarea16.modelo.Usuario;

import java.util.List;

@Dao
public interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(Usuario item);
    @Query("SELECT * FROM usuario ORDER BY id_usuario DESC")
    List<Usuario> listar();
    @Query("SELECT * FROM usuario WHERE sincronizado = 0")
    List<Usuario> pendientes();
}
