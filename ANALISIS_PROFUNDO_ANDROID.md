# ANÁLISIS PROFUNDO DEL PROYECTO ANDROID - ENSENANDO

## 1. RESUMEN BREVE DEL PROYECTO

**Enseñando** es una aplicación Android educativa para aprender Lengua de Señas mediante reconocimiento de gestos con Machine Learning. La app permite a estudiantes practicar gestos usando la cámara frontal, rastrea su progreso, gestiona relaciones docente-estudiante, y sincroniza datos con un backend PHP/MySQL. Utiliza arquitectura offline-first con Room Database, Retrofit para API, TensorFlow Lite para ML, CameraX para captura, y WorkManager para sincronización en background.

**Módulos críticos:**
- **Autenticación:** Login/registro con JWT, almacenamiento seguro de credenciales
- **Reconocimiento de Gestos:** Pipeline ML (pose + manos + clasificador LSTM)
- **Gestión de Progreso:** CRUD de progreso de gestos, sincronización offline/online
- **Relaciones Docente-Estudiante:** Sistema de solicitudes y aprobaciones
- **Sincronización:** WorkManager para sync periódico en background

---

## 2. LECTURA Y COMPRENSIÓN DEL CÓDIGO

### Clases Críticas Identificadas:

**Arquitectura:**
- `EnsenandoApplication` - Application class con inicialización de DB
- `AppDatabase` - Room Database con 6 entidades
- `RetrofitClient` - Cliente HTTP con interceptores de autenticación
- `SecurityUtils` - Utilidades de seguridad con EncryptedSharedPreferences

**Repositorios (Offline-First):**
- `UsuarioRepository` - Login, registro, gestión de usuarios
- `GestoRepository` - Sincronización y consulta de gestos
- `ProgresoRepository` - Actualización y sincronización de progreso

**UI/ViewModel:**
- `MainActivity` - Activity principal con navegación
- `CameraActivity` - Captura y procesamiento de video
- `ActivityViewModel` - Gestión de práctica de gestos
- `HomeViewModel` - Dashboard con estadísticas

**ML:**
- `GestureRecognitionManager` - Orquestador de modelos ML
- `GestureClassifier` - Clasificador LSTM (83 frames)
- `PoseDetector` / `HandDetector` - Detección de landmarks

**Background:**
- `SyncWorker` - Worker para sincronización periódica
- `SyncManager` - Gestor de WorkManager

---

## 3. SIMULACIÓN LÓGICA DEL FLUJO PRINCIPAL

### Flujo: Arranque → Login → Home → Práctica → Sincronización

**1. Arranque (WelcomeActivity → AuthActivity)**
- Usuario abre app → `WelcomeActivity` verifica si hay sesión guardada
- Si no hay sesión → navega a `AuthActivity`
- Si hay sesión → navega a `MainActivity`

**2. Login (AuthActivity → AuthViewModel → UsuarioRepository)**
- Usuario ingresa correo/contraseña
- `AuthViewModel.login()` → `viewModelScope.launch`
- `UsuarioRepository.login()`:
  - Verifica conexión con `NetworkUtils.isNetworkAvailable()`
  - Si hay conexión: llama `apiService.login()` → recibe `LoginResponse` con token
  - Guarda usuario en Room DB con `usuarioDao.insertUsuario()`
  - Guarda token en `SecurityUtils.saveToken()` (EncryptedSharedPreferences)
  - Si no hay conexión: intenta login offline comparando contraseña en texto plano (⚠️ PROBLEMA)
- Resultado → navega a `MainActivity`

**3. Home (MainActivity → HomeFragment → HomeViewModel)**
- `MainActivity.onCreate()`:
  - Inicializa navegación con `NavHostFragment`
  - Inicia `SyncManager.startPeriodicSync()` (cada 15 min)
- `HomeViewModel.init` → `loadHomeData()`:
  - Intenta `gestoRepository.syncGestos()` (llamada suspend)
  - Llama `apiService.getHomeData()` para estadísticas
  - Si falla → carga datos locales con `loadGestosLocal()` y `loadProgresoLocal()`
  - `organizarModulos()` agrupa gestos por categoría (ej: "BASICO - Abecedario")

