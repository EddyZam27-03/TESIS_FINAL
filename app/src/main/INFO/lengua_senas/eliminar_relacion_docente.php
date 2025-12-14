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

if (!isset($data['id_docente']) || !isset($data['id_estudiante'])) {
    http_response_code(400);
    echo jsonResponse(false, 'id_docente e id_estudiante son requeridos');
    $conn->close();
    exit();
}

$idDocente = (int)$data['id_docente'];
$idEstudiante = (int)$data['id_estudiante'];

// Verificar permisos: solo el docente, estudiante o administrador pueden eliminar
if ($payload['rol'] !== 'administrador' && 
    ($idDocente != $payload['id_usuario'] && $idEstudiante != $payload['id_usuario'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para eliminar esta relación');
    $conn->close();
    exit();
}

// Verificar que existe la relación
$stmt = $conn->prepare("SELECT estado FROM docenteestudiante WHERE id_docente = ? AND id_estudiante = ?");
$stmt->bind_param("ii", $idDocente, $idEstudiante);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    http_response_code(404);
    echo jsonResponse(false, 'No se encontró la relación');
    $stmt->close();
    $conn->close();
    exit();
}
$stmt->close();

// Eliminar la relación
$stmt = $conn->prepare("DELETE FROM docenteestudiante WHERE id_docente = ? AND id_estudiante = ?");
$stmt->bind_param("ii", $idDocente, $idEstudiante);

if (!$stmt->execute()) {
    http_response_code(500);
    echo jsonResponse(false, 'Error al eliminar relación');
    $stmt->close();
    $conn->close();
    exit();
}

$response = [
    'success' => true,
    'message' => 'Relación eliminada exitosamente',
    'data' => null
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
?>




