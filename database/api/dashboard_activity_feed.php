<?php

// Perlu dua penanda supaya polling tidak mengulang aktivitas lama.
function dashboardActivityCursor(array $query): ?array
{
    if (!array_key_exists('after_order', $query) && !array_key_exists('after_customer', $query)) {
        return null;
    }

    $cursor = [];
    foreach (['order', 'customer'] as $kind) {
        $value = $query['after_' . $kind] ?? null;
        if (!is_string($value) || !preg_match('/^(0|[1-9][0-9]*)$/D', $value)
            || filter_var($value, FILTER_VALIDATE_INT, ['options' => ['min_range' => 0]]) === false) {
            throw new InvalidArgumentException('Penanda notifikasi tidak valid.');
        }
        $cursor[$kind . '_id'] = $value;
    }
    return $cursor;
}

// Polling pertama menyimpan titik awal; notifikasi mulai dari aktivitas berikutnya.
function loadDashboardActivity(PDO $pdo, ?array $cursor): array
{
    if ($cursor === null) {
        $row = $pdo->query(
            'SELECT (SELECT COALESCE(MAX(id), 0) FROM tickets) AS order_id, '
            . "(SELECT COALESCE(MAX(id), 0) FROM users WHERE role = 'customer') AS customer_id"
        )->fetch(PDO::FETCH_ASSOC);
        return ['cursor' => array_map('strval', $row), 'events' => []];
    }

    // Ambil aktivitas bertahap lewat ID supaya data dengan waktu sama tidak terlewat.
    $orders = $pdo->prepare(
        'SELECT t.id, u.username, w.name AS wahana_name, t.quantity, t.total_price, t.booking_date '
        . 'FROM tickets t INNER JOIN users u ON u.id = t.user_id '
        . 'INNER JOIN wahana w ON w.id = t.wahana_id '
        . 'WHERE t.id > :after_id ORDER BY t.id ASC LIMIT 20'
    );
    $orders->bindValue(':after_id', (int) $cursor['order_id'], PDO::PARAM_INT);
    $orders->execute();
    $events = [];
    foreach ($orders->fetchAll(PDO::FETCH_ASSOC) as $row) {
        $cursor['order_id'] = (string) $row['id'];
        $events[] = [
            'id' => 'order:' . $row['id'],
            'type' => 'order',
            'entity_id' => (string) $row['id'],
            'username' => $row['username'],
            'occurred_at' => $row['booking_date'],
            'wahana_name' => $row['wahana_name'],
            'quantity' => (int) $row['quantity'],
            'total_price' => (int) $row['total_price'],
        ];
    }

    // Notifikasi pelanggan hanya muncul untuk akun customer baru.
    $customers = $pdo->prepare(
        "SELECT id, username, created_at FROM users WHERE role = 'customer' "
        . 'AND id > :after_id ORDER BY id ASC LIMIT 20'
    );
    $customers->bindValue(':after_id', (int) $cursor['customer_id'], PDO::PARAM_INT);
    $customers->execute();
    foreach ($customers->fetchAll(PDO::FETCH_ASSOC) as $row) {
        $cursor['customer_id'] = (string) $row['id'];
        $events[] = [
            'id' => 'customer:' . $row['id'],
            'type' => 'customer',
            'entity_id' => (string) $row['id'],
            'username' => $row['username'],
            'occurred_at' => $row['created_at'],
        ];
    }

    // Urutkan pesanan dan akun baru berdasarkan waktu, lalu ID kalau waktunya sama.
    usort($events, static fn (array $a, array $b): int =>
        strcmp($a['occurred_at'], $b['occurred_at']) ?: strcmp($a['id'], $b['id'])
    );
    return ['cursor' => $cursor, 'events' => $events];
}