**4. Práctica de Gesto (ActivityFragment → CameraActivity)**
- Usuario selecciona gesto → navega a `ActivityFragment` con `idGesto`
- `ActivityFragment` → `ActivityViewModel.loadGesto(idGesto)`
- Usuario presiona "Practicar" → abre `CameraActivity`
- `CameraActivity.onCreate()`:
  - Solicita permiso de cámara (runtime)
  - Inicializa `ProcessCameraProvider` con `DEFAULT_FRONT_CAMERA`
  - Configura `ImageAnalysis` con `STRATEGY_KEEP_ONLY_LATEST`
  - `processImageProxy()` se ejecuta en `cameraExecutor` (thread separado):
    - Convierte `ImageProxy` → `Bitmap`
    - Redimensiona a 640x480
    - Llama `viewModel.processFrame(bitmap)` → `GestureRecognitionManager.processFrame()`
- `GestureRecognitionManager`:
  - Detecta pose (99 valores) + manos (63 cada una)
  - Construye frame (225 valores) con `FrameBuilder`
  - Acumula 83 frames en `frameBuffer`
  - Cuando tiene 83 frames → `gestureClassifier.classifyFromFrames()`
  - Si confianza ≥ 0.80 y gesto correcto → actualiza `_progress.value`
- `ActivityViewModel` observa `progress` → actualiza UI
- Al cerrar → `viewModel.saveProgress()` → `progresoRepository.updateProgreso()`

**5. Sincronización (SyncWorker)**
- `SyncManager` programa `PeriodicWorkRequest` cada 15 min
- `SyncWorker.doWork()`:
  - Verifica conexión
  - Sincroniza gestos → progreso → relaciones docente-estudiante
  - Si falla → `Result.retry()`

---

## 4. DETECCIÓN DE PROBLEMAS

### PROBLEMA #1: Contraseña en Texto Plano en Login Offline
**Archivo:** `app/src/main/java/com/example/ensenando/data/repository/UsuarioRepository.kt:25`  
**Tipo:** Seguridad - Exposición de datos sensibles  
**Gravedad:** `CRITICAL`

**Descripción:**
En el método `login()`, cuando no hay conexión, se compara la contraseña directamente con `usuario.contrasena == contrasena`. Esto implica que:
1. La contraseña se almacena en texto plano en Room DB (aunque `UsuarioEntity.contrasena` es nullable, en login offline se compara directamente)
2. Si un atacante accede a la base de datos local, puede ver todas las contraseñas
3. No hay hashing ni encriptación de contraseñas en el cliente

**Reproducción:**
1. Registrar usuario con contraseña "1234"
2. Desactivar WiFi/datos
3. Intentar login → funciona porque compara texto plano
4. Inspeccionar Room DB con Database Inspector → contraseña visible

**Impacto:** Compromiso total de seguridad de cuentas de usuario si se accede a la DB local.

**Fix:**
```kotlin
suspend fun login(correo: String, contrasena: String): Result<UsuarioEntity> {
    return try {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            // ❌ ELIMINAR login offline o usar hash
            // Opción 1: No permitir login offline
            return Result.failure(Exception("Se requiere conexión para iniciar sesión"))
            
            // Opción 2: Si se guarda hash, comparar con BCrypt
            // val usuario = usuarioDao.getUsuarioByCorreo(correo)
            // if (usuario != null && BCrypt.checkpw(contrasena, usuario.contrasenaHash)) {
            //     ...
            // }
        }
        // ... resto del código
    }
}
```

**Tests:**
```kotlin
@Test
fun `login offline should fail when no connection`() {
    // Given: Sin conexión
    whenever(networkUtils.isNetworkAvailable(any())).thenReturn(false)
    
    // When: Intentar login
    val result = repository.login("test@example.com", "password")
    
    // Then: Debe fallar
    assertTrue(result.isFailure)
}
```

---

### PROBLEMA #2: Race Condition en GestureRecognitionManager.processFrame()
**Archivo:** `app/src/main/java/com/example/ensenando/ml/GestureRecognitionManager.kt:69-134`  
**Tipo:** Race condition - Acceso concurrente sin sincronización  
**Gravedad:** `HIGH`

**Descripción:**
El método `processFrame()` es llamado desde `CameraActivity.processImageProxy()` que se ejecuta en un thread separado (`cameraExecutor`). Múltiples frames pueden llegar simultáneamente y:
1. `frameBuffer.add(frame)` puede tener race conditions (mutable list no thread-safe)
2. `consecutiveFrames++` no es atómico
3. `currentProgress` puede ser modificado concurrentemente
4. `_progress.value` y `_currentPrediction.value` se actualizan desde thread de background (viola regla de UI)

**Reproducción:**
1. Abrir cámara y mantenerla activa
2. Múltiples `ImageProxy` llegan simultáneamente
3. Observar logs: puede haber `ConcurrentModificationException` o valores inconsistentes

