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

if (!isset($data['id_usuario']) || !isset($data['id_gesto']) || !isset($data['porcentaje']) || !isset($data['estado'])) {
    http_response_code(400);
    echo jsonResponse(false, 'id_usuario, id_gesto, porcentaje y estado son requeridos');
    $conn->close();
    exit();
}

$idUsuario = (int)$data['id_usuario'];
$idGesto = (int)$data['id_gesto'];
$porcentaje = (int)$data['porcentaje'];
$estado = normalizeGestoEstado($data['estado']);

// Verificar permisos
if ($idUsuario != $payload['id_usuario'] && !in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para sincronizar este progreso');
    $conn->close();
    exit();
}

// Verificar si existe
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
        $stmt = $conn->prepare("UPDATE usuario_gestos SET porcentaje = ?, estado = ? WHERE id_usuario = ? AND id_gesto = ?");
        $stmt->bind_param("isii", $porcentaje, $estado, $idUsuario, $idGesto);
        $stmt->execute();
        $stmt->close();
    }
} else {
    $stmt->close();
    $stmt = $conn->prepare("INSERT INTO usuario_gestos (id_usuario, id_gesto, porcentaje, estado) VALUES (?, ?, ?, ?)");
    $stmt->bind_param("iiis", $idUsuario, $idGesto, $porcentaje, $estado);
    $stmt->execute();
    $stmt->close();
}

$response = [
    'success' => true,
    'message' => 'Progreso sincronizado exitosamente',
    'data' => null
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>


