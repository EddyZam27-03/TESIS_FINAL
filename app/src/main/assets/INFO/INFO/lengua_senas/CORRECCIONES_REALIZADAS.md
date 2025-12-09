# Correcciones Realizadas en los Endpoints PHP

## Resumen
Se han corregido y creado todos los archivos PHP para que coincidan exactamente con las definiciones en `ApiService.kt`.

## Archivos Corregidos

### 1. `login.php`
- **Cambio**: La respuesta ahora coincide con `LoginResponse` de Kotlin
- **Estructura**: `{ success, message, usuario, token }`

### 2. `register.php`
- **Cambio**: La respuesta ahora coincide con `RegisterResponse` de Kotlin
- **Estructura**: `{ success, message, usuario, token }`

### 3. `gestos.php`
- **Cambio**: La respuesta ahora incluye el campo `gestos` en lugar de `data`
- **Estructura**: `{ success, message, gestos }`

### 4. `usuario_gestos.php`
- **Cambio**: La respuesta ahora devuelve directamente un array (sin envolver en success/message)
- **Estructura**: `[{ id_usuario, id_gesto, porcentaje, estado }, ...]`

### 5. `sync.php`
- **Cambio**: La respuesta ahora devuelve directamente el objeto `SyncResponse` (sin envolver en success/message/data)
- **Estructura**: `{ usuario_gestos: [...], docente_estudiante: [...] }`

## Archivos Creados

### 1. `registro.php`
- **Endpoint**: `POST /registro.php`
- **Descripción**: Alias de `register.php` para coincidir con `ApiService.kt`
- **Funcionalidad**: Registro de nuevos usuarios

### 2. `listar_gestos.php`
- **Endpoint**: `GET /listar_gestos.php`
- **Descripción**: Lista todos los gestos disponibles
- **Respuesta**: `{ success, message, gestos }`

### 3. `obtener_gestos_usuario.php`
- **Endpoint**: `GET /obtener_gestos_usuario.php`
- **Parámetros**: `id_usuario` o `usuario_id` (opcional)
- **Descripción**: Obtiene el progreso de gestos de un usuario
- **Respuesta**: Array de `UsuarioGestoResponse`

### 4. `obtener_home_data.php`
- **Endpoint**: `GET /obtener_home_data.php`
- **Parámetros**: `id_usuario`, `usuario_id` (opcional), `categoria` (opcional)
- **Descripción**: Obtiene datos para la pantalla Home
- **Respuesta**: `{ success, message, usuario, estadisticas, actividades, categorias }`

### 5. `obtener_progreso_usuarios.php`
- **Endpoint**: `GET /obtener_progreso_usuarios.php`
- **Parámetros**: `id_usuario`, `usuario_id`, `id_admin`, `id_estudiante` (opcionales)
- **Descripción**: Obtiene progreso detallado de usuarios
- **Respuesta**: `ProgresoResponse`

### 6. `actualizar_progreso_gesto.php`
- **Endpoint**: `POST /actualizar_progreso_gesto.php`
- **Body**: `ActualizarProgresoRequest`
- **Descripción**: Actualiza o crea el progreso de un gesto para un usuario
- **Respuesta**: `ApiResponse<UsuarioGestoResponse>`

### 7. `sync_progreso.php`
- **Endpoint**: `POST /sync_progreso.php`
- **Body**: `SyncProgresoRequest`
- **Descripción**: Sincroniza un único registro de progreso
- **Respuesta**: `ApiResponse<Unit>`

### 8. `enviar_solicitud_docente.php`
- **Endpoint**: `POST /enviar_solicitud_docente.php`
- **Body**: `EnviarSolicitudRequest`
- **Descripción**: Envía una solicitud de relación docente-estudiante
- **Respuesta**: `ApiResponse<Unit>`

### 9. `responder_solicitud.php`
- **Endpoint**: `POST /responder_solicitud.php`
- **Body**: `ResponderSolicitudRequest`
- **Descripción**: Responde una solicitud (aceptar/rechazar)
- **Respuesta**: `ApiResponse<Unit>`

### 10. `consultar_solicitud_estudiante.php`
- **Endpoint**: `GET /consultar_solicitud_estudiante.php`
- **Parámetros**: `id_estudiante`, `estudiante_id`, `id_usuario`, `usuario_id` (opcionales)
- **Descripción**: Consulta las solicitudes de un estudiante
- **Respuesta**: `SolicitudesResponse`