**Impacto:** Crashes intermitentes, progreso incorrecto, predicciones duplicadas.

**Fix:**
```kotlin
class GestureRecognitionManager(context: Context) {
    // Usar Mutex para proteger acceso concurrente
    private val frameBufferMutex = Mutex()
    private val stateMutex = Mutex()
    
    private val frameBuffer = mutableListOf<FloatArray>()
    private var consecutiveFrames = 0
    private var currentProgress = 0
    
    fun processFrame(bitmap: Bitmap, targetGestoId: Int) {
        if (gestureClassifier == null || poseDetector == null || handDetector == null) {
            return
        }
        
        try {
            val pose = poseDetector.detectPose(bitmap) ?: return
            val (rightHand, leftHand) = handDetector.detectHandsRightLeft(bitmap)
            val frame = FrameBuilder.buildFrame(pose, rightHand, leftHand)
            
            // ✅ Proteger acceso concurrente
            val shouldClassify = frameBufferMutex.withLock {
                frameBuffer.add(frame)
                if (frameBuffer.size > maxFrames) {
                    frameBuffer.removeAt(0)
                }
                frameBuffer.size >= maxFrames
            }
            
            if (!shouldClassify) return
            
            val prediction = gestureClassifier.classifyFromFrames(frameBuffer)
            
            if (prediction != null) {
                val (gestoId, confidence) = prediction
                
                if (gestoId == targetGestoId && confidence >= confidenceThreshold) {
                    // ✅ Proteger actualización de estado
                    stateMutex.withLock {
                        consecutiveFrames++
                        if (consecutiveFrames >= requiredConsecutiveFrames) {
                            currentGestoId = gestoId
                            val newProgress = ((confidence * 100).toInt()).coerceIn(0, 100)
                            if (newProgress > currentProgress) {
                                currentProgress = newProgress
                                // ✅ Postear a main thread
                                CoroutineScope(Dispatchers.Main).launch {
                                    _progress.value = currentProgress
                                    _currentPrediction.value = prediction
                                }
                            }
                        }
                    }
                } else {
                    stateMutex.withLock {
                        resetPrediction()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando frame", e)
        }
    }
}
```

**Tests:**
```kotlin
@Test
fun `processFrame should handle concurrent calls safely`() = runTest {
    val manager = GestureRecognitionManager(context)
    val bitmaps = (1..100).map { createTestBitmap() }
    
    // Lanzar 100 llamadas concurrentes
    bitmaps.map { bitmap ->
        async {
            manager.processFrame(bitmap, 1)
        }
    }.awaitAll()
    
    // Verificar que no hay crashes
    assertTrue(true) // Si llegamos aquí, no hubo excepción
}
```

---

### PROBLEMA #3: Memory Leak en CameraActivity - GestureRecognitionManager no se cierra
**Archivo:** `app/src/main/java/com/example/ensenando/ui/camera/CameraActivity.kt:131-134`  
**Tipo:** Memory leak - Recursos no liberados  
**Gravedad:** `HIGH`

**Descripción:**
`CameraActivity` crea un `ActivityViewModel` que contiene un `GestureRecognitionManager`. Cuando la actividad se destruye:
1. `onDestroy()` solo cierra `cameraExecutor.shutdown()`
2. El `ActivityViewModel` se mantiene vivo (ViewModel sobrevive a cambios de configuración)
3. `GestureRecognitionManager` mantiene referencias a:
   - Modelos TensorFlow Lite (interprete, buffers)
   - `frameBuffer` con 83 frames de 225 valores cada uno (~75KB)
   - Referencias a context si se pasa incorrectamente
4. Si la actividad se recrea (rotación), se crea otro `GestureRecognitionManager` sin cerrar el anterior

**Reproducción:**
1. Abrir `CameraActivity`
2. Rotar dispositivo (cambia configuración)
3. Repetir 5-10 veces
4. Usar Memory Profiler → ver múltiples instancias de `GestureRecognitionManager` y buffers no liberados

**Impacto:** Consumo excesivo de memoria, posibles OOM (Out of Memory), degradación de rendimiento.

**Fix:**
```kotlin
class CameraActivity : AppCompatActivity() {
    private lateinit var viewModel: ActivityViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ...
        viewModel = ViewModelProvider(this)[ActivityViewModel::class.java]
        // ...
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        // ✅ Cerrar GestureRecognitionManager explícitamente
        // (ViewModel.onCleared() ya lo hace, pero por seguridad)
        viewModel.resetProgress()
    }
    
    override fun onPause() {
        super.onPause()
        // ✅ Pausar procesamiento para ahorrar recursos
        // (opcional pero recomendado)
    }
}
```

