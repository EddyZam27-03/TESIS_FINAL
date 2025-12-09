# Análisis de Endpoints Existentes vs App

## 📊 Comparación de Estructura

### ✅ Endpoints que coinciden:
- `login.php` - Login (sin JWT)
- `registro.php` - Registro (sin JWT)
- `listar_gestos.php` - Listar gestos
- `actualizar_progreso_gesto.php` - Actualizar progreso
- `sync_progreso.php` - Sincronizar progreso

### ➕ Endpoints que debo AGREGAR a la app:
1. `obtener_home_data.php` - Datos completos del home (estadísticas, actividades, categorías)
2. `obtener_progreso_usuarios.php` - Estadísticas de progreso
3. `obtener_logros_usuarios.php` - Logros del usuario
4. `consultar_solicitud_estudiante.php` - Solicitudes para estudiantes
5. `listar_solicitudes_docente.php` - Solicitudes para docentes
6. `obtener_progreso_estudiante_docente.php` - Progreso para docentes
7. `obtener_logros_estudiante_docente.php` - Logros para docentes
8. `listar_docentes.php` - Listar docentes disponibles
9. `buscar_estudiante.php` - Buscar estudiantes
10. `eliminar_relacion_docente.php` - Eliminar relación

### 🔄 Cambios necesarios:

1. **Eliminar JWT**: Los endpoints NO usan autenticación JWT
2. **Estructura de respuestas**: Diferentes formatos según endpoint
3. **Sincronización**: `sync_progreso.php` usa formato diferente
4. **Home**: Usar `obtener_home_data.php` en lugar de múltiples llamadas

## 🎯 Plan de Ajustes

1. Actualizar modelos de respuesta para coincidir con estructura real
2. Eliminar JWT de toda la app
3. Agregar nuevos endpoints al ApiService
4. Actualizar repositorios para usar endpoints correctos
5. Agregar funcionalidad de logros
6. Mejorar home con endpoint dedicado


