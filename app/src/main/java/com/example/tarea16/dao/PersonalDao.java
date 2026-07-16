package com.example.tarea16.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.tarea16.modelo.Personal;

import java.util.List;

@Dao
public interface PersonalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertar(Personal item);
    @Update
    void actualizar(Personal item);
    @Query("SELECT * FROM personal_especialistas ORDER BY id_empleado DESC")
    List<Personal> listar();
    @Query("SELECT * FROM personal_especialistas WHERE sincronizado = 0")
    List<Personal> pendientes();
    @Query("SELECT COUNT(*) FROM personal_especialistas WHERE id_empleado = :id AND deleted = 0")
    int existe(int id);
    @Query("UPDATE personal_especialistas SET sincronizado = 1 WHERE id_empleado = :id")
    void marcarSincronizado(int id);
    @Query("SELECT * FROM personal_especialistas WHERE remote_uuid = :remoteUuid LIMIT 1")
    Personal buscarPorRemoteUuid(String remoteUuid);
}
