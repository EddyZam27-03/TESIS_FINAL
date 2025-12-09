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
$idDocente = isset($data['id_docente']) ? (int)$data['id_docente'] : (isset($data['docente_id']) ? (int)$data['docente_id'] : null);
$idEstudiante = isset($data['id_estudiante']) ? (int)$data['id_estudiante'] : (isset($data['estudiante_id']) ? (int)$data['estudiante_id'] : null);

// Si no se especifica, usar el usuario autenticado como estudiante
if ($idEstudiante === null && $payload['rol'] === 'estudiante') {
    $idEstudiante = $payload['id_usuario'];
}

if ($idDocente === null || $idEstudiante === null) {
    http_response_code(400);
    echo jsonResponse(false, 'id_docente e id_estudiante son requeridos');
    $conn->close();
    exit();
}

// Verificar que el docente existe y es docente
$stmt = $conn->prepare("SELECT id_usuario, rol FROM usuarios WHERE id_usuario = ?");
$stmt->bind_param("i", $idDocente);
$stmt->execute();
$result = $stmt->get_result();
$docente = $result->fetch_assoc();
$stmt->close();

if (!$docente || $docente['rol'] !== 'docente') {
    http_response_code(400);
    echo jsonResponse(false, 'El docente especificado no existe o no es docente');
    $conn->close();
    exit();
}

// Verificar permisos: solo estudiantes pueden enviar solicitudes
if ($payload['rol'] !== 'estudiante' && $payload['rol'] !== 'administrador') {
    http_response_code(403);
    echo jsonResponse(false, 'Solo estudiantes pueden enviar solicitudes');
    $conn->close();
    exit();
}

// Verificar si ya existe la relación
$stmt = $conn->prepare("SELECT estado FROM docenteestudiante WHERE id_docente = ? AND id_estudiante = ?");
$stmt->bind_param("ii", $idDocente, $idEstudiante);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    http_response_code(409);
    echo jsonResponse(false, 'Ya existe una relación entre este docente y estudiante');
    $stmt->close();
    $conn->close();
    exit();
}
$stmt->close();

// Crear la solicitud
$stmt = $conn->prepare("INSERT INTO docenteestudiante (id_docente, id_estudiante, estado) VALUES (?, ?, 'pendiente')");
$stmt->bind_param("ii", $idDocente, $idEstudiante);

if (!$stmt->execute()) {
    http_response_code(500);
    echo jsonResponse(false, 'Error al enviar solicitud');
    $stmt->close();
    $conn->close();
    exit();
}

$response = [
    'success' => true,
    'message' => 'Solicitud enviada exitosamente',
    'data' => null
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
?>




