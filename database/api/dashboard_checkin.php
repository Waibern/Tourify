<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireMethod('POST');
$staff = requireDashboardRole(['admin', 'gate']);

try {
    $input = readJsonInput();
    $ticketId = $input['ticket_id'] ?? null;
    if ((!is_string($ticketId) && !is_int($ticketId)) || !preg_match('/^[1-9][0-9]{0,19}$/', (string) $ticketId)) {
        respond(400, ['success' => false, 'message' => 'QR ini bukan tiket Tourify yang valid.']);
    }

    $pdo = getDatabaseConnection();
    $today = (new DateTimeImmutable('now', new DateTimeZone('Asia/Jakarta')))->format('Y-m-d');
    $pdo->beginTransaction();

    // Kunci pesanan saat scan supaya jumlah check-in tidak melebihi tiket yang dibeli.
    $statement = $pdo->prepare(
        'SELECT t.id, t.visit_date, t.quantity, t.checked_in_quantity, w.name AS wahana_name '
        . 'FROM tickets t INNER JOIN wahana w ON w.id = t.wahana_id '
        . 'WHERE t.id = :id FOR UPDATE'
    );
    $statement->execute(['id' => (string) $ticketId]);
    $ticket = $statement->fetch();

    if (!$ticket) {
        $pdo->rollBack();
        respond(404, ['success' => false, 'message' => 'Tiket tidak ditemukan.']);
    }
    if ($ticket['visit_date'] !== $today) {
        $pdo->rollBack();
        respond(409, ['success' => false, 'message' => 'Tiket hanya berlaku pada tanggal kunjungan.']);
    }

    $checkedIn = (int) $ticket['checked_in_quantity'];
    $quantity = (int) $ticket['quantity'];
    if ($checkedIn >= $quantity) {
        $pdo->rollBack();
        respond(409, ['success' => false, 'message' => 'Semua tiket pada pesanan ini sudah dipakai.']);
    }

    $checkedIn++;
    $status = $checkedIn >= $quantity ? 'Used' : 'Active';
    // Tandai pesanan terpakai setelah semua tiketnya sudah discan.
    $statement = $pdo->prepare(
        'UPDATE tickets SET checked_in_quantity = :checked_in_quantity, status = :status WHERE id = :id'
    );
    $statement->execute([
        'checked_in_quantity' => $checkedIn,
        'status' => $status,
        'id' => (string) $ticketId,
    ]);

    $statement = $pdo->prepare(
        'INSERT INTO ticket_checkins (ticket_id, staff_user_id) VALUES (:ticket_id, :staff_user_id)'
    );
    $statement->execute([
        'ticket_id' => (string) $ticketId,
        'staff_user_id' => $staff['id'],
    ]);
    $pdo->commit();

    respond(200, [
        'success' => true,
        'message' => 'Check-in berhasil.',
        'ticket' => [
            'id' => (string) $ticket['id'],
            'wahana_name' => $ticket['wahana_name'],
            'visit_date' => $ticket['visit_date'],
            'quantity' => $quantity,
            'checked_in_quantity' => $checkedIn,
            'remaining_quantity' => $quantity - $checkedIn,
            'status' => $status,
        ],
    ]);
} catch (Throwable $exception) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    handleApiError('dashboard check-in', $exception);
}
