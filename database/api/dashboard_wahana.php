<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
requireDashboardRole(['admin']);

// Gunakan bentuk data yang sama untuk daftar dan hasil simpan.
function dashboardWahanaRow(array $row): array
{
    return [
        'id' => (int) $row['id'],
        'name' => $row['name'],
        'description' => $row['description'],
        'ticket_price' => (int) $row['ticket_price'],
        'is_featured' => (bool) $row['is_featured'],
        'image_path' => $row['image_path'],
        'average_rating' => $row['average_rating'] === null ? null : round((float) $row['average_rating'], 1),
        'rating_count' => (int) $row['rating_count'],
        'ticket_count' => (int) $row['ticket_count'],
        'ticket_owner_count' => (int) $row['ticket_owner_count'],
    ];
}

// Gabungkan rating dan jumlah tiket terkait tanpa menggandakan baris wahana.
function dashboardWahanaSelectSql(): string
{
    return 'SELECT w.id, w.name, w.description, w.ticket_price, w.is_featured, w.image_path, '
        . 'rating_summary.average_rating, COALESCE(rating_summary.rating_count, 0) AS rating_count, '
        . 'COALESCE(rating_summary.ticket_count, 0) AS ticket_count, '
        . 'COALESCE(rating_summary.ticket_owner_count, 0) AS ticket_owner_count '
        . 'FROM wahana w LEFT JOIN ('
        . 'SELECT t.wahana_id, AVG(r.rating) AS average_rating, COUNT(r.ticket_id) AS rating_count, '
        . 'SUM(t.quantity) AS ticket_count, COUNT(DISTINCT t.user_id) AS ticket_owner_count '
        . 'FROM tickets t LEFT JOIN ratings r ON r.ticket_id = t.id '
        . 'GROUP BY t.wahana_id'
        . ') AS rating_summary ON rating_summary.wahana_id = w.id '
        . 'WHERE w.is_deleted = 0 ';
}

function storeWahanaImage(?array $image): ?string
{
    $error = $image['error'] ?? UPLOAD_ERR_NO_FILE;
    if ($error === UPLOAD_ERR_NO_FILE) {
        return null;
    }

    // Periksa isi foto sebelum menaruh file di folder publik.
    $limit = 5 * 1024 * 1024;
    $temporaryPath = $image['tmp_name'] ?? '';
    if (in_array($error, [UPLOAD_ERR_INI_SIZE, UPLOAD_ERR_FORM_SIZE], true)) {
        respond(413, ['success' => false, 'message' => 'Ukuran foto maksimal 5 MB.']);
    }
    if ($error !== UPLOAD_ERR_OK || !is_string($temporaryPath) || !is_uploaded_file($temporaryPath)) {
        respond(400, ['success' => false, 'message' => 'Foto gagal diterima server.']);
    }
    $size = filesize($temporaryPath);
    if ($size === false || $size < 1 || $size > $limit) {
        respond(413, ['success' => false, 'message' => 'Ukuran foto maksimal 5 MB.']);
    }

    $mime = (new finfo(FILEINFO_MIME_TYPE))->file($temporaryPath);
    $extensions = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];
    $info = @getimagesize($temporaryPath);
    if (!isset($extensions[$mime]) || !$info || $info['mime'] !== $mime
        || $info[0] < 1 || $info[1] < 1 || $info[0] * $info[1] > 40000000) {
        respond(415, ['success' => false, 'message' => 'Gunakan foto JPG, PNG, atau WebP yang valid.']);
    }
    // Dekode file untuk menolak gambar rusak yang masih punya header valid.
    $contents = file_get_contents($temporaryPath);
    if ($contents === false || @imagecreatefromstring($contents) === false) {
        respond(415, ['success' => false, 'message' => 'Foto tidak bisa dibuka. Pilih file lain.']);
    }

    $directory = __DIR__ . '/uploads/wahana';
    if (!is_dir($directory) && !mkdir($directory, 0755, true) && !is_dir($directory)) {
        respond(500, ['success' => false, 'message' => 'Folder foto wahana tidak bisa dibuat.']);
    }
    $path = 'uploads/wahana/' . bin2hex(random_bytes(16)) . '.' . $extensions[$mime];
    if (!move_uploaded_file($temporaryPath, __DIR__ . '/' . $path)) {
        respond(500, ['success' => false, 'message' => 'Foto wahana gagal disimpan.']);
    }
    return $path;
}

function removeWahanaUpload(?string $path): void
{
    if ($path !== null && preg_match('#^uploads/wahana/[a-f0-9]{32}\.(jpg|png|webp)$#D', $path)) {
        @unlink(__DIR__ . '/' . $path);
    }
}

