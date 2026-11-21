<?php
require_once __DIR__ . '/common.php';
header('Content-Type: application/json; charset=utf-8');
requireMethod('GET');

try {
    $pdo = getDatabaseConnection();
    $rows = $pdo->query(
        'SELECT w.id, w.name, w.description, w.ticket_price, w.is_featured, w.image_path, '
        . '(SELECT AVG(r.rating) FROM tickets t INNER JOIN ratings r ON r.ticket_id = t.id '
        . 'WHERE t.wahana_id = w.id) AS average_rating '
        . 'FROM wahana w WHERE w.is_deleted = 0 ORDER BY w.id'
    )->fetchAll();
    $wahana = array_map(static function (array $row): array {
        return [
            'id' => (int) $row['id'],
            'name' => $row['name'],
            'description' => $row['description'],
            'ticket_price' => (int) $row['ticket_price'],
            // Pilihan unggulan diatur admin dari website.
            'is_featured' => (bool) $row['is_featured'],
            // Path relatif mengikuti alamat API di perangkat.
            'image_path' => $row['image_path'],
            'average_rating' => $row['average_rating'] === null ? null : (float) $row['average_rating'],
        ];
    }, $rows);

    respond(200, ['success' => true, 'wahana' => $wahana]);
} catch (Throwable $exception) {
    handleApiError('wahana', $exception);
}
