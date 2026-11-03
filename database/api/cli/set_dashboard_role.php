<?php
if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}

require_once dirname(__DIR__) . '/common.php';

if ($argc !== 3 || !in_array($argv[2], ['customer', 'admin', 'gate'], true)) {
    fwrite(STDERR, "Pakai: php set_dashboard_role.php <username> <customer|admin|gate>\n");
    exit(64);
}

$username = trim($argv[1]);
if ($username === '') {
    fwrite(STDERR, "Username wajib diisi.\n");
    exit(64);
}

try {
    $pdo = getDatabaseConnection();
    $statement = $pdo->prepare('UPDATE users SET role = :role WHERE username = :username');
    $statement->execute(['role' => $argv[2], 'username' => $username]);

    $statement = $pdo->prepare('SELECT role FROM users WHERE username = :username LIMIT 1');
    $statement->execute(['username' => $username]);
    $role = $statement->fetchColumn();
    if ($role === false) {
        fwrite(STDERR, "Akun tidak ditemukan.\n");
        exit(1);
    }

    fwrite(STDOUT, "Role akun {$username} sekarang {$role}.\n");
} catch (Throwable $exception) {
    fwrite(STDERR, "Gagal mengubah role akun. Periksa koneksi database.\n");
    exit(1);
}
