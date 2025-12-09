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
$idDocente = isset($_GET['id_docente']) ? (int)$_GET['id_docente'] : (isset($_GET['docente_id']) ? (int)$_GET['docente_id'] : (isset($_GET['usuario_id']) ? (int)$_GET['usuario_id'] : $payload['id_usuario']));

// Verificar permisos: solo el docente puede ver sus estudiantes
if ($idDocente != $payload['id_usuario'] && $payload['rol'] !== 'administrador') {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver estos estudiantes');
    $conn->close();
    exit();
}

// Obtener estudiantes del docente (solo relaciones aceptadas)
$query = "SELECT 
    u.id_usuario,
    u.nombre,
    u.correo,
    u.rol,
    u.fecha_registro,
    de.estado
    FROM docenteestudiante de
    JOIN usuarios u ON de.id_estudiante = u.id_usuario
    WHERE de.id_docente = ? AND de.estado = 'aceptado'
    ORDER BY u.nombre";
$stmt = $conn->prepare($query);
$stmt->bind_param("i", $idDocente);
$stmt->execute();
$result = $stmt->get_result();

$estudiantes = [];
while ($row = $result->fetch_assoc()) {
    $estudiantes[] = [
        'id_usuario' => (int)$row['id_usuario'],
        'nombre' => $row['nombre'],
        'correo' => $row['correo'],
        'rol' => $row['rol'],
        'fecha_registro' => $row['fecha_registro']
    ];
}
$stmt->close();

echo json_encode($estudiantes, JSON_UNESCAPED_UNICODE);

$conn->close();
?>




