<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireMethod('GET');
requireDashboardRole(['admin']);

try {
    $pdo = getDatabaseConnection();
    // Tanggal pesanan ditampilkan dan difilter pakai waktu Jakarta.
    $pdo->exec("SET time_zone = '+07:00'");
    $today = (new DateTimeImmutable('now', new DateTimeZone('Asia/Jakarta')))->format('Y-m-d');
    $query = $_GET['q'] ?? '';
    $date = $_GET['date'] ?? '';
    $page = filter_var($_GET['page'] ?? 1, FILTER_VALIDATE_INT);
    // Cek filter dan nomor halaman sebelum dipakai di query.
    if (
        !is_string($query)
        || strlen($query) > 200
        || !is_string($date)
        || $page === false
        || $page < 1
        || $page > 1000000
    ) {
        respond(400, ['success' => false, 'message' => 'Parameter pencarian tidak valid.']);
    }
    if ($date !== '') {
        $parsed = DateTimeImmutable::createFromFormat('!Y-m-d', $date);
        if (!$parsed || $parsed->format('Y-m-d') !== $date) {
            respond(400, ['success' => false, 'message' => 'Tanggal tidak valid.']);
        }
    }
    $where = [];
    $params = [];
    // Pakai ID dari tautan secara utuh supaya tiket lain tidak ikut terbuka.
    $ticketId = $_GET['ticket_id'] ?? '';
    if (!is_string($ticketId) || ($ticketId !== '' && !preg_match('/^[1-9][0-9]{0,9}$/D', $ticketId))) {
        respond(400, ['success' => false, 'message' => 'Nomor tiket tidak valid.']);
    }
    if ($ticketId !== '') {
        $where[] = 't.id = :ticket_id';
        $params['ticket_id'] = $ticketId;
    }
    if (trim($query) !== '') {
        $where[] = '(CAST(t.id AS CHAR) LIKE :ticket OR u.username LIKE :username OR w.name LIKE :wahana)';
        $params['ticket'] = '%' . trim($query) . '%';
        $params['username'] = '%' . trim($query) . '%';
        $params['wahana'] = '%' . trim($query) . '%';

    }
    if ($date !== '') {
        $where[] = 'DATE(t.visit_date) = :date';
        $params['date'] = $date;
    }

    $status = $_GET['status'] ?? '';
    if (!is_string($status) || !in_array($status, ['', 'Active', 'Expire', 'Used'], true)) {
        respond(400, ['success' => false, 'message' => 'Filter status tidak valid.']);
    }
    if ($status !== '') {
        $where[] = "(CASE WHEN t.checked_in_quantity >= t.quantity THEN 'Used' "
            . "WHEN t.visit_date < :filter_today THEN 'Expire' ELSE 'Active' END) = :status";
        $params['filter_today'] = $today;
        $params['status'] = $status;
    }

    // Bagian tabel ini dipakai untuk menghitung total dan mengambil daftar pesanan.
    $from = 'FROM tickets t '
        . 'INNER JOIN users u ON u.id = t.user_id '
        . 'INNER JOIN wahana w ON w.id = t.wahana_id';
    $filter = $where ? ' WHERE ' . implode(' AND ', $where) : '';
    $count = $pdo->prepare('SELECT COUNT(*) ' . $from . $filter);
    $count->execute($params);
    $total = (int) $count->fetchColumn();

    // Ringkasan memakai semua pesanan meski tabel sedang difilter.
    $summary = $pdo->query(
        'SELECT COUNT(*) AS orders, COALESCE(SUM(quantity), 0) AS tickets, '
        . 'COALESCE(SUM(total_price), 0) AS booking_value FROM tickets'
    )->fetch();

    // Grafik ini menunjukkan jumlah pesanan tiap hari selama 14 hari terakhir.
    $todayDate = new DateTimeImmutable($today, new DateTimeZone('Asia/Jakarta'));
    $trendStart = $todayDate->modify('-13 days');
    $orderTrend = [];
    for ($day = $trendStart; $day <= $todayDate; $day = $day->modify('+1 day')) {
        $key = $day->format('Y-m-d');
        $orderTrend[$key] = ['date' => $key, 'order_count' => 0];
    }

    $trendStatement = $pdo->prepare(
        'SELECT DATE(booking_date) AS order_day, COUNT(*) AS order_count '
        . 'FROM tickets WHERE booking_date >= :trend_start AND booking_date < :trend_end '
        . 'GROUP BY DATE(booking_date) ORDER BY order_day'
    );
    $trendStatement->execute([
        'trend_start' => $trendStart->format('Y-m-d'),
        'trend_end' => $todayDate->modify('+1 day')->format('Y-m-d'),
    ]);
    foreach ($trendStatement->fetchAll() as $trendRow) {
        $key = (string) $trendRow['order_day'];
        if (isset($orderTrend[$key])) {
            $orderTrend[$key]['order_count'] = (int) $trendRow['order_count'];
        }
    }

    $perPage = 20;
    $page = min($page, max(1, (int) ceil($total / $perPage)));
    // Ambil 20 baris per halaman supaya respons tetap ringan.
    $statement = $pdo->prepare(
        'SELECT CAST(t.id AS CHAR) AS id, u.username, w.id AS wahana_id, '
        . 'w.name AS wahana_name, t.booking_date, t.visit_date, t.quantity, '
        . 't.checked_in_quantity, t.unit_price, t.total_price, t.payment_method, '
        . "CASE WHEN t.checked_in_quantity >= t.quantity THEN 'Used' "
        . "WHEN t.visit_date < :today THEN 'Expire' ELSE 'Active' END AS status "
        . $from
        . $filter
        . ' ORDER BY t.booking_date DESC, t.id DESC LIMIT 20 OFFSET '
        . (($page - 1) * $perPage)
    );
    $params['today'] = $today;
    $statement->execute($params);
    $rows = $statement->fetchAll();
    foreach ($rows as &$row) {
        foreach (['wahana_id', 'quantity', 'checked_in_quantity', 'unit_price', 'total_price'] as $field) {
            $row[$field] = (int) $row[$field];
        }
    }
    unset($row);
    respond(200, [
        'success' => true,
        'orders' => $rows,
        'total' => $total,
        'page' => $page,
        'per_page' => $perPage,
        'stats' => [
            'orders' => (int) $summary['orders'],
            'tickets' => (int) $summary['tickets'],
            'booking_value' => (int) $summary['booking_value'],
        ],
        'order_trend' => array_values($orderTrend),
    ]);
} catch (Throwable $exception) {
    handleApiError('dashboard orders', $exception);
}
