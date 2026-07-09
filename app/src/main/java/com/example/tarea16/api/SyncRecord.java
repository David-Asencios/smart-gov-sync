package com.example.tarea16.api;

import java.util.Map;

public class SyncRecord {
    public String tabla;
    public Map<String, Object> datos;

    public SyncRecord(String tabla, Map<String, Object> datos) {
        this.tabla = tabla;
        this.datos = datos;
    }
}
