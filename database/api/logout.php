<?php
require_once __DIR__ . '/common.php';
header('Content-Type: application/json; charset=utf-8');
requireMethod('POST');

respond(200, ['success' => true, 'message' => 'Logout berhasil di aplikasi.']);
