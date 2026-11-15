<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if (!in_array($method, ['POST', 'DELETE'], true)) {
    header('Allow: POST, DELETE, OPTIONS');
    respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
}

try {
    $dashboardUser = requireDashboardRole(['admin', 'gate']);
    $input = readJsonInput();
    $currentPassword = $input['current_password'] ?? '';
    if (!is_string($currentPassword) || $currentPassword === '') {
        respond(400, ['success' => false, 'message' => 'Masukkan password saat ini.']);
    }

    if ($method === 'POST') {
        $newPassword = $input['new_password'] ?? '';
        if (!is_string($newPassword) || strlen($newPassword) < 8 || strlen($newPassword) > 72) {
            respond(400, ['success' => false, 'message' => 'Password baru harus terdiri dari 8 sampai 72 byte.']);
        }
        if (hash_equals($currentPassword, $newPassword)) {
            respond(400, ['success' => false, 'message' => 'Password baru harus berbeda dari password saat ini.']);
        }

        $pdo = getDatabaseConnection();
        $pdo->beginTransaction();
        $statement = $pdo->prepare('SELECT password_hash FROM users WHERE id = :id LIMIT 1 FOR UPDATE');
        $statement->execute(['id' => (int) $dashboardUser['id']]);
        $passwordHash = $statement->fetchColumn();
        if (!is_string($passwordHash)) {
            $pdo->rollBack();
            respond(404, ['success' => false, 'message' => 'Akun dashboard tidak ditemukan.']);
        }
        if (!password_verify($currentPassword, $passwordHash)) {
            $pdo->rollBack();
            respond(401, ['success' => false, 'message' => 'Password saat ini tidak cocok.']);
        }

        $update = $pdo->prepare('UPDATE users SET password_hash = :password_hash WHERE id = :id');
        $update->execute([
            'password_hash' => password_hash($newPassword, PASSWORD_DEFAULT),
            'id' => (int) $dashboardUser['id'],
        ]);
        $pdo->commit();
        session_regenerate_id(true);

        respond(200, ['success' => true, 'message' => 'Password dashboard berhasil diperbarui.']);
    }

    if (($input['confirm'] ?? false) !== true) {
        respond(400, ['success' => false, 'message' => 'Konfirmasi penghapusan akun diperlukan.']);
    }

    $pdo = getDatabaseConnection();
    $pdo->beginTransaction();
    // Kunci daftar admin supaya dua request tidak menghapus admin terakhir bersamaan.
    $adminIds = $pdo->query(
        "SELECT id FROM users WHERE role = 'admin' ORDER BY id FOR UPDATE"
    )->fetchAll(PDO::FETCH_COLUMN);
    $statement = $pdo->prepare(
        'SELECT id, role, password_hash, profile_image FROM users WHERE id = :id LIMIT 1 FOR UPDATE'
    );
    $statement->execute(['id' => (int) $dashboardUser['id']]);
    $account = $statement->fetch();
    if (!$account || !in_array($account['role'], ['admin', 'gate'], true)) {
        $pdo->rollBack();
        respond(404, ['success' => false, 'message' => 'Akun dashboard tidak ditemukan.']);
    }
    if (!password_verify($currentPassword, $account['password_hash'])) {
        $pdo->rollBack();
        respond(401, ['success' => false, 'message' => 'Password saat ini tidak cocok.']);
    }
    if ($account['role'] === 'admin' && count($adminIds) <= 1) {
        $pdo->rollBack();
        respond(409, ['success' => false, 'message' => 'Admin terakhir tidak bisa dihapus.']);
    }

    // Jangan hapus akun yang sudah punya pesanan atau check-in.
    $ticketStatement = $pdo->prepare('SELECT id FROM tickets WHERE user_id = :id LIMIT 1 FOR UPDATE');
    $ticketStatement->execute(['id' => (int) $account['id']]);
    $checkinStatement = $pdo->prepare('SELECT id FROM ticket_checkins WHERE staff_user_id = :id LIMIT 1 FOR UPDATE');
    $checkinStatement->execute(['id' => (int) $account['id']]);
    if ($ticketStatement->fetchColumn() || $checkinStatement->fetchColumn()) {
        $pdo->rollBack();
        respond(409, ['success' => false, 'message' => 'Akun dengan riwayat pesanan atau check-in tidak bisa dihapus.']);
    }

    $delete = $pdo->prepare('DELETE FROM users WHERE id = :id');
    $delete->execute(['id' => (int) $account['id']]);
    $pdo->commit();

    $imagePath = $account['profile_image'] ?? '';
    if (is_string($imagePath) && preg_match('#^uploads/profile/[a-f0-9]{32}\.(jpg|png|webp)$#', $imagePath)) {
        @unlink(__DIR__ . '/' . $imagePath);
    }

    // Hapus akun sekaligus akhiri sesi dashboard-nya.
    destroyDashboardSession();
    respond(200, ['success' => true, 'message' => 'Akun dashboard berhasil dihapus.']);
} catch (PDOException $exception) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    if ($exception->getCode() === '23000') {
        respond(409, ['success' => false, 'message' => 'Akun masih terhubung ke riwayat.']);
    }
    handleApiError('dashboard security', $exception);
} catch (Throwable $exception) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    handleApiError('dashboard security', $exception);
}
