<?php
require_once __DIR__ . '/../api/dashboard_activity_feed.php';

// Tes ini pakai database di memori, jadi data akun dan pesanan asli aman.
$memoryDatabase = new PDO('sqlite::memory:', null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
$memoryDatabase->exec('CREATE TABLE users (id INTEGER PRIMARY KEY, username TEXT, role TEXT, created_at TEXT)');
$memoryDatabase->exec('CREATE TABLE wahana (id INTEGER PRIMARY KEY, name TEXT)');
$memoryDatabase->exec(
    'CREATE TABLE tickets ('
    . 'id INTEGER PRIMARY KEY, user_id INTEGER, wahana_id INTEGER, '
    . 'quantity INTEGER, total_price INTEGER, booking_date TEXT)'
);
$memoryDatabase->exec("INSERT INTO users VALUES (1, 'pelanggan_lama', 'customer', '2026-11-01 10:00:00')");
$memoryDatabase->exec("INSERT INTO wahana VALUES (1, 'Dunia Petualang')");
$memoryDatabase->exec("INSERT INTO tickets VALUES (1, 1, 1, 2, 300000, '2026-11-01 10:00:00')");

function check(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

$initialActivity = loadDashboardActivity($memoryDatabase, null);
check($initialActivity['events'] === [], 'Data lama tidak boleh memicu notifikasi.');
check($initialActivity['cursor'] === ['order_id' => '1', 'customer_id' => '1'], 'Patokan awal salah.');
check(dashboardActivityCursor([]) === null, 'Request awal harus tanpa penanda.');

// Tolak penanda polling yang tidak lengkap, negatif, bukan angka, atau terlalu besar.
$invalidCursorCases = [
    ['after_order' => '1'],
    ['after_order' => '-1', 'after_customer' => '0'],
    ['after_order' => [], 'after_customer' => '0'],
    ['after_order' => '999999999999999999999', 'after_customer' => '0'],
];
foreach ($invalidCursorCases as $invalidCursor) {
    try {
        dashboardActivityCursor($invalidCursor);
        throw new RuntimeException('Penanda tidak valid diterima.');
    } catch (InvalidArgumentException $exception) {
        // Input yang salah memang ditolak.
    }
}
check(
    dashboardActivityCursor(['after_order' => '1', 'after_customer' => '0']) === ['order_id' => '1', 'customer_id' => '0'],
    'Penanda valid berubah.'
);

$memoryDatabase->exec("INSERT INTO users VALUES (2, 'petugas', 'gate', '2026-11-01 11:00:00')");
$memoryDatabase->exec("INSERT INTO users VALUES (3, 'pelanggan_baru', 'customer', '2026-11-01 11:00:00')");
$memoryDatabase->exec("INSERT INTO tickets VALUES (2, 3, 1, 4, 600000, '2026-11-01 11:00:00')");
$newActivityBatch = loadDashboardActivity($memoryDatabase, $initialActivity['cursor']);
check(array_column($newActivityBatch['events'], 'id') === ['customer:3', 'order:2'], 'Pesanan dan akun pada detik sama harus terbaca.');
check(
    $newActivityBatch['events'][1]['quantity'] === 4
    && $newActivityBatch['events'][1]['total_price'] === 600000,
    'Detail pesanan harus mengikuti database.'
);
check(
    loadDashboardActivity($memoryDatabase, $newActivityBatch['cursor'])['events'] === [],
    'Polling ulang tidak boleh mengulang kejadian.'
);

// Pastikan sisa pesanan tetap muncul di polling berikutnya saat aktivitas ramai.
for ($ticketId = 3; $ticketId <= 27; $ticketId++) {
    $memoryDatabase->exec("INSERT INTO tickets VALUES ($ticketId, 3, 1, 1, 150000, '2026-11-01 11:01:00')");
}
$firstBurstBatch = loadDashboardActivity($memoryDatabase, $newActivityBatch['cursor']);
check(
    count($firstBurstBatch['events']) === 20 && $firstBurstBatch['cursor']['order_id'] === '22',
    'Batch pertama harus berhenti di ID terakhir yang dikirim.'
);
$remainingBurstBatch = loadDashboardActivity($memoryDatabase, $firstBurstBatch['cursor']);
check(
    count($remainingBurstBatch['events']) === 5 && $remainingBurstBatch['cursor']['order_id'] === '27',
    'Sisa batch harus tersedia pada polling berikutnya.'
);
check($remainingBurstBatch['cursor']['customer_id'] === '3', 'Penanda jenis lain harus tetap sama.');
echo "activity: baseline, validation, new events, deduplication and burst pagination passed\n";
