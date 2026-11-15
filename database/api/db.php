<?php

// Simpan konfigurasi dan rahasia di luar folder publik dan repo.
function getDatabaseConnection(): PDO
{
    $configPath = getenv('TOURIFY_DB_CONFIG') ?: '/etc/tourify/database.php';
    if (!is_file($configPath) || !is_readable($configPath)) {
        throw new RuntimeException('Konfigurasi database belum dipasang.');
    }

    $config = require $configPath;
    if (!is_array($config)) {
        throw new RuntimeException('Format konfigurasi database tidak valid.');
    }

    $host = $config['host'] ?? '127.0.0.1';
    $port = filter_var($config['port'] ?? 3306, FILTER_VALIDATE_INT, [
        'options' => [
            'min_range' => 1,
            'max_range' => 65535,
        ],
    ]);
    $database = $config['database'] ?? '';
    $username = $config['username'] ?? '';
    $password = $config['password'] ?? null;

    if (
        !is_string($host)
        || $host === ''
        || $port === false
        || !is_string($database)
        || $database === ''
        || !is_string($username)
        || $username === ''
        || !is_string($password)
        || $password === ''
    ) {
        throw new RuntimeException('Konfigurasi database belum lengkap.');
    }

    $dsn = sprintf(
        'mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4',
        $host,
        $port,
        $database
    );

    return new PDO($dsn, $username, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
}
