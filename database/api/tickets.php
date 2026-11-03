<?php
require_once __DIR__ . '/common.php';
header('Content-Type: application/json; charset=utf-8');

try {
    $pdo = getDatabaseConnection();
    // Akun diambil dari token, bukan username yang bisa diubah klien.
    $userId = optionalAppUserId($pdo);
    if ($userId === null) {
        respond(401, ['success' => false, 'message' => 'Sesi aplikasi tidak valid. Silakan login kembali.']);
    }

    $statement = $pdo->prepare('SELECT id, username FROM users WHERE id = :id LIMIT 1');
    $statement->execute(['id' => $userId]);
    $user = $statement->fetch();
    if (!$user) {
        respond(401, ['success' => false, 'message' => 'Akun tidak ditemukan. Silakan login kembali.']);
    }

    $method = $_SERVER['REQUEST_METHOD'] ?? '';
    if (!in_array($method, ['GET', 'POST'], true)) {
        header('Allow: GET, POST');
        respond(405, ['success' => false, 'message' => 'Gunakan metode GET atau POST.']);
    }
    $input = $method === 'POST' ? readJsonInput() : [];
    // tanggal acuan tiket mengikuti hari lokal aplikasi
    $today = (new DateTimeImmutable('now', new DateTimeZone('Asia/Jakarta')))->format('Y-m-d');

    if ($method === 'GET') {
        // Status terpakai tetap dipertahankan, lalu tiket lain disesuaikan dengan tanggal Jakarta.
        $statement = $pdo->prepare(
            "UPDATE tickets SET status = CASE "
            . "WHEN checked_in_quantity >= quantity THEN 'Used' "
            . "WHEN visit_date < :today THEN 'Expire' ELSE 'Active' END "
            . 'WHERE user_id = :user_id'
        );
        $statement->execute(['today' => $today, 'user_id' => $user['id']]);

        $statement = $pdo->prepare(
            'SELECT t.id, u.username, w.name AS wahana_name, t.booking_date, t.visit_date, t.quantity, '
            . 't.checked_in_quantity, t.unit_price, t.total_price, t.payment_method, t.status '
            . 'FROM tickets t INNER JOIN users u ON u.id = t.user_id INNER JOIN wahana w ON w.id = t.wahana_id '
            . 'WHERE t.user_id = :user_id ORDER BY t.booking_date DESC, t.id DESC'
        );
        $statement->execute(['user_id' => $user['id']]);
        $tickets = array_map(static function (array $row): array {
            return [
                'id' => (string) $row['id'],
                'username' => $row['username'],
                'wahana_name' => $row['wahana_name'],
                'booking_date' => $row['booking_date'],
                'visit_date' => $row['visit_date'],
                'quantity' => (int) $row['quantity'],
                'checked_in_quantity' => (int) $row['checked_in_quantity'],
                'unit_price' => (int) $row['unit_price'],
                'total_price' => (int) $row['total_price'],
                'payment_method' => $row['payment_method'],
                // kirim status tersimpan supaya tab aktif, terpakai, dan kedaluwarsa konsisten
                'status' => $row['status'],
            ];
        }, $statement->fetchAll());

        respond(200, ['success' => true, 'tickets' => $tickets]);
    }

    $wahanaId = filter_var($input['wahana_id'] ?? null, FILTER_VALIDATE_INT);
    $quantity = filter_var($input['quantity'] ?? null, FILTER_VALIDATE_INT);
    $visitDate = $input['visit_date'] ?? '';
    $paymentMethod = $input['payment_method'] ?? '';

    if ($wahanaId === false || $wahanaId < 1 || $quantity === false || $quantity < 1 || $quantity > 20) {
        respond(400, ['success' => false, 'message' => 'wahana_id tidak valid; quantity harus antara 1 dan 20.']);
    }
    if (!is_string($visitDate) || !preg_match('/^\d{4}-\d{2}-\d{2}$/', $visitDate)) {
        respond(400, ['success' => false, 'message' => 'visit_date harus memakai format YYYY-MM-DD.']);
    }
    $parsedDate = DateTimeImmutable::createFromFormat('!Y-m-d', $visitDate);
    if (!$parsedDate || $parsedDate->format('Y-m-d') !== $visitDate || $visitDate < $today) {
        respond(400, ['success' => false, 'message' => 'Tanggal kunjungan tidak valid atau sudah lewat.']);
    }
    if (!is_string($paymentMethod) || !in_array($paymentMethod, ['Bank', 'QRIS', 'E-Wallet'], true)) {
        respond(400, ['success' => false, 'message' => 'payment_method harus Bank, QRIS, atau E-Wallet.']);
    }

    $statement = $pdo->prepare('SELECT id, name, ticket_price FROM wahana WHERE id = :id LIMIT 1');
    $statement->execute(['id' => $wahanaId]);
    $selectedWahana = $statement->fetch();
    if (!$selectedWahana) {
        respond(404, ['success' => false, 'message' => 'Wahana tidak ditemukan.']);
    }

    $unitPrice = (int) $selectedWahana['ticket_price'];
    $totalPrice = $unitPrice * $quantity;
    // setiap pemesanan baru dimulai dengan status Active
    $statement = $pdo->prepare(
        'INSERT INTO tickets (user_id, wahana_id, visit_date, quantity, unit_price, total_price, payment_method, status) '
        . "VALUES (:user_id, :wahana_id, :visit_date, :quantity, :unit_price, :total_price, :payment_method, 'Active')"
    );
    $statement->execute([
        'user_id' => $user['id'],
        'wahana_id' => $wahanaId,
        'visit_date' => $visitDate,
        'quantity' => $quantity,
        'unit_price' => $unitPrice,
        'total_price' => $totalPrice,
        'payment_method' => $paymentMethod,
    ]);

    respond(201, [
        'success' => true,
        'message' => 'Tiket berhasil dipesan.',
        'ticket' => [
            'id' => (string) $pdo->lastInsertId(),
            'username' => $user['username'],
            'wahana_name' => $selectedWahana['name'],
            'visit_date' => $visitDate,
            // kirim status awal yang sama dengan nilai default database
            'status' => 'Active',
            'quantity' => $quantity,
            'checked_in_quantity' => 0,
            'unit_price' => $unitPrice,
            'total_price' => $totalPrice,
            'payment_method' => $paymentMethod,
        ],
    ]);
} catch (Throwable $exception) {
    handleApiError('tickets', $exception);
}
