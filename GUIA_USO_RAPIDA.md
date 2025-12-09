# Guía de Uso Rápida - Ensenando Lengua de Señas

## 📋 Pasos Previos

### 1. Configurar el Backend PHP

1. **Copiar archivos PHP al servidor:**
   - Copia la carpeta `app/src/main/INFO/lengua_senas/` a tu servidor web
   - Ejemplo: `C:\xampp\htdocs\api\` o `/var/www/html/api/`

2. **Configurar base de datos:**
   - Abre `config.php` en el servidor
   - Edita las credenciales:
   ```php
   define('DB_HOST', 'localhost');
   define('DB_USER', 'root');  // Tu usuario MySQL
   define('DB_PASS', '');      // Tu contraseña MySQL
   define('DB_NAME', 'lengua_senas');
   define('JWT_SECRET', 'cambiar-por-una-clave-secreta-segura-12345');
   ```

3. **Crear la base de datos:**
   - Abre phpMyAdmin o MySQL
   - Ejecuta el script SQL proporcionado para crear las tablas
   - O importa el archivo SQL directamente

### 2. Configurar la URL del API en la App

1. **Abrir el archivo:**
   - `app/src/main/java/com/example/ensenando/data/remote/RetrofitClient.kt`

2. **Cambiar la URL:**
   ```kotlin
   private const val BASE_URL = "http://tu-servidor.com/api/"
   // Ejemplo local: "http://10.0.2.2/api/" (para emulador)
   // Ejemplo local: "http://192.168.1.X/api/" (para dispositivo físico, reemplaza X con tu IP)
   // Ejemplo remoto: "https://tu-dominio.com/api/"
   ```

   **Para probar localmente:**
   - **Emulador Android:** `http://10.0.2.2/api/`
   - **Dispositivo físico:** `http://TU_IP_LOCAL/api/` (ej: `http://192.168.1.100/api/`)
     - Para saber tu IP: `ipconfig` (Windows) o `ifconfig` (Linux/Mac)

### 3. Configurar el Modelo TensorFlow Lite (Opcional)

1. **Copiar el modelo:**
   - El archivo `modelo_lsp.tflite` debe estar en:
   - `app/src/main/assets/INFO/modelo_lsp.tflite`
   - Si no existe la carpeta `assets`, créala

2. **Si no tienes el modelo aún:**
   - La app funcionará pero el reconocimiento de gestos no estará disponible
   - Puedes usar la app para ver videos y gestionar progreso

### 4. Agregar Videos de Gestos (Opcional)

1. **Ubicación:**
   - Los videos deben estar en `app/src/main/res/raw/`
   - Nombre del archivo: nombre del gesto en `snake_case`
   - Ejemplo: `aprender.mp4`, `calcular.mp4`, `hola.mp4`

2. **Formato:**
   - MP4 recomendado
   - Resolución: 480p o 720p
   - Duración: 2-5 segundos por gesto

## 🚀 Ejecutar la Aplicación

### Opción 1: Desde Android Studio

1. **Conectar dispositivo o iniciar emulador:**
   - Conecta tu dispositivo Android por USB
   - O inicia un emulador desde AVD Manager

2. **Ejecutar:**
   - Click en el botón ▶️ (Run)
   - O presiona `Shift + F10`
   - Selecciona tu dispositivo/emulador

### Opción 2: Instalar APK directamente

1. **Generar APK:**
   - Build > Build Bundle(s) / APK(s) > Build APK(s)
   - El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

2. **Instalar en dispositivo:**
   - Transfiere el APK al dispositivo
   - Abre el archivo e instala
   - Permite "Instalar desde fuentes desconocidas" si es necesario

## 📱 Uso de la Aplicación

### Primera Vez

1. **Pantalla de Bienvenida:**
   - Verás el logo ULEAM
   - Click en "Registrarse" o "Iniciar Sesión"

2. **Registrarse:**
   - Completa: Nombre, Correo, Contraseña
   - Selecciona Rol: Estudiante, Docente o Administrador
   - Click en "Registrarse"

