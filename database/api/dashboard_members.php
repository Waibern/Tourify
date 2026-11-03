<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');

// Pengelolaan akun dashboard hanya boleh dilakukan admin.
$dashboardUser = requireDashboardRole(['admin']);
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if (!in_array($method, ['GET', 'POST', 'PUT', 'DELETE'], true)) {
    header('Allow: GET, POST, PUT, DELETE, OPTIONS');
    respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
}

try {
    $pdo = getDatabaseConnection();

    if ($method === 'GET') {
        $statement = $pdo->query(
            "SELECT id, username, role, profile_image FROM users "
            . "WHERE role IN ('admin', 'gate') "
            . "ORDER BY FIELD(role, 'admin', 'gate'), username ASC"
        );
        $members = array_map(static function (array $row): array {
            return [
                'id' => (int) $row['id'],
                'username' => $row['username'],
                'role' => $row['role'],
                'profile_image' => $row['profile_image'] ?? null,
            ];
        }, $statement->fetchAll());

        respond(200, ['success' => true, 'members' => $members]);
    }

    if ($method === 'POST') {
        $input = readJsonInput();
        $username = $input['username'] ?? '';
        $password = $input['password'] ?? '';
        $role = $input['role'] ?? 'gate';
        if (!is_string($username) || !is_string($password) || !is_string($role)) {
            respond(400, ['success' => false, 'message' => 'Username, password, dan peran harus berupa teks.']);
        }

        $username = trim($username);
        if ($username === '' || mb_strlen($username) > 50) {
            respond(400, ['success' => false, 'message' => 'Username wajib diisi dan maksimal 50 karakter.']);
        }
        if (strlen($password) < 8 || strlen($password) > 72) {
            respond(400, ['success' => false, 'message' => 'Password harus terdiri dari 8 sampai 72 byte.']);
        }
        if (!in_array($role, ['admin', 'gate'], true)) {
            respond(400, ['success' => false, 'message' => 'Peran akun dashboard tidak valid.']);
        }

        $statement = $pdo->prepare(
            'INSERT INTO users (username, password_hash, role) VALUES (:username, :password_hash, :role)'
        );
        $statement->execute([
            'username' => $username,
            'password_hash' => password_hash($password, PASSWORD_DEFAULT),
            'role' => $role,
        ]);

        $memberStatement = $pdo->prepare(
            'SELECT id, username, role, profile_image FROM users WHERE id = :id LIMIT 1'
        );
        $memberStatement->execute(['id' => (int) $pdo->lastInsertId()]);
        $row = $memberStatement->fetch();
        if (!$row) {
            respond(500, ['success' => false, 'message' => 'Akun dibuat tetapi gagal dimuat ulang.']);
        }

        respond(201, [
            'success' => true,
            'message' => 'Akun dashboard berhasil dibuat.',
            'member' => [
                'id' => (int) $row['id'],
                'username' => $row['username'],
                'role' => $row['role'],
                'profile_image' => $row['profile_image'] ?? null,
            ],
        ]);
    }

    $input = readJsonInput();
    $idValue = $input['id'] ?? null;
    if (
        (!is_int($idValue) && !(is_string($idValue) && preg_match('/^[1-9][0-9]{0,9}$/D', $idValue)))
        || (int) $idValue < 1
    ) {
        respond(400, ['success' => false, 'message' => 'ID anggota tidak valid.']);
    }
    $memberId = (int) $idValue;
    if ($memberId === (int) $dashboardUser['id']) {
        respond(403, ['success' => false, 'message' => 'Ubah atau hapus akun sendiri dari halaman Keamanan.']);
    }

    if ($method === 'PUT') {
        $role = $input['role'] ?? null;
        if (!is_string($role) || !in_array($role, ['admin', 'gate'], true)) {
            respond(400, ['success' => false, 'message' => 'Peran akun dashboard tidak valid.']);
        }

        $pdo->beginTransaction();
        $adminIds = $pdo->query(
            "SELECT id FROM users WHERE role = 'admin' ORDER BY id FOR UPDATE"
        )->fetchAll(PDO::FETCH_COLUMN);
        $statement = $pdo->prepare(
            "SELECT id, username, role, profile_image FROM users WHERE id = :id AND role IN ('admin', 'gate') FOR UPDATE"
        );
        $statement->execute(['id' => $memberId]);
        $member = $statement->fetch();
        if (!$member) {
            $pdo->rollBack();
            respond(404, ['success' => false, 'message' => 'Anggota dashboard tidak ditemukan.']);
        }
        if ($member['role'] === 'admin' && $role === 'gate' && count($adminIds) <= 1) {
            $pdo->rollBack();
            respond(409, ['success' => false, 'message' => 'Admin terakhir tidak bisa diturunkan perannya.']);
        }

        $update = $pdo->prepare('UPDATE users SET role = :role WHERE id = :id');
        $update->execute(['role' => $role, 'id' => $memberId]);
        $pdo->commit();
        $member['role'] = $role;

        respond(200, [
            'success' => true,
            'message' => 'Peran anggota berhasil diperbarui.',
            'member' => [
                'id' => (int) $member['id'],
                'username' => $member['username'],
                'role' => $member['role'],
                'profile_image' => $member['profile_image'] ?? null,
            ],
        ]);
    }

    $pdo->beginTransaction();
    $adminIds = $pdo->query(
        "SELECT id FROM users WHERE role = 'admin' ORDER BY id FOR UPDATE"
    )->fetchAll(PDO::FETCH_COLUMN);
    $statement = $pdo->prepare(
        "SELECT id, username, role, profile_image FROM users WHERE id = :id AND role IN ('admin', 'gate') FOR UPDATE"
    );
    $statement->execute(['id' => $memberId]);
    $member = $statement->fetch();
    if (!$member) {
        $pdo->rollBack();
        respond(404, ['success' => false, 'message' => 'Anggota dashboard tidak ditemukan.']);
    }
    if ($member['role'] === 'admin' && count($adminIds) <= 1) {
        $pdo->rollBack();
        respond(409, ['success' => false, 'message' => 'Admin terakhir tidak bisa dihapus.']);
    }

    $ticketStatement = $pdo->prepare('SELECT id FROM tickets WHERE user_id = :id LIMIT 1 FOR UPDATE');
    $ticketStatement->execute(['id' => $memberId]);
    $checkinStatement = $pdo->prepare('SELECT id FROM ticket_checkins WHERE staff_user_id = :id LIMIT 1 FOR UPDATE');
    $checkinStatement->execute(['id' => $memberId]);
    if ($ticketStatement->fetchColumn() || $checkinStatement->fetchColumn()) {
        $pdo->rollBack();
        respond(409, ['success' => false, 'message' => 'Akun dengan riwayat pesanan atau check-in tidak bisa dihapus.']);
    }

    $delete = $pdo->prepare('DELETE FROM users WHERE id = :id AND role IN (\'admin\', \'gate\')');
    $delete->execute(['id' => $memberId]);
    $pdo->commit();

    $imagePath = $member['profile_image'] ?? '';
    if (is_string($imagePath) && preg_match('#^uploads/profile/[a-f0-9]{32}\.(jpg|png|webp)$#', $imagePath)) {
        @unlink(__DIR__ . '/' . $imagePath);
    }

    respond(200, ['success' => true, 'message' => 'Anggota dashboard berhasil dihapus.']);
} catch (PDOException $exception) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    if ($exception->getCode() === '23000') {
        respond(409, ['success' => false, 'message' => 'Username sudah digunakan atau akun masih terhubung ke riwayat.']);
    }
    handleApiError('dashboard members', $exception);
} catch (Throwable $exception) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    handleApiError('dashboard members', $exception);
}
