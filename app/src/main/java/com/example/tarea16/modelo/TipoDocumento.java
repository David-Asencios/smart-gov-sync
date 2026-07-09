package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tipos_documentos")
public class TipoDocumento {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_tipo_documento")
    public int idTipoDocumento;
    @ColumnInfo(name = "nombre_tipo_documento")
    public String nombreTipoDocumento;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
