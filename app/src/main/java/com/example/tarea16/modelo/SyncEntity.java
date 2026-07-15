package com.example.tarea16.modelo;

import androidx.room.ColumnInfo;

import java.util.UUID;

public abstract class SyncEntity {
    @ColumnInfo(name = "remote_uuid")
    public String remoteUuid = UUID.randomUUID().toString();
    @ColumnInfo(name = "server_id")
    public Long serverId;
    @ColumnInfo(name = "sync_status")
    public String syncStatus = "PENDING";
    @ColumnInfo(name = "sync_error")
    public String syncError;
    @ColumnInfo(name = "deleted")
    public boolean deleted = false;
    @ColumnInfo(name = "sync_version")
    public long syncVersion = 0L;
}
