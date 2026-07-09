package com.example.tarea16.fragments;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.tarea16.databinding.FragmentCrudBinding;
import com.example.tarea16.db.AppDatabase;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentCrud extends Fragment {
    private FragmentCrudBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<EntityConfig> entities = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCrudBinding.inflate(inflater, container, false);
        cargarEntidades();
        ArrayAdapter<EntityConfig> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, entities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerEntidad.setAdapter(adapter);
        binding.spinnerEntidad.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mostrarPlantilla();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        binding.btnPlantillaCrud.setOnClickListener(v -> mostrarPlantilla());
        binding.btnListarCrud.setOnClickListener(v -> ejecutar(this::listar));
        binding.btnCrearCrud.setOnClickListener(v -> ejecutar(this::crear));
        binding.btnActualizarCrud.setOnClickListener(v -> ejecutar(this::actualizar));
        binding.btnEliminarCrud.setOnClickListener(v -> ejecutar(this::eliminar));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void cargarEntidades() {
        entities.add(new EntityConfig("Oficinas", "oficinas", "id_oficina", "codigo_oficina", "siglas_oficiales", "nombre_unidad"));
        entities.add(new EntityConfig("Tipos documentos", "tipos_documentos", "id_tipo_documento", "nombre_tipo_documento"));
        entities.add(new EntityConfig("Administrados", "administrados", "id_administrado", "codigo_administrado", "dni_ruc", "nombre_razon_social", "telefono", "correo_notificaciones"));
        entities.add(new EntityConfig("Personal", "personal_especialistas", "id_empleado", "codigo_empleado", "nombre_completo", "cargo", "id_oficina"));
        entities.add(new EntityConfig("Direcciones", "administrados_direcciones", "id_direccion", "id_administrado", "tipo_inmueble", "calle", "numero", "comuna_distrito", "ciudad"));
        entities.add(new EntityConfig("Expedientes", "expedientes_generales", "id_expediente", "nro_expediente_anual", "fecha_hora_apertura", "asunto_general", "estado_global"));
        entities.add(new EntityConfig("Documentos", "documentos_ingresados", "id_documento", "nro_documento_unico", "id_expediente", "id_tipo_documento", "id_administrado", "cantidad_folios", "fecha_hora_recepcion", "ruta_foto"));
        entities.add(new EntityConfig("Hojas ruta", "hojas_ruta_derivaciones", "id_derivacion", "codigo_barras_seguimiento", "id_documento", "id_empleado_asignado", "id_oficina_procedencia", "fecha_hora_despacho", "prioridad_envio", "fecha_hora_recepcion", "observaciones_receptor", "estado_derivacion", "latitud", "longitud"));
        entities.add(new EntityConfig("Archivo fisico", "archivo_fisico_central", "id_ubicacion", "codigo_almacen", "nro_pabellon", "nro_estante", "nro_caja_fisica"));
        entities.add(new EntityConfig("Actas archivamiento", "actas_archivamiento", "id_acta", "nro_acta_unico", "id_derivacion", "id_ubicacion_archivo", "fecha_hora_guardado", "costo_digitalizacion", "costo_arancel_custodia", "costo_final_procesamiento"));
    }

    private EntityConfig entidadActual() {
        return (EntityConfig) binding.spinnerEntidad.getSelectedItem();
    }

    private void mostrarPlantilla() {
        EntityConfig entity = entidadActual();
        if (entity == null) {
            return;
        }
        JSONObject json = new JSONObject();
        long now = System.currentTimeMillis();
        try {
            for (String field : entity.fields) {
                json.put(field, valorEjemplo(field, now));
            }
            binding.txtJsonCrud.setText(json.toString(2));
        } catch (Exception e) {
            binding.txtResultadoCrud.setText(e.getMessage());
        }
    }

    private Object valorEjemplo(String field, long now) {
        if (field.startsWith("id_") || field.startsWith("nro_") && !field.contains("documento_unico") && !field.contains("expediente_anual") && !field.contains("acta_unico")) {
            return field.startsWith("nro_") ? 1 : 1;
        }
        if (field.startsWith("fecha_hora")) {
            return now;
        }
        if (field.equals("cantidad_folios") || field.startsWith("nro_pabellon") || field.startsWith("nro_estante") || field.startsWith("nro_caja")) {
            return 1;
        }
        if (field.startsWith("costo_") || field.equals("latitud") || field.equals("longitud")) {
            return 0.0;
        }
        if (field.contains("estado")) {
            return "ABIERTO";
        }
        if (field.contains("prioridad")) {
            return "NORMAL";
        }
        return "valor_" + field;
    }

    private void ejecutar(CrudOperation operation) {
        Context context = requireContext().getApplicationContext();
        EntityConfig entity = entidadActual();
        String id = binding.txtIdCrud.getText().toString().trim();
        String json = binding.txtJsonCrud.getText().toString();
        executor.execute(() -> {
            String result;
            try {
                result = operation.run(AppDatabase.getInstance(context).getOpenHelper().getWritableDatabase(), entity, id, json);
            } catch (Exception e) {
                result = "Error: " + e.getMessage();
            }
            publicar(result);
        });
    }

    private String listar(SupportSQLiteDatabase db, EntityConfig entity, String id, String json) {
        Cursor cursor = db.query(new SimpleSQLiteQuery("SELECT * FROM " + entity.table + " ORDER BY " + entity.idColumn + " DESC LIMIT 50"));
        try {
            StringBuilder result = new StringBuilder();
            result.append(entity.label).append('\n');
            while (cursor.moveToNext()) {
                result.append("{ ");
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    if (i > 0) {
                        result.append(", ");
                    }
                    String column = cursor.getColumnName(i);
                    result.append(column).append(": ");
                    if (cursor.isNull(i)) {
                        result.append("null");
                    } else {
                        result.append(cursor.getString(i));
                    }
                }
                result.append(" }\n");
            }
            return result.length() == entity.label.length() + 1 ? "Sin registros" : result.toString();
        } finally {
            cursor.close();
        }
    }

    private String crear(SupportSQLiteDatabase db, EntityConfig entity, String id, String json) throws Exception {
        ContentValues values = contentValuesDesdeJson(entity, json, false);
        long insertedId = db.insert(entity.table, SQLiteDatabase.CONFLICT_REPLACE, values);
        return String.format(Locale.US, "Creado en %s con ID %d", entity.table, insertedId);
    }

    private String actualizar(SupportSQLiteDatabase db, EntityConfig entity, String id, String json) throws Exception {
        validarId(id);
        ContentValues values = contentValuesDesdeJson(entity, json, true);
        int rows = db.update(entity.table, SQLiteDatabase.CONFLICT_REPLACE, values, entity.idColumn + " = ?", new Object[]{id});
        return rows > 0 ? "Registro actualizado" : "No existe un registro con ese ID";
    }

    private String eliminar(SupportSQLiteDatabase db, EntityConfig entity, String id, String json) {
        validarId(id);
        int rows = db.delete(entity.table, entity.idColumn + " = ?", new Object[]{id});
        return rows > 0 ? "Registro eliminado" : "No existe un registro con ese ID";
    }

    private ContentValues contentValuesDesdeJson(EntityConfig entity, String jsonText, boolean partial) throws Exception {
        JSONObject json = new JSONObject(jsonText);
        ContentValues values = new ContentValues();
        for (String field : entity.fields) {
            if (!partial || json.has(field)) {
                Object value = json.opt(field);
                if (value != null && value != JSONObject.NULL) {
                    putValue(values, field, value);
                }
            }
        }
        values.put("sincronizado", false);
        values.put("updated_at", System.currentTimeMillis());
        return values;
    }

    private void putValue(ContentValues values, String field, Object value) {
        if (value instanceof Boolean) {
            values.put(field, (Boolean) value);
        } else if (value instanceof Integer) {
            values.put(field, (Integer) value);
        } else if (value instanceof Long) {
            values.put(field, (Long) value);
        } else if (value instanceof Number) {
            values.put(field, ((Number) value).doubleValue());
        } else {
            values.put(field, String.valueOf(value));
        }
    }

    private void validarId(String id) {
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Ingresa el ID del registro");
        }
    }

    private void publicar(String result) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (binding != null) {
                binding.txtResultadoCrud.setText(result);
            }
        });
    }

    private interface CrudOperation {
        String run(SupportSQLiteDatabase db, EntityConfig entity, String id, String json) throws Exception;
    }

    private static class EntityConfig {
        final String label;
        final String table;
        final String idColumn;
        final String[] fields;

        EntityConfig(String label, String table, String idColumn, String... fields) {
            this.label = label;
            this.table = table;
            this.idColumn = idColumn;
            this.fields = fields;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }
}
