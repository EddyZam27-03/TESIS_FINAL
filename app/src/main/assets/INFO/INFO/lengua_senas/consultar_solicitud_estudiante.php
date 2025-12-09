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
$idEstudiante = isset($_GET['id_estudiante']) ? (int)$_GET['id_estudiante'] : (isset($_GET['estudiante_id']) ? (int)$_GET['estudiante_id'] : (isset($_GET['id_usuario']) ? (int)$_GET['id_usuario'] : (isset($_GET['usuario_id']) ? (int)$_GET['usuario_id'] : $payload['id_usuario'])));

// Verificar permisos
if ($idEstudiante != $payload['id_usuario'] && !in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver estas solicitudes');
    $conn->close();
    exit();
}

// Obtener solicitudes del estudiante
$stmt = $conn->prepare("SELECT 
    de.id_docente,
    de.id_estudiante,
    de.estado,
    u.id_usuario as docente_id,
    u.nombre as docente_nombre,
    u.correo as docente_correo
    FROM docenteestudiante de
    JOIN usuarios u ON de.id_docente = u.id_usuario
    WHERE de.id_estudiante = ?");
$stmt->bind_param("i", $idEstudiante);
$stmt->execute();
$result = $stmt->get_result();

$solicitudes = [];
$docenteActual = null;

while ($row = $result->fetch_assoc()) {
    $docenteInfo = [
        'id_usuario' => (int)$row['docente_id'],
        'nombre' => $row['docente_nombre'],
        'correo' => $row['docente_correo']
    ];
    
    $solicitud = [
        'id_docente' => (int)$row['id_docente'],
        'id_estudiante' => (int)$row['id_estudiante'],
        'estado' => $row['estado'],
        'docente' => $docenteInfo
    ];
    
    $solicitudes[] = $solicitud;
    
    // Si hay una relación aceptada, es el docente actual
    if ($row['estado'] === 'aceptado' && $docenteActual === null) {
        $docenteActual = $docenteInfo;
    }
}
$stmt->close();

$response = [
    'success' => true,
    'message' => 'Solicitudes obtenidas exitosamente',
    'solicitudes' => $solicitudes,
    'docente_actual' => $docenteActual,
    'total' => count($solicitudes)
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>




