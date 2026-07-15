package com.example.tarea16.api;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    public String token;
    public String username;
    public String rol;
    @SerializedName("id_usuario") public Integer idUsuario;
    @SerializedName("id_empleado") public Integer idEmpleado;
    @SerializedName("id_oficina") public Integer idOficina;
    @SerializedName("nombre_completo") public String nombreCompleto;
    @SerializedName("nombre_oficina") public String nombreOficina;
}
