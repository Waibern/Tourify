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
with tempfile.TemporaryDirectory() as sessions:
    def request(endpoint, params=None, role='admin', method='GET'):
        # Simpan sesi tes di folder sementara supaya login browser tetap aman.
        wrapper = '''
        session_save_path($argv[1]); session_start(); $_SESSION = [];
        $role = $argv[2];
        if ($role !== '') $_SESSION['dashboard_user'] = ['id' => 0, 'username' => 'test', 'role' => $role];
        $_SERVER['REQUEST_METHOD'] = $argv[3];
        $_GET = json_decode($argv[4], true);
        register_shutdown_function(function () { fwrite(STDERR, '\\nHTTP:' . http_response_code()); });
        require $argv[5];
        '''
        result = subprocess.run([php, '-r', wrapper, sessions, role, method,
                                 json.dumps(params or {}), str(api / endpoint)], capture_output=True, text=True, check=True)
        code = int(result.stderr.rsplit('HTTP:', 1)[1])
        return code, json.loads(result.stdout)

    endpoint = 'dashboard_analytics.php'
    for role, expected in [('', 401), ('gate', 403)]:
        assert request(endpoint, role=role)[0] == expected
    for params in [{'start': '2026-02-30'}, {'start': []}, {'start': '2026-10-31', 'end': '2026-10-01'}, {'start': '2020-01-01', 'end': '2026-10-30'}]:
        assert request(endpoint, params)[0] == 400
    for params in [{}, {'start': '2026-10-30', 'end': '2026-10-30'}, {'start': '2099-01-01', 'end': '2099-01-14'}]:
        code, response = request(endpoint, params)
        assert code == 200 and response['success']
        data = response['analytics']
        assert len(data['series']) == data['comparison']['days']
        assert len(set(row['date'] for row in data['series'])) == len(data['series'])
        for stat, series_key in [('new_customers', 'new_customers'), ('orders', 'order_count'), ('tickets', 'ticket_count'), ('checkins', 'checkin_count')]:
            assert sum(row[series_key] for row in data['series']) == data['stats'][stat]
        comparison = data['comparison']
        code, previous = request(endpoint, {'start': comparison['start'], 'end': comparison['end']})
        assert code == 200
        assert data['previous_stats'] == previous['analytics']['stats']
        assert len(previous['analytics']['series']) == len(data['series'])
    print('Analytics: access, validation, daily totals, equal-length comparison and empty ranges passed')
