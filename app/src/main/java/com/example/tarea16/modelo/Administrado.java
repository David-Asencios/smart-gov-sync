package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "administrados")
public class Administrado {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_administrado")
    public int idAdministrado;
    @ColumnInfo(name = "codigo_administrado")
    public String codigoAdministrado;
    @ColumnInfo(name = "dni_ruc")
    public String dniRuc;
    @ColumnInfo(name = "nombre_razon_social")
    public String nombreRazonSocial;
    public String telefono;
    @ColumnInfo(name = "correo_notificaciones")
    public String correoNotificaciones;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
