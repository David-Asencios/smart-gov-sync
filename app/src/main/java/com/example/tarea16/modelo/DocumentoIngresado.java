package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "documentos_ingresados")
public class DocumentoIngresado {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_documento")
    public int idDocumento;
    @ColumnInfo(name = "nro_documento_unico")
    public String nroDocumentoUnico;
    @ColumnInfo(name = "id_expediente")
    public int idExpediente;
    @ColumnInfo(name = "id_tipo_documento")
    public int idTipoDocumento;
    @ColumnInfo(name = "id_administrado")
    public int idAdministrado;
    @ColumnInfo(name = "cantidad_folios")
    public int cantidadFolios;
    @ColumnInfo(name = "fecha_hora_recepcion")
    public long fechaHoraRecepcion = System.currentTimeMillis();
    @ColumnInfo(name = "ruta_foto")
    public String rutaFoto;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
