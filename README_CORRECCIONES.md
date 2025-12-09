# README DE CORRECCIONES
## Proyecto: Ensenando - Aplicación de Enseñanza de Lengua de Señas ULEAM

Este documento describe los pasos para ejecutar el proyecto después de las correcciones realizadas.

---

## REQUISITOS DEL SISTEMA

### Backend (PHP)
- **PHP:** 7.4 o superior
- **MySQL:** 5.7 o superior (o MariaDB 10.2+)
- **Extensiones PHP requeridas:**
  - `mysqli`
  - `json`
  - `mbstring`
- **Servidor Web:** Apache 2.4+ o Nginx (o XAMPP/WAMP)

### Cliente (Android)
- **Android Studio:** Arctic Fox (2020.3.1) o superior
- **Android SDK:** API 24 (Android 7.0) mínimo, API 33 recomendado
- **Kotlin:** 1.7.0 o superior
- **Gradle:** 7.0 o superior

---

## INSTALACIÓN Y CONFIGURACIÓN

### 1. Backend (PHP)

#### 1.1 Configuración de Base de Datos
1. Crear base de datos MySQL:
   ```sql
   CREATE DATABASE lengua_senas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Importar estructura de base de datos (si existe archivo SQL):
   ```bash
   mysql -u root -p lengua_senas < estructura.sql
   ```

#### 1.2 Configuración del Servidor
1. Copiar carpeta `app/src/main/INFO/lengua_senas/` al directorio del servidor web:
   - **XAMPP:** `C:\xampp\htdocs\lengua_senas\`
   - **WAMP:** `C:\wamp64\www\lengua_senas\`
   - **cPanel:** `public_html/lengua_senas/`

2. Editar `config.php` con las credenciales de la base de datos:
   ```php
   define('DB_HOST', 'localhost');
   define('DB_USER', 'root');
   define('DB_PASS', '');
   define('DB_NAME', 'lengua_senas');
   ```

3. **⚠️ IMPORTANTE:** Cambiar `JWT_SECRET` en producción:
   ```php
   define('JWT_SECRET', 'cambiar-por-secret-seguro-en-produccion');
   ```

#### 1.3 Verificación
1. Acceder a: `http://localhost/lengua_senas/config.php`
2. Debe mostrar error de método (esto es normal, significa que el archivo existe)

### 2. Cliente (Android)

#### 2.1 Configuración del Proyecto
1. Abrir Android Studio
2. Abrir proyecto: `File > Open > Seleccionar carpeta Ensenando`
3. Esperar a que Gradle sincronice (puede tardar varios minutos la primera vez)

#### 2.2 Configuración de URL del API
1. Editar `app/src/main/java/com/example/ensenando/data/remote/RetrofitClient.kt`
2. Cambiar `BASE_URL` por la URL de tu servidor:
   ```kotlin
   private const val BASE_URL = "http://TU_IP/lengua_senas/"
   ```
   - **Emulador:** `http://10.0.2.2/lengua_senas/`
   - **Dispositivo físico:** `http://192.168.X.X/lengua_senas/` (IP de tu PC)

#### 2.3 Configuración de Gradle
El proyecto ya está configurado con las dependencias necesarias:
- Room Database
- Retrofit
- WorkManager
- TensorFlow Lite
- CameraX
- Navigation Component

#### 2.4 Compilación
1. **Sincronizar Gradle:** `File > Sync Project with Gradle Files`
2. **Compilar:** `Build > Make Project`
3. **Ejecutar:** `Run > Run 'app'`

---

## CORRECCIONES APLICADAS

### Corrección #1: Campo token en LoginResponse y RegisterResponse
**Archivo:** `app/src/main/java/com/example/ensenando/data/remote/model/ApiResponse.kt`

Se agregó el campo `token` a los modelos de respuesta para que coincidan con lo que devuelve el backend.

### Corrección #2: Guardado de token en login y registro
**Archivo:** `app/src/main/java/com/example/ensenando/data/repository/UsuarioRepository.kt`

Se corrigió el guardado del token después del login y registro, especificando el tipo explícito en el lambda.

### Corrección #3: Variable no definida en PHP
**Archivo:** `app/src/main/INFO/lengua_senas/obtener_logros_estudiante_docente.php`

