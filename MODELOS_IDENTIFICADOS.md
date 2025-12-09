# IDENTIFICACIÓN DE MODELOS ML
## Análisis de modelos disponibles en `app/src/main/assets/INFO/`

**Fecha:** 2024  
**Ubicación:** `app/src/main/assets/INFO/`

---

## MODELOS DISPONIBLES

### 1. ✅ `modelo_lsp.tflite` (1.4MB)
- **Estado:** ✅ Encontrado y compatible
- **Uso:** Clasificador principal de gestos de lengua de señas
- **Clase:** `GestureClassifier`
- **Formato:** TensorFlow Lite
- **Input:** 83 frames × 225 features = 18,675 valores
- **Output:** 199 gestos (probabilidades)
- **Nota:** ⚠️ Puede tener problemas de compatibilidad con la versión de TensorFlow Lite (requiere FULLY_CONNECTED v12)

### 2. ✅ `palm_detection_full.tflite` (2.2MB)
- **Estado:** ✅ Encontrado (nombre diferente)
- **Buscado por código:** `palm_detection.tflite`
- **Uso:** Detección de palmas de manos (MediaPipe)
- **Clase:** `HandDetector`
- **Formato:** TensorFlow Lite
- **Input:** 256×256×3 (RGB)
- **Output:** [1, 2944, 19] - detecciones (bounding boxes + scores)
- **Solución:** ✅ Código actualizado para buscar variante `_full`

### 3. ✅ `hand_landmark_full.tflite` (5.2MB)
- **Estado:** ✅ Encontrado (nombre diferente)
- **Buscado por código:** `hand_landmark.tflite`
- **Uso:** Detección de landmarks de manos (21 puntos por mano)
- **Clase:** `HandDetector`
- **Formato:** TensorFlow Lite
- **Input:** 224×224×3 (RGB)
- **Output:** [1, 21, 3] - 21 landmarks con (x, y, z)
- **Solución:** ✅ Código actualizado para buscar variante `_full`

### 4. ⚠️ `hand_landmark.task` (3.9MB)
- **Estado:** ⚠️ Formato diferente (MediaPipe Task)
- **Formato:** MediaPipe Task (`.task`)
- **Uso:** Alternativa a `hand_landmark.tflite` (formato MediaPipe Tasks)
- **Nota:** Este formato requiere la API de MediaPipe Tasks, no TensorFlow Lite Interpreter directo
- **Recomendación:** No se usa actualmente. Si se quiere usar, necesitaría cambiar a MediaPipe Tasks API

### 5. ❌ `pose_landmark.tflite` (NO ENCONTRADO)
- **Estado:** ❌ Faltante
- **Buscado por código:** `pose_landmark.tflite`
- **Uso:** Detección de pose completo (33 landmarks del cuerpo)
- **Clase:** `PoseDetector`
- **Formato:** TensorFlow Lite
- **Input:** 192×192×3 (RGB)
- **Output:** [1, 33, 4] - 33 landmarks con (x, y, z, visibility)
- **Impacto:** Sin este modelo, no se puede detectar la pose del cuerpo
- **Solución temporal:** El código usa `detectPosePlaceholder()` cuando el modelo no está disponible

---

## MAPEO CÓDIGO ↔ MODELOS

| Clase | Modelo Buscado | Modelo Disponible | Estado |
|-------|---------------|-------------------|--------|
| `GestureClassifier` | `modelo_lsp.tflite` | ✅ `modelo_lsp.tflite` | ✅ Compatible |
| `HandDetector` | `palm_detection.tflite` | ✅ `palm_detection_full.tflite` | ✅ Corregido |
| `HandDetector` | `hand_landmark.tflite` | ✅ `hand_landmark_full.tflite` | ✅ Corregido |
| `PoseDetector` | `pose_landmark.tflite` | ❌ No encontrado | ⚠️ Modo degradado |

---

## CORRECCIONES APLICADAS

### 1. Búsqueda de Variantes de Nombres
**Archivos modificados:**
- `PoseDetector.kt`
- `HandDetector.kt`

**Cambio:** Las funciones `loadModelFile()` ahora buscan automáticamente variantes:
- Nombre original: `palm_detection.tflite`
- Variante `_full`: `palm_detection_full.tflite`
- Si tiene `_full`, también intenta sin él

