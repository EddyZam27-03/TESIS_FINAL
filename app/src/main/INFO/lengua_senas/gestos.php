<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

requireAuth(); // Requiere autenticación

$conn = getDBConnection();

$query = "SELECT id_gesto, nombre, dificultad, categoria FROM gestos ORDER BY categoria, nombre";
$result = $conn->query($query);

if (!$result) {
    http_response_code(500);
    echo jsonResponse(false, 'Error al obtener gestos');
    $conn->close();
    exit();
}

$gestos = [];
while ($row = $result->fetch_assoc()) {
    $gestos[] = [
        'id_gesto' => (int)$row['id_gesto'],
        'nombre' => $row['nombre'],
        'dificultad' => $row['dificultad'],
        'categoria' => $row['categoria']
    ];
}

$response = [
    'success' => true,
    'message' => 'Gestos obtenidos exitosamente',
    'gestos' => $gestos
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$conn->close();
?>


