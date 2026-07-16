alter table actas_archivamiento
  add column if not exists id_usuario_archivo int references usuario(id_usuario);

create unique index if not exists ux_archivo_codigo_activo
  on archivo_fisico_central (lower(codigo_almacen)) where deleted = false;

create unique index if not exists ux_acta_derivacion_activa
  on actas_archivamiento (id_derivacion) where deleted = false;
