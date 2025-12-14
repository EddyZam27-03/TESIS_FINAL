<?php
require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo jsonResponse(false, 'Método no permitido');
    exit();
}

$data = json_decode(file_get_contents('php://input'), true);

if (!isset($data['nombre']) || !isset($data['correo']) || !isset($data['contrasena']) || !isset($data['rol'])) {
    http_response_code(400);
    echo jsonResponse(false, 'Todos los campos son requeridos');
    exit();
}

$conn = getDBConnection();
$nombre = $conn->real_escape_string($data['nombre']);
$correo = $conn->real_escape_string($data['correo']);
$contrasena = password_hash($data['contrasena'], PASSWORD_DEFAULT);
$rol = $conn->real_escape_string($data['rol']);

// Validar que el rol sea válido
if (!in_array($rol, ['estudiante', 'docente', 'administrador'])) {
    http_response_code(400);
    echo jsonResponse(false, 'Rol inválido');
    $conn->close();
    exit();
}

// Verificar si el correo ya existe
$stmt = $conn->prepare("SELECT id_usuario FROM usuarios WHERE correo = ?");
$stmt->bind_param("s", $correo);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    http_response_code(409);
    echo jsonResponse(false, 'El correo ya está registrado');
    $stmt->close();
    $conn->close();
    exit();
}
$stmt->close();

// Insertar nuevo usuario
$stmt = $conn->prepare("INSERT INTO usuarios (nombre, correo, contrasena, rol) VALUES (?, ?, ?, ?)");
$stmt->bind_param("ssss", $nombre, $correo, $contrasena, $rol);

if (!$stmt->execute()) {
    http_response_code(500);
    echo jsonResponse(false, 'Error al registrar usuario');
    $stmt->close();
    $conn->close();
    exit();
}

$idUsuario = $conn->insert_id;
$fechaRegistro = date('Y-m-d H:i:s');

$token = generateJWT($idUsuario, $rol);

$usuarioResponse = [
    'id_usuario' => $idUsuario,
    'nombre' => $nombre,
    'correo' => $correo,
    'contrasena' => null,
    'rol' => $rol,
    'fecha_registro' => $fechaRegistro
];

$response = [
    'success' => true,
    'message' => 'Registro exitoso',
    'usuario' => $usuarioResponse,
    'token' => $token
];

echo json_encode($response, JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
?>