try {
    $pdo = getDatabaseConnection();

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $rows = $pdo->query(dashboardWahanaSelectSql() . 'ORDER BY w.id')->fetchAll();
        respond(200, ['success' => true, 'wahana' => array_map('dashboardWahanaRow', $rows)]);
    }

    $method = $_SERVER['REQUEST_METHOD'];
    $action = $_GET['action'] ?? null;

    // Arsipkan wahana agar tiket lama tetap bisa dibuka dan diverifikasi.
    if ($method === 'DELETE' && $action === null) {
        $id = filter_var($_GET['id'] ?? null, FILTER_VALIDATE_INT);
        if ($id === false || $id === null || $id < 1) {
            respond(400, ['success' => false, 'message' => 'ID wahana tidak valid.']);
        }

        try {
            $pdo->beginTransaction();
            $statement = $pdo->prepare('SELECT id FROM wahana WHERE id = :id AND is_deleted = 0 FOR UPDATE');
            $statement->execute(['id' => $id]);
            $wahana = $statement->fetch();
            if (!$wahana) {
                $pdo->rollBack();
                respond(404, ['success' => false, 'message' => 'Wahana tidak ditemukan.']);
            }

            $statement = $pdo->prepare('UPDATE wahana SET is_deleted = 1, is_featured = 0 WHERE id = :id');
            $statement->execute(['id' => $id]);
            $pdo->commit();
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            throw $exception;
        }

        respond(200, [
            'success' => true,
            'message' => 'Wahana dihapus dari katalog. Tiket dan riwayat pelanggan tetap disimpan.',
        ]);
    }

    // Satu UPDATE mengganti unggulan agar pilihan lama langsung dinonaktifkan.
    if ($method === 'POST' && $action === null) {
        $input = readJsonInput();
        $id = filter_var($input['id'] ?? null, FILTER_VALIDATE_INT);
        if ($id === false || $id === null || $id < 1) {
            respond(400, ['success' => false, 'message' => 'ID wahana tidak valid.']);
        }
        $statement = $pdo->prepare('SELECT id FROM wahana WHERE id = :id AND is_deleted = 0');
        $statement->execute(['id' => $id]);
        if (!$statement->fetchColumn()) {
            respond(404, ['success' => false, 'message' => 'Wahana tidak ditemukan.']);
        }
        $statement = $pdo->prepare(
            'UPDATE wahana SET is_featured = CASE WHEN id = :id THEN 1 ELSE 0 END WHERE is_deleted = 0'
        );
        $statement->execute(['id' => $id]);
        respond(200, ['success' => true, 'message' => 'Wahana unggulan berhasil diperbarui.']);
    }

    $multipart = $method === 'POST' && in_array($action, ['create', 'update'], true);
    if ($method === 'PUT' || $multipart) {
        $creating = $multipart && $action === 'create';
        $input = $multipart ? $_POST : readJsonInput();
        if ($multipart && empty($input) && (int) ($_SERVER['CONTENT_LENGTH'] ?? 0) > 5 * 1024 * 1024) {
            respond(413, ['success' => false, 'message' => 'Ukuran foto maksimal 5 MB.']);
        }
        $id = filter_var($input['id'] ?? null, FILTER_VALIDATE_INT);
        $name = $input['name'] ?? null;
        $description = $input['description'] ?? '';
        $ticketPrice = filter_var($input['ticket_price'] ?? null, FILTER_VALIDATE_INT);

        if ((!$creating && ($id === false || $id < 1))
            || !is_string($name) || !is_string($description) || $ticketPrice === false) {
            respond(400, ['success' => false, 'message' => 'Data wahana belum valid.']);
        }
        $name = trim($name);
        $description = trim($description);
        if ($name === '' || mb_strlen($name) > 100 || mb_strlen($description) > 10000) {
            respond(400, ['success' => false, 'message' => 'Nama wajib diisi (maksimal 100 karakter) dan deskripsi maksimal 10.000 karakter.']);
        }
        // Harga dari form baru harus tetap muat di Android untuk pesanan maksimal 20 tiket.
        $maxPrice = $multipart ? 107374182 : 4294967295;
        if ($ticketPrice < 1 || $ticketPrice > $maxPrice) {
            respond(400, ['success' => false, 'message' => 'Harga tiket harus berupa bilangan positif yang valid.']);
        }

        $newPath = $multipart ? storeWahanaImage($_FILES['image'] ?? null) : null;
        $oldPath = null;
        try {
            $pdo->beginTransaction();
            $values = ['name' => $name, 'description' => $description, 'ticket_price' => $ticketPrice];
            if ($creating) {
                $statement = $pdo->prepare(
                    'INSERT INTO wahana (name, description, ticket_price, image_path) '
                    . 'VALUES (:name, :description, :ticket_price, :image_path)'
                );
                $statement->execute($values + ['image_path' => $newPath]);
                $id = (int) $pdo->lastInsertId();
            } else {
                // Kunci baris supaya upload bersamaan membaca foto terakhir yang benar.
                $statement = $pdo->prepare(
                    'SELECT image_path FROM wahana WHERE id = :id AND is_deleted = 0 FOR UPDATE'
                );
                $statement->execute(['id' => $id]);
                $previous = $statement->fetch();
                if (!$previous) {
                    $pdo->rollBack();
                    removeWahanaUpload($newPath);
                    respond(404, ['success' => false, 'message' => 'Wahana tidak ditemukan.']);
                }
                $oldPath = $previous['image_path'];
                $statement = $pdo->prepare(
                    'UPDATE wahana SET name = :name, description = :description, ticket_price = :ticket_price, '
                    . 'image_path = COALESCE(:image_path, image_path) WHERE id = :id'
                );
                $statement->execute($values + ['id' => $id, 'image_path' => $newPath]);
            }

            $statement = $pdo->prepare(dashboardWahanaSelectSql() . 'AND w.id = :id');
            $statement->execute(['id' => $id]);
            $row = dashboardWahanaRow($statement->fetch());
            $pdo->commit();
        } catch (Throwable $exception) {
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            removeWahanaUpload($newPath);
            throw $exception;
        }
        if ($newPath !== null) {
            removeWahanaUpload($oldPath);
        }
        respond($creating ? 201 : 200, [
            'success' => true,
            'message' => $creating ? 'Wahana baru tersimpan.' : 'Perubahan wahana tersimpan.',
            'wahana' => $row,
        ]);
    }

    header('Allow: GET, POST, PUT, DELETE');
    respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
} catch (Throwable $exception) {
    handleApiError('dashboard wahana', $exception);
}
