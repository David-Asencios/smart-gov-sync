package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "archivo_fisico_central")
public class ArchivoFisico extends SyncEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_ubicacion")
    public int idUbicacion;
    @ColumnInfo(name = "codigo_almacen")
    public String codigoAlmacen;
    @ColumnInfo(name = "nro_pabellon")
    public int nroPabellon;
    @ColumnInfo(name = "nro_estante")
    public int nroEstante;
    @ColumnInfo(name = "nro_caja_fisica")
    public int nroCajaFisica;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
