<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

// Aceptar múltiples parámetros para flexibilidad
$idUsuario = isset($_GET['id_usuario']) ? (int)$_GET['id_usuario'] : (isset($_GET['usuario_id']) ? (int)$_GET['usuario_id'] : null);
$idAdmin = isset($_GET['id_admin']) ? (int)$_GET['id_admin'] : null;
$idEstudiante = isset($_GET['id_estudiante']) ? (int)$_GET['id_estudiante'] : null;

// Determinar el usuario objetivo
$targetUsuarioId = $idEstudiante ?: $idUsuario ?: $payload['id_usuario'];

// Verificar permisos
if ($targetUsuarioId != $payload['id_usuario'] && !in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver este progreso');
    $conn->close();
    exit();
}

// Obtener información del usuario
$stmt = $conn->prepare("SELECT id_usuario, nombre, correo, rol FROM usuarios WHERE id_usuario = ?");
$stmt->bind_param("i", $targetUsuarioId);
$stmt->execute();
$result = $stmt->get_result();
$usuario = $result->fetch_assoc();
$stmt->close();

if (!$usuario) {
    http_response_code(404);
    echo jsonResponse(false, 'Usuario no encontrado');
    $conn->close();
    exit();
}

// Obtener progreso detallado
$query = "SELECT 
    ug.id_usuario,
    u.nombre,
    u.correo,
    ug.id_gesto,
    g.nombre as nombre_gesto,
    g.categoria,
    g.dificultad,
    ug.porcentaje,
    ug.estado,
    (SELECT COUNT(*) FROM gestos) as total_gestos,
    (SELECT COUNT(*) FROM usuario_gestos WHERE id_usuario = ? AND estado = 'aprendido') as gestos_aprendidos,
    (SELECT AVG(porcentaje) FROM usuario_gestos WHERE id_usuario = ?) as promedio_progreso
    FROM usuario_gestos ug
    JOIN usuarios u ON ug.id_usuario = u.id_usuario
    JOIN gestos g ON ug.id_gesto = g.id_gesto
    WHERE ug.id_usuario = ?
    ORDER BY g.categoria, g.nombre";
$stmt = $conn->prepare($query);
$stmt->bind_param("iii", $targetUsuarioId, $targetUsuarioId, $targetUsuarioId);
$stmt->execute();
$result = $stmt->get_result();

$progresoDetalle = [];
$totalGestos = 0;
$gestosAprendidos = 0;
$promedioProgreso = 0.0;

while ($row = $result->fetch_assoc()) {
    $totalGestos = (int)$row['total_gestos'];
    $gestosAprendidos = (int)$row['gestos_aprendidos'];
    $promedioProgreso = (float)$row['promedio_progreso'];
    
    $progresoDetalle[] = [
        'id_usuario' => (int)$row['id_usuario'],
        'nombre' => $row['nombre'],
        'correo' => $row['correo'],
        'id_gesto' => (int)$row['id_gesto'],
        'nombre_gesto' => $row['nombre_gesto'],
        'categoria' => $row['categoria'],
        'dificultad' => $row['dificultad'],
        'porcentaje' => (int)$row['porcentaje'],
        'estado' => $row['estado'],
        'total_gestos' => $totalGestos,
        'gestos_aprendidos' => $gestosAprendidos,
        'promedio_progreso' => round($promedioProgreso, 2)
    ];
}
$stmt->close();

// Si no hay progreso, obtener estadísticas básicas
if (empty($progresoDetalle)) {
    $stmt = $conn->prepare("SELECT COUNT(*) as total_gestos FROM gestos");
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $totalGestos = (int)$row['total_gestos'];
    $stmt->close();
}

$response = [
    'tiempoTotal' => 0, // No disponible en la BD actual
    'leccionesCompletadas' => $gestosAprendidos,
    'totalLecciones' => $totalGestos,
    'precision' => $promedioProgreso > 0 ? (float)$promedioProgreso / 100.0 : 0.0,
    'rachaDias' => 0, // No disponible en la BD actual
    'progreso' => $progresoDetalle
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>




