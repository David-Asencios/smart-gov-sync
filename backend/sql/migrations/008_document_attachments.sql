alter table documentos_ingresados add column if not exists ruta_adjunto text;
alter table documentos_ingresados add column if not exists nombre_adjunto varchar(255);
alter table documentos_ingresados add column if not exists tipo_mime_adjunto varchar(100);
