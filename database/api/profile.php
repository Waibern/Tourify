<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');

try {
    $pdo = getDatabaseConnection();
    $userId = optionalAppUserId($pdo);
    if ($userId === null) {
        $dashboardUser = requireDashboardRole(['admin', 'gate']);
        $userId = (int) $dashboardUser['id'];
    }

    $statement = $pdo->prepare(
        'SELECT username, profile_image FROM users WHERE id = :id LIMIT 1'
    );
    $statement->execute(['id' => $userId]);
    $profile = $statement->fetch();
    if (!$profile) {
        respond(404, ['success' => false, 'message' => 'Profil tidak ditemukan.']);
    }

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        respond(200, ['success' => true, 'profile' => $profile]);
    }

    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        header('Allow: GET, POST, OPTIONS');
        respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
    }

    $image = $_FILES['image'] ?? null;
    if (!is_array($image)) {
        respond(400, ['success' => false, 'message' => 'Pilih foto profil terlebih dahulu.']);
    }
    $uploadError = $image['error'] ?? UPLOAD_ERR_NO_FILE;
    if (in_array($uploadError, [UPLOAD_ERR_INI_SIZE, UPLOAD_ERR_FORM_SIZE], true)) {
        respond(413, ['success' => false, 'message' => 'Ukuran foto maksimal 5 MB.']);
    }
    if ($uploadError !== UPLOAD_ERR_OK) {
        respond(400, ['success' => false, 'message' => 'Foto gagal diterima server. Coba pilih ulang.']);
    }

    // Pastikan file berasal dari unggahan HTTP yang sedang diproses.
    $temporaryPath = $image['tmp_name'] ?? '';
    if (!is_string($temporaryPath) || $temporaryPath === '' || !is_uploaded_file($temporaryPath)) {
        respond(400, ['success' => false, 'message' => 'Foto unggahan tidak valid.']);
    }
    $maxBytes = 5 * 1024 * 1024;
    $fileSize = filesize($temporaryPath);
    if ($fileSize === false || $fileSize < 1 || $fileSize > $maxBytes) {
        respond(413, ['success' => false, 'message' => 'Ukuran foto maksimal 5 MB.']);
    }

    $mimeType = (new finfo(FILEINFO_MIME_TYPE))->file($temporaryPath);
    $extensions = [
        'image/jpeg' => 'jpg',
        'image/png' => 'png',
        'image/webp' => 'webp',
    ];
    $imageInfo = @getimagesize($temporaryPath);
    if (
        !is_string($mimeType)
        || !isset($extensions[$mimeType])
        || $imageInfo === false
        || $imageInfo['mime'] !== $mimeType
        || $imageInfo[0] < 1
        || $imageInfo[1] < 1
        || $imageInfo[0] * $imageInfo[1] > 40000000
    ) {
        respond(415, ['success' => false, 'message' => 'Gunakan foto JPG, PNG, atau WebP yang valid.']);
    }

    $uploadDirectory = __DIR__ . '/uploads/profile';
    if (!is_dir($uploadDirectory) && !mkdir($uploadDirectory, 0755, true) && !is_dir($uploadDirectory)) {
        respond(500, ['success' => false, 'message' => 'Folder foto profil tidak bisa dibuat.']);
    }
    $fileName = bin2hex(random_bytes(16)) . '.' . $extensions[$mimeType];
    $storedPath = 'uploads/profile/' . $fileName;
    $destination = $uploadDirectory . '/' . $fileName;
    if (!move_uploaded_file($temporaryPath, $destination)) {
        respond(500, ['success' => false, 'message' => 'Foto profil gagal disimpan.']);
    }

    // Path database baru dipakai setelah file berhasil disimpan.
    try {
        $update = $pdo->prepare('UPDATE users SET profile_image = :profile_image WHERE id = :id');
        $update->execute(['profile_image' => $storedPath, 'id' => $userId]);
    } catch (Throwable $exception) {
        @unlink($destination);
        throw $exception;
    }

    $oldPath = $profile['profile_image'] ?? '';
    if (is_string($oldPath) && preg_match('#^uploads/profile/[a-f0-9]{32}\.(jpg|png|webp)$#', $oldPath)) {
        @unlink(__DIR__ . '/' . $oldPath);
    }

    $profile['profile_image'] = $storedPath;
    respond(200, ['success' => true, 'message' => 'Foto profil berhasil diperbarui.', 'profile' => $profile]);
} catch (Throwable $exception) {
    handleApiError('profile', $exception);
}
