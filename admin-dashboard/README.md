# BabyOI Admin Dashboard

Admin dashboard for the BabyOI PostgreSQL database using AdminJS.

AdminJS is a Node.js admin panel. This project runs next to the Spring Boot API and connects to the same PostgreSQL database.

## Run

```powershell
cd admin-dashboard
copy .env.example .env
npm install
npm run dev
```

Or run it together with the Spring Boot backend from the repository root:

```powershell
.\scripts\run-backend-with-admin.ps1
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
