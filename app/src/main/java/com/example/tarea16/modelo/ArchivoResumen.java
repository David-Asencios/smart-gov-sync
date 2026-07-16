package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;

public class ArchivoResumen {
    @ColumnInfo(name = "id_acta")
    public int idActa;
    @ColumnInfo(name = "nro_acta_unico")
    public String nroActaUnico;
    @ColumnInfo(name = "fecha_hora_guardado")
    public long fechaHoraGuardado;
    @ColumnInfo(name = "id_derivacion")
    public int idDerivacion;
    @ColumnInfo(name = "nro_expediente_anual")
    public String nroExpedienteAnual;
    @ColumnInfo(name = "asunto_general")
    public String asuntoGeneral;
    @ColumnInfo(name = "codigo_almacen")
    public String codigoAlmacen;
    @ColumnInfo(name = "nro_pabellon")
    public int nroPabellon;
    @ColumnInfo(name = "nro_estante")
    public int nroEstante;
    @ColumnInfo(name = "nro_caja_fisica")
    public int nroCajaFisica;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado;
    @ColumnInfo(name = "sync_status")
    public String syncStatus;
}
