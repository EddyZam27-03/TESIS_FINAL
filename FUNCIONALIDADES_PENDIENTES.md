# Funcionalidades Pendientes y Completar

## 🔴 CRÍTICO - Debe Implementarse

### 1. **Procesamiento de Cámara en CameraActivity**
**Archivo:** `app/src/main/java/com/example/ensenando/ui/camera/CameraActivity.kt`

**Problema:** El método `processImageProxy()` está vacío, solo cierra el proxy sin procesar.

**Solución necesaria:**
```kotlin
private fun processImageProxy(imageProxy: ImageProxy) {
    // Convertir ImageProxy a Bitmap
    val bitmap = imageProxy.toBitmap() // Necesita implementación
    // Procesar con ViewModel
    viewModel.processFrame(bitmap)
    imageProxy.close()
}
```

**Archivo a crear/modificar:**
- Agregar extensión `ImageProxy.toBitmap()` o usar `YuvToRgbConverter`
- Conectar con `viewModel.processFrame()`

---

### 2. **Detección de Manos Real (HandDetector)**
**Archivo:** `app/src/main/java/com/example/ensenando/ml/HandDetector.kt`

**Problema:** Actualmente retorna lista vacía, no detecta manos reales.

**Opciones:**
1. **Usar MediaPipe Hands** (Recomendado)
   - Agregar dependencia de MediaPipe
   - Implementar detección real de landmarks

2. **Usar TensorFlow Lite Hand Detection**
   - Modelo separado para detección de manos
   - Luego pasar landmarks al GestureClassifier

3. **Implementación básica con OpenCV** (si está disponible)

**Archivo a modificar:**
- `HandDetector.kt` - Implementar `detectHands()` real

---

### 3. **Configuración de URL del Backend**
**Archivo:** `app/src/main/java/com/example/ensenando/data/remote/RetrofitClient.kt`

**Problema:** URL placeholder `"https://your-api-domain.com/api/"`

**Solución:**
```kotlin
private const val BASE_URL = "http://tu-servidor.com/lengua_senas/" // Cambiar por URL real
```

---

### 4. **Conversión ImageProxy a Bitmap**
**Archivo:** Crear nuevo archivo o agregar a `CameraActivity.kt`

**Necesario:** Función para convertir `ImageProxy` (YUV) a `Bitmap` (RGB)

**Implementación sugerida:**
```kotlin
// Extensión o función helper
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    
    val nv21 = ByteArray(ySize + uSize + vSize)
    
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
```

---

## 🟡 IMPORTANTE - Mejoras Necesarias

### 5. **Navegación con Argumentos**
**Archivo:** `app/src/main/java/com/example/ensenando/ui/activity/ActivityFragment.kt`

**Problema:** Usa `arguments?.getInt("idGesto")` pero debería usar Safe Args de Navigation.

**Solución:**
- Verificar que `nav_graph.xml` tenga argumentos definidos correctamente
- Usar Safe Args generados o verificar que los argumentos se pasen correctamente

---

### 6. **Carga de Videos desde Raw Resources**
**Archivo:** `app/src/main/java/com/example/ensenando/ui/activity/ActivityFragment.kt`

**Problema:** Busca videos con nombre basado en `gesto.nombre`, pero los videos deben estar en `/res/raw/` con nombres en snake_case.

**Verificar:**
- Que los videos estén en `app/src/main/res/raw/`
- Que los nombres coincidan con los nombres de gestos (convertidos a snake_case)
- Ejemplo: Si gesto se llama "Hola", el video debe ser `hola.mp4` o `hola.3gp`

---

### 7. **Modelo TensorFlow Lite**
**Archivo:** `app/src/main/java/com/example/ensenando/ml/GestureClassifier.kt`

**Problema:** Busca modelo en `INFO/modelo_lsp.tflite` pero puede no estar en la ubicación correcta.

