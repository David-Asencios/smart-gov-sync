package com.example.tarea16.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.json.JSONObject;

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
            AppDatabase db = AppDatabase.getInstance(app);
            List<Personal> trabajadores = db.personalDao().listar();
            List<Usuario> usuarios = db.usuarioDao().listar();
            if (isAdded()) requireActivity().runOnUiThread(() -> mostrarFormulario(actual, trabajadores, usuarios));
        });
    }

    private void mostrarFormulario(Usuario actual, List<Personal> trabajadores, List<Usuario> usuarios) {
        Context context = requireContext();
        View layout = LayoutInflater.from(context).inflate(R.layout.dialog_usuario, null, false);
        EditText username = layout.findViewById(R.id.txtUsuarioCuenta);
        EditText password = layout.findViewById(R.id.txtPasswordCuenta);
        Spinner trabajador = layout.findViewById(R.id.spinnerEmpleadoCuenta);
        Spinner rol = layout.findViewById(R.id.spinnerRolCuenta);
        TextView ayudaEmpleado = layout.findViewById(R.id.txtAyudaEmpleado);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Administrador", "Mesa de Partes", "Especialista", "Archivo"});
        rol.setAdapter(adapter);

        Set<Integer> empleadosOcupados = new HashSet<>();
        for (Usuario usuario : usuarios) {
            if (usuario.activo && usuario.idEmpleado > 0
                    && (actual == null || usuario.idUsuario != actual.idUsuario)) empleadosOcupados.add(usuario.idEmpleado);
        }
        List<Personal> disponibles = new ArrayList<>();
        rol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                actualizarEmpleados(context, trabajador, ayudaEmpleado, trabajadores, empleadosOcupados,
                        roles[position], actual == null ? 0 : actual.idEmpleado, disponibles);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        if (actual != null) {
            username.setText(actual.username);
            rol.setSelection(roleIndex(actual.rol));
        } else rol.setSelection(0);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(actual == null ? "Nuevo usuario" : "Editar usuario")
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.guardar, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String user = username.getText().toString().trim();
                    String pass = password.getText().toString();
                    int empleadoIndex = trabajador.getSelectedItemPosition() - 1;
                    Personal empleado = empleadoIndex >= 0 && empleadoIndex < disponibles.size() ? disponibles.get(empleadoIndex) : null;
                    String selectedRole = roles[rol.getSelectedItemPosition()];
                    if (!user.matches("[A-Za-z0-9._-]{3,50}")) {
                        username.setError("Use de 3 a 50 letras, numeros, punto, guion o guion bajo");
                        username.requestFocus();
                        return;
                    }
                    if (nombreDuplicado(user, actual, usuarios)) {
                        username.setError("El nombre de usuario ya existe");
                        username.requestFocus();
                        return;
                    }
                    if ((actual == null || !pass.isEmpty()) && (pass.length() < 8 || pass.length() > 72)) {
                        password.setError("La contrasena debe tener entre 8 y 72 caracteres");
                        password.requestFocus();
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
                    Toast.makeText(requireContext(), mensajeError(response), Toast.LENGTH_LONG).show();
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

    private void actualizarEmpleados(Context context, Spinner spinner, TextView ayuda,
                                     List<Personal> trabajadores, Set<Integer> ocupados,
                                     String rol, int idActual, List<Personal> disponibles) {
        disponibles.clear();
        for (Personal item : trabajadores) {
            if ((item.idEmpleado == idActual || !ocupados.contains(item.idEmpleado)) && cargoCompatible(item, rol)) disponibles.add(item);
        }
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                nombresTrabajadores(disponibles)));
        spinner.setSelection(trabajadorIndex(disponibles, idActual));
        boolean requerido = RoleManager.ESPECIALISTA.equals(rol) || RoleManager.ARCHIVO.equals(rol);
        ayuda.setText(requerido
                ? (disponibles.isEmpty() ? "No hay empleados disponibles con el cargo requerido." : "Este rol requiere un empleado compatible y sin otra cuenta activa.")
                : "La asociacion con un empleado es opcional para este rol.");
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
        if (RoleManager.ADMIN.equals(rol)) return false;
        String cargo = empleado.cargo == null ? "" : empleado.cargo.trim().toUpperCase();
        if (RoleManager.MESA_PARTES.equals(rol)) return cargo.contains("MESA");
        if (RoleManager.ESPECIALISTA.equals(rol)) return cargo.contains("ESPECIALISTA");
        return RoleManager.ARCHIVO.equals(rol) && cargo.contains("ARCHIVO");
    }

    private boolean nombreDuplicado(String username, Usuario actual, List<Usuario> usuarios) {
        for (Usuario usuario : usuarios) {
            if ((actual == null || usuario.idUsuario != actual.idUsuario)
                    && usuario.username != null && usuario.username.equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    private String mensajeError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String mensaje = new JSONObject(response.errorBody().string()).optString("error");
                if (!mensaje.trim().isEmpty()) return mensaje;
            }
        } catch (Exception ignored) { }
        return "No se pudo guardar el usuario (" + response.code() + ")";
    }

}
