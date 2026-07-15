package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "personal_especialistas")
public class Personal extends SyncEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_empleado")
    public int idEmpleado;
    @ColumnInfo(name = "codigo_empleado")
    public String codigoEmpleado;
    @ColumnInfo(name = "nombre_completo")
    public String nombreCompleto;
    public String cargo;
    @ColumnInfo(name = "id_oficina")
    public int idOficina;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
