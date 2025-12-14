# Compatibilidad Base de Datos: MySQL ↔ Room

## Resumen
Este documento verifica que las entidades de Room son compatibles con la estructura de la base de datos MySQL.

## Estructura de Tablas

### 1. Tabla `usuarios`

**MySQL:**
```sql
CREATE TABLE usuarios (
  id_usuario int(11) NOT NULL,
  nombre varchar(100) NOT NULL,
  correo varchar(100) NOT NULL,
  contrasena varchar(255) DEFAULT NULL,
  rol enum('administrador','docente','estudiante') NOT NULL,
  fecha_registro timestamp NOT NULL DEFAULT current_timestamp()
)
```

**Room (UsuarioEntity):**
- ✅ `id_usuario` → `idUsuario: Int`
- ✅ `nombre` → `nombre: String`
- ✅ `correo` → `correo: String`
- ✅ `contrasena` → `contrasena: String?`
- ✅ `rol` → `rol: String`
- ✅ `fecha_registro` → `fechaRegistro: String` (convertido a String para Room)
- 🔵 `sync_status` → Solo Room (NO en MySQL)
- 🔵 `last_updated` → Solo Room (NO en MySQL)

**Sincronización:** Los campos `sync_status` y `last_updated` NO se envían al servidor.

---

### 2. Tabla `gestos`

**MySQL:**
```sql
CREATE TABLE gestos (
  id_gesto int(11) NOT NULL,
  nombre varchar(50) NOT NULL,
  dificultad enum('baja','media','alta') DEFAULT NULL,
  categoria varchar(50) DEFAULT NULL
)
```

**Room (GestoEntity):**
- ✅ `id_gesto` → `idGesto: Int`
- ✅ `nombre` → `nombre: String`
- ✅ `dificultad` → `dificultad: String?`
- ✅ `categoria` → `categoria: String?`
- 🔵 `sync_status` → Solo Room (NO en MySQL)
- 🔵 `last_updated` → Solo Room (NO en MySQL)

**Sincronización:** Los campos `sync_status` y `last_updated` NO se envían al servidor.

---

### 3. Tabla `usuario_gestos`

**MySQL:**
```sql
CREATE TABLE usuario_gestos (
  id_usuario int(11) NOT NULL,
  id_gesto int(11) NOT NULL,
  porcentaje int(11) DEFAULT 0,
  estado enum('pendiente','aprendido') DEFAULT 'pendiente'
)
```

**Room (UsuarioGestoEntity):**
- ✅ `id_usuario` → `idUsuario: Int`
- ✅ `id_gesto` → `idGesto: Int`
- ✅ `porcentaje` → `porcentaje: Int`
- ✅ `estado` → `estado: String`
- 🔵 `sync_status` → Solo Room (NO en MySQL)
- 🔵 `last_updated` → Solo Room (NO en MySQL)

**Sincronización:** Solo se envían `id_usuario`, `id_gesto`, `porcentaje`, `estado` al servidor.

---

### 4. Tabla `docenteestudiante`

**MySQL:**
```sql
CREATE TABLE docenteestudiante (
  id_docente int(11) NOT NULL,
  id_estudiante int(11) NOT NULL,
  estado enum('pendiente','aceptado','rechazado') DEFAULT 'pendiente'
)
```

**Room (DocenteEstudianteEntity):**
- ✅ `id_docente` → `idDocente: Int`
- ✅ `id_estudiante` → `idEstudiante: Int`
- ✅ `estado` → `estado: String`
- 🔵 `sync_status` → Solo Room (NO en MySQL)
- 🔵 `last_updated` → Solo Room (NO en MySQL)

**Sincronización:** Solo se envían `id_docente`, `id_estudiante`, `estado` al servidor.

---

### 5. Tabla `logros`

**MySQL:**
```sql
CREATE TABLE logros (
  id_logro int(11) NOT NULL,
  titulo varchar(50) DEFAULT NULL,
  descripcion text DEFAULT NULL
)
```

**Room (LogroEntity):**
- ✅ `id_logro` → `idLogro: Int`
- ✅ `titulo` → `titulo: String?`
- ✅ `descripcion` → `descripcion: String?`
- 🔵 `sync_status` → Solo Room (NO en MySQL)
- 🔵 `last_updated` → Solo Room (NO en MySQL)

**Sincronización:** Los logros se obtienen del servidor, no se sincronizan cambios locales.

---

### 6. Tabla `usuario_logros`

**MySQL:**
```sql
CREATE TABLE usuario_logros (
  id_usuario int(11) NOT NULL,
  id_logro int(11) NOT NULL,
  fecha_obtenido timestamp NOT NULL DEFAULT current_timestamp()
)
```

**Room (UsuarioLogroEntity):**
- ✅ `id_usuario` → `idUsuario: Int`
- ✅ `id_logro` → `idLogro: Int`
- ✅ `fecha_obtenido` → `fechaObtenido: String` (convertido a String para Room)
- 🔵 `sync_status` → Solo Room (NO en MySQL)
- 🔵 `last_updated` → Solo Room (NO en MySQL)

**Sincronización:** Se envía `id_usuario`, `id_logro`, `fecha_obtenido` al servidor cuando se desbloquea un logro.

---

## Campos Adicionales en Room

### ¿Por qué existen `sync_status` y `last_updated`?

Estos campos son **solo para Room** y permiten:

1. **`sync_status`**: 
   - `"pending"`: Cambio local pendiente de sincronizar
   - `"synced"`: Ya sincronizado con el servidor
   - `"error"`: Error al sincronizar (reintentar)

2. **`last_updated`**: 
   - Timestamp de última actualización local
   - Usado para resolver conflictos en sincronización

### ✅ Garantía de Compatibilidad

- **NO se envían al servidor**: Estos campos se filtran antes de enviar datos
- **NO existen en MySQL**: El servidor los ignora si se envían por error
- **Solo para Room**: Se usan para gestión offline y sincronización

---

## Verificación de Sincronización

### Endpoint: `sync.php`

**Campos enviados para `usuario_gestos`:**
```json
{
  "usuario_gestos": [
    {
      "id_usuario": 1,
      "id_gesto": 5,
      "porcentaje": 75,
      "estado": "aprendido"
    }
  ]
}
```

✅ **Correcto**: Solo se envían los campos que existen en MySQL.

---

## Conclusión

✅ **Todas las entidades de Room son compatibles con MySQL**
✅ **Los campos adicionales NO se envían al servidor**
✅ **La sincronización funciona correctamente**
✅ **No hay conflictos de estructura**

Los campos `sync_status` y `last_updated` son una **característica de Room** para soporte offline y no afectan la compatibilidad con MySQL.
