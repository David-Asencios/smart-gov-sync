package com.example.tarea16.fragments;

import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.tarea16.R;
import com.example.tarea16.adapter.SimpleTextAdapter;
import com.example.tarea16.api.ApiClient;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.api.UsuarioRequest;
import com.example.tarea16.api.UsuarioResponse;
import com.example.tarea16.db.AppDatabase;
import com.example.tarea16.modelo.Personal;
import com.example.tarea16.modelo.Usuario;
import com.example.tarea16.security.RoleManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentUsuarios extends SimpleListFragment {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String[] roles = {RoleManager.ADMIN, RoleManager.MESA_PARTES, RoleManager.ESPECIALISTA, RoleManager.ARCHIVO};

    @Override
    protected String descripcion() {
        return "";
    }

    @Override
    protected String primaryActionText() {
        return "Nuevo usuario";
    }

    @Override
    protected void onPrimaryAction() {
        mostrarFormulario(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        descargarUsuarios();
    }

    @Override
    protected void onItemSelected(SimpleTextAdapter.Item item) {
        if (!(item.source instanceof Usuario)) return;
        Usuario usuario = (Usuario) item.source;
        String estado = usuario.activo ? "Desactivar" : "Activar";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(usuario.username)
                .setItems(new String[]{"Editar", estado}, (dialog, which) -> {
                    if (which == 0) mostrarFormulario(usuario);
                    else cambiarEstado(usuario);
                })
                .show();
    }

    @Override
    protected List<SimpleTextAdapter.Item> cargarItems(AppDatabase db) {
        db.usuarioDao().limpiarContrasenas();
        List<SimpleTextAdapter.Item> items = new ArrayList<>();
        for (Usuario item : db.usuarioDao().listar()) {
            items.add(new SimpleTextAdapter.Item(
                    item.username,
                    RoleManager.displayName(item.rol)
                            + " / " + (item.idEmpleado > 0 ? "Empleado ID: " + item.idEmpleado : "Sin empleado asociado")
                            + " / " + (item.activo ? "Activo" : "Inactivo"),
                    item.activo ? estado(item.sincronizado) : "INACTIVO",
                    item));
        }
        return items;
    }

    private void mostrarFormulario(Usuario actual) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            List<Personal> trabajadores = AppDatabase.getInstance(app).personalDao().listar();
            if (isAdded()) requireActivity().runOnUiThread(() -> mostrarFormulario(actual, trabajadores));
        });
    }

    private void mostrarFormulario(Usuario actual, List<Personal> trabajadores) {
        Context context = requireContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);

        EditText username = input(context, "Usuario", InputType.TYPE_CLASS_TEXT);
        EditText password = input(context, actual == null ? "Contrasena" : "Nueva contrasena (opcional)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Spinner trabajador = new Spinner(context);
        ArrayAdapter<String> trabajadorAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, nombresTrabajadores(trabajadores));
        trabajador.setAdapter(trabajadorAdapter);
        Spinner rol = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Administrador", "Mesa de Partes", "Especialista", "Archivo"});
        rol.setAdapter(adapter);

        if (actual != null) {
            username.setText(actual.username);
            trabajador.setSelection(trabajadorIndex(trabajadores, actual.idEmpleado));
            rol.setSelection(roleIndex(actual.rol));
        }
        layout.addView(username);
        layout.addView(password);
        layout.addView(trabajador, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.addView(rol, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(actual == null ? "Nuevo usuario" : "Editar usuario")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.guardar, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String user = username.getText().toString().trim();
                    String pass = password.getText().toString().trim();
                    int empleadoIndex = trabajador.getSelectedItemPosition() - 1;
                    Personal empleado = empleadoIndex >= 0 ? trabajadores.get(empleadoIndex) : null;
                    String selectedRole = roles[rol.getSelectedItemPosition()];
                    if (user.isEmpty() || (actual == null && pass.isEmpty())) {
                        Toast.makeText(context, "Completa usuario y contrasena", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if ((RoleManager.ESPECIALISTA.equals(selectedRole) || RoleManager.ARCHIVO.equals(selectedRole)) && empleado == null) {
                        Toast.makeText(context, "El rol seleccionado requiere un empleado asociado", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (empleado != null && !cargoCompatible(empleado, selectedRole)) {
                        Toast.makeText(context, "El cargo del empleado no corresponde al rol seleccionado", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (empleado != null && (empleado.remoteUuid == null || empleado.remoteUuid.trim().isEmpty())) {
                        Toast.makeText(context, "Sincroniza el trabajador antes de asociarlo", Toast.LENGTH_LONG).show();
                        return;
                    }
                    dialog.dismiss();
                    guardarUsuario(actual, user, pass, empleado == null ? "" : empleado.remoteUuid, selectedRole);
                }));
        dialog.show();
    }

    private void guardarUsuario(Usuario actual, String username, String password, String empleadoRemoteUuid, String rol) {
        UsuarioRequest request = new UsuarioRequest();
        request.username = username;
        request.password = password.trim().isEmpty() ? null : password;
        request.idEmpleadoRemoteUuid = empleadoRemoteUuid;
        request.rol = rol;
        request.activo = actual == null ? true : actual.activo;
        Call<UsuarioResponse> call = actual == null
                ? ApiClient.getService().crearUsuario(authorization(), request)
                : ApiClient.getService().actualizarUsuario(authorization(), actual.idUsuario, request);
        ejecutarGuardado(call);
    }

    private void cambiarEstado(Usuario usuario) {
        UsuarioRequest request = new UsuarioRequest();
        request.activo = !usuario.activo;
        ejecutarGuardado(ApiClient.getService().actualizarUsuario(authorization(), usuario.idUsuario, request));
    }

    private void ejecutarGuardado(Call<UsuarioResponse> call) {
        call.enqueue(new Callback<UsuarioResponse>() {
            @Override public void onResponse(Call<UsuarioResponse> ignored, Response<UsuarioResponse> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "No se pudo guardar el usuario (" + response.code() + ")", Toast.LENGTH_LONG).show();
                    return;
                }
                guardarRespuesta(response.body());
            }
            @Override public void onFailure(Call<UsuarioResponse> ignored, Throwable error) {
                if (isAdded()) Toast.makeText(requireContext(), "Sin conexion con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void descargarUsuarios() {
        ApiClient.getService().usuarios(authorization()).enqueue(new Callback<List<UsuarioResponse>>() {
            @Override public void onResponse(Call<List<UsuarioResponse>> ignored, Response<List<UsuarioResponse>> response) {
                if (!response.isSuccessful() || response.body() == null || !isAdded()) return;
                Context app = requireContext().getApplicationContext();
                executor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(app);
                    db.usuarioDao().borrarTodos();
                    for (UsuarioResponse item : response.body()) db.usuarioDao().insertar(toEntity(item));
                    if (isAdded()) requireActivity().runOnUiThread(FragmentUsuarios.this::recargar);
                });
            }
            @Override public void onFailure(Call<List<UsuarioResponse>> ignored, Throwable error) { }
        });
    }

    private void guardarRespuesta(UsuarioResponse response) {
        Context app = requireContext().getApplicationContext();
        executor.execute(() -> {
            AppDatabase.getInstance(app).usuarioDao().insertar(toEntity(response));
            if (isAdded()) requireActivity().runOnUiThread(this::recargar);
        });
    }

    private Usuario toEntity(UsuarioResponse response) {
        Usuario item = new Usuario();
        item.idUsuario = response.idUsuario;
        item.username = response.username;
        item.passwordHash = null;
        item.rol = response.rol;
        item.activo = response.activo;
        Personal empleado = AppDatabase.getInstance(requireContext()).personalDao().buscarPorRemoteUuid(response.idEmpleadoRemoteUuid);
        item.idEmpleado = empleado == null ? 0 : empleado.idEmpleado;
        item.updatedAt = response.updatedAt;
        item.sincronizado = true;
        item.syncStatus = "SYNCED";
        return item;
    }

    private String authorization() {
        return "Bearer " + new TokenManager(requireContext()).obtenerToken();
    }

    private int roleIndex(String role) {
        String normalized = RoleManager.normalize(role);
        for (int i = 0; i < roles.length; i++) if (roles[i].equals(normalized)) return i;
        return 1;
    }

    private List<String> nombresTrabajadores(List<Personal> trabajadores) {
        List<String> nombres = new ArrayList<>();
        nombres.add("Sin empleado asociado");
        for (Personal item : trabajadores) {
            String nombre = item.nombreCompleto == null || item.nombreCompleto.trim().isEmpty()
                    ? "Trabajador " + item.idEmpleado
                    : item.nombreCompleto;
            nombres.add(nombre + " / " + (item.cargo == null ? "Sin cargo" : item.cargo));
        }
        return nombres;
    }

    private int trabajadorIndex(List<Personal> trabajadores, int idEmpleado) {
        for (int i = 0; i < trabajadores.size(); i++) {
            if (trabajadores.get(i).idEmpleado == idEmpleado) return i + 1;
        }
        return 0;
    }

    private boolean cargoCompatible(Personal empleado, String rol) {
        if (RoleManager.ADMIN.equals(rol) || RoleManager.MESA_PARTES.equals(rol)) return true;
        String cargo = empleado.cargo == null ? "" : empleado.cargo.trim().toUpperCase();
        if (RoleManager.ESPECIALISTA.equals(rol)) return cargo.contains("ESPECIALISTA");
        return RoleManager.ARCHIVO.equals(rol) && cargo.contains("ARCHIVO");
    }

    private EditText input(Context context, String hint, int type) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setInputType(type);
        return input;
    }

}
