begin;

alter table expedientes_generales
  add column if not exists id_usuario_registro int references usuario(id_usuario);

create index if not exists ix_expedientes_usuario_registro
  on expedientes_generales(id_usuario_registro, fecha_hora_apertura desc);

commit;
