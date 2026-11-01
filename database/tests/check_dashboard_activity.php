<?php
require_once __DIR__ . '/../api/dashboard_activity_feed.php';

// Semua data uji tinggal di memori; pesanan dan akun asli tidak disentuh.
$pdo = new PDO('sqlite::memory:', null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
$pdo->exec('CREATE TABLE users (id INTEGER PRIMARY KEY, username TEXT, role TEXT, created_at TEXT)');
$pdo->exec('CREATE TABLE wahana (id INTEGER PRIMARY KEY, name TEXT)');
$pdo->exec('CREATE TABLE tickets (id INTEGER PRIMARY KEY, user_id INTEGER, wahana_id INTEGER, quantity INTEGER, total_price INTEGER, booking_date TEXT)');
$pdo->exec("INSERT INTO users VALUES (1, 'pelanggan_lama', 'customer', '2026-11-01 10:00:00')");
$pdo->exec("INSERT INTO wahana VALUES (1, 'Dunia Petualang')");
$pdo->exec("INSERT INTO tickets VALUES (1, 1, 1, 2, 300000, '2026-11-01 10:00:00')");

function check(bool $condition, string $message): void
{
    if (!$condition) throw new RuntimeException($message);
}

$baseline = loadDashboardActivity($pdo, null);
check($baseline['events'] === [], 'Data lama tidak boleh memicu notifikasi.');
check($baseline['cursor'] === ['order_id' => '1', 'customer_id' => '1'], 'Patokan awal salah.');
check(dashboardActivityCursor([]) === null, 'Request awal harus tanpa penanda.');

// Penanda parsial, negatif, array, dan angka melampaui integer harus ditolak.
foreach ([['after_order' => '1'], ['after_order' => '-1', 'after_customer' => '0'],
    ['after_order' => [], 'after_customer' => '0'], ['after_order' => '999999999999999999999', 'after_customer' => '0']] as $invalid) {
    try {
        dashboardActivityCursor($invalid);
        throw new RuntimeException('Penanda tidak valid diterima.');
    } catch (InvalidArgumentException $exception) {
        // Validasi berhasil menolak input.
    }
}
check(dashboardActivityCursor(['after_order' => '1', 'after_customer' => '0']) === ['order_id' => '1', 'customer_id' => '0'], 'Penanda valid berubah.');

$pdo->exec("INSERT INTO users VALUES (2, 'petugas', 'gate', '2026-11-01 11:00:00')");
$pdo->exec("INSERT INTO users VALUES (3, 'pelanggan_baru', 'customer', '2026-11-01 11:00:00')");
$pdo->exec("INSERT INTO tickets VALUES (2, 3, 1, 4, 600000, '2026-11-01 11:00:00')");
$next = loadDashboardActivity($pdo, $baseline['cursor']);
check(array_column($next['events'], 'id') === ['customer:3', 'order:2'], 'Pesanan dan akun pada detik sama harus terbaca.');
check($next['events'][1]['quantity'] === 4 && $next['events'][1]['total_price'] === 600000, 'Detail pesanan harus mengikuti database.');
check(loadDashboardActivity($pdo, $next['cursor'])['events'] === [], 'Polling ulang tidak boleh mengulang kejadian.');

// Batas satu batch tidak boleh melompati sisa pesanan saat ada lonjakan aktivitas.
for ($id = 3; $id <= 27; $id++) {
    $pdo->exec("INSERT INTO tickets VALUES ($id, 3, 1, 1, 150000, '2026-11-01 11:01:00')");
}
$batch = loadDashboardActivity($pdo, $next['cursor']);
check(count($batch['events']) === 20 && $batch['cursor']['order_id'] === '22', 'Batch pertama harus berhenti di ID terakhir yang dikirim.');
$rest = loadDashboardActivity($pdo, $batch['cursor']);
check(count($rest['events']) === 5 && $rest['cursor']['order_id'] === '27', 'Sisa batch harus tersedia pada polling berikutnya.');
check($rest['cursor']['customer_id'] === '3', 'Penanda jenis lain harus tetap sama.');
echo "activity: baseline, validation, new events, deduplication and burst pagination passed\n";
