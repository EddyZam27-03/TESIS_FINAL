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

// Aceptar múltiples nombres de parámetros
$idUsuario = isset($data['id_usuario']) ? (int)$data['id_usuario'] : (isset($data['usuario_id']) ? (int)$data['usuario_id'] : $payload['id_usuario']);
$idGesto = isset($data['id_gesto']) ? (int)$data['id_gesto'] : (isset($data['gesto_id']) ? (int)$data['gesto_id'] : null);
$porcentaje = isset($data['porcentaje']) ? (int)$data['porcentaje'] : null;
$estado = isset($data['estado']) ? normalizeGestoEstado($data['estado']) : null;

if ($idGesto === null || $porcentaje === null || $estado === null) {
    http_response_code(400);
    echo jsonResponse(false, 'id_gesto, porcentaje y estado son requeridos');
    $conn->close();
    exit();
}

// Verificar permisos: solo puede actualizar su propio progreso o ser docente/administrador
if ($idUsuario != $payload['id_usuario'] && !in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para actualizar este progreso');
    $conn->close();
    exit();
}

// Verificar si existe el registro
$stmt = $conn->prepare("SELECT id_usuario, id_gesto, porcentaje, estado FROM usuario_gestos WHERE id_usuario = ? AND id_gesto = ?");
$stmt->bind_param("ii", $idUsuario, $idGesto);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    // Actualizar existente
    $stmt->close();
    $stmt = $conn->prepare("UPDATE usuario_gestos SET porcentaje = ?, estado = ? WHERE id_usuario = ? AND id_gesto = ?");
    $stmt->bind_param("isii", $porcentaje, $estado, $idUsuario, $idGesto);
} else {
    // Insertar nuevo
    $stmt->close();
    $stmt = $conn->prepare("INSERT INTO usuario_gestos (id_usuario, id_gesto, porcentaje, estado) VALUES (?, ?, ?, ?)");
    $stmt->bind_param("iiis", $idUsuario, $idGesto, $porcentaje, $estado);
}

if (!$stmt->execute()) {
    http_response_code(500);
    echo jsonResponse(false, 'Error al actualizar progreso');
    $stmt->close();
    $conn->close();
    exit();
}

// Obtener el registro actualizado
$stmt->close();
$stmt = $conn->prepare("SELECT id_usuario, id_gesto, porcentaje, estado FROM usuario_gestos WHERE id_usuario = ? AND id_gesto = ?");
$stmt->bind_param("ii", $idUsuario, $idGesto);
$stmt->execute();
$result = $stmt->get_result();
$row = $result->fetch_assoc();

$response = [
    'success' => true,
    'message' => 'Progreso actualizado exitosamente',
    'data' => [
        'id_usuario' => (int)$row['id_usuario'],
        'id_gesto' => (int)$row['id_gesto'],
        'porcentaje' => (int)$row['porcentaje'],
        'estado' => $row['estado']
    ]
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
?>


