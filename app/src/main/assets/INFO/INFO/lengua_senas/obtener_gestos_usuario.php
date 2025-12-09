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

// Verificar permisos: solo puede ver su propio progreso o ser docente/administrador
if ($idUsuario != $payload['id_usuario'] && !in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver este progreso');
    $conn->close();
    exit();
}

$stmt = $conn->prepare("SELECT id_usuario, id_gesto, porcentaje, estado FROM usuario_gestos WHERE id_usuario = ?");
$stmt->bind_param("i", $idUsuario);
$stmt->execute();
$result = $stmt->get_result();

$progresos = [];
while ($row = $result->fetch_assoc()) {
    $progresos[] = [
        'id_usuario' => (int)$row['id_usuario'],
        'id_gesto' => (int)$row['id_gesto'],
        'porcentaje' => (int)$row['porcentaje'],
        'estado' => $row['estado']
    ];
}

echo json_encode($progresos, JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
?>




