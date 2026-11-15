<?php
require_once __DIR__ . '/common.php';
require_once __DIR__ . '/dashboard_activity_feed.php';
configureDashboardCors();
header('Content-Type: application/json; charset=utf-8');
// Selalu ambil aktivitas terbaru; jangan pakai cache browser.
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
