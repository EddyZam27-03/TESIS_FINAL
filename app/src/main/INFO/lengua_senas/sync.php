<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

$data = json_decode(file_get_contents('php://input'), true);

$responseData = [
    'usuario_gestos' => [],
    'docente_estudiante' => []
];

// Sincronizar usuario_gestos
if (isset($data['usuario_gestos']) && is_array($data['usuario_gestos'])) {
    foreach ($data['usuario_gestos'] as $item) {
        $idUsuario = (int)$item['id_usuario'];
        $idGesto = (int)$item['id_gesto'];
        $porcentaje = (int)$item['porcentaje'];
        $estado = normalizeGestoEstado($item['estado'] ?? null);
        
        $stmt = $conn->prepare("SELECT porcentaje, estado FROM usuario_gestos WHERE id_usuario = ? AND id_gesto = ?");
        $stmt->bind_param("ii", $idUsuario, $idGesto);
        $stmt->execute();
        $result = $stmt->get_result();
        
        if ($result->num_rows > 0) {
            $existing = $result->fetch_assoc();
            $stmt->close();
            
            if (shouldUpdateGestoRegistro(
                (int)$existing['porcentaje'],
                $existing['estado'],
                $porcentaje,
                $estado
            )) {
                $stmtUpdate = $conn->prepare("UPDATE usuario_gestos SET porcentaje = ?, estado = ? WHERE id_usuario = ? AND id_gesto = ?");
                $stmtUpdate->bind_param("isii", $porcentaje, $estado, $idUsuario, $idGesto);
                $stmtUpdate->execute();
                $stmtUpdate->close();
            }
        } else {
            $stmt->close();
            $stmtInsert = $conn->prepare("INSERT INTO usuario_gestos (id_usuario, id_gesto, porcentaje, estado) VALUES (?, ?, ?, ?)");
            $stmtInsert->bind_param("iiis", $idUsuario, $idGesto, $porcentaje, $estado);
            $stmtInsert->execute();
            $stmtInsert->close();
        }
    }
    
    // Obtener todos los usuario_gestos del usuario
    $stmt = $conn->prepare("SELECT id_usuario, id_gesto, porcentaje, estado FROM usuario_gestos WHERE id_usuario = ?");
    $stmt->bind_param("i", $payload['id_usuario']);
    $stmt->execute();
    $result = $stmt->get_result();
    
    while ($row = $result->fetch_assoc()) {
        $responseData['usuario_gestos'][] = [
            'id_usuario' => (int)$row['id_usuario'],
            'id_gesto' => (int)$row['id_gesto'],
            'porcentaje' => (int)$row['porcentaje'],
            'estado' => $row['estado']
        ];
    }
    $stmt->close();
}

// Sincronizar docente_estudiante
if (isset($data['docente_estudiante']) && is_array($data['docente_estudiante'])) {
    foreach ($data['docente_estudiante'] as $item) {
        $idDocente = (int)$item['id_docente'];
        $idEstudiante = (int)$item['id_estudiante'];
        $estado = $conn->real_escape_string($item['estado']);
        
        $stmt = $conn->prepare("INSERT INTO docenteestudiante (id_docente, id_estudiante, estado) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE estado = ?");
        $stmt->bind_param("iiss", $idDocente, $idEstudiante, $estado, $estado);
        $stmt->execute();
        $stmt->close();
    }
    
    // Obtener relaciones del usuario
    $stmt = $conn->prepare("SELECT id_docente, id_estudiante, estado FROM docenteestudiante WHERE id_docente = ? OR id_estudiante = ?");
    $userId = $payload['id_usuario'];
    $stmt->bind_param("ii", $userId, $userId);
    $stmt->execute();
    $result = $stmt->get_result();
    
    while ($row = $result->fetch_assoc()) {
        $responseData['docente_estudiante'][] = [
            'id_docente' => (int)$row['id_docente'],
            'id_estudiante' => (int)$row['id_estudiante'],
            'estado' => $row['estado']
        ];
    }
    $stmt->close();
}

// Devolver directamente el objeto SyncResponse (sin envolver en success/message/data)
echo json_encode($responseData, JSON_UNESCAPED_UNICODE);

$conn->close();
?>


