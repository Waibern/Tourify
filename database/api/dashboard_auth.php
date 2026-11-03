<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');

try {
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $user = requireDashboardRole(['admin', 'gate']);
        respond(200, ['success' => true, 'user' => $user]);
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $input = readJsonInput();
        $username = $input['username'] ?? '';
        $password = $input['password'] ?? '';
        if (!is_string($username) || !is_string($password) || trim($username) === '' || $password === '') {
            respond(400, ['success' => false, 'message' => 'Username dan password wajib diisi.']);
        }

        $pdo = getDatabaseConnection();
        $statement = $pdo->prepare(
            'SELECT id, username, password_hash, role FROM users WHERE username = :username LIMIT 1'
        );
        $statement->execute(['username' => trim($username)]);
        $account = $statement->fetch();
        if (
            !$account
            || !in_array($account['role'], ['admin', 'gate'], true)
            || !password_verify($password, $account['password_hash'])
        ) {
            respond(401, ['success' => false, 'message' => 'Akun dashboard tidak ditemukan atau password salah.']);
        }

        startDashboardSession();
        session_regenerate_id(true);
        $user = [
            'id' => (int) $account['id'],
            'username' => $account['username'],
            'role' => $account['role'],
        ];
        $_SESSION['dashboard_user'] = $user;
        respond(200, ['success' => true, 'user' => $user]);
    }

    if ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
        destroyDashboardSession();
        respond(200, ['success' => true, 'message' => 'Logout berhasil.']);
    }

    header('Allow: GET, POST, DELETE');
    respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
} catch (Throwable $exception) {
    handleApiError('dashboard auth', $exception);
}
