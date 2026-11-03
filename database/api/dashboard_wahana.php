<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireDashboardRole(['admin']);

try {
    $pdo = getDatabaseConnection();

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
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
    }

    if ($_SERVER['REQUEST_METHOD'] === 'PUT') {
        $input = readJsonInput();
        $id = filter_var($input['id'] ?? null, FILTER_VALIDATE_INT);
        $name = $input['name'] ?? null;
        $description = $input['description'] ?? '';
        $ticketPrice = filter_var($input['ticket_price'] ?? null, FILTER_VALIDATE_INT);

        if ($id === false || $id < 1 || !is_string($name) || !is_string($description) || $ticketPrice === false) {
            respond(400, ['success' => false, 'message' => 'Data wahana belum valid.']);
        }

        $name = trim($name);
        $description = trim($description);
        if ($name === '' || mb_strlen($name) > 100 || mb_strlen($description) > 10000) {
            respond(400, ['success' => false, 'message' => 'Nama wajib diisi dan deskripsi terlalu panjang.']);
        }
        if ($ticketPrice < 1 || $ticketPrice > 4294967295) {
            respond(400, ['success' => false, 'message' => 'Harga tiket harus berupa bilangan positif yang valid.']);
        }

        $statement = $pdo->prepare(
            'UPDATE wahana SET name = :name, description = :description, ticket_price = :ticket_price WHERE id = :id'
        );
        $statement->execute([
            'name' => $name,
            'description' => $description,
            'ticket_price' => $ticketPrice,
            'id' => $id,
        ]);

        $statement = $pdo->prepare('SELECT id, name, description, ticket_price FROM wahana WHERE id = :id LIMIT 1');
        $statement->execute(['id' => $id]);
        $row = $statement->fetch();
        if (!$row) {
            respond(404, ['success' => false, 'message' => 'Wahana tidak ditemukan.']);
        }

        respond(200, [
            'success' => true,
            'message' => 'Perubahan wahana tersimpan.',
            'wahana' => [
                'id' => (int) $row['id'],
                'name' => $row['name'],
                'description' => $row['description'],
                'ticket_price' => (int) $row['ticket_price'],
            ],
        ]);
    }

    header('Allow: GET, PUT');
    respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
} catch (Throwable $exception) {
    handleApiError('dashboard wahana', $exception);
}
