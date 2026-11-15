<?php
require_once __DIR__ . '/db.php';

function respond(int $statusCode, array $body): void
{
    http_response_code($statusCode);
    echo json_encode($body, JSON_UNESCAPED_UNICODE);
    exit;
}

function requireMethod(string $method): void
{
    if ($_SERVER['REQUEST_METHOD'] !== $method) {
        header('Allow: ' . $method);
        respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
    }
}

// Token ditandatangani dengan hash password akun dan berlaku 24 jam.
function createAppAccessToken(int $userId, string $passwordHash): string
{
    $payload = $userId . ':' . (time() + 86400) . ':' . bin2hex(random_bytes(16));
    $encodedPayload = rtrim(strtr(base64_encode($payload), '+/', '-_'), '=');
    $signature = hash_hmac('sha256', $encodedPayload, $passwordHash, true);
    $encodedSignature = rtrim(strtr(base64_encode($signature), '+/', '-_'), '=');

    return $encodedPayload . '.' . $encodedSignature;
}

// Profil bisa diakses dengan token aplikasi atau sesi dashboard yang sah.
function optionalAppUserId(PDO $pdo): ?int
{
    $authorization = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    // Di MAMP, header Authorization bisa pindah ke REDIRECT_* saat PHP memakai FastCGI.
    if ($authorization === '') {
        $authorization = $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
    }
    if ($authorization === '' && function_exists('getallheaders')) {
        foreach (getallheaders() as $name => $value) {
            if (strtolower($name) === 'authorization') {
                $authorization = $value;
                break;
            }
        }
    }
    if ($authorization === '') {
        return null;
    }
    if (!preg_match('/^Bearer\s+(\S+)$/i', trim($authorization), $matches)) {
        respond(401, ['success' => false, 'message' => 'Sesi aplikasi tidak valid. Silakan login kembali.']);
    }

    $tokenParts = explode('.', $matches[1]);
    if (count($tokenParts) !== 2) {
        respond(401, ['success' => false, 'message' => 'Sesi aplikasi tidak valid. Silakan login kembali.']);
    }
    [$encodedPayload, $encodedSignature] = $tokenParts;
    $decodeBase64Url = static function (string $value) {
        $base64 = strtr($value, '-_', '+/');
        $base64 .= str_repeat('=', (4 - strlen($base64) % 4) % 4);
        return base64_decode($base64, true);
    };
    $payload = $decodeBase64Url($encodedPayload);
    $signature = $decodeBase64Url($encodedSignature);
    if ($payload === false || $signature === false) {
        respond(401, ['success' => false, 'message' => 'Sesi aplikasi tidak valid. Silakan login kembali.']);
    }

    $claims = explode(':', $payload);
    if (
        count($claims) !== 3
        || !ctype_digit($claims[0])
        || !ctype_digit($claims[1])
        || !preg_match('/^[a-f0-9]{32}$/', $claims[2])
        || (int) $claims[1] <= time()
    ) {
        respond(401, ['success' => false, 'message' => 'Sesi aplikasi sudah tidak berlaku. Silakan login kembali.']);
    }

    $userId = (int) $claims[0];
    $statement = $pdo->prepare('SELECT password_hash FROM users WHERE id = :id LIMIT 1');
    $statement->execute(['id' => $userId]);
    $passwordHash = $statement->fetchColumn();
    if (!is_string($passwordHash)) {
        respond(401, ['success' => false, 'message' => 'Akun tidak ditemukan. Silakan login kembali.']);
    }

    $expectedSignature = hash_hmac('sha256', $encodedPayload, $passwordHash, true);
    if (!hash_equals($expectedSignature, $signature)) {
        respond(401, ['success' => false, 'message' => 'Sesi aplikasi tidak valid. Silakan login kembali.']);
    }

    return $userId;
}

function readJsonInput(): array
{
    $input = json_decode(file_get_contents('php://input'), true);
    if (!is_array($input)) {
        respond(400, ['success' => false, 'message' => 'Body harus berupa JSON yang valid.']);
    }
    return $input;
}

// Izinkan alamat dashboard yang terdaftar dan dukung cookie sesi.
function configureDashboardCors(): void
{
    $origin = $_SERVER['HTTP_ORIGIN'] ?? '';
    $allowedOrigin = getenv('TOURIFY_DASHBOARD_ORIGIN') ?: 'http://localhost:3000';

    header('Vary: Origin');
    if ($origin !== '' && $origin !== $allowedOrigin) {
        respond(403, ['success' => false, 'message' => 'Asal dashboard tidak diizinkan.']);
    }

    if ($origin === $allowedOrigin) {
        header('Access-Control-Allow-Origin: ' . $origin);
        header('Access-Control-Allow-Credentials: true');
        header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
        header('Access-Control-Allow-Headers: Content-Type');
    }

    if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
        http_response_code(204);
        exit;
    }
}

// Sesi ini untuk dashboard; pelanggan tetap login lewat aplikasi Android.
function startDashboardSession(): void
{
    if (session_status() === PHP_SESSION_ACTIVE) {
        return;
    }

    $isHttps = !empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off';
    session_set_cookie_params([
        'lifetime' => 0,
        'path' => getenv('TOURIFY_API_SESSION_PATH') ?: '/tourify-api',
        'secure' => $isHttps,
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    session_start();
}

function destroyDashboardSession(): void
{
    startDashboardSession();
    $_SESSION = [];
    $cookie = session_get_cookie_params();
    setcookie(session_name(), '', [
        'expires' => time() - 42000,
        'path' => $cookie['path'],
        'secure' => $cookie['secure'],
        'httponly' => $cookie['httponly'],
        'samesite' => $cookie['samesite'] ?? 'Lax',
    ]);
    session_destroy();
}

// Server tetap memeriksa peran admin atau gate untuk setiap aksi.
function requireDashboardRole(array $allowedRoles): array
{
    startDashboardSession();
    $user = $_SESSION['dashboard_user'] ?? null;

    if (!is_array($user) || !isset($user['id'], $user['username'], $user['role'])) {
        respond(401, ['success' => false, 'message' => 'Silakan login ke dashboard.']);
    }

    // Cek lagi peran di database supaya perubahan akses langsung berlaku.
    try {
        $statement = getDatabaseConnection()->prepare(
            'SELECT id, username, role FROM users WHERE id = :id LIMIT 1'
        );
        $statement->execute(['id' => (int) $user['id']]);
        $account = $statement->fetch();
    } catch (Throwable $exception) {
        handleApiError('dashboard authorization', $exception);
    }
    if (!$account || !in_array($account['role'], ['admin', 'gate'], true)) {
        destroyDashboardSession();
        respond(401, ['success' => false, 'message' => 'Akun dashboard tidak lagi tersedia. Silakan login kembali.']);
    }

    $user = [
        'id' => (int) $account['id'],
        'username' => $account['username'],
        'role' => $account['role'],
    ];
    $_SESSION['dashboard_user'] = $user;
    if (!in_array($user['role'], $allowedRoles, true)) {
        respond(403, ['success' => false, 'message' => 'Akun tidak punya akses ke fitur ini.']);
    }

    return $user;
}

function handleApiError(string $context, Throwable $exception): void
{
    error_log('Tourify ' . $context . ' error: ' . $exception->getMessage());
    respond(500, ['success' => false, 'message' => 'Terjadi kesalahan pada server.']);
}
