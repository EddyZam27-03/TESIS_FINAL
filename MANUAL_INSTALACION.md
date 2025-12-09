# Manual de Instalación - Ensenando Lengua de Señas ULEAM

## Requisitos Previos

### Android Studio
- Android Studio Hedgehog o superior
- Android SDK 29 (Android 10) o superior
- Gradle 8.0 o superior

### Servidor Backend
- PHP 7.4 o superior
- MySQL 5.7 o superior
- Servidor web (Apache/Nginx)
- Extensiones PHP: mysqli, json, hash

## Instalación del Backend

1. **Configurar Base de Datos**
   ```sql
   -- Ejecutar el script SQL proporcionado para crear la base de datos
   mysql -u root -p < database.sql
   ```

2. **Configurar PHP**
   - Copiar la carpeta `app/src/main/INFO/lengua_senas/` al servidor web
   - Editar `config.php` con las credenciales de la base de datos:
     ```php
     define('DB_HOST', 'localhost');
     define('DB_USER', 'tu_usuario');
     define('DB_PASS', 'tu_contraseña');
     define('DB_NAME', 'lengua_senas');
     define('JWT_SECRET', 'clave-secreta-segura');
     ```

3. **Configurar URL del API**
   - Editar `app/src/main/java/com/example/ensenando/data/remote/RetrofitClient.kt`
   - Cambiar `BASE_URL` por la URL de tu servidor:
     ```kotlin
     private const val BASE_URL = "https://tu-servidor.com/api/"
     ```

## Instalación de la Aplicación Android

1. **Abrir el Proyecto**
   - Abrir Android Studio
   - Seleccionar "Open an Existing Project"
   - Navegar a la carpeta del proyecto

2. **Sincronizar Dependencias**
   - Android Studio descargará automáticamente las dependencias
   - Si hay errores, ejecutar: `File > Sync Project with Gradle Files`

3. **Configurar Firebase (Opcional)**
   - Si se usa Firebase PhoneAuth, agregar `google-services.json` a `app/`
   - Si no, comentar el plugin en `app/build.gradle.kts`

4. **Copiar Modelo TensorFlow Lite**
   - El modelo `modelo_lsp.tflite` debe estar en `app/src/main/assets/INFO/`
   - Si no existe, copiarlo desde `app/src/main/INFO/modelo_lsp.tflite`

5. **Copiar Videos de Gestos**
   - Los videos deben estar en `app/src/main/res/raw/`
   - Nombre de archivo: `snake_case` basado en el nombre del gesto
   - Ejemplo: `aprender.mp4`, `calcular.mp4`, etc.

6. **Compilar y Ejecutar**
   - Conectar dispositivo Android o iniciar emulador
   - Ejecutar: `Run > Run 'app'`
   - O presionar `Shift + F10`

## Configuración Adicional

### Permisos
La aplicación solicitará automáticamente:
- Cámara (para reconocimiento de gestos)
- Internet (para sincronización)
- Almacenamiento (para guardar reportes)

### Primera Ejecución
1. La app mostrará la pantalla de bienvenida
2. Crear una cuenta o iniciar sesión
3. La app sincronizará los gestos desde el servidor
4. Comenzar a practicar gestos

## Solución de Problemas

### Error de Conexión
- Verificar que el servidor PHP esté funcionando
- Verificar la URL en `RetrofitClient.kt`
- Verificar permisos CORS en el servidor

### Error de Base de Datos
- Verificar credenciales en `config.php`
- Verificar que la base de datos esté creada
- Verificar que las tablas existan

### Error de TensorFlow Lite
- Verificar que el modelo esté en `assets/INFO/`
- Verificar que el modelo sea compatible con la versión de TensorFlow Lite

### Error de Compilación
- Limpiar proyecto: `Build > Clean Project`
- Reconstruir: `Build > Rebuild Project`
- Invalidar caché: `File > Invalidate Caches / Restart`

## Estructura del Proyecto

```
Ensenando/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/ensenando/
│   │   │   │   ├── data/          # Capa de datos
│   │   │   │   ├── ui/            # Interfaz de usuario
│   │   │   │   ├── ml/            # TensorFlow Lite
│   │   │   │   ├── work/          # WorkManager
│   │   │   │   └── util/          # Utilidades
│   │   │   ├── res/
│   │   │   │   ├── layout/        # Layouts XML
│   │   │   │   ├── values/        # Recursos
│   │   │   │   └── raw/           # Videos
│   │   │   └── INFO/
│   │   │       ├── lengua_senas/  # Backend PHP
│   │   │       └── modelo_lsp.tflite
│   │   └── test/                  # Pruebas
│   └── build.gradle.kts
├── gradle/
└── build.gradle.kts
```

## Contacto y Soporte

Para problemas o preguntas, contactar al equipo de desarrollo.


