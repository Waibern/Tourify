<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireMethod('POST');

try {
    $pdo = getDatabaseConnection();
    $userId = optionalAppUserId($pdo);
    if ($userId === null) {
        respond(401, ['success' => false, 'message' => 'Sesi aplikasi tidak valid. Silakan login kembali.']);
    }

    // Status online hanya berlaku untuk akun pelanggan.
    $statement = $pdo->prepare(
        "UPDATE users SET last_seen_at = CURRENT_TIMESTAMP WHERE id = :id AND role = 'customer'"
    );
    $statement->execute(['id' => $userId]);

    respond(200, ['success' => true]);
} catch (Throwable $exception) {
    handleApiError('customer presence', $exception);
}
