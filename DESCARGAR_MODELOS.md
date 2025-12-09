# Instrucciones para Descargar Modelos de MediaPipe

## Modelos Necesarios

1. **palm_detection.tflite** - Para detectar palmas
2. **hand_landmark.tflite** - Para extraer landmarks

## Opción 1: Descarga Manual (Recomendado)
@@
1. Visita: https://github.com/google/mediapipe/tree/master/mediapipe/modules
2. Descarga estos archivos:
   - `palm_detection/palm_detection_full.tflite`
   - `hand_landmark/hand_landmark_full.tflite`
3. Renombra a:
   - `palm_detection.tflite`
   - `hand_landmark.tflite`
4. Colócalos en: `app/src/main/assets/INFO/`

## Opción 2: Usar Git

```bash
cd app/src/main/assets/INFO
git clone https://github.com/google/mediapipe.git temp
cp temp/mediapipe/modules/palm_detection/palm_detection_full.tflite palm_detection.tflite
cp temp/mediapipe/modules/hand_landmark/hand_landmark_full.tflite hand_landmark.tflite
rm -rf temp
```

## Opción 3: URLs Directas (si funcionan)

```bash
# Palm Detection
curl -L https://github.com/google/mediapipe/raw/master/mediapipe/modules/palm_detection/palm_detection_full.tflite -o app/src/main/assets/INFO/palm_detection.tflite

# Hand Landmark
curl -L https://github.com/google/mediapipe/raw/master/mediapipe/modules/hand_landmark/hand_landmark_full.tflite -o app/src/main/assets/INFO/hand_landmark.tflite
```

## Verificación

Después de descargar, verifica que existan:
- `app/src/main/assets/INFO/palm_detection.tflite` (debe tener varios MB)
- `app/src/main/assets/INFO/hand_landmark.tflite` (debe tener varios MB)
- `app/src/main/assets/INFO/modelo_lsp.tflite` (ya lo tienes)








