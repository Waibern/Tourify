<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireMethod('GET');
requireDashboardRole(['admin']);

try {
    $pdo = getDatabaseConnection();

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

    respond(200, ['success' => true, 'customers' => $customers]);
} catch (Throwable $exception) {
    handleApiError('dashboard customers', $exception);
}
