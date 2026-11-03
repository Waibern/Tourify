<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireDashboardRole(['admin']);

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if (!in_array($method, ['GET', 'DELETE'], true)) {
    header('Allow: GET, DELETE');
    respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
}

try {
    $pdo = getDatabaseConnection();

    // Pelanggan online adalah akun customer yang mengirim heartbeat dalam 5 menit terakhir.
    $onlineCount = (int) $pdo->query(
        "SELECT COUNT(*) FROM users WHERE role = 'customer' AND last_seen_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 MINUTE)"
    )->fetchColumn();

    // Hapus dilakukan sekaligus dan ditolak kalau akun punya riwayat transaksi.
    if ($method === 'DELETE') {
        $input = readJsonInput();
        $ids = $input['ids'] ?? null;
        if (!is_array($ids) || count($ids) < 1 || count($ids) > 100) {
            respond(400, ['success' => false, 'message' => 'Pilih 1 sampai 100 akun pelanggan.']);
        }

        $normalizedIds = [];
        foreach ($ids as $id) {
            if ((!is_int($id) && !(is_string($id) && preg_match('/^[1-9][0-9]{0,9}$/D', $id))) || (int) $id < 1) {
                respond(400, ['success' => false, 'message' => 'Daftar ID pelanggan tidak valid.']);
            }
            $normalizedIds[] = (int) $id;
        }
        $normalizedIds = array_values(array_unique($normalizedIds));

        $pdo->beginTransaction();
        $placeholders = implode(', ', array_fill(0, count($normalizedIds), '?'));
        $accountStatement = $pdo->prepare(
            "SELECT id FROM users WHERE role = 'customer' AND id IN ($placeholders) FOR UPDATE"
        );
        $accountStatement->execute($normalizedIds);
        $foundIds = array_map('intval', $accountStatement->fetchAll(PDO::FETCH_COLUMN));
        sort($foundIds);
        $expectedIds = $normalizedIds;
        sort($expectedIds);
        if ($foundIds !== $expectedIds) {
            $pdo->rollBack();
            respond(404, ['success' => false, 'message' => 'Sebagian akun tidak ditemukan atau bukan akun pelanggan. Muat ulang daftar.']);
        }

        $orderStatement = $pdo->prepare(
            "SELECT user_id FROM tickets WHERE user_id IN ($placeholders) GROUP BY user_id"
        );
        $orderStatement->execute($normalizedIds);
        if ($orderStatement->fetchAll(PDO::FETCH_COLUMN)) {
            $pdo->rollBack();
            respond(409, ['success' => false, 'message' => 'Akun yang dipilih sudah memiliki pesanan. Muat ulang daftar; riwayat pesanan tetap disimpan.']);
        }

        $deleteStatement = $pdo->prepare(
            "DELETE FROM users WHERE role = 'customer' AND id IN ($placeholders)"
        );
        $deleteStatement->execute($normalizedIds);
        $deletedCount = $deleteStatement->rowCount();
        $pdo->commit();

        respond(200, ['success' => true, 'deleted_count' => $deletedCount]);
    }

    // Pelanggan berasal dari akun customer dan membawa path foto profil.
    $statement = $pdo->query(
        'SELECT u.id, u.username, u.profile_image, u.created_at, COUNT(t.id) AS order_count, '
        . 'COALESCE(SUM(t.quantity), 0) AS ticket_count, '
        . 'COALESCE(SUM(t.total_price), 0) AS booking_value '
        . 'FROM users u LEFT JOIN tickets t ON t.user_id = u.id '
        . "WHERE u.role = 'customer' "
        . 'GROUP BY u.id, u.username, u.profile_image, u.created_at '
        . 'ORDER BY u.created_at DESC, u.id DESC'
    );

    $customers = array_map(static function (array $row): array {
        return [
            'id' => (int) $row['id'],
            'username' => $row['username'],
            'profile_image' => $row['profile_image'] ?? null,
            'created_at' => $row['created_at'],
            'order_count' => (int) $row['order_count'],
            'ticket_count' => (int) $row['ticket_count'],
            'booking_value' => (int) $row['booking_value'],
        ];
    }, $statement->fetchAll());

    respond(200, ['success' => true, 'online_count' => $onlineCount, 'customers' => $customers]);
} catch (Throwable $exception) {
    handleApiError('dashboard customers', $exception);
}
