#!/usr/bin/env bash
set -Eeuo pipefail

REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
DASHBOARD_DIR="$REPO_ROOT/web-dashboard"

if [[ "$(git -C "$REPO_ROOT" branch --show-current)" != "main" ]]; then
    echo "Jalankan deploy dari branch main." >&2
    exit 1
fi

if ! git -C "$REPO_ROOT" diff --quiet || ! git -C "$REPO_ROOT" diff --cached --quiet; then
    echo "Ada perubahan Git lokal. Simpan atau commit dulu sebelum deploy." >&2
    exit 1
fi

if ! command -v pnpm >/dev/null 2>&1 || ! command -v curl >/dev/null 2>&1; then
    echo "pnpm dan curl harus tersedia di VPS." >&2
    exit 1
fi

git -C "$REPO_ROOT" pull --ff-only origin main

(
    cd "$DASHBOARD_DIR"
    pnpm install --frozen-lockfile
    pnpm build
)

sudo systemctl restart tourify-dashboard

for attempt in {1..20}; do
    if curl --fail --silent --output /dev/null http://127.0.0.1:3000/login; then
        echo "Tourify dashboard aktif setelah update."
        exit 0
    fi

    sleep 1
done

echo "Dashboard belum merespons di 127.0.0.1:3000/login; cek journal tourify-dashboard." >&2
exit 1
