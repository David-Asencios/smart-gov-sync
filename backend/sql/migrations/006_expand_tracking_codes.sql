begin;
alter table hojas_ruta_derivaciones alter column codigo_barras_seguimiento type varchar(50);
alter table actas_archivamiento alter column nro_acta_unico type varchar(50);
commit;
