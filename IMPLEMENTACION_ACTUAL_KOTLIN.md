# Implementación Actual en Kotlin - Para Adaptación

## 📋 Resumen del Modelo Esperado

Tu modelo espera:
- **Hasta 83 frames**
- **Por frame:**
  - 33 pose landmarks (full body)
  - 21 right_hand landmarks
  - 21 left_hand landmarks
- **Total:** FloatArray(18675) = 83 frames × 225 valores por frame

## 🔍 Cálculo del Tamaño

```
Por frame:
- 33 pose × 4 valores (x, y, z, visibility) = 132
- 21 right_hand × 3 valores (x, y, z) = 63
- 21 left_hand × 3 valores (x, y, z) = 63
Total por frame: 132 + 63 + 63 = 258 valores

83 frames × 258 = 21,414 valores

Pero mencionas 18,675... ¿Podría ser?
- 33 pose × 3 (solo x,y,z) = 99
- 21 right_hand × 3 = 63
- 21 left_hand × 3 = 63
Total: 99 + 63 + 63 = 225 por frame
83 frames × 225 = 18,675 ✅
```

**Conclusión:** Probablemente el modelo espera:
- 33 pose × 3 (x, y, z) = 99 valores (sin visibility)
- 21 right_hand × 3 (x, y, z) = 63 valores
- 21 left_hand × 3 (x, y, z) = 63 valores
- **Total: 225 valores por frame × 83 frames = 18,675 valores**

---

## 📝 Implementación Actual

### 1. Cómo Obtengo Pose (Full Body)

**Archivo:** `PoseDetector.kt`

```kotlin
class PoseDetector(private val context: Context) {
    private var poseLandmarker: Interpreter? = null
    private lateinit var poseInputBuffer: ByteBuffer
    private lateinit var poseOutputBuffer: Array<FloatArray>
    
    // MediaPipe Pose: 33 landmarks × 4 valores (x, y, z, visibility)
    private const val NUM_LANDMARKS = 33
    private const val LANDMARK_SIZE = 4
    private const val POSE_INPUT_SIZE = 256
    
    fun detectPose(bitmap: Bitmap): FloatArray? {
        // 1. Redimensionar imagen a 256x256
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, POSE_INPUT_SIZE, POSE_INPUT_SIZE, true)
        
        // 2. Preprocesar: Bitmap → ByteBuffer normalizado [0, 1]
        preprocessImage(resizedBitmap, poseInputBuffer, POSE_INPUT_SIZE)
        
        // 3. Ejecutar inferencia
        poseLandmarker?.run(poseInputBuffer, poseOutputBuffer)
        
        // 4. Extraer landmarks del output
        val output = poseOutputBuffer[0] // [132 valores: 33 × 4]
        val landmarks = FloatArray(NUM_LANDMARKS * LANDMARK_SIZE) // 132 valores
        
        for (i in 0 until NUM_LANDMARKS) {
            val baseIndex = i * LANDMARK_SIZE
            landmarks[baseIndex] = output[baseIndex]     // x (0-1)
            landmarks[baseIndex + 1] = output[baseIndex + 1] // y (0-1)
            landmarks[baseIndex + 2] = output[baseIndex + 2] // z
            landmarks[baseIndex + 3] = output[baseIndex + 3] // visibility (0-1)
        }
        
        return landmarks // Retorna FloatArray(132)
    }
    
    private fun preprocessImage(bitmap: Bitmap, buffer: ByteBuffer, size: Int) {
        buffer.rewind()
        val intValues = IntArray(size * size)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (pixel in intValues) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // Normalizar a [0, 1]
            buffer.putFloat(r / 255.0f)
            buffer.putFloat(g / 255.0f)
            buffer.putFloat(b / 255.0f)
        }
    }
}
```

**Retorna:** `FloatArray(132)` con formato:
```
[x0, y0, z0, v0, x1, y1, z1, v1, ..., x32, y32, z32, v32]
```

---

### 2. Cómo Obtengo Right Hand y Left Hand

**Archivo:** `HandDetector.kt`

```kotlin
class HandDetector(private val context: Context) {
    private var palmDetector: Interpreter? = null
    private var handLandmarker: Interpreter? = null
    
    // MediaPipe Hands: 21 landmarks × 3 valores (x, y, z)
    private const val NUM_LANDMARKS = 21
    private const val LANDMARK_SIZE = 3
    
    fun detectHands(bitmap: Bitmap): List<HandLandmarks> {
        // 1. Detectar palmas en la imagen
        val palmDetections = detectPalms(bitmap)
        
        // 2. Para cada palma, extraer landmarks
        val hands = mutableListOf<HandLandmarks>()
        for (detection in palmDetections) {
            val landmarks = extractLandmarks(bitmap, detection)
            if (landmarks != null) {
                hands.add(HandLandmarks(landmarks))
            }
        }
        
        return hands // Lista de manos detectadas
    }
    
    data class HandLandmarks(
        val landmarks: FloatArray // 21 landmarks × 3 = 63 valores
    )
}
```

**Retorna:** `List<HandLandmarks>` donde cada `HandLandmarks` tiene:
- `landmarks: FloatArray(63)` = `[x0, y0, z0, x1, y1, z1, ..., x20, y20, z20]`

**Proceso:**
1. `detectPalms()`: Detecta palmas usando `palm_detection.tflite` (256×256 input)
2. `extractLandmarks()`: Para cada palma, extrae 21 landmarks usando `hand_landmark.tflite` (224×224 input)
3. Retorna lista de manos detectadas

