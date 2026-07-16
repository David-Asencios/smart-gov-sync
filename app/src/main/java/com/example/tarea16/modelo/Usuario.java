package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuario")
public class Usuario extends SyncEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_usuario")
    public int idUsuario;
    public String username;
    @ColumnInfo(name = "password_hash")
    public String passwordHash;
    @ColumnInfo(name = "rol", defaultValue = "'MESA_PARTES'")
    public String rol = "MESA_PARTES";
    @ColumnInfo(name = "activo", defaultValue = "1")
    public boolean activo = true;
    @ColumnInfo(name = "id_empleado")
    public int idEmpleado;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