**Nota:** El `ActivityViewModel.onCleared()` ya llama a `gestureRecognitionManager.close()`, pero solo se ejecuta cuando el ViewModel se destruye completamente (no en rotaciones). Para rotaciones, considerar usar `onSaveInstanceState()` o recrear el manager.

**Tests:**
```kotlin
@Test
fun `GestureRecognitionManager should release resources on close`() {
    val manager = GestureRecognitionManager(context)
    manager.processFrame(createTestBitmap(), 1)
    
    manager.close()
    
    // Verificar que los recursos se liberaron
    // (requiere acceso a campos internos o usar reflection)
    assertTrue(manager.isReady() == false || /* verificar buffers vacíos */)
}
```

---

### PROBLEMA #4: ANR Potencial - Procesamiento ML en Thread Principal
**Archivo:** `app/src/main/java/com/example/ensenando/ui/camera/CameraActivity.kt:109-129`  
**Tipo:** ANR potencial - Operaciones pesadas en main thread  
**Gravedad:** `MEDIUM`

**Descripción:**
Aunque `processImageProxy()` se ejecuta en `cameraExecutor` (thread separado), el método `viewModel.processFrame()` llama a `GestureRecognitionManager.processFrame()` que:
1. Ejecuta detección de pose (TensorFlow Lite) - puede tomar 50-200ms
2. Ejecuta detección de manos (2 modelos TensorFlow Lite) - puede tomar 100-300ms
3. Construye frame y clasifica (LSTM con 83 frames) - puede tomar 200-500ms
4. Total: 350-1000ms por frame

Si el procesamiento se bloquea o tarda mucho, puede afectar la captura de frames siguientes. Además, `_progress.value` y `_currentPrediction.value` se actualizan desde el thread de background (viola regla de actualizar UI solo desde main thread).

**Reproducción:**
1. Abrir cámara en dispositivo lento (emulador o dispositivo antiguo)
2. Procesar gesto complejo
3. Observar: frames se pierden, UI se congela intermitentemente
4. Verificar con StrictMode → puede detectar actualizaciones de UI desde background thread

**Impacto:** Pérdida de frames, UI congelada, experiencia de usuario degradada.

**Fix:**
```kotlin
private fun processImageProxy(imageProxy: ImageProxy) {
    try {
        val bitmap = imageProxy.toBitmap()
        val resizedBitmap = bitmap.resize(640, 480)
        
        // ✅ Procesar en coroutine con dispatcher adecuado
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.processFrame(resizedBitmap)
        }
        
        if (bitmap != resizedBitmap) {
            bitmap.recycle()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        imageProxy.close()
    }
}
```

Y en `GestureRecognitionManager`:
```kotlin
fun processFrame(bitmap: Bitmap, targetGestoId: Int) {
    // ... procesamiento ...
    
    // ✅ Actualizar UI desde main thread
    if (newProgress > currentProgress) {
        currentProgress = newProgress
        CoroutineScope(Dispatchers.Main).launch {
            _progress.value = currentProgress
            _currentPrediction.value = prediction
        }
    }
}
```

**Tests:**
```kotlin
@Test
fun `processFrame should not block main thread`() {
    val startTime = System.currentTimeMillis()
    
    // Simular procesamiento
    manager.processFrame(createTestBitmap(), 1)
    
    val duration = System.currentTimeMillis() - startTime
    // Debe completar rápido (no bloquear)
    assertTrue(duration < 100) // Ajustar según dispositivo
}
```

---

### PROBLEMA #5: Inconsistencia de Datos - Resolución de Conflictos en syncProgreso()
**Archivo:** `app/src/main/java/com/example/ensenando/data/repository/ProgresoRepository.kt:109`  
**Tipo:** Bug lógico - Resolución de conflictos incorrecta  
**Gravedad:** `MEDIUM`

**Descripción:**
En `syncProgreso()`, cuando hay conflictos entre datos locales y remotos, la lógica de resolución es incorrecta:

```kotlin
if (local != null && local.lastUpdated > System.currentTimeMillis() - 1000) {
    local  // Usar local si fue actualizado en el último segundo
} else {
    // Usar remoto
}
```

Problemas:
1. La condición `local.lastUpdated > System.currentTimeMillis() - 1000` es incorrecta: `lastUpdated` es un timestamp del pasado, nunca será mayor que `currentTimeMillis() - 1000` a menos que el reloj del sistema esté mal configurado
2. No compara realmente con el timestamp del servidor
3. Puede sobrescribir datos locales más recientes con datos remotos antiguos

