<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$data = json_decode(file_get_contents('php://input'), true);

if (!isset($data['correo']) || !isset($data['contrasena'])) {
    http_response_code(400);
    echo jsonResponse(false, 'Correo y contraseña son requeridos');
    exit();
}

$conn = getDBConnection();
$correo = $conn->real_escape_string($data['correo']);
$contrasena = $data['contrasena'];

$stmt = $conn->prepare("SELECT id_usuario, nombre, correo, contrasena, rol, fecha_registro FROM usuarios WHERE correo = ?");
$stmt->bind_param("s", $correo);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    http_response_code(401);
    echo jsonResponse(false, 'Credenciales inválidas');
    $stmt->close();
    $conn->close();
    exit();
}

$usuario = $result->fetch_assoc();

// Verificar contraseña (puede estar hasheada o en texto plano)
$passwordValid = false;
if (password_verify($contrasena, $usuario['contrasena'])) {
    $passwordValid = true;
} elseif ($usuario['contrasena'] === $contrasena) {
    // Compatibilidad con contraseñas en texto plano (solo para desarrollo)
    $passwordValid = true;
}

if (!$passwordValid) {
    http_response_code(401);
    echo jsonResponse(false, 'Credenciales inválidas');
    $stmt->close();
    $conn->close();
    exit();
}

$token = generateJWT($usuario['id_usuario'], $usuario['rol']);

$usuarioResponse = [
    'id_usuario' => (int)$usuario['id_usuario'],
    'nombre' => $usuario['nombre'],
    'correo' => $usuario['correo'],
    'contrasena' => null, // No enviar contraseña
    'rol' => $usuario['rol'],
    'fecha_registro' => $usuario['fecha_registro']
];

$response = [
    'success' => true,
    'message' => 'Login exitoso',
    'usuario' => $usuarioResponse,
    'token' => $token
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
?>


