package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "oficinas")
public class Oficina extends SyncEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_oficina")
    public int idOficina;
    @ColumnInfo(name = "codigo_oficina")
    public String codigoOficina;
    @ColumnInfo(name = "siglas_oficiales")
    public String siglasOficiales;
    @ColumnInfo(name = "nombre_unidad")
    public String nombreUnidad;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
