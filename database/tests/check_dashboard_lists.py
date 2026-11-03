"""Cek endpoint baca dengan sesi CLI terisolasi; database tidak diubah.
Usage: python3 check_dashboard_lists.py /path/to/api /path/to/php
"""
import json
import subprocess
import sys
import tempfile
from pathlib import Path
from datetime import datetime
from zoneinfo import ZoneInfo

api, php = Path(sys.argv[1]).resolve(), sys.argv[2]
users_result = subprocess.run([
    php,
    '-r',
    '''
    require $argv[1] . '/db.php';
    $pdo = getDatabaseConnection();
    $users = [];
    foreach (['admin', 'gate', 'customer'] as $role) {
        $statement = $pdo->prepare('SELECT id FROM users WHERE role = :role ORDER BY id LIMIT 1');
        $statement->execute(['role' => $role]);
        $id = $statement->fetchColumn();
        if ($id !== false) $users[$role] = (int) $id;
    }
    echo json_encode($users);
    ''',
    str(api),
], capture_output=True, text=True, check=True)
user_ids = json.loads(users_result.stdout)
if 'admin' not in user_ids:
    raise SystemExit('No admin account is available for the read-only endpoint checks.')

with tempfile.TemporaryDirectory() as sessions:
    def request(endpoint, params=None, role='admin', method='GET'):
        # Sesi hanya dibuat di folder sementara agar login browser tidak tersentuh.
        wrapper = '''
        session_save_path($argv[1]); session_start(); $_SESSION = [];
        $role = $argv[2];
        if ($role !== '') $_SESSION['dashboard_user'] = ['id' => (int) $argv[3], 'username' => 'test', 'role' => $role];
        $_SERVER['REQUEST_METHOD'] = $argv[4];
        $_GET = json_decode($argv[5], true);
        register_shutdown_function(function () { fwrite(STDERR, '\\nHTTP:' . http_response_code()); });
        require $argv[6];
        '''
        user_id = user_ids.get(role, '') if role else ''
        result = subprocess.run([php, '-r', wrapper, sessions, role, str(user_id), method,
                                 json.dumps(params or {}), str(api / endpoint)], capture_output=True, text=True, check=True)
        code = int(result.stderr.rsplit('HTTP:', 1)[1])
        return code, json.loads(result.stdout)

    for key in ['orders', 'checkins']:
        endpoint = f'dashboard_{key}.php'
        # Akun aplikasi customer bukan sesi dashboard dan harus mendapat 401.
        denied_roles = [('', 401)]
        if 'gate' in user_ids:
            denied_roles.append(('gate', 403))
        if 'customer' in user_ids:
            denied_roles.append(('customer', 401))
        for role, expected in denied_roles:
            actual = request(endpoint, role=role)[0]
            assert actual == expected, f'{endpoint} role {role or "guest"}: expected {expected}, got {actual}'
        assert request(endpoint, method='POST')[0] == 405
        for params in [{'page': 0}, {'page': 'x'}, {'date': '2026-02-30'}, {'q': []}, {'date': []}]:
            assert request(endpoint, params)[0] == 400
        code, data = request(endpoint)
        assert code == 200 and data['success']
        assert len(data[key]) <= data['per_page'] == 20
        assert data['total'] >= len(data[key]) and data['page'] == 1
        assert request(endpoint, {'q': 'nonexistent-check-5488c2'})[1]['total'] == 0
        assert request(endpoint, {'page': 999999})[1]['page'] == max(1, (data['total'] + 19) // 20)
        if data[key]:
            row = data[key][0]
            date = row['visit_date'] if key == 'orders' else row['scanned_at'][:10]
            filtered = request(endpoint, {'date': date})[1]
            assert all((r['visit_date'] if key == 'orders' else r['scanned_at'][:10]) == date for r in filtered[key])
            searched = request(endpoint, {'q': row['username']})[1]
            assert searched['total'] > 0
        if key == 'orders':
            today = datetime.now(ZoneInfo('Asia/Jakarta')).date().isoformat()
            for row in data[key]:
                expected = 'Used' if row['checked_in_quantity'] >= row['quantity'] else 'Expire' if row['visit_date'] < today else 'Active'
                assert row['status'] == expected
            for status in ['Active', 'Used', 'Expire']:
                result = request(endpoint, {'status': status})[1]
                assert all(r['status'] == status for r in result['orders'])
            assert request(endpoint, {'status': 'Paid'})[0] == 400
        print(f'{key}: access, validation, filters, pagination and response checks passed')
