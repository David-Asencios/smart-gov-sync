package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hojas_ruta_derivaciones")
public class HojaRuta extends SyncEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_derivacion")
    public int idDerivacion;
    @ColumnInfo(name = "codigo_barras_seguimiento")
    public String codigoBarrasSeguimiento;
    @ColumnInfo(name = "id_documento")
    public int idDocumento;
    @ColumnInfo(name = "id_empleado_asignado")
    public int idEmpleadoAsignado;
    @ColumnInfo(name = "id_oficina_procedencia")
    public int idOficinaProcedencia;
    @ColumnInfo(name = "fecha_hora_despacho")
    public long fechaHoraDespacho = System.currentTimeMillis();
    @ColumnInfo(name = "prioridad_envio")
    public String prioridadEnvio = "NORMAL";
    @ColumnInfo(name = "fecha_hora_recepcion")
    public long fechaHoraRecepcion;
    @ColumnInfo(name = "observaciones_receptor")
    public String observacionesReceptor;
    @ColumnInfo(name = "estado_derivacion")
    public String estadoDerivacion = "PENDIENTE";
    public double latitud;
    public double longitud;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado = false;
    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();
}