### 11. `listar_solicitudes_docente.php`
- **Endpoint**: `GET /listar_solicitudes_docente.php`
- **Parámetros**: `id_docente`, `docente_id`, `id_usuario`, `usuario_id`, `estado` (opcionales)
- **Descripción**: Lista las solicitudes de un docente
- **Respuesta**: `SolicitudesResponse`

### 12. `eliminar_relacion_docente.php`
- **Endpoint**: `POST /eliminar_relacion_docente.php`
- **Body**: `EliminarRelacionRequest`
- **Descripción**: Elimina una relación docente-estudiante
- **Respuesta**: `ApiResponse<Unit>`

### 13. `obtener_progreso_estudiante_docente.php`
- **Endpoint**: `GET /obtener_progreso_estudiante_docente.php`
- **Parámetros**: `id_docente`, `docente_id`, `id_usuario`, `usuario_id`, `id_estudiante`, `estudiante_id` (opcionales)
- **Descripción**: Obtiene el progreso de un estudiante para su docente
- **Respuesta**: `ProgresoResponse`

### 14. `obtener_logros_estudiante_docente.php`
- **Endpoint**: `GET /obtener_logros_estudiante_docente.php`
- **Parámetros**: `id_docente`, `docente_id`, `id_usuario`, `usuario_id`, `id_estudiante`, `estudiante_id` (opcionales)
- **Descripción**: Obtiene los logros de un estudiante para su docente
- **Respuesta**: `LogrosResponse`

### 15. `obtener_logros_usuarios.php`
- **Endpoint**: `GET /obtener_logros_usuarios.php`
- **Parámetros**: `id_usuario`, `usuario_id`, `id_admin`, `id_estudiante` (opcionales)
- **Descripción**: Obtiene los logros de usuarios
- **Respuesta**: `List<LogrosResponse>`

### 16. `listar_docentes.php`
- **Endpoint**: `GET /listar_docentes.php`
- **Descripción**: Lista todos los docentes
- **Respuesta**: `List<UsuarioResponse>`

### 17. `buscar_estudiante.php`
- **Endpoint**: `GET /buscar_estudiante.php`
- **Parámetros**: `busqueda` o `correo` (requerido)
- **Descripción**: Busca estudiantes por nombre o correo
- **Respuesta**: `List<UsuarioResponse>`

### 18. `listar_estudiantes_docente.php`
- **Endpoint**: `GET /listar_estudiantes_docente.php`
- **Parámetros**: `id_docente`, `docente_id`, `usuario_id` (opcionales)
- **Descripción**: Lista los estudiantes de un docente (solo relaciones aceptadas)
- **Respuesta**: `List<UsuarioResponse>`

## Características Implementadas

### Flexibilidad en Parámetros
Todos los endpoints aceptan múltiples nombres de parámetros para mayor compatibilidad:
- `id_usuario` / `usuario_id`
- `id_docente` / `docente_id`
- `id_estudiante` / `estudiante_id`

### Validación de Permisos
- Todos los endpoints validan los permisos según el rol del usuario
- Los estudiantes solo pueden ver/modificar sus propios datos
- Los docentes pueden ver los datos de sus estudiantes
- Los administradores tienen acceso completo

### Manejo de Errores
- Códigos HTTP apropiados (400, 401, 403, 404, 409, 500)
- Mensajes de error descriptivos
- Validación de datos de entrada

### Estructura de Respuestas
- Todas las respuestas coinciden exactamente con los modelos de Kotlin
- Uso de `JSON_UNESCAPED_UNICODE` para caracteres especiales
- Tipos de datos correctos (int, string, etc.)

## Notas Importantes

1. **Logros**: Los endpoints de logros (`obtener_logros_*`) actualmente generan logros simulados basados en el progreso. En una implementación real, deberías tener una tabla `logros` en la base de datos.

2. **Sincronización**: El endpoint `sync.php` mantiene la funcionalidad original de sincronización masiva, mientras que `sync_progreso.php` sincroniza un único registro.

3. **Compatibilidad**: Se mantienen los archivos originales (`register.php`, `gestos.php`, `usuario_gestos.php`) para compatibilidad, pero se recomienda usar los nuevos nombres que coinciden con `ApiService.kt`.

4. **Base de Datos**: Todos los endpoints asumen la estructura de base de datos existente. No se requieren cambios en el esquema.

## Próximos Pasos

1. Probar todos los endpoints con la aplicación Android
2. Verificar que las respuestas se deserialicen correctamente en Kotlin
3. Ajustar los endpoints de logros si decides implementar una tabla de logros real
4. Configurar CORS adecuadamente para producción
5. Cambiar el `JWT_SECRET` en `config.php` por una clave segura