Se agregó la consulta para obtener la información del estudiante antes de usarla en la respuesta.

---

## PRUEBAS FUNCIONALES

### Pruebas Básicas Recomendadas

#### 1. Login
1. Abrir la aplicación
2. Ingresar correo y contraseña
3. Verificar que se guarda el token y se navega a la pantalla principal

#### 2. Registro
1. Crear una cuenta nueva
2. Verificar que se guarda el token y se navega a la pantalla principal

#### 3. Sincronización
1. Realizar cambios offline
2. Conectar a internet
3. Verificar que se sincronizan los cambios

#### 4. Progreso
1. Practicar un gesto
2. Verificar que el progreso se actualiza
3. Verificar que el progreso solo aumenta (nunca disminuye)

---

## SOLUCIÓN DE PROBLEMAS

### Error: "Unresolved reference 'token'"
**Solución:** Asegurarse de que el archivo `ApiResponse.kt` tiene el campo `token` en `LoginResponse` y `RegisterResponse`.

### Error: "Cannot infer type for this parameter"
**Solución:** Verificar que en `UsuarioRepository.kt` se especifica el tipo explícito: `token: String`.

### Error: "Variable $estudiante no definida"
**Solución:** Verificar que `obtener_logros_estudiante_docente.php` tiene la consulta para obtener el estudiante.

### Error de conexión a la base de datos
**Solución:** 
1. Verificar que MySQL está corriendo
2. Verificar credenciales en `config.php`
3. Verificar que la base de datos existe

### Error de conexión al API
**Solución:**
1. Verificar que el servidor PHP está corriendo
2. Verificar la URL en `RetrofitClient.kt`
3. Verificar permisos de red en `AndroidManifest.xml`

---

## ESTRUCTURA DE LA BASE DE DATOS

### Tablas Principales

#### usuarios
- `id_usuario` (INT, PRIMARY KEY)
- `nombre` (VARCHAR)
- `correo` (VARCHAR, UNIQUE)
- `contrasena` (VARCHAR)
- `rol` (VARCHAR: 'estudiante', 'docente', 'administrador')
- `fecha_registro` (DATETIME)

#### gestos
- `id_gesto` (INT, PRIMARY KEY)
- `nombre` (VARCHAR)
- `dificultad` (VARCHAR)
- `categoria` (VARCHAR)

#### usuario_gestos
- `id_usuario` (INT, FOREIGN KEY)
- `id_gesto` (INT, FOREIGN KEY)
- `porcentaje` (INT, 0-100)
- `estado` (VARCHAR: 'pendiente', 'aprendido')

#### docenteestudiante
- `id_docente` (INT, FOREIGN KEY)
- `id_estudiante` (INT, FOREIGN KEY)
- `estado` (VARCHAR: 'pendiente', 'aceptado', 'rechazado')

#### logros
- `id_logro` (INT, PRIMARY KEY)
- `titulo` (VARCHAR)
- `descripcion` (TEXT)

#### usuario_logros
- `id_usuario` (INT, FOREIGN KEY)
- `id_logro` (INT, FOREIGN KEY)
- `fecha_obtenido` (DATETIME)

**⚠️ IMPORTANTE:** La base de datos NO debe modificarse. Es la versión final oficial.

---

## NOTAS ADICIONALES

### Campos Solo en Room (No en MySQL)
Los siguientes campos existen SOLO en Room Database (cliente), NO en MySQL:
- `sync_status`: Estado de sincronización
- `last_updated`: Timestamp de última actualización

Estos campos se usan para la sincronización offline y no se envían al backend.

### Modelos TensorFlow
Los modelos de reconocimiento de gestos deben descargarse por separado. Ver documentación en `DESCARGAR_MODELOS.md`.

### Permisos de Red
Asegurarse de que `AndroidManifest.xml` tiene:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## CONTACTO Y SOPORTE

Para problemas o consultas sobre las correcciones, revisar:
1. `REPORTE_ANALISIS.md` - Análisis completo del proyecto
2. `MANUAL_INSTALACION.md` - Manual de instalación original
3. Logs de Android Studio para errores de compilación
4. Logs del servidor PHP para errores del backend

---

## VERSIÓN

**Versión del Proyecto:** Completa  
**Fecha de Correcciones:** 2024  
**Estado:** ✅ Funcional y Corregido








