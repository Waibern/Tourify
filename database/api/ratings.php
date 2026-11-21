<?php
require_once __DIR__ . '/common.php';
header('Content-Type: application/json; charset=utf-8');
requireMethod('POST');

try {
    $pdo = getDatabaseConnection();
    $userId = optionalAppUserId($pdo);
    if ($userId === null) {
        respond(401, ['success' => false, 'message' => 'Sesi aplikasi tidak valid. Silakan login kembali.']);
    }

    $input = readJsonInput();
    $ticketId = $input['ticket_id'] ?? null;
    $rating = filter_var($input['rating'] ?? null, FILTER_VALIDATE_INT);
    if (
        (!is_string($ticketId) && !is_int($ticketId))
        || !preg_match('/^[1-9][0-9]{0,19}$/', (string) $ticketId)
    ) {
        respond(400, ['success' => false, 'message' => 'ID tiket tidak valid.']);
    }
    if ($rating === false || $rating < 1 || $rating > 5) {
        respond(400, ['success' => false, 'message' => 'Rating harus berupa angka dari 1 sampai 5.']);
    }

    // Pastikan booking milik akun ini dan sudah punya check-in.
    $statement = $pdo->prepare(
        'SELECT checked_in_quantity FROM tickets '
        . 'WHERE id = :ticket_id AND user_id = :user_id LIMIT 1'
    );
    $statement->execute([
        'ticket_id' => (string) $ticketId,
        'user_id' => $userId,
    ]);
    $checkedInQuantity = $statement->fetchColumn();
    if ($checkedInQuantity === false) {
        respond(404, ['success' => false, 'message' => 'Tiket tidak ditemukan.']);
    }
    if ((int) $checkedInQuantity < 1) {
        respond(409, ['success' => false, 'message' => 'Rating tersedia setelah check-in pertama.']);
    }

    // Satu booking menyimpan satu rating yang bisa diperbarui.
    $statement = $pdo->prepare(
        'INSERT INTO ratings (ticket_id, rating) VALUES (:ticket_id, :rating) '
        . 'ON DUPLICATE KEY UPDATE rating = :updated_rating'
    );
    $statement->execute([
        'ticket_id' => (string) $ticketId,
        'rating' => $rating,
        'updated_rating' => $rating,
    ]);

    respond(200, [
        'success' => true,
        'message' => 'Rating wahana berhasil disimpan.',
        'rating' => $rating,
    ]);
} catch (Throwable $exception) {
    handleApiError('ratings', $exception);
}
