<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$payload = requireAuth();
$conn = getDBConnection();

$idUsuario = isset($_GET['id_usuario']) ? (int)$_GET['id_usuario'] : $payload['id_usuario'];
$formato = isset($_GET['formato']) ? $_GET['formato'] : 'pdf';

// Verificar permisos
if ($idUsuario != $payload['id_usuario'] && !in_array($payload['rol'], ['docente', 'administrador'])) {
    http_response_code(403);
    echo jsonResponse(false, 'No tiene permisos para ver este reporte');
    $conn->close();
    exit();
}

// Obtener información del usuario
$stmt = $conn->prepare("SELECT nombre, correo, rol FROM usuarios WHERE id_usuario = ?");
$stmt->bind_param("i", $idUsuario);
$stmt->execute();
$result = $stmt->get_result();
$usuario = $result->fetch_assoc();
$stmt->close();

// Obtener progreso
$stmt = $conn->prepare("SELECT ug.id_gesto, g.nombre, ug.porcentaje, ug.estado FROM usuario_gestos ug JOIN gestos g ON ug.id_gesto = g.id_gesto WHERE ug.id_usuario = ?");
$stmt->bind_param("i", $idUsuario);
$stmt->execute();
$result = $stmt->get_result();
$progresos = [];
while ($row = $result->fetch_assoc()) {
    $progresos[] = $row;
}
$stmt->close();

if ($formato === 'csv') {
    header('Content-Type: text/csv; charset=utf-8');
    header('Content-Disposition: attachment; filename="reporte_' . $idUsuario . '.csv"');
    
    $output = fopen('php://output', 'w');
    fprintf($output, chr(0xEF).chr(0xBB).chr(0xBF)); // BOM para UTF-8
    
    fputcsv($output, ['Usuario', 'Correo', 'Rol'], ';');
    fputcsv($output, [$usuario['nombre'], $usuario['correo'], $usuario['rol']], ';');
    fputcsv($output, [], ';');
    fputcsv($output, ['Gesto', 'Porcentaje', 'Estado'], ';');
    
    foreach ($progresos as $progreso) {
        fputcsv($output, [$progreso['nombre'], $progreso['porcentaje'], $progreso['estado']], ';');
    }
    
    fclose($output);
} else {
    // PDF (requiere librería como TCPDF o FPDF)
    // Por ahora, retornamos JSON con los datos
    header('Content-Type: application/json');
    echo jsonResponse(true, 'Reporte generado', [
        'usuario' => $usuario,
        'progresos' => $progresos
    ]);
}

$conn->close();
?>


