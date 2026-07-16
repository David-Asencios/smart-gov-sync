package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expedientes_generales")
public class Expediente extends SyncEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_expediente")
    public int idExpediente;
    @ColumnInfo(name = "nro_expediente_anual")
    public String nroExpedienteAnual;
    @ColumnInfo(name = "fecha_hora_apertura")
    public long fechaHoraApertura = System.currentTimeMillis();
    @ColumnInfo(name = "asunto_general")
    public String asuntoGeneral;
    @ColumnInfo(name = "estado_global")
    public String estadoGlobal = "ABIERTO";
    @ColumnInfo(name = "id_usuario_registro", defaultValue = "0")
    public int idUsuarioRegistro;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
