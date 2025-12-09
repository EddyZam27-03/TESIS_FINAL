package com.example.ensenando.ml

/**
 * Utilidad para construir frames con el formato correcto para el modelo LSTM
 * Cada frame debe tener 225 valores: pose(99) + right_hand(63) + left_hand(63)
 */
object FrameBuilder {
    
    /**
     * Construye un frame completo con pose, right_hand y left_hand
     * @param pose FloatArray(99) - 33 landmarks × 3 (x, y, z)
     * @param rightHand FloatArray(63) o null - 21 landmarks × 3 (x, y, z)
     * @param leftHand FloatArray(63) o null - 21 landmarks × 3 (x, y, z)
     * @return FloatArray(225) con el orden correcto: pose → right_hand → left_hand
     */
    fun buildFrame(
        pose: FloatArray,
        rightHand: FloatArray?,
        leftHand: FloatArray?
    ): FloatArray {
        require(pose.size == 99) { "Pose debe tener 99 valores (33 × 3)" }
        
        val frame = FloatArray(225)
        var index = 0
        
        // 1. Agregar pose (99 valores)
        // Orden: x0, y0, z0, x1, y1, z1, ..., x32, y32, z32
        pose.copyInto(frame, index)
        index += 99
        
        // 2. Agregar right_hand (63 valores) o ceros si es null
        if (rightHand != null) {
            require(rightHand.size == 63) { "Right hand debe tener 63 valores (21 × 3)" }
            // Orden: x0, y0, z0, x1, y1, z1, ..., x20, y20, z20
            rightHand.copyInto(frame, index)
        } else {
            // Rellenar con ceros si no se detecta la mano
            for (i in 0 until 63) {
                frame[index + i] = 0.0f
            }
        }
        index += 63
        
        // 3. Agregar left_hand (63 valores) o ceros si es null
        if (leftHand != null) {
            require(leftHand.size == 63) { "Left hand debe tener 63 valores (21 × 3)" }
            // Orden: x0, y0, z0, x1, y1, z1, ..., x20, y20, z20
            leftHand.copyInto(frame, index)
        } else {
            // Rellenar con ceros si no se detecta la mano
            for (i in 0 until 63) {
                frame[index + i] = 0.0f
            }
        }
        
        return frame
    }
    
    /**
     * Valida que una lista de frames tenga el formato correcto
     * @param frames Lista de frames, cada uno debe ser FloatArray(225)
     * @param expectedFrames Número esperado de frames (83 para el modelo LSTM)
     * @return true si es válido, false en caso contrario
     */
    fun validateFrames(frames: List<FloatArray>, expectedFrames: Int = 83): Boolean {
        if (frames.size != expectedFrames) return false
        return frames.all { it.size == 225 }
    }
}
