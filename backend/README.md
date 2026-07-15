# Smart-Gov Sync Backend

API REST para la aplicacion Android. La app no se conecta directamente a Neon:

```text
Android (Room) -> API en Render -> PostgreSQL en Neon
```

## Variables requeridas

```env
DATABASE_URL=postgresql://usuario:password@host/neondb?sslmode=require&channel_binding=require
JWT_SECRET=clave_segura
PORT=3000
```

## Inicio local

```powershell
npm.cmd install
npm.cmd run seed
npm.cmd start
```

`npm run seed` carga datos de demostracion sin duplicarlos y crea estas cuentas:

| Rol | Usuario | Password | Acciones principales |
|---|---|---|---|
| Administrador | `admin` | `admin123` | Acceso completo |
| Mesa de Partes | `mesa_partes` | `mesa123` | Expedientes, administrados y documentos |
| Especialista | `especialista` | `especialista123` | Bandeja, derivaciones, estados y mapa |
| Archivo Central | `archivo` | `archivo123` | Bandeja, ubicacion fisica y actas |

Cambie estas contrasenas antes de usar el sistema fuera de una demostracion academica.

## Render

- Root Directory: `backend`
- Build Command: `npm install && npm run migrate`
- Start Command: `npm start`
- Variables: `DATABASE_URL` y `JWT_SECRET`

Despues del despliegue, compruebe:

```text
GET https://smart-gov-sync.onrender.com/health
```
