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

const APP_TIME_ZONE = 'Asia/Ho_Chi_Minh'
const ANALYTICS_TABLES = {
  dailyActivity: `${PUBLIC_SCHEMA}.user_daily_activity`,
  featureUsage: `${PUBLIC_SCHEMA}.app_feature_daily_usage`,
}
const FEATURE_ANALYTICS_EXCLUDED_KEYS = [
  'ai',
  'ai_learning',
  'ai_teaching',
  'booking',
  'diary',
  'home',
  'notification',
  'profile',
  'settings',
]
const FEATURE_FILTER_SQL = `
  LOWER(COALESCE(afu.feature_key, '')) NOT IN (${FEATURE_ANALYTICS_EXCLUDED_KEYS.map((key) => `'${key}'`).join(', ')})
`

const USER_FILTER_SQL = `
  COALESCE(UPPER(r.role_name), '') <> 'ADMIN'
  AND LOWER(COALESCE(u.email, '')) NOT LIKE '%@babyoi.local'
`

function emptyAnalytics() {
  return {
    generatedAt: new Date().toISOString(),
    totalUsers: 0,
    activeUsersToday: 0,
    featureViewsToday: 0,
    uniqueFeatureUsersToday: 0,
    topFeatureToday: null,
    topFeaturesToday: [],
    topFeatures30Days: [],
    dailySeries: [],
    featureMonthlySeries: [],
  }
}

function toNumber(value) {
  if (value === null || value === undefined) {
    return 0
  }
  return Number(value)
}

function mapFeatureRow(row) {
  return {
    featureKey: row.feature_key,
    featureName: row.feature_name || row.feature_key,
    views: toNumber(row.views),
    uniqueUsers: toNumber(row.unique_users),
  }
}

