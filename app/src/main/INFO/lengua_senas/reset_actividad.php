<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

if (!isset($_GET['id_usuario']) || !isset($_GET['id_gesto'])) {
    http_response_code(400);
    echo jsonResponse(false, 'id_usuario e id_gesto son requeridos');
    $conn->close();
    exit();
}

$idUsuario = (int)$_GET['id_usuario'];
$idGesto = (int)$_GET['id_gesto'];

// Solo administrador puede resetear
if ($payload['rol'] !== 'administrador') {
    http_response_code(403);
    echo jsonResponse(false, 'Solo administradores pueden resetear actividades');
    $conn->close();
    exit();
}

$stmt = $conn->prepare("UPDATE usuario_gestos SET porcentaje = 0, estado = 'pendiente' WHERE id_usuario = ? AND id_gesto = ?");
$stmt->bind_param("ii", $idUsuario, $idGesto);

if ($stmt->execute()) {
    echo jsonResponse(true, 'Actividad reseteada exitosamente');
} else {
    http_response_code(500);
    echo jsonResponse(false, 'Error al resetear actividad');
}

$stmt->close();
$conn->close();
?>