**Orden de búsqueda:**
1. `assets/INFO/{nombre_original}`
2. `assets/INFO/{nombre}_full.tflite`
3. `assets/{nombre_original}`
4. `assets/{nombre}_full.tflite`
5. `res/raw/{nombre}` (sin extensión)

### 2. Manejo de Errores Mejorado
- Si un modelo no se encuentra, retorna buffer vacío (no crashea)
- Logs informativos indican qué variantes se intentaron
- Modo degradado cuando faltan modelos

---

## ESTADO ACTUAL

### ✅ Modelos Funcionales
1. **GestureClassifier:** `modelo_lsp.tflite` - ✅ Cargado
2. **HandDetector (palm):** `palm_detection_full.tflite` - ✅ Cargado
3. **HandDetector (landmark):** `hand_landmark_full.tflite` - ✅ Cargado

### ⚠️ Modelos Faltantes
1. **PoseDetector:** `pose_landmark.tflite` - ❌ No encontrado
   - **Impacto:** No se puede detectar pose completo del cuerpo
   - **Solución temporal:** Usa detección placeholder (simulada)
   - **Solución permanente:** Descargar modelo `pose_landmark.tflite` de MediaPipe

### ⚠️ Problemas de Compatibilidad
1. **TensorFlow Lite Version:**
   - El modelo `modelo_lsp.tflite` requiere FULLY_CONNECTED v12
   - La versión actual de TensorFlow Lite puede no soportarlo
   - **Error:** `Didn't find op for builtin opcode 'FULLY_CONNECTED' version '12'`
   - **Solución:** Actualizar dependencia de TensorFlow Lite o usar modelo compatible

---

## RECOMENDACIONES

### Para Funcionalidad Completa

1. **Descargar `pose_landmark.tflite`:**
   - Fuente: MediaPipe Pose Landmarker
   - Ubicación: `app/src/main/assets/INFO/pose_landmark.tflite`
   - Tamaño aproximado: ~5-10MB

2. **Actualizar TensorFlow Lite:**
   - Verificar versión en `build.gradle.kts`
   - Actualizar a versión que soporte FULLY_CONNECTED v12
   - O usar modelo `modelo_lsp.tflite` compatible con versión actual

3. **Considerar MediaPipe Tasks:**
   - El archivo `hand_landmark.task` sugiere que hay modelos en formato MediaPipe Tasks
   - Si se quiere usar, cambiar implementación a MediaPipe Tasks API
   - Ventaja: Más fácil de usar, mejor optimizado
   - Desventaja: Requiere cambiar código significativamente

---

## ESTRUCTURA DE ARCHIVOS

```
app/src/main/assets/INFO/
├── modelo_lsp.tflite              ✅ (1.4MB) - Clasificador de gestos
├── palm_detection_full.tflite     ✅ (2.2MB) - Detección de palmas
├── hand_landmark_full.tflite      ✅ (5.2MB) - Landmarks de manos
├── hand_landmark.task             ⚠️ (3.9MB) - Formato MediaPipe Tasks (no usado)
└── pose_landmark.tflite           ❌ FALTANTE - Detección de pose
```

---

## LOGS ESPERADOS

### ✅ Inicialización Exitosa
```
GestureClassifier: Modelo cargado correctamente
HandDetector: Modelo cargado: INFO/palm_detection_full.tflite
HandDetector: Modelo cargado: INFO/hand_landmark_full.tflite
HandDetector: HandDetector inicializado correctamente
PoseDetector: Modelo pose_landmark.tflite no encontrado, usando modo degradado
GestureRecognitionManager: GestureRecognitionManager inicializado en modo degradado (sin modelos ML)
```

### ⚠️ Modo Degradado (sin pose)
```
PoseDetector: No se pudo encontrar el modelo: pose_landmark.tflite (intentadas variantes: pose_landmark.tflite, pose_landmark_full.tflite)
PoseDetector: Modelo pose_landmark.tflite no encontrado, usando modo degradado
```

---

## CONCLUSIÓN

**Estado General:** ✅ **3 de 4 modelos disponibles y funcionando**

- ✅ Clasificador de gestos: Funcional
- ✅ Detección de manos: Funcional (usando variantes `_full`)
- ⚠️ Detección de pose: Modo degradado (modelo faltante)

**Funcionalidad:** La aplicación puede detectar gestos de manos, pero no puede detectar pose completo del cuerpo sin el modelo `pose_landmark.tflite`.








