begin;

do $$
declare table_name text;
begin
  foreach table_name in array array[
    'oficinas','tipos_documentos','administrados','personal_especialistas',
    'administrados_direcciones','expedientes_generales','documentos_ingresados',
    'hojas_ruta_derivaciones','archivo_fisico_central','actas_archivamiento'
  ] loop
    execute format('alter table %I add column if not exists remote_uuid uuid', table_name);
    execute format('alter table %I add column if not exists deleted boolean not null default false', table_name);
    execute format('alter table %I add column if not exists version bigint not null default 0', table_name);
    execute format('update %I set remote_uuid = gen_random_uuid() where remote_uuid is null', table_name);
    execute format('alter table %I alter column remote_uuid set not null', table_name);
    execute format('create unique index if not exists %I on %I(remote_uuid)', 'ux_' || table_name || '_remote_uuid', table_name);
  end loop;
end $$;

commit;
