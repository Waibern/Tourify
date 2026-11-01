<?php
require_once __DIR__ . '/common.php';
require_once __DIR__ . '/dashboard_activity_feed.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
// Feed harus membaca aktivitas terbaru dan tidak boleh disimpan oleh cache browser.
header('Cache-Control: no-store');
requireMethod('GET');
requireDashboardRole(['admin']);

try {
    $cursor = dashboardActivityCursor($_GET);
    $activity = loadDashboardActivity(getDatabaseConnection(), $cursor);
    respond(200, ['success' => true] + $activity);
} catch (InvalidArgumentException $exception) {
    respond(400, ['success' => false, 'message' => $exception->getMessage()]);
} catch (Throwable $exception) {
    handleApiError('dashboard activity', $exception);
}
