<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

// Obtener todos los docentes
$query = "SELECT id_usuario, nombre, correo, rol, fecha_registro FROM usuarios WHERE rol = 'docente' ORDER BY nombre";
$result = $conn->query($query);

if (!$result) {
    http_response_code(500);
    echo jsonResponse(false, 'Error al obtener docentes');
    $conn->close();
    exit();
}

$docentes = [];
while ($row = $result->fetch_assoc()) {
    $docentes[] = [
        'id_usuario' => (int)$row['id_usuario'],
        'nombre' => $row['nombre'],
        'correo' => $row['correo'],
        'rol' => $row['rol'],
        'fecha_registro' => $row['fecha_registro']
    ];
}

echo json_encode($docentes, JSON_UNESCAPED_UNICODE);

$conn->close();
?>




