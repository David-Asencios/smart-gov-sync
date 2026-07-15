# Smart-Gov Sync

Aplicacion Android Java con funcionamiento offline (Room) y sincronizacion con un backend Node.js desplegable en Render y PostgreSQL/Neon.

## Despliegue correcto

1. En Neon, conserva la cadena de conexion en `DATABASE_URL`. No se coloca dentro de Android.
2. En Render, configura `DATABASE_URL` y un `JWT_SECRET` aleatorio de al menos 32 caracteres.
3. Despliega usando `render.yaml`. El build ejecuta `npm run migrate` antes de iniciar el servidor.
4. Ejecuta una vez `npm run seed` desde una consola segura de Render si necesitas datos de demostracion.
5. Verifica `https://smart-gov-sync.onrender.com/health` y luego instala el APK.

Las migraciones se registran en `schema_migrations`, por lo que no se vuelven a ejecutar. No uses `fallbackToDestructiveMigration`: Android conserva la base Room existente mediante `MIGRATION_1_2`.

## Datos minimos para demostrar el trabajo

- Oficinas: codigo, siglas y nombre de unidad.
- Tipos de documento: Solicitud, Oficio e Informe.
- Personal: codigo, nombre, cargo y oficina.
- Usuarios: username, hash de password, rol, estado activo y empleado asociado.
- Administrado: DNI/RUC, nombre o razon social, telefono, correo y direccion.
- Expediente: numero anual, asunto, fecha y estado.
- Documento: expediente, tipo, administrado, folios y evidencia.
- Derivacion: documento, especialista, oficina, prioridad, estado y coordenadas reales.
- Archivo fisico: almacen, pabellon, estante y caja.
- Acta: derivacion, ubicacion, fecha y costos.

Roles validos: `ADMIN`, `MESA_PARTES`, `ESPECIALISTA` y `ARCHIVO`. Un rol vacio o desconocido no recibe permisos.

## Verificacion local

```powershell
$env:JWT_SECRET='una_clave_local_de_al_menos_32_caracteres'
npm.cmd --prefix backend test
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