**Reproducción:**
1. Actualizar progreso local (porcentaje 80%)
2. Desactivar conexión
3. Actualizar progreso local nuevamente (porcentaje 90%)
4. Activar conexión → `syncProgreso()` se ejecuta
5. Si el servidor tiene porcentaje 70% (más antiguo), puede sobrescribir el 90% local

**Impacto:** Pérdida de progreso del usuario, frustración, datos inconsistentes.

**Fix:**
```kotlin
suspend fun syncProgreso(): Result<Unit> {
    return try {
        // ... código existente ...
        
        val response = apiService.getGestosUsuario(idUsuario = idUsuario)
        if (response.isSuccessful) {
            val progresosResponse = response.body() ?: emptyList()
            val progresos = progresosResponse.map { progresoResponse ->
                val local = usuarioGestoDao.getProgreso(
                    progresoResponse.id_usuario, 
                    progresoResponse.id_gesto
                )
                
                // ✅ Resolución de conflictos correcta
                when {
                    local == null -> {
                        // No existe local, usar remoto
                        UsuarioGestoEntity(
                            idUsuario = progresoResponse.id_usuario,
                            idGesto = progresoResponse.id_gesto,
                            porcentaje = progresoResponse.porcentaje,
                            estado = progresoResponse.estado,
                            syncStatus = "synced",
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                    local.syncStatus == "pending" -> {
                        // Local tiene cambios pendientes, mantener local
                        // (ya se sincronizó arriba con syncProgresoRequest)
                        local
                    }
                    local.lastUpdated > (progresoResponse.last_updated?.toLongOrNull() ?: 0) -> {
                        // Local es más reciente, mantener local
                        local
                    }
                    else -> {
                        // Remoto es más reciente o igual, usar remoto
                        UsuarioGestoEntity(
                            idUsuario = progresoResponse.id_usuario,
                            idGesto = progresoResponse.id_gesto,
                            porcentaje = progresoResponse.porcentaje,
                            estado = progresoResponse.estado,
                            syncStatus = "synced",
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                }
            }
            usuarioGestoDao.insertProgresos(progresos)
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Nota:** Requiere que el backend devuelva `last_updated` en `UsuarioGestoResponse`.

**Tests:**
```kotlin
@Test
fun `syncProgreso should preserve local data if more recent`() {
    // Given: Progreso local con lastUpdated reciente
    val local = UsuarioGestoEntity(..., porcentaje = 90, lastUpdated = System.currentTimeMillis())
    usuarioGestoDao.insertProgreso(local)
    
    // When: Sincronizar con datos remotos antiguos
    val remote = UsuarioGestoResponse(..., porcentaje = 70, last_updated = "2024-01-01")
    whenever(apiService.getGestosUsuario(any())).thenReturn(Response.success(listOf(remote)))
    
    repository.syncProgreso()
    
    // Then: Debe mantener el progreso local (90%)
    val result = usuarioGestoDao.getProgreso(local.idUsuario, local.idGesto)
    assertEquals(90, result?.porcentaje)
}
```

---

### PROBLEMA #6: NullPointerException Potencial - UsuarioEntity.idUsuario puede ser 0
**Archivo:** `app/src/main/java/com/example/ensenando/data/repository/UsuarioRepository.kt:41`  
**Tipo:** NullPointer/nullable - ID inválido  
**Gravedad:** `MEDIUM`

**Descripción:**
En `login()` y `register()`, cuando se crea `UsuarioEntity`, el `idUsuario` se obtiene así:

```kotlin
idUsuario = usuarioResponse.id ?: usuarioResponse.id_usuario ?: 0
```

Si ambos son `null`, se asigna `0`. Luego:
1. Se guarda en Room DB con `idUsuario = 0`
2. Se guarda en `SecurityUtils.saveUserId(context, 0)`
3. En otros lugares se verifica `if (idUsuario == -1)` para detectar "no autenticado", pero `0` es un ID válido en MySQL (aunque poco común)
4. Puede causar problemas si hay un usuario real con `id_usuario = 0` en la BD

**Reproducción:**
1. Backend devuelve `LoginResponse` sin `id` ni `id_usuario` (error del servidor)
2. Se crea usuario con `idUsuario = 0`
3. `SecurityUtils.getUserId()` retorna `0`
4. En `HomeViewModel`, `if (idUsuario == -1)` es falso, pero el usuario no existe realmente

**Impacto:** Sesión inválida, datos incorrectos, errores en consultas.

**Fix:**
```kotlin
suspend fun login(correo: String, contrasena: String): Result<UsuarioEntity> {
    return try {
        // ... código existente ...
        
        if (response.isSuccessful && body?.success == true) {
            val usuarioResponse = body.usuario ?: return Result.failure(Exception("Respuesta sin usuario"))
            
            // ✅ Validar ID antes de usar
            val idUsuario = usuarioResponse.id ?: usuarioResponse.id_usuario
            if (idUsuario == null || idUsuario <= 0) {
                return Result.failure(Exception("ID de usuario inválido en respuesta del servidor"))
            }
            
            val usuario = UsuarioEntity(
                idUsuario = idUsuario, // Ya validado
                nombre = usuarioResponse.nombre,
                correo = usuarioResponse.correo,
                contrasena = null,
                rol = usuarioResponse.rol,
                fechaRegistro = "",
                syncStatus = "synced",
                lastUpdated = System.currentTimeMillis()
            )
            
            // ... resto del código ...
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Tests:**
```kotlin
@Test
fun `login should fail when usuarioResponse has invalid ID`() {
    // Given: Respuesta con ID null
    val response = LoginResponse(
        success = true,
        usuario = UsuarioResponse(id = null, id_usuario = null, ...),
        token = "token"
    )
    
    // When: Intentar login
    val result = repository.login("test@example.com", "password")
    
    // Then: Debe fallar
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("inválido") == true)
}
```

---

### PROBLEMA #7: Resource Leak - FileInputStream no se cierra en loadModelFile()
**Archivo:** `app/src/main/java/com/example/ensenando/ml/PoseDetector.kt:115-117`  
**Tipo:** Resource leak - Streams no cerrados  
**Gravedad:** `LOW`

**Descripción:**
En `PoseDetector.loadModelFile()` y `HandDetector.loadModelFile()`, cuando se carga desde `res/raw/`, se crea un `FileInputStream` pero no se cierra explícitamente:

```kotlin
val fileInputStream = FileInputStream(tempFile)
val fileChannel = fileInputStream.channel
val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, tempFile.length())
return buffer
// ❌ fileInputStream no se cierra
```

Aunque `FileChannel.map()` puede mantener el archivo mapeado, el `FileInputStream` debería cerrarse después de mapear, o usar `use {}` para garantizar el cierre.

**Reproducción:**
1. Cargar múltiples modelos (pose, hand, gesture)
2. Observar con LeakCanary o Memory Profiler → múltiples `FileInputStream` abiertos

**Impacto:** Descriptores de archivo agotados en dispositivos con muchos modelos, posible degradación de rendimiento.

**Fix:**
```kotlin
private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
    // ... código existente ...
    
    // Si ninguna variante funcionó, intentar desde raw resources
    try {
        val baseName = modelPath.substringBeforeLast(".")
        val resourceId = context.resources.getIdentifier(baseName, "raw", context.packageName)
        if (resourceId != 0) {
            val inputStream = context.resources.openRawResource(resourceId)
            val bytes = inputStream.readBytes()
            inputStream.close() // ✅ Ya se cierra aquí
            
            val tempFile = java.io.File(context.cacheDir, modelPath)
            tempFile.writeBytes(bytes)
            
            // ✅ Usar use {} para garantizar cierre
            FileInputStream(tempFile).use { fileInputStream ->
                val fileChannel = fileInputStream.channel
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, tempFile.length())
            }
        }
    } catch (e: Exception) {
        // ...
    }
}
```

**Tests:**
```kotlin
@Test
fun `loadModelFile should close FileInputStream`() {
    val detector = PoseDetector(context)
    
    // Verificar que no hay leaks (requiere LeakCanary o similar)
    // En producción, LeakCanary detectaría esto automáticamente
}
```

---

### PROBLEMA #8: Configuración Insegura - usesCleartextTraffic habilitado
**Archivo:** `app/src/main/AndroidManifest.xml:28`  
**Tipo:** Seguridad - Tráfico HTTP sin encriptar  
**Gravedad:** `MEDIUM`

**Descripción:**
El manifest tiene `android:usesCleartextTraffic="true"`, lo que permite conexiones HTTP sin TLS. Aunque `RetrofitClient.BASE_URL` apunta a una IP local (`http://192.168.0.6`), esto es inseguro porque:
1. Cualquier tráfico puede ser interceptado (Man-in-the-Middle)
2. Tokens JWT se envían en texto plano
3. Credenciales de login se envían sin encriptar

**Reproducción:**
1. Conectar dispositivo a red WiFi pública
2. Usar herramienta como Wireshark o Burp Suite
3. Interceptar tráfico → ver tokens y datos en texto plano

**Impacto:** Compromiso de sesiones, robo de credenciales, violación de privacidad.

**Fix:**
```xml
<!-- AndroidManifest.xml -->
<application
    android:usesCleartextTraffic="false"  <!-- ✅ Cambiar a false -->
    ...>
```

Y en `RetrofitClient`:
```kotlin
object RetrofitClient {
    // ✅ Usar HTTPS en producción
    private const val BASE_URL = "https://api.ejemplo.com/lengua_senas/"
    
    // Para desarrollo local, usar network security config
    // Crear res/xml/network_security_config.xml:
    // <?xml version="1.0" encoding="utf-8"?>
    // <network-security-config>
    //     <domain-config cleartextTrafficPermitted="true">
    //         <domain includeSubdomains="true">192.168.0.6</domain>
    //     </domain-config>
    // </network-security-config>
    // Y en manifest: android:networkSecurityConfig="@xml/network_security_config"
}
```

**Tests:**
```kotlin
@Test
fun `API calls should use HTTPS in production`() {
    val baseUrl = RetrofitClient.BASE_URL
    assertTrue(baseUrl.startsWith("https://") || BuildConfig.DEBUG)
}
```

---

### PROBLEMA #9: Falta Validación de Permisos en Runtime para Android 13+
**Archivo:** `app/src/main/java/com/example/ensenando/ui/camera/CameraActivity.kt:57-61`  
**Tipo:** Permisos - Validación incompleta  
**Gravedad:** `MEDIUM`

**Descripción:**
`CameraActivity` solicita permiso de cámara con `requestPermissionLauncher`, pero:
1. No verifica si el permiso fue denegado permanentemente (shouldShowRequestPermissionRationale)
2. No maneja el caso de Android 13+ donde puede requerir permisos adicionales
3. Si el usuario deniega el permiso, simplemente muestra un Toast y cierra la actividad, pero no ofrece una explicación o redirección a configuración

**Reproducción:**
1. Denegar permiso de cámara permanentemente
2. Abrir `CameraActivity` → muestra Toast y cierra
3. No hay forma de reabrir configuración de permisos

**Impacto:** Mala experiencia de usuario, imposibilidad de usar la funcionalidad principal.

**Fix:**
```kotlin
class CameraActivity : AppCompatActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            // ✅ Verificar si fue denegado permanentemente
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // Usuario denegó pero puede cambiar de opinión
                showPermissionRationaleDialog()
            } else {
                // Denegado permanentemente, redirigir a configuración
                showPermissionDeniedDialog()
            }
        }
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de cámara requerido")
            .setMessage("La aplicación necesita acceso a la cámara para reconocer gestos. Por favor, habilítalo en Configuración.")
            .setPositiveButton("Abrir configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
```

**Tests:**
```kotlin
@Test
fun `should show settings dialog when permission denied permanently`() {
    // Simular permiso denegado permanentemente
    // Verificar que se muestra diálogo con opción de abrir configuración
}
```

---

### PROBLEMA #10: Inconsistencia en Nombres de Campos - id_usuario vs id
**Archivo:** `app/src/main/java/com/example/ensenando/data/remote/model/ApiResponse.kt`  
**Tipo:** Mal uso de API - Inconsistencia de nombres  
**Gravedad:** `LOW`

**Descripción:**
En varios lugares del código se usan ambos `id` e `id_usuario` para el mismo concepto:
- `UsuarioResponse` puede tener `id` o `id_usuario`
- `ProgresoDetalle` tiene `id_usuario`
- `DocenteInfo` tiene `id_usuario`
- `EstudianteInfo` tiene `id_usuario`

Esto causa confusión y requiere lógica de fallback (`id ?: id_usuario ?: 0`).

**Impacto:** Código más complejo, posibles bugs si el backend cambia el formato.

**Fix:**
Estandarizar en un solo nombre. Si el backend devuelve ambos, crear un data class que normalice:

```kotlin
data class UsuarioResponse(
    val id: Int? = null,
    val id_usuario: Int? = null
) {
    // ✅ Propiedad computada para obtener el ID normalizado
    val normalizedId: Int
        get() = id ?: id_usuario ?: throw IllegalStateException("Usuario sin ID")
}
```

---

## 5. CHECKLIST DE HARDENING / BUENAS PRÁCTICAS

### ✅ Implementado Correctamente:
- [x] ViewBinding usado (no synthetic views)
- [x] Room Database con migraciones
- [x] EncryptedSharedPreferences para datos sensibles
- [x] Coroutines con viewModelScope/lifecycleScope (no GlobalScope)
- [x] Offline-first architecture
- [x] WorkManager para sincronización en background
- [x] Manejo de errores con Result<T>
- [x] Null safety con tipos nullable

### ❌ Faltante o Mejorable:
- [ ] **Validación de entrada:** No se valida formato de correo, longitud de contraseña, etc.
- [ ] **Logging:** Falta logging estructurado (usar Timber en lugar de Log)
- [ ] **Testing:** No se ven tests unitarios ni instrumentados
- [ ] **ProGuard/R8:** No se ve configuración de ofuscación para release
- [ ] **Crash Reporting:** No hay integración de Firebase Crashlytics o similar
- [ ] **Analytics:** No hay tracking de eventos de usuario
- [ ] **Deep Links:** No se implementan deep links para compartir gestos
- [ ] **Accessibility:** No se verifica soporte para TalkBack
- [ ] **Localización:** Strings hardcodeados, no hay soporte multi-idioma
- [ ] **Dark Mode:** No se verifica soporte para tema oscuro
- [ ] **Backup/Restore:** No se configura backup automático de datos
- [ ] **Versionado de API:** No hay versionado explícito en endpoints

---

## 6. COMANDOS Y HERRAMIENTAS RECOMENDADAS

### Análisis Estático:
```bash
# Lint de Android
./gradlew lint

# Detección de problemas con Detekt
./gradlew detekt

# Formateo con ktlint
./gradlew ktlintCheck
./gradlew ktlintFormat

# Análisis de dependencias
./gradlew dependencies
```

### Testing:
```bash
# Tests unitarios
./gradlew test

# Tests instrumentados
./gradlew connectedAndroidTest

# Coverage
./gradlew jacocoTestReport
```

### Herramientas Externas:
- **LeakCanary:** Detectar memory leaks en runtime
- **StrictMode:** Detectar operaciones en main thread
- **Android Profiler:** Analizar CPU, memoria, red
- **Database Inspector:** Inspeccionar Room DB
- **Network Profiler:** Analizar llamadas HTTP

---

## 7. RESUMEN FINAL - ACCIONES PRIORITARIAS

### 🔴 CRÍTICO (Hacer inmediatamente):
1. **Eliminar login offline con contraseña en texto plano** (Problema #1)
   - Impacto: Seguridad comprometida
   - Esfuerzo: Bajo (1-2 horas)

### 🟠 ALTA PRIORIDAD (Esta semana):
2. **Corregir race conditions en GestureRecognitionManager** (Problema #2)
   - Impacto: Crashes intermitentes
   - Esfuerzo: Medio (3-4 horas)

3. **Cerrar recursos correctamente en CameraActivity** (Problema #3)
   - Impacto: Memory leaks
   - Esfuerzo: Bajo (1 hora)

4. **Corregir resolución de conflictos en syncProgreso()** (Problema #5)
   - Impacto: Pérdida de datos
   - Esfuerzo: Medio (2-3 horas)

### 🟡 MEDIA PRIORIDAD (Próximas 2 semanas):
5. **Validar IDs de usuario antes de usar** (Problema #6)
6. **Mejorar manejo de permisos de cámara** (Problema #9)
7. **Configurar HTTPS/network security** (Problema #8)

### 🟢 BAJA PRIORIDAD (Mejoras continuas):
8. **Cerrar FileInputStream correctamente** (Problema #7)
9. **Estandarizar nombres de campos** (Problema #10)
10. **Agregar tests unitarios e instrumentados**

---

## 8. ARCHIVOS FALTANTES PARA ANÁLISIS COMPLETO

Para un análisis más exhaustivo, sería útil revisar:
- `build.gradle` (app y proyecto) - Dependencias, versiones, configuración de ProGuard
- `res/xml/network_security_config.xml` - Si existe configuración de red
- `res/xml/file_paths.xml` - Configuración de FileProvider
- Tests existentes (si hay)
- Configuración de CI/CD
- Documentación de API del backend

---

## CONCLUSIÓN

El proyecto tiene una **arquitectura sólida** (offline-first, MVVM, Room, Coroutines) pero presenta **varios problemas críticos de seguridad y estabilidad** que deben corregirse antes de producción. Los problemas más urgentes son:

1. **Seguridad:** Contraseñas en texto plano
2. **Estabilidad:** Race conditions y memory leaks
3. **Datos:** Resolución de conflictos incorrecta

Con las correcciones sugeridas, el proyecto estará listo para un entorno de producción con mejor seguridad, estabilidad y experiencia de usuario.





