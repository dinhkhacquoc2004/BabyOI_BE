import { fileURLToPath } from 'node:url'

export const PORT = Number(process.env.ADMIN_PORT || 8090)
export const ROOT_PATH = process.env.ADMIN_ROOT_PATH || '/admin'
export const PUBLIC_SCHEMA = 'public'
export const SPRING_API_BASE_URL = process.env.SPRING_API_BASE_URL || 'http://localhost:8085'
export const ADMIN_PUBLIC_DIR = fileURLToPath(new URL('./public', import.meta.url))

export function buildConnectionConfig() {
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
