<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireMethod('GET');
requireDashboardRole(['admin']);

function parseDashboardDate(mixed $value, DateTimeZone $timezone): ?DateTimeImmutable
{
    if (!is_string($value) || !preg_match('/^\d{4}-\d{2}-\d{2}$/', $value)) {
        return null;
    }

    $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value, $timezone);
    $errors = DateTimeImmutable::getLastErrors();
    if (
        !$date
        || ($errors !== false && ($errors['warning_count'] > 0 || $errors['error_count'] > 0))
        || $date->format('Y-m-d') !== $value
    ) {
        return null;
    }

    return $date;
}

try {
    $timezone = new DateTimeZone('Asia/Jakarta');
    $today = new DateTimeImmutable('today', $timezone);
    $startInput = $_GET['start'] ?? $today->modify('-13 days')->format('Y-m-d');
    $endInput = $_GET['end'] ?? $today->format('Y-m-d');
    $start = parseDashboardDate($startInput, $timezone);
    $end = parseDashboardDate($endInput, $timezone);

    if (!$start || !$end || $start > $end || $start->diff($end)->days > 366) {
        respond(400, ['success' => false, 'message' => 'Rentang tanggal analytics belum valid.']);
    }

    $startDate = $start->format('Y-m-d');
    $endDate = $end->format('Y-m-d');
    $pdo = getDatabaseConnection();
    // Semua batas tanggal mengikuti WIB, termasuk timestamp akun dan check-in.
    $pdo->exec("SET time_zone = '+07:00'");
    $days = $start->diff($end)->days + 1;
    $previousStart = $start->modify('-' . $days . ' days')->format('Y-m-d');
    $previousEnd = $start->modify('-1 day')->format('Y-m-d');

    // Hitung pelanggan baru berdasarkan tanggal akun dibuat.
    $statement = $pdo->prepare(
        'SELECT COUNT(*) FROM users '
        . 'WHERE role = :role AND DATE(created_at) BETWEEN :start_date AND :end_date'
    );
    $statement->execute([
        'role' => 'customer',
        'start_date' => $startDate,
        'end_date' => $endDate,
    ]);
    $newCustomers = (int) $statement->fetchColumn();

    // Nilai pesanan dihitung dari pemesanan, bukan dianggap sudah dibayar.
    $statement = $pdo->prepare(
        'SELECT COUNT(*) AS orders, COALESCE(SUM(quantity), 0) AS tickets, '
        . 'COALESCE(SUM(total_price), 0) AS booking_value '
        . 'FROM tickets WHERE DATE(booking_date) BETWEEN :start_date AND :end_date'
    );
    $statement->execute([
        'start_date' => $startDate,
        'end_date' => $endDate,
    ]);
    $orderStats = $statement->fetch();

    // Jumlah wahana berasal dari katalog, bukan rentang analytics.
    $wahanaCount = (int) $pdo->query('SELECT COUNT(*) FROM wahana')->fetchColumn();

    // Riwayat scan dipakai agar hitungan check-in mengikuti waktu scan sebenarnya.
    $statement = $pdo->prepare(
        'SELECT COUNT(*) FROM ticket_checkins '
        . 'WHERE DATE(scanned_at) BETWEEN :start_date AND :end_date'
    );
    $statement->execute([
        'start_date' => $startDate,
        'end_date' => $endDate,
    ]);
    $checkins = (int) $statement->fetchColumn();

    $seriesByDate = [];
    for ($date = $start; $date <= $end; $date = $date->modify('+1 day')) {
        $key = $date->format('Y-m-d');
        $seriesByDate[$key] = [
            'date' => $key,
            'order_count' => 0,
            'booking_value' => 0,
            'new_customers' => 0,
            'ticket_count' => 0,
            'checkin_count' => 0,
        ];
    }

    $statement = $pdo->prepare(
        'SELECT DATE(booking_date) AS booking_day, COUNT(*) AS order_count, '
        . 'COALESCE(SUM(total_price), 0) AS booking_value, COALESCE(SUM(quantity), 0) AS ticket_count '
        . 'FROM tickets WHERE DATE(booking_date) BETWEEN :start_date AND :end_date '
        . 'GROUP BY DATE(booking_date) ORDER BY booking_day'
    );
    $statement->execute([
        'start_date' => $startDate,
        'end_date' => $endDate,
    ]);
    foreach ($statement->fetchAll() as $row) {
        $key = (string) $row['booking_day'];
        if (isset($seriesByDate[$key])) {
            $seriesByDate[$key]['order_count'] = (int) $row['order_count'];
            $seriesByDate[$key]['booking_value'] = (int) $row['booking_value'];
            $seriesByDate[$key]['ticket_count'] = (int) $row['ticket_count'];
        }
    }

    // Hari tanpa aktivitas tetap nol; grafik tidak memakai angka contoh atau interpolasi data.
    foreach ([
        'new_customers' => "SELECT DATE(created_at) AS day, COUNT(*) AS amount FROM users WHERE role = 'customer' AND DATE(created_at) BETWEEN :start AND :end GROUP BY DATE(created_at)",
        'checkin_count' => 'SELECT DATE(scanned_at) AS day, COUNT(*) AS amount FROM ticket_checkins WHERE DATE(scanned_at) BETWEEN :start AND :end GROUP BY DATE(scanned_at)',
    ] as $metric => $sql) {
        $statement = $pdo->prepare($sql);
        $statement->execute(['start' => $startDate, 'end' => $endDate]);
        foreach ($statement->fetchAll() as $row) {
            $seriesByDate[$row['day']][$metric] = (int) $row['amount'];
        }
    }

    // Pembanding adalah rentang tepat sebelum pilihan pengguna, dengan jumlah hari yang sama.
    $previousStats = [];
    foreach ([
        'booking_value' => 'SELECT COALESCE(SUM(total_price), 0) FROM tickets WHERE DATE(booking_date) BETWEEN :start AND :end',
        'new_customers' => "SELECT COUNT(*) FROM users WHERE role = 'customer' AND DATE(created_at) BETWEEN :start AND :end",
        'orders' => 'SELECT COUNT(*) FROM tickets WHERE DATE(booking_date) BETWEEN :start AND :end',
        'tickets' => 'SELECT COALESCE(SUM(quantity), 0) FROM tickets WHERE DATE(booking_date) BETWEEN :start AND :end',
        'checkins' => 'SELECT COUNT(*) FROM ticket_checkins WHERE DATE(scanned_at) BETWEEN :start AND :end',
    ] as $metric => $sql) {
        $statement = $pdo->prepare($sql);
        $statement->execute(['start' => $previousStart, 'end' => $previousEnd]);
        $previousStats[$metric] = (int) $statement->fetchColumn();
    }
    $previousStats['wahana'] = $wahanaCount;

    // Tabel ringkas mengambil pemesanan terbaru pada rentang yang dipilih.
    $statement = $pdo->prepare(
        'SELECT t.id, u.username, w.name AS wahana_name, t.booking_date, t.visit_date, '
        . 't.quantity, t.total_price FROM tickets t '
        . 'INNER JOIN users u ON u.id = t.user_id '
        . 'INNER JOIN wahana w ON w.id = t.wahana_id '
        . 'WHERE DATE(t.booking_date) BETWEEN :start_date AND :end_date '
        . 'ORDER BY t.booking_date DESC, t.id DESC LIMIT 10'
    );
    $statement->execute([
        'start_date' => $startDate,
        'end_date' => $endDate,
    ]);
    $recentOrders = array_map(static function (array $row): array {
        return [
            'id' => (string) $row['id'],
            'username' => $row['username'],
            'wahana_name' => $row['wahana_name'],
            'booking_date' => $row['booking_date'],
            'visit_date' => $row['visit_date'],
            'quantity' => (int) $row['quantity'],
            'total_price' => (int) $row['total_price'],
        ];
    }, $statement->fetchAll());

    // Tiga wahana teratas mengikuti jumlah tiket pada rentang pilihan.
    $statement = $pdo->prepare(
        'SELECT w.id, w.name, COUNT(*) AS order_count, '
        . 'COALESCE(SUM(t.quantity), 0) AS ticket_count, '
        . 'COALESCE(SUM(t.total_price), 0) AS booking_value '
        . 'FROM tickets t INNER JOIN wahana w ON w.id = t.wahana_id '
        . 'WHERE DATE(t.booking_date) BETWEEN :start_date AND :end_date '
        . 'GROUP BY w.id, w.name '
        . 'ORDER BY ticket_count DESC, order_count DESC, w.id ASC LIMIT 3'
    );
    $statement->execute([
        'start_date' => $startDate,
        'end_date' => $endDate,
    ]);
    $topWahanaDistribution = array_map(static function (array $row): array {
        return [
            'id' => (int) $row['id'],
            'name' => $row['name'],
            'order_count' => (int) $row['order_count'],
            'ticket_count' => (int) $row['ticket_count'],
            'booking_value' => (int) $row['booking_value'],
        ];
    }, $statement->fetchAll());
    $topWahana = $topWahanaDistribution[0] ?? null;

    respond(200, [
        'success' => true,
        'analytics' => [
            'stats' => [
                'booking_value' => (int) $orderStats['booking_value'],
                'new_customers' => $newCustomers,
                'orders' => (int) $orderStats['orders'],
                'tickets' => (int) $orderStats['tickets'],
                'checkins' => $checkins,
                'wahana' => $wahanaCount,
            ],
            'previous_stats' => $previousStats,
            'comparison' => ['start' => $previousStart, 'end' => $previousEnd, 'days' => $days],
            'series' => array_values($seriesByDate),
            'recent_orders' => $recentOrders,
            'top_wahana' => $topWahana,
            // Daftar ini dipakai chart untuk menampilkan tiga wahana teratas.
            'top_wahana_distribution' => $topWahanaDistribution,
        ],
    ]);
} catch (Throwable $exception) {
    handleApiError('dashboard analytics', $exception);
}
