<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Configuración de la base de datos
define('DB_HOST', 'localhost');
define('DB_USER', 'root');
define('DB_PASS', '');
define('DB_NAME', 'lengua_senas');

const GESTO_ESTADOS_PERMITIDOS = ['pendiente', 'aprendido'];

// Secret key para JWT (cambiar en producción)
define('JWT_SECRET', 'your-secret-key-change-in-production');
define('JWT_ALGORITHM', 'HS256');

// Conexión a la base de datos
function getDBConnection() {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
    
    if ($conn->connect_error) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error de conexión a la base de datos'
        ]);
        exit();
    }
    
    $conn->set_charset('utf8mb4');
    return $conn;
}

// Función para generar JWT
function generateJWT($userId, $rol) {
    $header = base64_encode(json_encode(['typ' => 'JWT', 'alg' => JWT_ALGORITHM]));
    $payload = base64_encode(json_encode([
        'id_usuario' => $userId,
        'rol' => $rol,
        'iat' => time(),
        'exp' => time() + (7 * 24 * 60 * 60) // 7 días
    ]));
    $signature = base64_encode(hash_hmac('sha256', "$header.$payload", JWT_SECRET, true));
    return "$header.$payload.$signature";
}

// Función para validar JWT
function validateJWT($token) {
    $parts = explode('.', $token);
    if (count($parts) !== 3) {
        return null;
    }
    
    $header = json_decode(base64_decode($parts[0]), true);
    $payload = json_decode(base64_decode($parts[1]), true);
    $signature = base64_decode($parts[2]);
    
    $expectedSignature = hash_hmac('sha256', "$parts[0].$parts[1]", JWT_SECRET, true);
    
    if (!hash_equals($signature, $expectedSignature)) {
        return null;
    }
    
    if (isset($payload['exp']) && $payload['exp'] < time()) {
        return null;
    }
    
    return $payload;
}

// Función para obtener token del header
function getTokenFromHeader() {
    $headers = getallheaders();
    if (isset($headers['Authorization'])) {
        $authHeader = $headers['Authorization'];
        if (preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
            return $matches[1];
        }
    }
    return null;
}

// Función para validar autenticación
function requireAuth() {
    $token = getTokenFromHeader();
    if (!$token) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Token no proporcionado'
        ]);
        exit();
    }
    
    $payload = validateJWT($token);
    if (!$payload) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Token inválido o expirado'
        ]);
        exit();
    }
    
    return $payload;
}

// Función para respuesta JSON
function jsonResponse($success, $message, $data = null, $token = null) {
    $response = [
        'success' => $success,
        'message' => $message
    ];
    
    if ($data !== null) {
        $response['data'] = $data;
    }
    
    if ($token !== null) {
        $response['token'] = $token;
    }
    
    return json_encode($response, JSON_UNESCAPED_UNICODE);
}

function normalizeGestoEstado($estado) {
    if (is_string($estado) && in_array($estado, GESTO_ESTADOS_PERMITIDOS, true)) {
        return $estado;
    }
    return 'pendiente';
}

function shouldUpdateGestoRegistro($existingPorcentaje, $existingEstado, $nuevoPorcentaje, $nuevoEstado) {
    if ($nuevoPorcentaje > $existingPorcentaje) {
        return true;
    }
    if ($nuevoPorcentaje === $existingPorcentaje &&
        $existingEstado !== 'aprendido' &&
        $nuevoEstado === 'aprendido') {
        return true;
    }
    return false;
}

?>


