package com.example.tarea16.api;

import com.google.gson.annotations.SerializedName;

public class UsuarioRequest {
    public String username;
    public String password;
    public String rol;
    public Boolean activo;
    @SerializedName("id_empleado_remote_uuid") public String idEmpleadoRemoteUuid;
}
