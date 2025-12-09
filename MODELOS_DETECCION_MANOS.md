# Modelos de Detección de Manos - TensorFlow Lite

## 🎯 Importante: Dos Tipos de Modelos

**Tu modelo personalizado (`modelo_lsp.tflite`):**
- ✅ Este es el modelo que TÚ entrenaste para clasificar gestos
- ✅ Recibe: 21 landmarks (63 valores: x, y, z de cada punto)
- ✅ Devuelve: 199 gestos clasificados
- ✅ **Este modelo ya lo tienes y es el más importante**

**Modelos de detección de manos (MediaPipe u otros):**
- ⚠️ Estos son SOLO para extraer los landmarks de la imagen
- ⚠️ NO clasifican gestos, solo detectan manos y extraen puntos
- ⚠️ Los landmarks que extraen se pasan a TU modelo para clasificar

## 📋 Modelos Requeridos para Detección

Para que la detección de manos funcione correctamente, necesitas dos modelos TensorFlow Lite **SOLO para extraer landmarks**:

### 1. **palm_detection.tflite**
- **Propósito**: Detecta palmas de manos en la imagen
- **Tamaño de entrada**: 256x256 píxeles (RGB)
- **Formato de salida**: Detecciones con bounding boxes y scores
- **Nota**: Solo detecta dónde está la mano, NO clasifica gestos

### 2. **hand_landmark.tflite**
- **Propósito**: Extrae 21 puntos de referencia (landmarks) de una mano
- **Tamaño de entrada**: 224x224 píxeles (RGB)
- **Formato de salida**: 21 landmarks con coordenadas (x, y, z)
- **Nota**: Solo extrae puntos, NO clasifica gestos

**Estos landmarks se pasan a TU modelo `modelo_lsp.tflite` para la clasificación real.**

---

## 📥 Dónde Obtener los Modelos

### Opción 1: MediaPipe Hands (Recomendado)

Los modelos oficiales de MediaPipe Hands están disponibles en:

1. **GitHub de MediaPipe**:
   - Repositorio: https://github.com/google/mediapipe
   - Modelos: https://github.com/google/mediapipe/tree/master/mediapipe/modules/hand_landmark

2. **Descarga directa**:
   - **Palm Detection**: 
     - URL: https://github.com/google/mediapipe/raw/master/mediapipe/modules/palm_detection/palm_detection.tflite
     - O busca en: `mediapipe/modules/palm_detection/palm_detection.tflite`
   
   - **Hand Landmark**:
     - URL: https://github.com/google/mediapipe/raw/master/mediapipe/modules/hand_landmark/hand_landmark.tflite
     - O busca en: `mediapipe/modules/hand_landmark/hand_landmark.tflite`

### Opción 2: TensorFlow Hub

1. Visita: https://www.tensorflow.org/hub
2. Busca "hand detection" o "hand landmarks"
3. Descarga los modelos en formato `.tflite`

### Opción 3: Modelos Pre-entrenados de MediaPipe

Puedes usar los modelos pre-entrenados de MediaPipe que ya están optimizados para móviles:

- **Palm Detection Model**: `palm_detection_full.tflite` o `palm_detection_lite.tflite`
- **Hand Landmark Model**: `hand_landmark_full.tflite` o `hand_landmark_lite.tflite`

**Nota**: Los modelos "lite" son más rápidos pero menos precisos. Los modelos "full" son más precisos pero más lentos.

---

## 📁 Ubicación de los Modelos

Coloca los modelos en una de estas ubicaciones (en orden de prioridad):

### Opción 1: `app/src/main/assets/INFO/` (Recomendado)
```
app/src/main/assets/INFO/
  ├── palm_detection.tflite
  └── hand_landmark.tflite
```

### Opción 2: `app/src/main/assets/`
```
app/src/main/assets/
  ├── palm_detection.tflite
  └── hand_landmark.tflite
```

### Opción 3: `app/src/main/res/raw/`
```
app/src/main/res/raw/
  ├── palm_detection.tflite
  └── hand_landmark.tflite
```

**Nota**: Si usas `res/raw/`, los nombres deben ser sin extensión en el código, pero el archivo físico debe tener `.tflite`.

---

## 🔧 Verificación de Modelos

### Modelos que ya tienes:
- ✅ `modelo_lsp.tflite` - Tu modelo de clasificación de gestos (ya está en `app/src/main/assets/INFO/`)

### Modelos que necesitas (solo para detección):
- ⚠️ `palm_detection.tflite` - Para detectar dónde está la mano
- ⚠️ `hand_landmark.tflite` - Para extraer los 21 landmarks

### Verificar que los modelos existen:

1. **Desde Android Studio**:
   - Navega a `app/src/main/assets/INFO/`
   - Verifica que existan los 3 archivos `.tflite`:
     - `modelo_lsp.tflite` ✅ (tu modelo, ya lo tienes)
     - `palm_detection.tflite` ⚠️ (falta descargar)
     - `hand_landmark.tflite` ⚠️ (falta descargar)

