package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "administrados_direcciones")
public class Direccion extends SyncEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_direccion")
    public int idDireccion;
    @ColumnInfo(name = "id_administrado")
    public int idAdministrado;
    @ColumnInfo(name = "tipo_inmueble")
    public String tipoInmueble;
    public String calle;
    public String numero;
    @ColumnInfo(name = "comuna_distrito")
    public String comunaDistrito;
    public String ciudad;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
