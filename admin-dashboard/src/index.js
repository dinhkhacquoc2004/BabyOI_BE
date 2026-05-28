import AdminJS from 'adminjs'
import AdminJSExpress from '@adminjs/express'
import { Adapter, Database, Resource } from '@adminjs/sql'
import dotenv from 'dotenv'
import express from 'express'
import session from 'express-session'
import pg from 'pg'

dotenv.config()

AdminJS.registerAdapter({
  Database,
  Resource,
})

const PORT = Number(process.env.ADMIN_PORT || 8090)
const ROOT_PATH = process.env.ADMIN_ROOT_PATH || '/admin'
const PUBLIC_SCHEMA = 'public'
const SPRING_API_BASE_URL = process.env.SPRING_API_BASE_URL || 'http://localhost:8085'

const TABLES = [
  'users',
  'roles',
  'profile',
  'type_code',
  'type_value',
  'food_library',
  'food_ingredients',
  'food_library_ingredients',
  'ingredient_nutrition',
  'food_nutrition_summary',
  'food_recommendation',
  'favorite_food',
  'restricted_food',
  'vaccine_type',
  'vaccine_schedule',
  'vaccine_record',
  'vaccination_center',
  'vaccine_pricing',
  'ai_teaching_lesson',
  'ai_lesson_progress',
  'handbook_posts',
  'handbook_comments',
  'auth_otp',
  'auth_refresh_token',
]

const HIDDEN_PROPERTIES = [
  'password_hash',
  'token_hash',
  'otp_hash',
  'code_hash',
  'api_secret',
]

function buildConnectionConfig() {
  if (process.env.DATABASE_URL) {
    return {
      connectionString: process.env.DATABASE_URL,
      database: databaseNameFromUrl(process.env.DATABASE_URL),
    }
  }

  const jdbcUrl = process.env.SPRING_DATASOURCE_URL || 'jdbc:postgresql://localhost:55195/babyoi'
  const username = process.env.SPRING_DATASOURCE_USERNAME || 'postgres'
  const password = process.env.SPRING_DATASOURCE_PASSWORD || '123'
  const normalizedUrl = jdbcUrl.replace(/^jdbc:/, '')
  const parsedUrl = new URL(normalizedUrl)
  parsedUrl.username = encodeURIComponent(username)
  parsedUrl.password = encodeURIComponent(password)

  return {
    connectionString: parsedUrl.toString(),
    database: parsedUrl.pathname.replace('/', ''),
  }
}

function databaseNameFromUrl(connectionString) {
  return new URL(connectionString).pathname.replace('/', '')
}

async function existingTables(connectionString) {
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

function resourceOptions(tableName) {
  const properties = Object.fromEntries(
    HIDDEN_PROPERTIES.map((propertyName) => [
      propertyName,
      { isVisible: { list: false, filter: false, show: false, edit: false } },
    ]),
  )

  return {
    navigation: navigationFor(tableName),
    properties,
  }
}

function navigationFor(tableName) {
  if (tableName.startsWith('food_') || ['favorite_food', 'restricted_food'].includes(tableName)) {
    return { name: 'Food' }
  }
  if (tableName.startsWith('vaccine') || tableName === 'vaccination_center') {
    return { name: 'Vaccine' }
  }
  if (tableName.startsWith('ai_')) {
    return { name: 'AI Teaching' }
  }
  if (tableName.startsWith('handbook_')) {
    return { name: 'Handbook' }
  }
  if (tableName.startsWith('auth_') || ['users', 'roles', 'profile'].includes(tableName)) {
    return { name: 'Users & Auth' }
  }
  if (tableName.startsWith('type_')) {
    return { name: 'Catalog' }
  }
  return { name: 'System' }
}

async function authenticate(username, password) {
  if (!username || !password) {
    return null
  }

  try {
    const response = await fetch(`${SPRING_API_BASE_URL}/api/admin/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.trim(),
        password,
      }),
    })

    if (!response.ok) {
      return null
    }

    const admin = await response.json()

    return {
      id: admin.id,
      email: admin.email,
      title: admin.userName || admin.email,
      role: admin.role,
    }
  } catch (error) {
    console.warn('Spring admin authentication failed:', error.message)
    return null
  }
}

async function start() {
  const app = express()
  const connectionConfig = buildConnectionConfig()
  const db = await initAdminDatabase(connectionConfig)
  const tableNames = await existingTables(connectionConfig.connectionString)

  const resources = TABLES
    .filter((tableName) => tableNames.has(tableName))
    .map((tableName) => ({
      resource: db.table(tableName),
      options: resourceOptions(tableName),
    }))

  const admin = new AdminJS({
    rootPath: ROOT_PATH,
    branding: {
      companyName: 'BabyOI Admin',
      softwareBrothers: false,
    },
    resources,
    dashboard: {
      handler: async () => ({
        tables: resources.length,
        database: connectionConfig.database,
      }),
      component: false,
    },
  })

  const router = AdminJSExpress.buildAuthenticatedRouter(
    admin,
    {
      authenticate,
      cookieName: 'babyoi_admin',
      cookiePassword: process.env.ADMIN_COOKIE_SECRET || 'change-this-long-random-secret',
    },
    null,
    {
      secret: process.env.ADMIN_COOKIE_SECRET || 'change-this-long-random-secret',
      resave: false,
      saveUninitialized: false,
      cookie: {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
      },
    },
  )

  app.use(admin.options.rootPath, router)
  app.get('/health', (_, res) => res.json({ status: 'UP' }))

  app.listen(PORT, () => {
    console.log(`BabyOI AdminJS running at http://localhost:${PORT}${ROOT_PATH}`)
  })
}

async function initAdminDatabase(connectionConfig) {
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

start().catch((error) => {
  console.error('Failed to start BabyOI AdminJS dashboard')
  console.error(error)
  process.exit(1)
})
