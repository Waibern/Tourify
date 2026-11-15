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
if ($username === '' || $password === '') {
    respond(400, ['success' => false, 'message' => 'Username dan password wajib diisi.']);
}
if (mb_strlen($username) > 50) {
    respond(400, ['success' => false, 'message' => 'Username maksimal 50 karakter.']);
}
// Minta password yang cukup panjang untuk akun baru.
if (mb_strlen($password, 'UTF-8') < 8) {
    respond(400, ['success' => false, 'message' => 'Password minimal 8 karakter.']);
}

try {
    $pdo = getDatabaseConnection();
    $statement = $pdo->prepare('INSERT INTO users (username, password_hash) VALUES (:username, :password_hash)');
    $statement->execute([
        'username' => $username,
        'password_hash' => password_hash($password, PASSWORD_DEFAULT),
    ]);

    respond(201, [
        'success' => true,
        'message' => 'Akun berhasil dibuat.',
        'user' => ['id' => (int) $pdo->lastInsertId(), 'username' => $username],
    ]);
} catch (PDOException $exception) {
    if ($exception->getCode() === '23000') {
        respond(409, ['success' => false, 'message' => 'Username sudah terdaftar.']);
    }
    handleApiError('register', $exception);
} catch (Throwable $exception) {
    handleApiError('register', $exception);
}