2. **Desde el código**:
   - La aplicación intentará cargar los modelos al inicializar `HandDetector`
   - Si los modelos de detección no se encuentran, usará un placeholder (detección simulada)
   - Tu modelo `modelo_lsp.tflite` se carga en `GestureClassifier`
   - Revisa los logs con el tag `HandDetector` para ver el estado

### Verificar formato de los modelos:

Los modelos deben ser compatibles con TensorFlow Lite 2.x:
- Formato: `.tflite`
- Versión: TensorFlow Lite 2.0 o superior
- Optimización: Preferiblemente cuantizados para mejor rendimiento

---

## 📊 Especificaciones Técnicas

### Palm Detection Model
- **Input**: `[1, 256, 256, 3]` (RGB, normalizado [-1, 1])
- **Output**: `[1, 2944, 19]` (detecciones con bounding boxes y scores)
- **Formato de salida**: 
  - `[x_center, y_center, width, height, score, ...]` por detección

### Hand Landmark Model
- **Input**: `[1, 224, 224, 3]` (RGB, normalizado [-1, 1])
- **Output**: `[1, 21, 3]` (21 landmarks con x, y, z)
- **Coordenadas**: Normalizadas (0-1) relativas a la imagen de entrada

---

## 🚀 Instalación Rápida

1. **Descargar modelos**:
   ```bash
   # Crear directorio si no existe
   mkdir -p app/src/main/assets/INFO
   
   # Descargar palm detection (ejemplo con curl)
   curl -L https://github.com/google/mediapipe/raw/master/mediapipe/modules/palm_detection/palm_detection.tflite \
        -o app/src/main/assets/INFO/palm_detection.tflite
   
   # Descargar hand landmark
   curl -L https://github.com/google/mediapipe/raw/master/mediapipe/modules/hand_landmark/hand_landmark.tflite \
        -o app/src/main/assets/INFO/hand_landmark.tflite
   ```

2. **Verificar**:
   - Los archivos deben estar en `app/src/main/assets/INFO/`
   - Los nombres deben ser exactamente: `palm_detection.tflite` y `hand_landmark.tflite`

3. **Compilar y probar**:
   - Compila la aplicación
   - Abre la cámara para practicar gestos
   - Revisa los logs para confirmar que los modelos se cargaron correctamente

---

## ⚠️ Notas Importantes

1. **Tamaño de los modelos**:
   - Los modelos pueden ser grandes (varios MB)
   - Considera usar modelos "lite" si el tamaño es un problema
   - Los modelos se incluyen en el APK, aumentando su tamaño

2. **Rendimiento**:
   - Los modelos se ejecutan en CPU por defecto
   - Para mejor rendimiento, considera usar GPU delegate (ya configurado en dependencias)
   - Los modelos "lite" son más rápidos pero menos precisos

3. **Fallback**:
   - Si los modelos no se encuentran, la app usará detección simulada
   - Esto permite probar el flujo completo sin los modelos
   - Los landmarks simulados permiten probar el `GestureClassifier`

4. **Actualización de modelos**:
   - Si actualizas los modelos, limpia y reconstruye el proyecto
   - Los modelos en `assets/` se incluyen en tiempo de compilación

---

## 🔗 Enlaces Útiles

- **MediaPipe Hands**: https://google.github.io/mediapipe/solutions/hands
- **TensorFlow Lite**: https://www.tensorflow.org/lite
- **TensorFlow Hub**: https://www.tensorflow.org/hub
- **MediaPipe GitHub**: https://github.com/google/mediapipe

---

## ✅ Checklist

- [ ] Descargar `palm_detection.tflite`
- [ ] Descargar `hand_landmark.tflite`
- [ ] Colocar modelos en `app/src/main/assets/INFO/`
- [ ] Verificar nombres de archivos (exactos)
- [ ] Compilar proyecto
- [ ] Probar detección de manos en la app
- [ ] Verificar logs para confirmar carga de modelos

---

## 🐛 Solución de Problemas

### Los modelos no se cargan
- Verifica que los archivos estén en la ubicación correcta
- Verifica que los nombres sean exactos (case-sensitive)
- Revisa los logs con tag `HandDetector`
- Limpia y reconstruye el proyecto

### Detección no funciona
- Verifica que los modelos sean compatibles con TensorFlow Lite 2.x
- Revisa que las dimensiones de entrada coincidan
- Verifica que la cámara tenga buena iluminación
- Asegúrate de que las manos estén visibles en el frame

### Rendimiento lento
- Considera usar modelos "lite"
- Habilita GPU delegate (ya está en dependencias)
- Reduce la resolución de entrada de la cámara
- Procesa frames cada N frames en lugar de todos

---

**Última actualización**: Noviembre 2024

