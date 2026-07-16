package com.example.tarea16.fragments;

import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Usuario;

import java.util.ArrayList;
import java.util.List;

public class FragmentUsuarios extends SimpleListFragment {
    @Override
    protected String descripcion() {
        return "Administra las cuentas del sistema. Los roles son fijos: Administrador, Mesa de Partes, Especialista y Archivista. Cada usuario debe estar asociado a un trabajador registrado.";
    }

    @Override
    protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        List<SimpleTextAdapter.Item> items = new ArrayList<>();
        for (Usuario item : db.usuarioDao().listar()) {
            items.add(new SimpleTextAdapter.Item(
                    item.username,
                    "Rol: " + com.example.tarea16.security.RoleManager.displayName(item.rol)
                            + " / Empleado ID: " + item.idEmpleado
                            + " / " + (item.activo ? "Activo" : "Inactivo"),
                    item.activo ? estado(item.sincronizado) : "INACTIVO"));
        }
        return items;
    }
}
