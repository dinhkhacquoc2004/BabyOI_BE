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
