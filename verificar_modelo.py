#!/usr/bin/env python3
"""
Script para verificar las especificaciones del modelo TensorFlow Lite
Ejecutar: python verificar_modelo.py
"""

import tensorflow as tf
import os

def verificar_modelo(ruta_modelo):
    """Verifica las especificaciones del modelo TFLite"""
    
    if not os.path.exists(ruta_modelo):
        print(f"❌ Error: No se encontró el modelo en {ruta_modelo}")
        print(f"\nUbicaciones posibles:")
        print(f"  - app/src/main/assets/INFO/modelo_lsp.tflite")
        print(f"  - app/src/main/INFO/modelo_lsp.tflite")
        return
    
    print(f"📦 Analizando modelo: {ruta_modelo}")
    print("=" * 60)
    
    try:
        # Cargar el modelo
        interpreter = tf.lite.Interpreter(model_path=ruta_modelo)
        interpreter.allocate_tensors()
        
        # Obtener información de entrada
        input_details = interpreter.get_input_details()
        print("\n📥 INPUT (Entrada):")
        print(f"  Nombre: {input_details[0]['name']}")
        print(f"  Shape: {input_details[0]['shape']}")
        print(f"  Tipo: {input_details[0]['dtype']}")
        
        input_shape = input_details[0]['shape']
        input_size = input_shape[1] if len(input_shape) > 1 else input_shape[0]
        
        print(f"\n  📊 Tamaño del input: {input_size} valores")
        
        # Analizar qué tipo de input es
        if input_size == 63:
            print("  ✅ Parece ser: 21 landmarks de manos × 3 (x, y, z)")
        elif input_size == 132:
            print("  ✅ Parece ser: 33 landmarks de full body × 4 (x, y, z, visibility)")
        elif input_size % 132 == 0:
            num_frames = input_size // 132
            print(f"  ✅ Parece ser: {num_frames} frames de full body")
            print(f"     ({num_frames} frames × 33 landmarks × 4 valores = {input_size})")
        elif input_size % 63 == 0:
            num_frames = input_size // 63
            print(f"  ✅ Parece ser: {num_frames} frames de manos")
            print(f"     ({num_frames} frames × 21 landmarks × 3 valores = {input_size})")
        else:
            print(f"  ⚠️  Tamaño no reconocido. Verificar manualmente.")
        
        # Obtener información de salida
        output_details = interpreter.get_output_details()
        print("\n📤 OUTPUT (Salida):")
        print(f"  Nombre: {output_details[0]['name']}")
        print(f"  Shape: {output_details[0]['shape']}")
        print(f"  Tipo: {output_details[0]['dtype']}")
        
        output_shape = output_details[0]['shape']
        num_gestos = output_shape[1] if len(output_shape) > 1 else output_shape[0]
        print(f"  📊 Número de gestos: {num_gestos}")
        
        # Información adicional
        print("\n📋 RESUMEN:")
        print(f"  Input size: {input_size}")
        print(f"  Output size: {num_gestos}")
        print(f"  ¿Full body?: {'Sí' if input_size >= 132 or (input_size % 132 == 0) else 'No (solo manos)'}")
        print(f"  ¿Múltiples frames?: {'Sí' if input_size > 132 and input_size % 132 == 0 else 'Sí' if input_size > 63 and input_size % 63 == 0 else 'No'}")
        
        if input_size > 132 and input_size % 132 == 0:
            num_frames = input_size // 132
            print(f"  Número de frames: {num_frames}")
        
        print("\n✅ Análisis completado")
        
    except Exception as e:
        print(f"❌ Error al analizar el modelo: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    # Buscar el modelo en ubicaciones comunes
    rutas_posibles = [
        "app/src/main/assets/INFO/modelo_lsp.tflite",
        "app/src/main/INFO/modelo_lsp.tflite",
        "modelo_lsp.tflite"
    ]
    
    modelo_encontrado = None
    for ruta in rutas_posibles:
        if os.path.exists(ruta):
            modelo_encontrado = ruta
            break
    
    if modelo_encontrado:
        verificar_modelo(modelo_encontrado)
    else:
        print("❌ No se encontró el modelo en ninguna ubicación.")
        print("\nPor favor, ejecuta este script desde la raíz del proyecto")
        print("o proporciona la ruta del modelo como argumento:")
        print("  python verificar_modelo.py ruta/al/modelo.tflite")

