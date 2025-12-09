<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

// Aceptar tanto usuario_id como id_usuario
$idUsuario = isset($_GET['id_usuario']) ? (int)$_GET['id_usuario'] : (isset($_GET['usuario_id']) ? (int)$_GET['usuario_id'] : $payload['id_usuario']);
$categoria = isset($_GET['categoria']) ? $conn->real_escape_string($_GET['categoria']) : null;

// Verificar permisos
if ($idUsuario != $payload['id_usuario'] && !in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver estos datos');
    $conn->close();
    exit();
}

// Obtener información del usuario
$stmt = $conn->prepare("SELECT id_usuario, nombre, correo, rol FROM usuarios WHERE id_usuario = ?");
$stmt->bind_param("i", $idUsuario);
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

$usuarioHome = [
    'id_usuario' => (int)$usuario['id_usuario'],
    'nombre' => $usuario['nombre'],
    'correo' => $usuario['correo'],
    'rol' => $usuario['rol']
];

// Obtener estadísticas
$queryEstadisticas = "SELECT 
    COALESCE(SUM(porcentaje), 0) as tiempo_total_minutos,
    COALESCE(AVG(porcentaje), 0) as promedio_progreso,
    COUNT(CASE WHEN estado = 'pendiente' THEN 1 END) as actividades_incompletas,
    COUNT(CASE WHEN estado = 'aprendido' THEN 1 END) as gestos_aprendidos
    FROM usuario_gestos WHERE id_usuario = ?";
$stmt = $conn->prepare($queryEstadisticas);
$stmt->bind_param("i", $idUsuario);
$stmt->execute();
$result = $stmt->get_result();
$estadisticasRow = $result->fetch_assoc();
$stmt->close();

$estadisticas = [
    'tiempo_total_minutos' => (int)$estadisticasRow['tiempo_total_minutos'],
    'promedio_progreso' => (int)round($estadisticasRow['promedio_progreso']),
    'actividades_incompletas' => (int)$estadisticasRow['actividades_incompletas'],
    'gestos_aprendidos' => (int)$estadisticasRow['gestos_aprendidos']
];

// Obtener actividades (gestos con progreso)
$queryActividades = "SELECT 
    g.id_gesto, 
    g.nombre, 
    g.categoria, 
    g.dificultad,
    COALESCE(ug.porcentaje, 0) as porcentaje,
    COALESCE(ug.estado, 'pendiente') as estado
    FROM gestos g
    LEFT JOIN usuario_gestos ug ON g.id_gesto = ug.id_gesto AND ug.id_usuario = ?
    " . ($categoria ? "WHERE g.categoria = ?" : "") . "
    ORDER BY g.categoria, g.nombre";
$stmt = $categoria ? $conn->prepare($queryActividades) : $conn->prepare($queryActividades);
if ($categoria) {
    $stmt->bind_param("is", $idUsuario, $categoria);
} else {
    $stmt->bind_param("i", $idUsuario);
}
$stmt->execute();
$result = $stmt->get_result();

$actividades = [];
while ($row = $result->fetch_assoc()) {
    $actividades[] = [
        'id_gesto' => (int)$row['id_gesto'],
        'nombre' => $row['nombre'],
        'categoria' => $row['categoria'],
        'dificultad' => $row['dificultad'],
        'porcentaje' => (int)$row['porcentaje'],
        'estado' => $row['estado']
    ];
}
$stmt->close();

// Obtener categorías con estadísticas
$queryCategorias = "SELECT 
    g.categoria,
    COUNT(DISTINCT g.id_gesto) as total,
    COUNT(DISTINCT CASE WHEN ug.estado = 'aprendido' THEN g.id_gesto END) as aprendidos
    FROM gestos g
    LEFT JOIN usuario_gestos ug ON g.id_gesto = ug.id_gesto AND ug.id_usuario = ?
    GROUP BY g.categoria
    ORDER BY g.categoria";
$stmt = $conn->prepare($queryCategorias);
$stmt->bind_param("i", $idUsuario);
$stmt->execute();
$result = $stmt->get_result();

$categorias = [];
while ($row = $result->fetch_assoc()) {
    $categorias[] = [
        'categoria' => $row['categoria'],
        'total' => (int)$row['total'],
        'aprendidos' => (int)$row['aprendidos']
    ];
}
$stmt->close();

$response = [
    'success' => true,
    'message' => 'Datos obtenidos exitosamente',
    'usuario' => $usuarioHome,
    'estadisticas' => $estadisticas,
    'actividades' => $actividades,
    'categorias' => $categorias
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>