3. **Iniciar Sesión:**
   - Ingresa correo y contraseña
   - Click en "Iniciar Sesión"

### Pantalla Principal (Home)

1. **Ver Módulos:**
   - Se muestran los módulos: Básico, Social, Académico
   - Cada módulo tiene submódulos
   - Cada submódulo tiene gestos

2. **Resumen de Progreso:**
   - Total de gestos
   - Gestos aprendidos
   - Promedio de progreso

3. **Navegar:**
   - Click en un módulo → se expanden submódulos
   - Click en un gesto → abre la actividad

### Practicar un Gesto

1. **Pantalla de Actividad:**
   - Verás el video del gesto
   - Barra de progreso actual
   - Botón "Practicar Gesto"

2. **Practicar:**
   - Click en "Practicar Gesto"
   - Se abre la cámara
   - Realiza el gesto frente a la cámara
   - El progreso aumenta automáticamente cuando reconoces el gesto correctamente

3. **Guardar Progreso:**
   - El progreso se guarda automáticamente
   - Cierra la cámara para volver

### Perfil

1. **Ver Información:**
   - Tu nombre, correo y rol
   - Solicitudes de docentes (si eres estudiante)

2. **Gestionar Solicitudes:**
   - Si eres estudiante, puedes aceptar/rechazar solicitudes de docentes
   - Los docentes pueden ver reportes de estudiantes que aceptaron

3. **Descargar Reporte:**
   - Click en "Descargar Reporte PDF"
   - El reporte se descarga y se abre con una app externa

### Funciones por Rol

#### Estudiante
- Ver y practicar gestos
- Gestionar solicitudes de docentes
- Ver su propio progreso
- Descargar su reporte

#### Docente
- Todo lo de Estudiante +
- Ver reportes de estudiantes que lo aceptaron
- Solicitar acceso a reportes de estudiantes

#### Administrador
- Todo lo de Docente +
- Ver todos los reportes sin permiso
- Resetear actividades de cualquier usuario
- Eliminar relaciones docente-estudiante

## 🔧 Solución de Problemas

### Error de Conexión
- Verifica que el servidor PHP esté funcionando
- Verifica la URL en `RetrofitClient.kt`
- Verifica que el dispositivo/emulador tenga internet
- Para emulador: usa `10.0.2.2` en lugar de `localhost`

### Error de Base de Datos
- Verifica credenciales en `config.php`
- Verifica que la BD esté creada
- Verifica que las tablas existan

### La App se Cierra
- Revisa Logcat en Android Studio
- Verifica permisos de cámara (si usas reconocimiento)
- Verifica que el modelo TFLite esté en assets (si usas reconocimiento)

### No Sincroniza
- Verifica conexión a internet
- Verifica que el token JWT sea válido
- La app funciona offline, sincroniza cuando hay conexión

## 📝 Notas Importantes

1. **Offline-First:**
   - La app funciona sin internet
   - Los datos se guardan localmente
   - Se sincronizan automáticamente cuando hay conexión

2. **Sincronización:**
   - Se ejecuta automáticamente cada 15 minutos
   - También se ejecuta al abrir la app si hay cambios pendientes

3. **Progreso Incremental:**
   - El progreso solo aumenta, nunca disminuye
   - Se actualiza cuando reconoces el gesto correctamente

4. **Videos:**
   - Si no hay videos, la app funcionará pero no se mostrarán
   - Agrega videos según los nombres de los gestos en la BD

## 🎯 Próximos Pasos

1. **Personalizar:**
   - Agregar logo ULEAM real
   - Agregar videos de gestos
   - Configurar colores si es necesario

2. **Mejorar:**
   - Integrar modelo TensorFlow Lite real para reconocimiento
   - Agregar más funcionalidades según necesidades

3. **Desplegar:**
   - Generar APK de release
   - Firmar la aplicación
   - Publicar en Google Play Store (opcional)

¡Listo! Ya puedes usar la aplicación. 🎉


