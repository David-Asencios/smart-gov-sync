begin;

alter table usuario add column if not exists rol varchar(30);
alter table usuario add column if not exists activo boolean not null default true;
update usuario set rol = case
  when lower(username) = 'admin' then 'ADMIN'
  when lower(username) = 'mesa_partes' then 'MESA_PARTES'
  when lower(username) = 'especialista' then 'ESPECIALISTA'
  when lower(username) = 'archivo' then 'ARCHIVO'
  else null
end where rol is null;
alter table usuario add constraint usuario_rol_valido
  check (rol in ('ADMIN', 'MESA_PARTES', 'ESPECIALISTA', 'ARCHIVO')) not valid;
alter table usuario validate constraint usuario_rol_valido;
alter table usuario alter column rol set not null;

create unique index if not exists ux_oficinas_codigo on oficinas (lower(codigo_oficina));
create unique index if not exists ux_tipos_documentos_nombre on tipos_documentos (lower(nombre_tipo_documento));
create unique index if not exists ux_administrados_dni_ruc on administrados (dni_ruc);
create unique index if not exists ux_personal_codigo on personal_especialistas (lower(codigo_empleado));
create unique index if not exists ux_expedientes_numero on expedientes_generales (lower(nro_expediente_anual));
create unique index if not exists ux_documentos_numero on documentos_ingresados (lower(nro_documento_unico));
create unique index if not exists ux_derivaciones_codigo on hojas_ruta_derivaciones (lower(codigo_barras_seguimiento));
create unique index if not exists ux_actas_numero on actas_archivamiento (lower(nro_acta_unico));

create index if not exists ix_personal_oficina on personal_especialistas (id_oficina);
create index if not exists ix_direcciones_administrado on administrados_direcciones (id_administrado);
create index if not exists ix_documentos_expediente on documentos_ingresados (id_expediente);
create index if not exists ix_documentos_administrado on documentos_ingresados (id_administrado);
create index if not exists ix_derivaciones_empleado_estado on hojas_ruta_derivaciones (id_empleado_asignado, estado_derivacion);
create index if not exists ix_derivaciones_oficina_estado on hojas_ruta_derivaciones (id_oficina_procedencia, estado_derivacion);

commit;
