<?php
require_once __DIR__ . '/common.php';
header('Content-Type: application/json; charset=utf-8');
requireMethod('POST');

$input = readJsonInput();
$username = $input['username'] ?? '';
$password = $input['password'] ?? '';
if (!is_string($username) || !is_string($password)) {
    respond(400, ['success' => false, 'message' => 'Username dan password harus berupa teks.']);
}
$username = trim($username);
if ($username === '' || !is_string($password) || $password === '') {
    respond(400, ['success' => false, 'message' => 'Username dan password wajib diisi.']);
}

try {
    $pdo = getDatabaseConnection();
    $statement = $pdo->prepare('SELECT id, username, password_hash FROM users WHERE username = :username LIMIT 1');
    $statement->execute(['username' => $username]);
    $user = $statement->fetch();

    if (!$user || !password_verify($password, $user['password_hash'])) {
        respond(401, ['success' => false, 'message' => 'Username atau password salah.']);
    }

    respond(200, [
        'success' => true,
        'message' => 'Login berhasil.',
        'user' => [
            'id' => (int) $user['id'],
            'username' => $user['username'],
        ],
        // Token memastikan foto diunggah ke akun yang baru login.
        'access_token' => createAppAccessToken((int) $user['id'], $user['password_hash']),
    ]);
} catch (Throwable $exception) {
    handleApiError('login', $exception);
}
