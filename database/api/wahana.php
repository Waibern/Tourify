<?php
require_once __DIR__ . '/common.php';
header('Content-Type: application/json; charset=utf-8');
requireMethod('GET');

try {
    $pdo = getDatabaseConnection();
    $rows = $pdo->query('SELECT id, name, description, ticket_price FROM wahana ORDER BY id')->fetchAll();
    $wahana = array_map(static function (array $row): array {
        return [
            'id' => (int) $row['id'],
            'name' => $row['name'],
            'description' => $row['description'],
            'ticket_price' => (int) $row['ticket_price'],
        ];
    }, $rows);

    respond(200, ['success' => true, 'wahana' => $wahana]);
} catch (Throwable $exception) {
    handleApiError('wahana', $exception);
}
