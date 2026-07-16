package com.example.tarea16.api;

import com.google.gson.annotations.SerializedName;

public class UsuarioResponse {
    @SerializedName("id_usuario") public int idUsuario;
    public String username;
    public String rol;
    public boolean activo;
    @SerializedName("id_empleado") public int idEmpleado;
    @SerializedName("updated_at") public long updatedAt;
}
