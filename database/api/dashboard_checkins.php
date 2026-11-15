<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireMethod('GET');
requireDashboardRole(['admin']);

try {
    $pdo = getDatabaseConnection();
    // Tanggal scan ditampilkan dan difilter pakai waktu Jakarta.
    $pdo->exec("SET time_zone = '+07:00'");
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
    // Kalau dibuka dari pesanan, tampilkan check-in untuk tiket itu saja.
    $ticketId = $_GET['ticket_id'] ?? '';
    if (!is_string($ticketId) || ($ticketId !== '' && !preg_match('/^[1-9][0-9]{0,9}$/D', $ticketId))) {
        respond(400, ['success' => false, 'message' => 'Nomor tiket tidak valid.']);
    }
    if ($ticketId !== '') {
        $where[] = 't.id = :ticket_id';
        $params['ticket_id'] = $ticketId;
    }
    if (trim($query) !== '') {
        $where[] = '(CAST(t.id AS CHAR) LIKE :ticket OR u.username LIKE :username OR w.name LIKE :wahana OR staff.username LIKE :staff)';
        $params['ticket'] = '%' . trim($query) . '%';
        $params['username'] = '%' . trim($query) . '%';
        $params['wahana'] = '%' . trim($query) . '%';
        $params['staff'] = '%' . trim($query) . '%';
    }
    if ($date !== '') {
        $where[] = 'DATE(c.scanned_at) = :date';
        $params['date'] = $date;
    }

    // Bagian tabel ini dipakai untuk menghitung total dan mengambil daftar check-in.
    $from = 'FROM ticket_checkins c '
        . 'INNER JOIN tickets t ON t.id = c.ticket_id '
        . 'INNER JOIN users u ON u.id = t.user_id '
        . 'INNER JOIN wahana w ON w.id = t.wahana_id '
        . 'LEFT JOIN users staff ON staff.id = c.staff_user_id';
    $filter = $where ? ' WHERE ' . implode(' AND ', $where) : '';
    $count = $pdo->prepare('SELECT COUNT(*) ' . $from . $filter);
    $count->execute($params);
    $total = (int) $count->fetchColumn();

    // Ringkasan menghitung semua check-in, bukan cuma baris di halaman ini.
    $summary = $pdo->query(
        'SELECT COUNT(*) AS checkins, COUNT(DISTINCT ticket_id) AS tickets '
        . 'FROM ticket_checkins'
    )->fetch();

    // Urutkan wahana berdasarkan total check-in yang pernah tercatat.
    $topWahana = $pdo->query(
        'SELECT w.id, w.name, COUNT(*) AS checkins '
        . 'FROM ticket_checkins c '
        . 'INNER JOIN tickets t ON t.id = c.ticket_id '
        . 'INNER JOIN wahana w ON w.id = t.wahana_id '
        . 'GROUP BY w.id, w.name '
        . 'ORDER BY checkins DESC, w.name ASC '
        . 'LIMIT 1'
    )->fetch();

    $perPage = 20;
    $page = min($page, max(1, (int) ceil($total / $perPage)));
    // Ambil 20 baris per halaman supaya respons tetap ringan.
    $statement = $pdo->prepare(
        'SELECT CAST(c.id AS CHAR) AS id, CAST(t.id AS CHAR) AS ticket_id, '
        . 'u.username, w.name AS wahana_name, c.scanned_at, '
        . 'COALESCE(staff.username, \'Petugas tidak tersedia\') AS staff_username '
        . $from
        . $filter
        . ' ORDER BY c.scanned_at DESC, c.id DESC LIMIT 20 OFFSET '
        . (($page - 1) * $perPage)
    );

    $statement->execute($params);
    $rows = $statement->fetchAll();

    respond(200, [
        'success' => true,
        'checkins' => $rows,
        'total' => $total,
        'page' => $page,
        'per_page' => $perPage,
        'stats' => [
            'checkins' => (int) $summary['checkins'],
            'tickets' => (int) $summary['tickets'],
            'top_wahana_id' => $topWahana ? (int) $topWahana['id'] : null,
            'top_wahana' => $topWahana ? $topWahana['name'] : null,
            'top_wahana_checkins' => $topWahana ? (int) $topWahana['checkins'] : 0,
        ],
    ]);
} catch (Throwable $exception) {
    handleApiError('dashboard checkins', $exception);
}
