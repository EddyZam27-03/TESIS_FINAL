<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

// Aceptar múltiples nombres de parámetros
$idDocente = isset($_GET['id_docente']) ? (int)$_GET['id_docente'] : (isset($_GET['docente_id']) ? (int)$_GET['docente_id'] : (isset($_GET['id_usuario']) ? (int)$_GET['id_usuario'] : (isset($_GET['usuario_id']) ? (int)$_GET['usuario_id'] : $payload['id_usuario'])));
$estado = isset($_GET['estado']) ? $conn->real_escape_string($_GET['estado']) : null;

// Verificar permisos
if ($idDocente != $payload['id_usuario'] && !in_array($payload['rol'], ['administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver estas solicitudes');
    $conn->close();
    exit();
}

// Construir query con filtro de estado opcional
$query = "SELECT 
    de.id_docente,
    de.id_estudiante,
    de.estado,
    u.id_usuario as estudiante_id,
    u.nombre as estudiante_nombre,
    u.correo as estudiante_correo
    FROM docenteestudiante de
    JOIN usuarios u ON de.id_estudiante = u.id_usuario
    WHERE de.id_docente = ?";
    
if ($estado) {
    $query .= " AND de.estado = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("is", $idDocente, $estado);
} else {
    $stmt = $conn->prepare($query);
    $stmt->bind_param("i", $idDocente);
}

$stmt->execute();
$result = $stmt->get_result();

$solicitudes = [];
while ($row = $result->fetch_assoc()) {
    $estudianteInfo = [
        'id_usuario' => (int)$row['estudiante_id'],
        'nombre' => $row['estudiante_nombre'],
        'correo' => $row['estudiante_correo']
    ];
    
    $solicitudes[] = [
        'id_docente' => (int)$row['id_docente'],
        'id_estudiante' => (int)$row['id_estudiante'],
        'estado' => $row['estado'],
        'estudiante' => $estudianteInfo
    ];
}
$stmt->close();

$response = [
    'success' => true,
    'message' => 'Solicitudes obtenidas exitosamente',
    'solicitudes' => $solicitudes,
    'total' => count($solicitudes)
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>




