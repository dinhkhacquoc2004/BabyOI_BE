import { Adapter } from '@adminjs/sql'
import pg from 'pg'
import { PUBLIC_SCHEMA } from './config.js'

export async function existingTables(connectionString) {
  const client = new pg.Client({ connectionString })
  await client.connect()
  try {
    const result = await client.query(
      `
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = $1
          AND table_type = 'BASE TABLE'
      `,
      [PUBLIC_SCHEMA],
    )
    return new Set(result.rows.map((row) => row.table_name))
  } finally {
    await client.end()
  }
}

export async function initAdminDatabase(connectionConfig) {
  const originalConsoleError = console.error
  console.error = (...args) => {
    const message = args.map((arg) => String(arg)).join(' ')
    if (message.includes('has no primary key')) {
      return
    }
    originalConsoleError(...args)
  }

  try {
    return await new Adapter('postgresql', {
      connectionString: connectionConfig.connectionString,
      database: connectionConfig.database,
    }).init()
  } finally {
    console.error = originalConsoleError
  }
}

export async function loadDashboardAnalytics(connectionString, days = 30) {
  const client = new pg.Client({ connectionString })
  await client.connect()

  try {
    const [registeredUsersResult, activeUsersTodayResult, nutritionPageViewsResult, dailyResult] = await Promise.all([
      client.query(
        `
          SELECT COUNT(*)::int AS total
          FROM users u
          JOIN roles r ON r.id = u.roles_id
          WHERE UPPER(r.role_name) <> 'ADMIN'
            AND LOWER(u.email) NOT LIKE '%@babyoi.local'
        `,
      ),
      client.query(
        `
          SELECT COUNT(*)::int AS total
          FROM user_daily_activity
          WHERE activity_date = (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
        `,
      ),
      client.query(
        `
          SELECT COALESCE(SUM(view_count), 0) AS total
          FROM app_page_daily_activity
          WHERE page_key = 'NUTRITION'
        `,
      ),
      client.query(
        `
          WITH app_today AS (
            SELECT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS value
          ),
          days AS (
            SELECT generate_series(
              (SELECT value FROM app_today) - ($1::int - 1),
              (SELECT value FROM app_today),
              INTERVAL '1 day'
            )::date AS activity_date
          ),
          active_users AS (
            SELECT activity_date, COUNT(*)::int AS total
            FROM user_daily_activity
            WHERE activity_date >= (SELECT value FROM app_today) - ($1::int - 1)
            GROUP BY activity_date
          ),
          nutrition_views AS (
            SELECT activity_date, SUM(view_count)::int AS total
            FROM app_page_daily_activity
            WHERE page_key = 'NUTRITION'
              AND activity_date >= (SELECT value FROM app_today) - ($1::int - 1)
            GROUP BY activity_date
          )
          SELECT
            TO_CHAR(days.activity_date, 'YYYY-MM-DD') AS date,
            COALESCE(active_users.total, 0)::int AS active_users,
            COALESCE(nutrition_views.total, 0)::int AS nutrition_page_views
          FROM days
          LEFT JOIN active_users USING (activity_date)
          LEFT JOIN nutrition_views USING (activity_date)
          ORDER BY days.activity_date
        `,
        [days],
      ),
    ])

    return {
      summary: {
        registeredUsers: Number(registeredUsersResult.rows[0]?.total || 0),
        activeUsersToday: Number(activeUsersTodayResult.rows[0]?.total || 0),
        nutritionPageViews: Number(nutritionPageViewsResult.rows[0]?.total || 0),
      },
      daily: dailyResult.rows.map((row) => ({
        date: row.date,
        activeUsers: Number(row.active_users || 0),
        nutritionPageViews: Number(row.nutrition_page_views || 0),
      })),
    }
  } finally {
    await client.end()
  }
}
