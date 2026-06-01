import { aiSection } from './ai.js'
import { camNangSection } from './camnang.js'
import { danhMucSection } from './danhmuc.js'
import { dinhDuongSection } from './dinhduong.js'
import { foodLibraryFullFormActions } from './food-library-action.js'
import { nguoiDungSection } from './nguoidung.js'
import { tiemChungSection } from './tiemchung.js'

const HIDDEN_PROPERTIES = [
  'password_hash',
  'token_hash',
  'otp_hash',
  'code_hash',
  'api_secret',
]

const PROFILE_TYPE_OPTIONS = [
  { value: 'MOTHER', label: 'Mẹ' },
  { value: 'CHILD', label: 'Bé' },
]

const SEX_OPTIONS = [
  { value: 'MALE', label: 'Nam' },
  { value: 'FEMALE', label: 'Nữ' },
  { value: 'OTHER', label: 'Khác' },
]

const FOOD_FUNCTION_CODE_OPTIONS = [
  { value: 1, label: 'Mother' },
  { value: 2, label: 'Baby' },
]

const FOOD_ADVICE_FOR_OPTIONS = [
  { value: 'FOR_MOTHER_DIET', label: 'Mother diet' },
  { value: 'FOR_MOTHER_CHANGE_DIET', label: 'Mother change diet' },
  { value: 'FOR_MOTHER_POSTPARTUM_BREASTFEEDING', label: 'Mother postpartum breastfeeding' },
  { value: 'FOR_MOTHER_HEALTHY_ENERGY', label: 'Mother healthy energy' },
  { value: 'FOR_MOTHER_DIGESTION_RECOVERY', label: 'Mother digestion recovery' },
  { value: 'FOR_MOTHER_INCREASE_MILK_SUPPLY', label: 'Mother increase milk supply' },
  { value: 'FOR_MOTHER_SLEEP_STRESS_SUPPORT', label: 'Mother sleep stress support' },
  { value: 'FOR_BABY_6_8_MONTHS', label: 'Baby 6-8 months' },
  { value: 'FOR_BABY_9_11_MONTHS', label: 'Baby 9-11 months' },
  { value: 'FOR_BABY_12_18_MONTHS', label: 'Baby 12-18 months' },
  { value: 'FOR_BABY_19_24_MONTHS', label: 'Baby 19-24 months' },
  { value: 'FOR_BABY_6_8_MONTHS_DEVELOPMENT', label: 'Baby 6-8 months development' },
  { value: 'FOR_BABY_9_11_MONTHS_DEVELOPMENT', label: 'Baby 9-11 months development' },
  { value: 'FOR_BABY_12_18_MONTHS_DEVELOPMENT', label: 'Baby 12-18 months development' },
  { value: 'FOR_BABY_19_24_MONTHS_DEVELOPMENT', label: 'Baby 19-24 months development' },
]

const TITLE_PROPERTIES = {
  ai_lesson_progress: 'lesson_id',
  ai_teaching_lesson: 'lesson_name',
  auth_otp: 'email',
  auth_refresh_token: 'user_id',
  favorite_food: 'food_libarary_id',
  food_ingredients: 'name_ingredients',
  food_library: 'name',
  food_library_ingredients: 'food_lib_id',
  food_nutrition_summary: 'food_id',
  food_recommendation: 'food_id',
  handbook_comments: 'content',
  handbook_posts: 'title',
  ingredient_nutrition: 'food_ingredient_id',
  profile: 'name',
  restricted_food: 'food_libarary_id',
  roles: 'role_name',
  type_code: 'name',
  type_value: 'value_name',
  users: 'email',
  vaccination_center: 'name',
  vaccine_pricing: 'vaccine_type_id',
  vaccine_record: 'vaccine_type_id',
  vaccine_schedule: 'vaccine_type_id',
  vaccine_type: 'name',
}

