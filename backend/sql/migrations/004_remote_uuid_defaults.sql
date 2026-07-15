begin;

do $$
declare table_name text;
begin
  foreach table_name in array array[
    'oficinas','tipos_documentos','administrados','personal_especialistas',
    'administrados_direcciones','expedientes_generales','documentos_ingresados',
    'hojas_ruta_derivaciones','archivo_fisico_central','actas_archivamiento'
  ] loop
    execute format('alter table %I alter column remote_uuid set default gen_random_uuid()', table_name);
  end loop;
end $$;

commit;
