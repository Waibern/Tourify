<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireMethod('GET');
requireDashboardRole(['admin']);

try {
    $pdo = getDatabaseConnection();
    // Timestamp scan ditampilkan dan difilter dalam waktu Jakarta.
    $pdo->exec("SET time_zone = '+07:00'");
    $today = (new DateTimeImmutable('now', new DateTimeZone('Asia/Jakarta')))->format('Y-m-d');
    $query = $_GET['q'] ?? '';
    $date = $_GET['date'] ?? '';
    $page = filter_var($_GET['page'] ?? 1, FILTER_VALIDATE_INT);
    if (!is_string($query) || strlen($query) > 200 || !is_string($date) || $page === false || $page < 1 || $page > 1000000) {
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
    // Tautan dari riwayat harus mengambil ID persis, bukan tiket lain dengan angka serupa.
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
        $where[] = "(CASE WHEN t.checked_in_quantity >= t.quantity THEN 'Used' WHEN t.visit_date < :filter_today THEN 'Expire' ELSE 'Active' END) = :status";
        $params['filter_today'] = $today;
        $params['status'] = $status;
    }

    $from = 'FROM tickets t INNER JOIN users u ON u.id = t.user_id INNER JOIN wahana w ON w.id = t.wahana_id';
    $filter = $where ? ' WHERE ' . implode(' AND ', $where) : '';
    $count = $pdo->prepare('SELECT COUNT(*) ' . $from . $filter);
    $count->execute($params);
    $total = (int) $count->fetchColumn();

    // Kartu ringkasan membaca semua pesanan, terpisah dari filter tabel.
    $summary = $pdo->query(
        'SELECT COUNT(*) AS orders, COALESCE(SUM(quantity), 0) AS tickets, '
        . 'COALESCE(SUM(total_price), 0) AS booking_value FROM tickets'
    )->fetch();

    $perPage = 20;
    $page = min($page, max(1, (int) ceil($total / $perPage)));
    // Pagination membatasi jumlah baris yang dibawa browser setiap permintaan.
    $statement = $pdo->prepare('SELECT CAST(t.id AS CHAR) AS id, u.username, w.name AS wahana_name, t.booking_date, t.visit_date, t.quantity, t.checked_in_quantity, t.unit_price, t.total_price, t.payment_method, CASE WHEN t.checked_in_quantity >= t.quantity THEN \'Used\' WHEN t.visit_date < :today THEN \'Expire\' ELSE \'Active\' END AS status ' . $from . $filter . ' ORDER BY t.booking_date DESC, t.id DESC LIMIT 20 OFFSET ' . (($page - 1) * $perPage));
    $params['today'] = $today;
    $statement->execute($params);
    $rows = $statement->fetchAll();
    foreach ($rows as &$row) {
        foreach (['quantity', 'checked_in_quantity', 'unit_price', 'total_price'] as $field) {
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
    ]);
} catch (Throwable $exception) {
    handleApiError('dashboard orders', $exception);
}
