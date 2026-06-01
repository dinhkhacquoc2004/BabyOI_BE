# BabyOI Admin Dashboard

Admin dashboard for the BabyOI PostgreSQL database using AdminJS.

AdminJS is a Node.js admin panel. This project runs next to the Spring Boot API and connects to the same PostgreSQL database.

## Run

Requires Node.js 20.10 or newer. AdminJS dependencies use modern JSON import attributes that fail on older Node 18 releases.

### Ubuntu / Linux / macOS

```bash
cd admin-dashboard
cp .env.example .env
corepack enable
pnpm install --frozen-lockfile
pnpm run dev
```

If you use `nvm`:

```bash
cd admin-dashboard
nvm install
nvm use
corepack enable
pnpm install --frozen-lockfile
pnpm run dev
```

### Windows PowerShell

```powershell
cd admin-dashboard
copy .env.example .env
corepack enable
pnpm install --frozen-lockfile
pnpm run dev
```

On Windows, you can run it together with the Spring Boot backend from the repository root:

```powershell
.\scripts\run-backend-with-admin.ps1
```

On Ubuntu / Linux / macOS, run both services from the repository root:

```bash
chmod +x scripts/run-backend-with-admin.sh
./scripts/run-backend-with-admin.sh quoc
```

The Spring Boot app also auto-starts this dashboard by default when the backend is ready.
Set `ADMIN_DASHBOARD_ENABLED=false` if you want to disable that behavior.

Open:

```text
http://localhost:8090/admin
```

Login is validated by Spring Boot through `POST /api/admin/auth/login`.
Only users authenticated by Spring Security with `ADMIN` role can access `/admin`.
Set `ADMIN_COOKIE_SECRET` in `.env` before sharing or deploying.

## Database

The dashboard uses `DATABASE_URL` first. If that is not set, it builds the connection string from:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

The default points to the `quoc` Spring profile database:

```text
postgresql://postgres:123@localhost:55195/babyoi
```
