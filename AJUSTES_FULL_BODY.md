# Ajustes para Modelo Full Body con Videos

## ✅ Cambios Realizados

1. **`PoseDetector.kt`** - Creado para detectar full body (33 landmarks)
2. **`GestureClassifier.kt`** - Actualizado con soporte para múltiples frames
3. **`GestureRecognitionManager.kt`** - Cambiado a usar `PoseDetector` y soporte para videos

## ⚠️ Valores a Ajustar

### 1. Input Size en `GestureClassifier.kt`

**Línea 15:** Cambia `inputSize` según tu modelo:

```kotlin
// Ejecuta verificar_modelo.py para obtener el valor exacto
private val inputSize = 132 // ⚠️ CAMBIAR AQUÍ
```

**Valores comunes:**
- **132** = 1 frame de full body (33 landmarks × 4 valores)
- **1320** = 10 frames de full body (132 × 10)
- **2640** = 20 frames de full body (132 × 20)
- **3960** = 30 frames de full body (132 × 30)

### 2. Número de Frames en `GestureRecognitionManager.kt`

**Línea 28:** Cambia `maxFrames` según tu modelo:

```kotlin
private val maxFrames = 1 // ⚠️ CAMBIAR AQUÍ
```

**Cálculo:**
```
maxFrames = inputSize / 132

Ejemplos:
- Si inputSize = 132 → maxFrames = 1
- Si inputSize = 1320 → maxFrames = 10
- Si inputSize = 2640 → maxFrames = 20
```

## 🔍 Cómo Verificar Tu Modelo

### Opción 1: Usar el Script Python

```bash
python verificar_modelo.py
```

El script te dirá:
- Input shape y size
- Si es full body o solo manos
- Si usa múltiples frames
- Número exacto de frames

### Opción 2: Verificar Manualmente

```python
import tensorflow as tf

interpreter = tf.lite.Interpreter(model_path="app/src/main/assets/INFO/modelo_lsp.tflite")
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
input_shape = input_details[0]['shape']
input_size = input_shape[1]  # Ejemplo: [1, 1320]

print(f"Input size: {input_size}")
print(f"Número de frames: {input_size // 132}")
```

## 📋 Checklist de Ajustes

1. [ ] Ejecutar `verificar_modelo.py` o verificar manualmente
2. [ ] Anotar el `input_size` del modelo
3. [ ] Calcular `maxFrames = input_size / 132`
4. [ ] Cambiar `inputSize` en `GestureClassifier.kt` línea 15
5. [ ] Cambiar `maxFrames` en `GestureRecognitionManager.kt` línea 28
6. [ ] Descargar `pose_landmark.tflite` de MediaPipe
7. [ ] Colocar en `app/src/main/assets/INFO/pose_landmark.tflite`
8. [ ] Compilar y probar

## 📥 Modelo Necesario

**Archivo:** `pose_landmark.tflite`

**Descarga:**
- GitHub: https://github.com/google/mediapipe/tree/master/mediapipe/modules/pose_landmark
- Archivo: `pose_landmark_full.tflite` → renombra a `pose_landmark.tflite`

**Ubicación:** `app/src/main/assets/INFO/pose_landmark.tflite`

## 🎯 Ejemplo de Ajuste

Si tu modelo tiene:
- **Input size: 1320**
- **Significa: 10 frames de full body**

**Ajustes:**
1. `GestureClassifier.kt` línea 15: `inputSize = 1320`
2. `GestureRecognitionManager.kt` línea 28: `maxFrames = 10`

## ⚠️ Notas Importantes

- Si `inputSize = 132`: El modelo usa **1 frame** (no videos)
- Si `inputSize > 132` y es múltiplo de 132: El modelo usa **videos**
- El código automáticamente detecta si usar 1 frame o múltiples frames
- Los landmarks son de **full body** (33 puntos), no solo manos

## 🐛 Solución de Problemas

**Error: "Input size mismatch"**
- Verifica que `inputSize` en `GestureClassifier.kt` coincida con tu modelo

**Error: "Model not found"**
- Descarga `pose_landmark.tflite` y colócalo en `app/src/main/assets/INFO/`

**No detecta gestos:**
- Verifica que `maxFrames` sea correcto
- Verifica que el modelo de pose esté cargado correctamente






