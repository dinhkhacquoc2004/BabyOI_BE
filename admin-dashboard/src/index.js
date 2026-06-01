import { fileURLToPath } from 'node:url'
import AdminJS, { ComponentLoader } from 'adminjs'
import AdminJSExpress from '@adminjs/express'
import { Database, Resource } from '@adminjs/sql'
import dotenv from 'dotenv'
import express from 'express'
import { authenticate } from './auth.js'
import { ADMIN_PUBLIC_DIR, buildConnectionConfig, PORT, ROOT_PATH } from './config.js'
import { existingTables, initAdminDatabase } from './database.js'
import { buildResources } from './admin-sections/index.js'
import { ADMIN_ASSETS, ADMIN_BRANDING, ADMIN_LOCALE } from './theme.js'

dotenv.config()

const componentLoader = new ComponentLoader()
const BabyOiDashboard = componentLoader.add(
  'BabyOiDashboard',
  fileURLToPath(new URL('./components/BabyOiDashboard.js', import.meta.url)),
)
const ProfileSegmentProperty = componentLoader.add(
  'ProfileSegmentProperty',
  fileURLToPath(new URL('./components/ProfileSegmentProperty.js', import.meta.url)),
)
const FoodLibraryForm = componentLoader.add(
  'FoodLibraryForm',
  fileURLToPath(new URL('./components/FoodLibraryForm.js', import.meta.url)),
)

AdminJS.registerAdapter({
  Database,
  Resource,
})

async function start() {
  const app = express()
  const connectionConfig = buildConnectionConfig()
  const db = await initAdminDatabase(connectionConfig)
  const tableNames = await existingTables(connectionConfig.connectionString)
  const resources = buildResources(db, tableNames, {
    ProfileSegmentProperty,
    FoodLibraryForm,
    connectionString: connectionConfig.connectionString,
  })

  const admin = new AdminJS({
    rootPath: ROOT_PATH,
    componentLoader,
    branding: ADMIN_BRANDING,
    assets: ADMIN_ASSETS,
    locale: ADMIN_LOCALE,
    resources,
    dashboard: {
      handler: async () => ({
        tables: resources.length,
        database: connectionConfig.database,
      }),
      component: BabyOiDashboard,
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

  app.use('/admin-assets', express.static(ADMIN_PUBLIC_DIR))
  app.use(`${admin.options.rootPath}/frontend/assets/components.bundle.js`, (_, res, next) => {
    res.setHeader('Cache-Control', 'no-store')
    next()
  })
  app.use(admin.options.rootPath, router)
  app.get('/health', (_, res) => res.json({ status: 'UP' }))

  app.listen(PORT, () => {
    console.log(`BabyOI AdminJS running at http://localhost:${PORT}${ROOT_PATH}`)
  })
}

start().catch((error) => {
  console.error('Failed to start BabyOI AdminJS dashboard')
  console.error(error)
  process.exit(1)
})
