#!/usr/bin/env bash
set -euo pipefail

PROFILE="${1:-quoc}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ADMIN_DIR="$ROOT_DIR/admin-dashboard"

if ! command -v node >/dev/null 2>&1; then
  echo "Missing command 'node'. Install Node.js 20.10+ and try again." >&2
  exit 1
fi

NODE_VERSION="$(node --version | sed 's/^v//')"
NODE_MAJOR="${NODE_VERSION%%.*}"
NODE_MINOR="$(echo "$NODE_VERSION" | cut -d. -f2)"
if [ "$NODE_MAJOR" -lt 20 ] || { [ "$NODE_MAJOR" -eq 20 ] && [ "$NODE_MINOR" -lt 10 ]; }; then
  echo "AdminJS requires Node.js 20.10+. Current version is v$NODE_VERSION." >&2
  exit 1
fi

if [ ! -f "$ADMIN_DIR/.env" ] && [ -f "$ADMIN_DIR/.env.example" ]; then
  cp "$ADMIN_DIR/.env.example" "$ADMIN_DIR/.env"
fi

if ! command -v pnpm >/dev/null 2>&1; then
  if ! command -v corepack >/dev/null 2>&1; then
    echo "Missing pnpm/corepack. Install pnpm or enable corepack, then try again." >&2
    exit 1
  fi
  corepack enable
fi

PNPM=(pnpm)
if ! command -v pnpm >/dev/null 2>&1; then
  PNPM=(corepack pnpm)
fi

echo "Installing AdminJS dependencies for this OS..."
(cd "$ADMIN_DIR" && "${PNPM[@]}" install --frozen-lockfile)

echo "Starting BabyOI backend with profile '$PROFILE'..."
(cd "$ROOT_DIR" && ./mvnw spring-boot:run "-Dspring-boot.run.profiles=$PROFILE") &
BACKEND_PID=$!

echo "Starting AdminJS dashboard..."
(cd "$ADMIN_DIR" && "${PNPM[@]}" start) &
ADMIN_PID=$!

cleanup() {
  kill "$BACKEND_PID" "$ADMIN_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

echo ""
echo "Backend: http://localhost:8085"
echo "AdminJS: http://localhost:8090/admin"
wait -n "$BACKEND_PID" "$ADMIN_PID"