export async function loadDashboardAnalytics(connectionString) {
  const analytics = emptyAnalytics()
  const client = new pg.Client({ connectionString })
  let connected = false

  try {
    await client.connect()
    connected = true

    const totalUsersResult = await client.query(`
      SELECT COUNT(*)::bigint AS total_users
      FROM users u
      LEFT JOIN roles r ON r.id = u.roles_id
      WHERE ${USER_FILTER_SQL}
    `)
    analytics.totalUsers = toNumber(totalUsersResult.rows[0]?.total_users)

    const tableResult = await client.query(
      'SELECT to_regclass($1) AS daily_activity, to_regclass($2) AS feature_usage',
      [ANALYTICS_TABLES.dailyActivity, ANALYTICS_TABLES.featureUsage],
    )
    const hasAnalyticsTables = Boolean(tableResult.rows[0]?.daily_activity && tableResult.rows[0]?.feature_usage)

    if (!hasAnalyticsTables) {
      return analytics
    }

    const todaySql = `(CURRENT_TIMESTAMP AT TIME ZONE '${APP_TIME_ZONE}')::date`
    const chartStartSql = `GREATEST(${todaySql} - INTERVAL '29 days', DATE '2026-06-26')::date`

    const summaryResult = await client.query(`
      SELECT
        (
          SELECT COUNT(DISTINCT uda.user_id)::bigint
          FROM user_daily_activity uda
          JOIN users u ON u.id = uda.user_id
          LEFT JOIN roles r ON r.id = u.roles_id
          WHERE uda.activity_date = ${todaySql}
            AND ${USER_FILTER_SQL}
        ) AS active_users_today,
        (
          SELECT COALESCE(SUM(afu.view_count), 0)::bigint
          FROM app_feature_daily_usage afu
          JOIN users u ON u.id = afu.user_id
          LEFT JOIN roles r ON r.id = u.roles_id
          WHERE afu.activity_date = ${todaySql}
            AND ${USER_FILTER_SQL}
            AND ${FEATURE_FILTER_SQL}
        ) AS feature_views_today,
        (
          SELECT COUNT(DISTINCT afu.user_id)::bigint
          FROM app_feature_daily_usage afu
          JOIN users u ON u.id = afu.user_id
          LEFT JOIN roles r ON r.id = u.roles_id
          WHERE afu.activity_date = ${todaySql}
            AND ${USER_FILTER_SQL}
            AND ${FEATURE_FILTER_SQL}
        ) AS unique_feature_users_today
    `)

    analytics.activeUsersToday = toNumber(summaryResult.rows[0]?.active_users_today)
    analytics.featureViewsToday = toNumber(summaryResult.rows[0]?.feature_views_today)
    analytics.uniqueFeatureUsersToday = toNumber(summaryResult.rows[0]?.unique_feature_users_today)

    const topTodayResult = await client.query(`
      SELECT
        afu.feature_key,
        COALESCE(NULLIF(MAX(afu.feature_name), ''), afu.feature_key) AS feature_name,
        COALESCE(SUM(afu.view_count), 0)::bigint AS views,
        COUNT(DISTINCT afu.user_id)::bigint AS unique_users
      FROM app_feature_daily_usage afu
      JOIN users u ON u.id = afu.user_id
      LEFT JOIN roles r ON r.id = u.roles_id
      WHERE afu.activity_date = ${todaySql}
        AND ${USER_FILTER_SQL}
        AND ${FEATURE_FILTER_SQL}
      GROUP BY afu.feature_key
      ORDER BY unique_users DESC, views DESC, feature_name ASC
      LIMIT 8
    `)
    analytics.topFeaturesToday = topTodayResult.rows.map(mapFeatureRow)
    analytics.topFeatureToday = analytics.topFeaturesToday[0] || null

    const top30DaysResult = await client.query(`
      SELECT
        afu.feature_key,
        COALESCE(NULLIF(MAX(afu.feature_name), ''), afu.feature_key) AS feature_name,
        COALESCE(SUM(afu.view_count), 0)::bigint AS views,
        COUNT(DISTINCT afu.user_id)::bigint AS unique_users
      FROM app_feature_daily_usage afu
      JOIN users u ON u.id = afu.user_id
      LEFT JOIN roles r ON r.id = u.roles_id
      WHERE afu.activity_date BETWEEN ${chartStartSql} AND ${todaySql}
        AND ${USER_FILTER_SQL}
        AND ${FEATURE_FILTER_SQL}
      GROUP BY afu.feature_key
      ORDER BY unique_users DESC, views DESC, feature_name ASC
      LIMIT 8
    `)
    analytics.topFeatures30Days = top30DaysResult.rows.map(mapFeatureRow)

    const dailySeriesResult = await client.query(`
      WITH days AS (
        SELECT generate_series(${chartStartSql}, ${todaySql}, INTERVAL '1 day')::date AS activity_date
      ),
      active AS (
        SELECT uda.activity_date, COUNT(DISTINCT uda.user_id)::bigint AS active_users
        FROM user_daily_activity uda
        JOIN users u ON u.id = uda.user_id
        LEFT JOIN roles r ON r.id = u.roles_id
        WHERE uda.activity_date BETWEEN ${chartStartSql} AND ${todaySql}
          AND ${USER_FILTER_SQL}
        GROUP BY uda.activity_date
      ),
      feature_views AS (
        SELECT afu.activity_date, COALESCE(SUM(afu.view_count), 0)::bigint AS feature_views
        FROM app_feature_daily_usage afu
        JOIN users u ON u.id = afu.user_id
        LEFT JOIN roles r ON r.id = u.roles_id
        WHERE afu.activity_date BETWEEN ${chartStartSql} AND ${todaySql}
          AND ${USER_FILTER_SQL}
          AND ${FEATURE_FILTER_SQL}
        GROUP BY afu.activity_date
      )
      SELECT
        to_char(days.activity_date, 'YYYY-MM-DD') AS date,
        to_char(days.activity_date, 'DD/MM') AS label,
        COALESCE(active.active_users, 0)::bigint AS active_users,
        COALESCE(feature_views.feature_views, 0)::bigint AS feature_views
      FROM days
      LEFT JOIN active ON active.activity_date = days.activity_date
      LEFT JOIN feature_views ON feature_views.activity_date = days.activity_date
      ORDER BY days.activity_date
    `)

    analytics.dailySeries = dailySeriesResult.rows.map((row) => ({
      date: row.date,
      label: row.label,
      activeUsers: toNumber(row.active_users),
      featureViews: toNumber(row.feature_views),
    }))

    const featureMonthlyResult = await client.query(`
      WITH days AS (
        SELECT generate_series(${chartStartSql}, ${todaySql}, INTERVAL '1 day')::date AS activity_date
      ),
      features AS (
        SELECT
          afu.feature_key,
          COALESCE(NULLIF(MAX(afu.feature_name), ''), afu.feature_key) AS feature_name
        FROM app_feature_daily_usage afu
        JOIN users u ON u.id = afu.user_id
        LEFT JOIN roles r ON r.id = u.roles_id
        WHERE afu.activity_date BETWEEN ${chartStartSql} AND ${todaySql}
          AND ${USER_FILTER_SQL}
          AND ${FEATURE_FILTER_SQL}
        GROUP BY afu.feature_key
      ),
      daily AS (
        SELECT
          afu.feature_key,
          afu.activity_date,
          COUNT(DISTINCT afu.user_id)::bigint AS unique_users,
          COALESCE(SUM(afu.view_count), 0)::bigint AS views
        FROM app_feature_daily_usage afu
        JOIN users u ON u.id = afu.user_id
        LEFT JOIN roles r ON r.id = u.roles_id
        WHERE afu.activity_date BETWEEN ${chartStartSql} AND ${todaySql}
          AND ${USER_FILTER_SQL}
          AND ${FEATURE_FILTER_SQL}
        GROUP BY afu.feature_key, afu.activity_date
      )
      SELECT
        f.feature_key,
        f.feature_name,
        to_char(d.activity_date, 'YYYY-MM-DD') AS date,
        to_char(d.activity_date, 'DD/MM') AS label,
        COALESCE(daily.unique_users, 0)::bigint AS unique_users,
        COALESCE(daily.views, 0)::bigint AS views
      FROM features f
      CROSS JOIN days d
      LEFT JOIN daily
        ON daily.feature_key = f.feature_key
       AND daily.activity_date = d.activity_date
      ORDER BY f.feature_name ASC, f.feature_key ASC, d.activity_date ASC
    `)

    const featureMap = new Map()
    featureMonthlyResult.rows.forEach((row) => {
      const featureKey = row.feature_key
      if (!featureMap.has(featureKey)) {
        featureMap.set(featureKey, {
          featureKey,
          featureName: row.feature_name || featureKey,
          totalUsers: 0,
          totalViews: 0,
          todayUsers: 0,
          todayViews: 0,
          series: [],
        })
      }

      const feature = featureMap.get(featureKey)
      const uniqueUsers = toNumber(row.unique_users)
      const views = toNumber(row.views)
      feature.totalUsers += uniqueUsers
      feature.totalViews += views
      feature.series.push({
        date: row.date,
        label: row.label,
        uniqueUsers,
        views,
      })
    })

    const today = analytics.dailySeries[analytics.dailySeries.length - 1]?.date
    analytics.featureMonthlySeries = Array.from(featureMap.values())
      .map((feature) => {
        const todayPoint = feature.series.find((item) => item.date === today)
        return {
          ...feature,
          todayUsers: todayPoint?.uniqueUsers || 0,
          todayViews: todayPoint?.views || 0,
        }
      })
      .sort((left, right) => (
        right.todayViews - left.todayViews
        || right.totalViews - left.totalViews
        || left.featureName.localeCompare(right.featureName, 'vi')
      ))

    return analytics
  } catch (error) {
    console.warn('Could not load dashboard analytics', error)
    return analytics
  } finally {
    if (connected) {
      await client.end()
    }
  }
}