const FOOD_PROPERTY_OPTIONS = {
  food_library: {
    id: { label: 'ID' },
    name: { label: 'Tên món ăn' },
    function_code: { label: 'Nhóm món ăn' },
    advance_for: { label: 'Mục tiêu dinh dưỡng' },
  },
  food_ingredients: {
    id: { label: 'ID' },
    name_ingredients: { label: 'Tên nguyên liệu' },
  },
  food_library_ingredients: {
    id: { label: 'ID' },
    food_lib_id: { label: 'Món ăn' },
    food_ing_id: { label: 'Nguyên liệu' },
  },
  ingredient_nutrition: {
    id: { label: 'ID' },
    food_ingredient_id: { label: 'Nguyên liệu' },
  },
  food_nutrition_summary: {
    id: { label: 'ID' },
    food_id: { label: 'Món ăn' },
  },
  food_recommendation: {
    id: { label: 'ID' },
    food_id: { label: 'Món ăn' },
  },
  favorite_food: {
    id: { label: 'ID' },
    food_libarary_id: { label: 'Món ăn' },
    profile_id: { label: 'Hồ sơ' },
    user_id: { label: 'Email' },
  },
  restricted_food: {
    id: { label: 'ID' },
    food_libarary_id: { label: 'Món ăn' },
    profile_id: { label: 'Hồ sơ' },
    user_id: { label: 'Email' },
  },
}

const FOOD_LIST_PROPERTIES = {
  food_library: ['id', 'name', 'function_code', 'advance_for', 'status'],
  food_ingredients: ['id', 'name_ingredients', 'image_url', 'status'],
  food_library_ingredients: ['id', 'food_lib_id', 'food_ing_id', 'amount_per_serving', 'unit', 'status'],
  ingredient_nutrition: ['id', 'food_ingredient_id', 'base_amount', 'base_unit', 'calories', 'protein', 'carbs', 'fat', 'status'],
  food_nutrition_summary: ['id', 'food_id', 'total_calories', 'total_protein', 'total_carbs', 'total_fat'],
  food_recommendation: ['id', 'food_id', 'status'],
  favorite_food: ['id', 'profile_id', 'user_id', 'food_libarary_id'],
  restricted_food: ['id', 'profile_id', 'user_id', 'food_libarary_id'],
}

const REFERENCE_PROPERTIES = {
  ai_lesson_progress: {
    lesson_id: 'ai_teaching_lesson',
    profile_id: 'profile',
  },
  auth_refresh_token: {
    user_id: 'users',
  },
  favorite_food: {
    food_libarary_id: 'food_library',
    profile_id: 'profile',
    user_id: 'users',
  },
  food_library_ingredients: {
    food_ing_id: 'food_ingredients',
    food_lib_id: 'food_library',
  },
  food_nutrition_summary: {
    food_id: 'food_library',
  },
  food_recommendation: {
    food_id: 'food_library',
  },
  handbook_comments: {
    parent_id: 'handbook_comments',
    post_id: 'handbook_posts',
    user_id: 'users',
  },
  ingredient_nutrition: {
    food_ingredient_id: 'food_ingredients',
  },
  profile: {
    user_id: 'users',
  },
  restricted_food: {
    food_libarary_id: 'food_library',
    profile_id: 'profile',
    user_id: 'users',
  },
  type_value: {
    type_code_id: 'type_code',
  },
  users: {
    roles_id: 'roles',
  },
  vaccine_pricing: {
    center_id: 'vaccination_center',
    vaccine_type_id: 'vaccine_type',
  },
  vaccine_record: {
    profile_id: 'profile',
    vaccine_type_id: 'vaccine_type',
  },
  vaccine_schedule: {
    vaccine_type_id: 'vaccine_type',
  },
  vaccine_type: {
    created_by: 'users',
    updated_by: 'users',
  },
}

export const ADMIN_SECTIONS = [
  nguoiDungSection,
  danhMucSection,
  dinhDuongSection,
  tiemChungSection,
  aiSection,
  camNangSection,
]

export function buildResources(db, existingTableNames, components = {}) {
  return ADMIN_SECTIONS.flatMap((section) =>
    section.tables
      .filter((tableName) => existingTableNames.has(tableName))
      .map((tableName) => ({
        resource: db.table(tableName),
        options: resourceOptions(section, tableName, components),
      })),
  )
}

function resourceOptions(section, tableName, components) {
  const hiddenProperties = Object.fromEntries(
    HIDDEN_PROPERTIES.map((propertyName) => [
      propertyName,
      { isVisible: { list: false, filter: false, show: false, edit: false } },
    ]),
  )

  const options = {
    navigation: section.navigation,
    titleProperty: titlePropertyFor(tableName),
    properties: {
      ...hiddenProperties,
      ...propertiesFor(tableName, components),
    },
    actions: actionsFor(tableName, components),
  }

  if (FOOD_LIST_PROPERTIES[tableName]) {
    options.listProperties = FOOD_LIST_PROPERTIES[tableName]
  }

  return options
}

function titlePropertyFor(tableName) {
  return TITLE_PROPERTIES[tableName]
}

