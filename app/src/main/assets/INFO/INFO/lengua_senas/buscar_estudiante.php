<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

// Solo docentes y administradores pueden buscar estudiantes
if (!in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para buscar estudiantes');
    $conn->close();
    exit();
}

$busqueda = isset($_GET['busqueda']) ? $conn->real_escape_string($_GET['busqueda']) : null;
$correo = isset($_GET['correo']) ? $conn->real_escape_string($_GET['correo']) : null;

if ($busqueda === null && $correo === null) {
    http_response_code(400);
    echo jsonResponse(false, 'busqueda o correo es requerido');
    $conn->close();
    exit();
}

// Construir query de búsqueda
if ($correo) {
    $query = "SELECT id_usuario, nombre, correo, rol, fecha_registro FROM usuarios WHERE rol = 'estudiante' AND correo = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("s", $correo);
} else {
    $busquedaPattern = "%$busqueda%";
    $query = "SELECT id_usuario, nombre, correo, rol, fecha_registro FROM usuarios WHERE rol = 'estudiante' AND (nombre LIKE ? OR correo LIKE ?)";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("ss", $busquedaPattern, $busquedaPattern);
}

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