**NOTA IMPORTANTE:** 
- Actualmente retorna `List<HandLandmarks>` (no distingue right/left)
- Para el modelo necesito `Pair<FloatArray?, FloatArray?>` (right, left)
- Necesito determinar qué mano es cuál basándome en posición (x coordinate) o usar MediaPipe que ya lo distingue

---

### 3. Cómo Construyo el FloatArray/ByteBuffer Actualmente

**Archivo:** `GestureClassifier.kt`

```kotlin
class GestureClassifier(context: Context) {
    private var interpreter: Interpreter? = null
    private lateinit var inputBuffer: ByteBuffer
    private lateinit var outputBuffer: FloatArray
    private val inputSize = 132 // ⚠️ Necesita cambiar a 18675
    
    init {
        val model = loadModelFile(context, "modelo_lsp.tflite")
        interpreter = Interpreter(model)
        
        // ByteBuffer para input
        inputBuffer = ByteBuffer.allocateDirect(4 * inputSize) // 4 bytes por float
        inputBuffer.order(ByteOrder.nativeOrder())
        
        outputBuffer = FloatArray(199) // 199 gestos
    }
    
    fun classify(landmarks: FloatArray): Pair<Int, Float>? {
        if (landmarks.size != inputSize) return null
        
        // Convertir FloatArray → ByteBuffer
        inputBuffer.rewind()
        for (landmark in landmarks) {
            inputBuffer.putFloat(landmark)
        }
        
        // Ejecutar inferencia
        interpreter?.run(inputBuffer, outputBuffer)
        
        // Procesar output
        var maxIndex = 0
        var maxConfidence = outputBuffer[0]
        for (i in 1 until outputBuffer.size) {
            if (outputBuffer[i] > maxConfidence) {
                maxConfidence = outputBuffer[i]
                maxIndex = i
            }
        }
        
        return Pair(maxIndex + 1, maxConfidence)
    }
}
```

**Problema actual:** Solo maneja un frame de pose (132 valores), no 83 frames con pose + hands.

---

## 🔧 Lo Que Necesito

Una función que:

1. **Tome hasta 83 frames** con:
   - `pose: FloatArray(132)` o `FloatArray(99)` (33 landmarks × 3 o × 4)
   - `rightHand: FloatArray(63)` (21 landmarks × 3)
   - `leftHand: FloatArray(63)` (21 landmarks × 3)

2. **Los empaquete en orden correcto:**
   ```
   Frame 0: [pose(99), rightHand(63), leftHand(63)] = 225 valores
   Frame 1: [pose(99), rightHand(63), leftHand(63)] = 225 valores
   ...
   Frame 82: [pose(99), rightHand(63), leftHand(63)] = 225 valores
   Total: 83 × 225 = 18,675 valores
   ```

3. **Cree el ByteBuffer correcto:**
   ```kotlin
   val inputBuffer = ByteBuffer.allocateDirect(4 * 18675)
   inputBuffer.order(ByteOrder.nativeOrder())
   for (value in concatenatedArray) {
       inputBuffer.putFloat(value)
   }
   ```

4. **Llame a Interpreter.run() con las formas adecuadas:**
   ```kotlin
   interpreter.run(inputBuffer, outputBuffer)
   ```

---

## 📊 Estructura de Datos Esperada

### Por Frame (225 valores):
```
[pose_x0, pose_y0, pose_z0,           // Landmark 0 de pose (3 valores)
 pose_x1, pose_y1, pose_z1,           // Landmark 1 de pose
 ...
 pose_x32, pose_y32, pose_z32,        // Landmark 32 de pose (99 valores totales)
 
 rightHand_x0, rightHand_y0, rightHand_z0,  // Landmark 0 de mano derecha
 rightHand_x1, rightHand_y1, rightHand_z1,  // Landmark 1 de mano derecha
 ...
 rightHand_x20, rightHand_y20, rightHand_z20, // Landmark 20 de mano derecha (63 valores)
 
 leftHand_x0, leftHand_y0, leftHand_z0,     // Landmark 0 de mano izquierda
 leftHand_x1, leftHand_y1, leftHand_z1,     // Landmark 1 de mano izquierda
 ...
 leftHand_x20, leftHand_y20, leftHand_z20]  // Landmark 20 de mano izquierda (63 valores)
```

### 83 Frames Concatenados:
```
[Frame0_pose(99), Frame0_rightHand(63), Frame0_leftHand(63),
 Frame1_pose(99), Frame1_rightHand(63), Frame1_leftHand(63),
 ...
 Frame82_pose(99), Frame82_rightHand(63), Frame82_leftHand(63)]
```

---

## ❓ Preguntas para Clarificar

1. **¿El modelo espera 33 pose × 3 (sin visibility) o 33 × 4 (con visibility)?**
   - Si es × 3: 99 valores por pose
   - Si es × 4: 132 valores por pose

2. **¿El orden es exactamente: pose → rightHand → leftHand por cada frame?**

3. **¿Si hay menos de 83 frames, se hace padding con ceros o se usa solo los frames disponibles?**

4. **¿Las coordenadas están normalizadas (0-1) o en píxeles?**

---

## 🎯 Función Esperada

```kotlin
fun classifyFromFrames(
    frames: List<Triple<FloatArray, FloatArray?, FloatArray?>>
): Pair<Int, Float>? {
    // frames: Lista de (pose, rightHand?, leftHand?)
    // Cada frame: Triple(pose(99 o 132), rightHand(63), leftHand(63))
    // Máximo 83 frames
    
    // 1. Validar y normalizar frames
    // 2. Empaquetar en orden: pose → rightHand → leftHand por frame
    // 3. Concatenar todos los frames
    // 4. Crear ByteBuffer
    // 5. Llamar interpreter.run()
    // 6. Procesar output
    
    return Pair(gestoId, confidence)
}
```

