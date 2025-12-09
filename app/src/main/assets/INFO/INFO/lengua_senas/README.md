# Backend PHP - Lengua de Señas ULEAM

## Instalación

1. Copiar todos los archivos PHP a la carpeta del servidor web (ej: `htdocs/api/` o `/var/www/html/api/`)

2. Configurar la base de datos en `config.php`:
   ```php
   define('DB_HOST', 'localhost');
   define('DB_USER', 'tu_usuario');
   define('DB_PASS', 'tu_contraseña');
   define('DB_NAME', 'lengua_senas');
   ```

3. Configurar el secret key para JWT en `config.php`:
   ```php
   define('JWT_SECRET', 'cambiar-por-una-clave-secreta-segura');
   ```

4. Asegurarse de que la base de datos MySQL esté creada y tenga la estructura correcta (ver SQL proporcionado)

5. Configurar permisos del servidor web para escribir en la carpeta si es necesario

## Endpoints API

### Autenticación

#### POST /login.php
Iniciar sesión
```json
{
  "correo": "usuario@example.com",
  "contrasena": "password"
}
```

Respuesta:
```json
{
  "success": true,
  "message": "Login exitoso",
  "data": {
    "id_usuario": 1,
    "nombre": "Juan",
    "correo": "usuario@example.com",
    "rol": "estudiante",
    "fecha_registro": "2025-01-01 00:00:00"
  },
  "token": "jwt_token_here"
}
```

#### POST /register.php
Registrar nuevo usuario
```json
{
  "nombre": "Juan",
  "correo": "usuario@example.com",
  "contrasena": "password",
  "rol": "estudiante"
}
```

### Gestos

#### GET /gestos.php
Obtener todos los gestos (requiere autenticación)
Headers: `Authorization: Bearer {token}`

### Progreso

#### GET /usuario_gestos.php
Obtener progreso del usuario
Headers: `Authorization: Bearer {token}`
Query params: `id_usuario` (opcional)

### Sincronización

#### POST /sync.php
Sincronizar datos offline
Headers: `Authorization: Bearer {token}`
```json
{
  "usuario_gestos": [
    {
      "id_usuario": 1,
      "id_gesto": 1,
      "porcentaje": 80,
      "estado": "aprendido",
      "last_updated": 1234567890
    }
  ],
  "docente_estudiante": [
    {
      "id_docente": 2,
      "id_estudiante": 1,
      "estado": "aceptado",
      "last_updated": 1234567890
    }
  ]
}
```

### Relaciones Docente-Estudiante

#### GET /docente_estudiante.php
Obtener relaciones
Headers: `Authorization: Bearer {token}`
Query params: `id_usuario` (opcional), `tipo` (opcional: "docente" o "estudiante")

#### POST /docente_estudiante.php
Crear solicitud
```json
{
  "id_docente": 2,
  "id_estudiante": 1,
  "estado": "pendiente"
}
```

#### PUT /docente_estudiante.php
Actualizar solicitud
```json
{
  "id_docente": 2,
  "id_estudiante": 1,
  "estado": "aceptado"
}
```

#### DELETE /docente_estudiante.php
Eliminar relación (solo administrador)
Query params: `id_docente`, `id_estudiante`

### Reportes

#### GET /reporte.php
Generar reporte
Headers: `Authorization: Bearer {token}`
Query params: `id_usuario` (opcional), `formato` ("pdf" o "csv")

### Administración

#### POST /reset_actividad.php
Resetear actividad (solo administrador)
Headers: `Authorization: Bearer {token}`
Query params: `id_usuario`, `id_gesto`

## Seguridad

- Todos los endpoints (excepto login y register) requieren autenticación JWT
- El token se envía en el header: `Authorization: Bearer {token}`
- Los tokens expiran después de 7 días
- Las contraseñas se almacenan con hash bcrypt

## Notas

- La estructura de la base de datos NO debe modificarse
- Los campos `sync_status` y `last_updated` existen solo en la app (Room), NO en MySQL
- Todas las respuestas son en formato JSON
- CORS está habilitado para desarrollo (ajustar en producción)


