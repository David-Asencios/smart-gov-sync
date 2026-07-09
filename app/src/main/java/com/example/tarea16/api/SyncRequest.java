package com.example.tarea16.api;

import java.util.List;

public class SyncRequest {
    public List<SyncRecord> registros;

    public SyncRequest(List<SyncRecord> registros) {
        this.registros = registros;
    }
}
