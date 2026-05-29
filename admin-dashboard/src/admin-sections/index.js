import { aiSection } from './ai.js'
import { camNangSection } from './camnang.js'
import { danhMucSection } from './danhmuc.js'
import { dinhDuongSection } from './dinhduong.js'
import { nguoiDungSection } from './nguoidung.js'
import { tiemChungSection } from './tiemchung.js'

const HIDDEN_PROPERTIES = [
  'password_hash',
  'token_hash',
  'otp_hash',
  'code_hash',
  'api_secret',
]

export const ADMIN_SECTIONS = [
  nguoiDungSection,
  danhMucSection,
  dinhDuongSection,
  tiemChungSection,
  aiSection,
  camNangSection,
]

export function buildResources(db, existingTableNames) {
  return ADMIN_SECTIONS.flatMap((section) =>
    section.tables
      .filter((tableName) => existingTableNames.has(tableName))
      .map((tableName) => ({
        resource: db.table(tableName),
        options: resourceOptions(section),
      })),
  )
}

function resourceOptions(section) {
  const properties = Object.fromEntries(
    HIDDEN_PROPERTIES.map((propertyName) => [
      propertyName,
      { isVisible: { list: false, filter: false, show: false, edit: false } },
    ]),
  )

  return {
    navigation: section.navigation,
    properties,
  }
}
