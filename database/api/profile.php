<?php
require_once __DIR__ . '/common.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');

try {
    $pdo = getDatabaseConnection();
    $method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
    $hasUsername = $method === 'PUT'
        || ($method === 'POST' && array_key_exists('username', $_POST));
    $dashboardUser = null;
    if ($hasUsername) {
        // Username hanya bisa diubah dari sesi dashboard admin atau petugas.
        $dashboardUser = requireDashboardRole(['admin', 'gate']);
        $userId = (int) $dashboardUser['id'];
    } else {
        $userId = optionalAppUserId($pdo);
        if ($userId === null) {
            $dashboardUser = requireDashboardRole(['admin', 'gate']);
            $userId = (int) $dashboardUser['id'];
        }
    }

    $statement = $pdo->prepare(
        'SELECT username, profile_image FROM users WHERE id = :id LIMIT 1'
    );
    $statement->execute(['id' => $userId]);
    $profile = $statement->fetch();
    if (!$profile) {
        respond(404, ['success' => false, 'message' => 'Profil tidak ditemukan.']);
    }

    if ($method === 'GET') {
        respond(200, ['success' => true, 'profile' => $profile]);
    }

    if ($method === 'PUT') {
        $input = readJsonInput();
        $username = $input['username'] ?? null;
        if (!is_string($username)) {
            respond(400, ['success' => false, 'message' => 'Username harus berupa teks.']);
        }
        $username = trim($username);
        if ($username === '' || mb_strlen($username) > 50) {
            respond(400, ['success' => false, 'message' => 'Username wajib diisi dan maksimal 50 karakter.']);
        }

        $update = $pdo->prepare('UPDATE users SET username = :username WHERE id = :id');
        $update->execute(['username' => $username, 'id' => $userId]);
        $_SESSION['dashboard_user']['username'] = $username;
        $profile['username'] = $username;
        respond(200, ['success' => true, 'message' => 'Username berhasil diperbarui.', 'profile' => $profile]);
    }

    if ($method !== 'POST') {
        header('Allow: GET, POST, PUT, OPTIONS');
        respond(405, ['success' => false, 'message' => 'Metode request tidak diizinkan.']);
    }

    $username = null;
    if ($hasUsername) {
        $username = $_POST['username'] ?? null;
        if (!is_string($username)) {
            respond(400, ['success' => false, 'message' => 'Username harus berupa teks.']);
        }
        $username = trim($username);
        if ($username === '' || mb_strlen($username) > 50) {
            respond(400, ['success' => false, 'message' => 'Username wajib diisi dan maksimal 50 karakter.']);
        }
    }

    $image = $_FILES['image'] ?? null;
    if (!is_array($image) && $username === null) {
        respond(400, ['success' => false, 'message' => 'Pilih foto profil terlebih dahulu.']);
    }
    $uploadError = is_array($image) ? ($image['error'] ?? UPLOAD_ERR_NO_FILE) : UPLOAD_ERR_NO_FILE;
    if ($uploadError === UPLOAD_ERR_NO_FILE) {
        if ($username === null) {
            respond(400, ['success' => false, 'message' => 'Pilih foto profil terlebih dahulu.']);
        }
        $image = null;
    } elseif (!is_array($image)) {
        respond(400, ['success' => false, 'message' => 'Foto profil tidak valid.']);
    }
    $temporaryPath = null;
    $fileName = null;
    $storedPath = null;
    if ($image !== null && $uploadError !== UPLOAD_ERR_NO_FILE) {
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
    }

    // Username dan foto disimpan bersama agar tombol profil tidak membuat update setengah jalan.
    try {
        if ($username !== null && $storedPath !== null) {
            $update = $pdo->prepare(
                'UPDATE users SET username = :username, profile_image = :profile_image WHERE id = :id'
            );
            $update->execute(['username' => $username, 'profile_image' => $storedPath, 'id' => $userId]);
        } elseif ($username !== null) {
            $update = $pdo->prepare('UPDATE users SET username = :username WHERE id = :id');
            $update->execute(['username' => $username, 'id' => $userId]);
        } elseif ($storedPath !== null) {
            $update = $pdo->prepare('UPDATE users SET profile_image = :profile_image WHERE id = :id');
            $update->execute(['profile_image' => $storedPath, 'id' => $userId]);
        }
    } catch (Throwable $exception) {
        if ($fileName !== null) {
            @unlink($uploadDirectory . '/' . $fileName);
        }
        throw $exception;
    }

    if ($dashboardUser !== null && $username !== null) {
        $_SESSION['dashboard_user']['username'] = $username;
        $profile['username'] = $username;
    }
    if ($storedPath !== null) {
        $oldPath = $profile['profile_image'] ?? '';
        if (is_string($oldPath) && preg_match('#^uploads/profile/[a-f0-9]{32}\.(jpg|png|webp)$#', $oldPath)) {
            @unlink(__DIR__ . '/' . $oldPath);
        }
        $profile['profile_image'] = $storedPath;
    }

    respond(200, ['success' => true, 'message' => 'Profil berhasil diperbarui.', 'profile' => $profile]);
} catch (PDOException $exception) {
    if ($exception->getCode() === '23000') {
        respond(409, ['success' => false, 'message' => 'Username sudah digunakan.']);
    }
    handleApiError('profile', $exception);
} catch (Throwable $exception) {
    handleApiError('profile', $exception);
}