function propertiesFor(tableName, components) {
  const referenceProperties = referencePropertiesFor(tableName)
  const foodProperties = mergePropertyOptions(referenceProperties, FOOD_PROPERTY_OPTIONS[tableName])
  if (tableName === 'food_library') {
    foodProperties.function_code = {
      ...foodProperties.function_code,
      availableValues: FOOD_FUNCTION_CODE_OPTIONS,
    }
    foodProperties.advance_for = {
      ...foodProperties.advance_for,
      availableValues: FOOD_ADVICE_FOR_OPTIONS,
    }
  }

  if (tableName === 'food_recommendation') {
    return {
      ...foodProperties,
      good_points: {
        isVisible: { list: false, filter: false, show: true, edit: true },
      },
      bad_points: {
        isVisible: { list: false, filter: false, show: true, edit: true },
      },
      advice: {
        isVisible: { list: false, filter: false, show: true, edit: true },
      },
      cooking_way: {
        isVisible: { list: false, filter: false, show: true, edit: true },
      },
    }
  }

  if (tableName !== 'profile') {
    return foodProperties
  }

  const segmentComponent = components.ProfileSegmentProperty

  return {
    ...referenceProperties,
    user_id: {
      ...referenceProperties.user_id,
      position: 20,
    },
    profile_code: {
      isVisible: { list: true, filter: true, show: true, edit: false },
    },
    profile_type: {
      position: 40,
      availableValues: PROFILE_TYPE_OPTIONS,
      custom: {
        typeCode: 'PROFILE_TYPE',
      },
      components: segmentComponent ? { edit: segmentComponent } : undefined,
    },
    sex: {
      position: 50,
      availableValues: SEX_OPTIONS,
      custom: {
        typeCode: 'SEX',
      },
      components: segmentComponent ? { edit: segmentComponent } : undefined,
    },
  }
}

function referencePropertiesFor(tableName) {
  return Object.fromEntries(
    Object.entries(REFERENCE_PROPERTIES[tableName] || {}).map(([propertyName, reference]) => [
      propertyName,
      { reference },
    ]),
  )
}

function mergePropertyOptions(baseProperties, overrideProperties = {}) {
  const propertyNames = new Set([...Object.keys(baseProperties), ...Object.keys(overrideProperties)])
  return Object.fromEntries(
    [...propertyNames].map((propertyName) => [
      propertyName,
      {
        ...(baseProperties[propertyName] || {}),
        ...(overrideProperties[propertyName] || {}),
      },
    ]),
  )
}

function actionsFor(tableName, components) {
  const actions = {
    edit: stayOnEditPageAfterUpdateAction(),
  }

  if (tableName === 'food_library') {
    return {
      ...actions,
      ...foodLibraryFullFormActions(components.FoodLibraryForm, components.connectionString),
    }
  }

  if (tableName === 'profile') {
    actions.new = {
      before: async (request) => {
        if (request.method === 'post') {
          request.payload = {
            ...request.payload,
            profile_code: request.payload?.profile_code || generateProfileCode(),
          }
        }

        return request
      },
    }
  }

  return actions
}

function stayOnEditPageAfterUpdateAction() {
  return {
    after: async (response, request, context) => {
      if (request.method !== 'post' || response.notice?.type !== 'success' || !response.redirectUrl) {
        return response
      }

      const resourceId = context.resource?._decorated?.id?.() || context.resource?.id?.() || request.params?.resourceId
      const recordId = context.record?.id?.() || response.record?.id || request.params?.recordId

      if (!resourceId || !recordId) {
        return response
      }

      const returnTo = safeReturnTo(request.query?.returnTo, resourceId)
      const search = returnTo ? `?returnTo=${encodeURIComponent(returnTo)}` : undefined

      return {
        ...response,
        redirectUrl: context.h.recordActionUrl({
          resourceId,
          recordId,
          actionName: 'edit',
          search,
        }),
      }
    },
  }
}

function generateProfileCode() {
  const timestamp = Date.now().toString(36).toUpperCase()
  const random = Math.random().toString(36).slice(2, 8).toUpperCase()
  return `PF-${timestamp}-${random}`
}

function safeReturnTo(returnTo, resourceId) {
  if (typeof returnTo !== 'string' || !returnTo.startsWith('/')) {
    return null
  }

  const resourcePath = `/admin/resources/${encodeURIComponent(resourceId)}`
  if (returnTo === resourcePath || returnTo.startsWith(`${resourcePath}?`)) {
    return returnTo
  }

  return null
}