**Verificar:**
- Modelo debe estar en `app/src/main/assets/INFO/modelo_lsp.tflite` O
- Modelo debe estar en `app/src/main/res/raw/modelo_lsp.tflite`
- Verificar que el modelo tenga las dimensiones correctas (63 inputs, 199 outputs)

---

### 8. **Firebase Configuration (Opcional)**
**Archivo:** `app/google-services.json` (NO existe)

**Si se usa Firebase Phone Auth:**
- Descargar `google-services.json` desde Firebase Console
- Colocar en `app/`
- Descomentar plugin en `build.gradle.kts`

**Si NO se usa Firebase:**
- Remover dependencias de Firebase del `build.gradle.kts`
- Remover referencias en código

---

## 🟢 OPCIONAL - Mejoras Adicionales

### 9. **Manejo de Errores Mejorado**
- Agregar try-catch más específicos
- Mostrar mensajes de error al usuario
- Logging para debugging

### 10. **Validaciones Adicionales**
- Validar que el usuario esté autenticado antes de ciertas operaciones
- Validar conexión a internet antes de sincronizar
- Validar permisos de cámara antes de abrir CameraActivity

### 11. **Optimizaciones de Performance**
- Cache de imágenes/videos
- Lazy loading de listas grandes
- Optimización de consultas a Room

### 12. **Testing**
- Unit tests para ViewModels
- Integration tests para Repositories
- UI tests para pantallas principales

---

## 📋 Checklist de Implementación

### Fase 1: Funcionalidad Básica (CRÍTICO)
- [ ] Implementar conversión ImageProxy → Bitmap
- [ ] Conectar CameraActivity con GestureRecognitionManager
- [ ] Implementar detección de manos real (HandDetector)
- [ ] Configurar URL del backend en RetrofitClient
- [ ] Verificar que modelo TFLite esté en ubicación correcta
- [ ] Verificar que videos estén en `/res/raw/` con nombres correctos

### Fase 2: Configuración (IMPORTANTE)
- [ ] Configurar Firebase (si se usa) o remover dependencias
- [ ] Verificar navegación y argumentos
- [ ] Probar sincronización offline-first
- [ ] Probar WorkManager

### Fase 3: Mejoras (OPCIONAL)
- [ ] Agregar manejo de errores mejorado
- [ ] Agregar validaciones
- [ ] Optimizar performance
- [ ] Agregar tests

---

## 🔧 Archivos que Necesitan Modificación

1. **CameraActivity.kt** - Agregar procesamiento de ImageProxy
2. **HandDetector.kt** - Implementar detección real
3. **RetrofitClient.kt** - Cambiar URL
4. **ActivityFragment.kt** - Verificar carga de videos
5. **GestureClassifier.kt** - Verificar ubicación del modelo

---

## 📝 Notas Importantes

1. **Modelo TFLite**: El modelo debe estar entrenado y coincidir con los IDs de gestos en la base de datos
2. **Videos**: Deben estar en formato compatible (MP4, 3GP) y en `/res/raw/`
3. **Backend**: Debe estar accesible desde la red del dispositivo o usar IP local para testing
4. **Permisos**: Verificar que todos los permisos estén declarados en `AndroidManifest.xml`

---

## 🚀 Próximos Pasos Inmediatos

1. **Implementar `processImageProxy()` en CameraActivity**
2. **Implementar detección de manos real en HandDetector**
3. **Configurar URL del backend**
4. **Verificar ubicación del modelo TFLite y videos**
5. **Probar flujo completo: Home → Activity → Camera → Reconocimiento**

---

## 📚 Recursos Útiles

- [CameraX Documentation](https://developer.android.com/training/camerax)
- [TensorFlow Lite Android](https://www.tensorflow.org/lite/android)
- [MediaPipe Hands](https://google.github.io/mediapipe/solutions/hands.html)
- [Retrofit Documentation](https://square.github.io/retrofit/)

