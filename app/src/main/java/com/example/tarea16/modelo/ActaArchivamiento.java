package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "actas_archivamiento")
public class ActaArchivamiento extends SyncEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_acta")
    public int idActa;
    @ColumnInfo(name = "nro_acta_unico")
    public String nroActaUnico;
    @ColumnInfo(name = "id_derivacion")
    public int idDerivacion;
    @ColumnInfo(name = "id_ubicacion_archivo")
    public int idUbicacionArchivo;
    @ColumnInfo(name = "fecha_hora_guardado")
    public long fechaHoraGuardado = System.currentTimeMillis();
    @ColumnInfo(name = "costo_digitalizacion")
    public double costoDigitalizacion;
    @ColumnInfo(name = "costo_arancel_custodia")
    public double costoArancelCustodia;
    @ColumnInfo(name = "costo_final_procesamiento")
    public double costoFinalProcesamiento;
    @ColumnInfo(name = "id_usuario_archivo")
    public Integer idUsuarioArchivo;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
