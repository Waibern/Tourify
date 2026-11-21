<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if (!in_array($method, ['GET', 'DELETE'], true)) {
    header('Allow: GET, DELETE');
    respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
}
requireDashboardRole(['admin']);

try {
    $pdo = getDatabaseConnection();
    // Hapus hanya tiket yang belum pernah dipakai agar riwayat check-in tetap utuh.
    if ($method === 'DELETE') {
        $input = readJsonInput();
        $ids = $input['ids'] ?? null;
        if (!is_array($ids) || count($ids) < 1 || count($ids) > 100) {
            respond(400, ['success' => false, 'message' => 'Pilih 1 sampai 100 tiket.']);
        }

        $ticketIds = [];
        foreach ($ids as $id) {
            if ((!is_int($id) && !is_string($id)) || !preg_match('/^[1-9][0-9]{0,19}$/D', (string) $id)) {
                respond(400, ['success' => false, 'message' => 'Daftar ID tiket tidak valid.']);
            }
            $ticketIds[] = (string) $id;
        }
        $ticketIds = array_values(array_unique($ticketIds, SORT_STRING));
        $placeholders = implode(', ', array_fill(0, count($ticketIds), '?'));

        $pdo->beginTransaction();
        $ticketStatement = $pdo->prepare(
            "SELECT id, status, checked_in_quantity FROM tickets WHERE id IN ($placeholders) FOR UPDATE"
        );
        $ticketStatement->execute($ticketIds);
        $tickets = $ticketStatement->fetchAll();
        $foundIds = array_map(static fn (array $ticket): string => (string) $ticket['id'], $tickets);
        sort($foundIds, SORT_STRING);
        $expectedIds = $ticketIds;
        sort($expectedIds, SORT_STRING);
        if ($foundIds !== $expectedIds) {
            $pdo->rollBack();
            respond(404, ['success' => false, 'message' => 'Sebagian tiket tidak ditemukan. Muat ulang daftar.']);
        }

        foreach ($tickets as $ticket) {
            if ($ticket['status'] === 'Used' || (int) $ticket['checked_in_quantity'] > 0) {
                $pdo->rollBack();
                respond(409, ['success' => false, 'message' => 'Tiket terpakai atau sudah check-in tidak bisa dihapus.']);
            }
        }

        // Cek tabel riwayat juga karena catatan check-in tidak memakai foreign key cascade.
        $checkinStatement = $pdo->prepare(
            "SELECT ticket_id FROM ticket_checkins WHERE ticket_id IN ($placeholders) LIMIT 1"
        );
        $checkinStatement->execute($ticketIds);
        if ($checkinStatement->fetchColumn() !== false) {
            $pdo->rollBack();
            respond(409, ['success' => false, 'message' => 'Tiket yang memiliki riwayat check-in tidak bisa dihapus.']);
        }

        $deleteStatement = $pdo->prepare("DELETE FROM tickets WHERE id IN ($placeholders)");
        $deleteStatement->execute($ticketIds);
        $deletedCount = $deleteStatement->rowCount();
        $pdo->commit();

        respond(200, ['success' => true, 'deleted_count' => $deletedCount]);
    }

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
        $where[] = "(CASE WHEN t.status = 'Used' OR t.checked_in_quantity >= t.quantity THEN 'Used' "
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
        . 'EXISTS (SELECT 1 FROM ticket_checkins c WHERE c.ticket_id = t.id) AS has_checkins, '
        . "CASE WHEN t.status = 'Used' OR t.checked_in_quantity >= t.quantity THEN 'Used' "
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
        $row['can_delete'] = $row['status'] !== 'Used'
            && $row['checked_in_quantity'] === 0
            && !(bool) $row['has_checkins'];
        unset($row['has_checkins']);
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
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    handleApiError('dashboard orders', $exception);
}
