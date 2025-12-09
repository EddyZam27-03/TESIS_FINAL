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
$idEstudiante = isset($_GET['id_estudiante']) ? (int)$_GET['id_estudiante'] : (isset($_GET['estudiante_id']) ? (int)$_GET['estudiante_id'] : null);

if ($idEstudiante === null) {
    http_response_code(400);
    echo jsonResponse(false, 'id_estudiante es requerido');
    $conn->close();
    exit();
}

// Verificar permisos: solo el docente puede ver el progreso de sus estudiantes
if ($idDocente != $payload['id_usuario'] && $payload['rol'] !== 'administrador') {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver este progreso');
    $conn->close();
    exit();
}

// Verificar que existe la relación docente-estudiante
$stmt = $conn->prepare("SELECT estado FROM docenteestudiante WHERE id_docente = ? AND id_estudiante = ? AND estado = 'aceptado'");
$stmt->bind_param("ii", $idDocente, $idEstudiante);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0 && $payload['rol'] !== 'administrador') {
    http_response_code(403);
    echo jsonResponse(false, 'No existe una relación activa con este estudiante');
    $stmt->close();
    $conn->close();
    exit();
}
$stmt->close();

// Obtener información del estudiante
$stmt = $conn->prepare("SELECT id_usuario, nombre, correo, rol FROM usuarios WHERE id_usuario = ?");
$stmt->bind_param("i", $idEstudiante);
$stmt->execute();
$result = $stmt->get_result();
$estudiante = $result->fetch_assoc();
$stmt->close();

if (!$estudiante) {
    http_response_code(404);
    echo jsonResponse(false, 'Estudiante no encontrado');
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
$stmt->bind_param("iii", $idEstudiante, $idEstudiante, $idEstudiante);
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
    'tiempoTotal' => 0,
    'leccionesCompletadas' => $gestosAprendidos,
    'totalLecciones' => $totalGestos,
    'precision' => $promedioProgreso > 0 ? (float)$promedioProgreso / 100.0 : 0.0,
    'rachaDias' => 0,
    'progreso' => $progresoDetalle
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>




