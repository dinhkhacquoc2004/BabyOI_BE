import pg from 'pg'

const DEFAULT_UNIT = 'g'
const DEFAULT_STATUS = 1
const SUMMARY_UNITS = {
  total_calories_unit: 'kcal',
  total_protein_unit: 'g',
  total_carbs_unit: 'g',
  total_fat_unit: 'g',
  total_fiber_unit: 'g',
  total_sugar_unit: 'g',
  total_sodium_unit: 'mg',
}

export function foodLibraryFullFormActions(component, connectionString) {
  if (!component || !connectionString) {
    return {}
  }

  return {
    new: {
      component,
      handler: foodLibraryNewHandler(connectionString),
    },
    edit: {
      component,
      handler: foodLibraryEditHandler(connectionString),
    },
  }
}

function foodLibraryNewHandler(connectionString) {
  return async (request, response, context) => {
    if (request.method === 'get') {
      return {
        data: {
          ingredients: await loadIngredients(connectionString),
          foodFunctionCodes: await loadTypeValues(connectionString, 'FOOD_FUNCTION_CODE', 'value_number'),
          adviceTargets: await loadTypeValues(connectionString, 'FOOD_ADVICE_FOR', 'value_text'),
        },
      }
    }

    const payload = parsePayload(request.payload)
    const result = await saveFoodLibrary(connectionString, payload, context)

    return {
      redirectUrl: context.h.recordActionUrl({
        resourceId: 'food_library',
        recordId: String(result.foodId),
        actionName: 'edit',
      }),
      notice: {
        message: 'Tạo món ăn thành công.',
        type: 'success',
      },
      record: result.record,
    }
  }
}

function foodLibraryEditHandler(connectionString) {
  return async (request, response, context) => {
    if (request.method === 'get') {
      return {
        record: context.record.toJSON(context.currentAdmin),
        data: {
          ingredients: await loadIngredients(connectionString),
          foodFunctionCodes: await loadTypeValues(connectionString, 'FOOD_FUNCTION_CODE', 'value_number'),
          adviceTargets: await loadTypeValues(connectionString, 'FOOD_ADVICE_FOR', 'value_text'),
          food: await loadFood(connectionString, request.params.recordId),
        },
      }
    }

    const payload = parsePayload(request.payload)
    const result = await saveFoodLibrary(connectionString, payload, context, request.params.recordId)
    const returnTo = safeReturnTo(request.query?.returnTo)
    const search = returnTo ? `?returnTo=${encodeURIComponent(returnTo)}` : undefined

    return {
      redirectUrl: context.h.recordActionUrl({
        resourceId: 'food_library',
        recordId: String(result.foodId),
        actionName: 'edit',
        search,
      }),
      notice: {
        message: 'Cập nhật món ăn thành công.',
        type: 'success',
      },
      record: result.record,
    }
  }
}

async function loadIngredients(connectionString) {
  const client = new pg.Client({ connectionString })
  await client.connect()
  try {
    const result = await client.query(`
      SELECT
        fi.id,
        fi.name_ingredients,
        COALESCE(inu.base_amount, 100) AS base_amount,
        COALESCE(inu.base_unit, $1) AS base_unit,
        COALESCE(inu.calories, 0) AS calories,
        COALESCE(inu.protein, 0) AS protein,
        COALESCE(inu.carbs, 0) AS carbs,
        COALESCE(inu.fat, 0) AS fat,
        COALESCE(inu.fiber, 0) AS fiber,
        COALESCE(inu.sugar, 0) AS sugar,
        COALESCE(inu.sodium, 0) AS sodium
      FROM food_ingredients fi
      LEFT JOIN ingredient_nutrition inu ON inu.food_ingredient_id = fi.id
      ORDER BY fi.name_ingredients ASC
    `, [DEFAULT_UNIT])

    return result.rows.map((row) => ({
      id: Number(row.id),
      name: row.name_ingredients,
      baseAmount: toNumber(row.base_amount, 100),
      baseUnit: row.base_unit || DEFAULT_UNIT,
      calories: toNumber(row.calories),
      protein: toNumber(row.protein),
      carbs: toNumber(row.carbs),
      fat: toNumber(row.fat),
      fiber: toNumber(row.fiber),
      sugar: toNumber(row.sugar),
      sodium: toNumber(row.sodium),
    }))
  } finally {
    await client.end()
  }
}

async function loadTypeValues(connectionString, typeCode, valueColumn) {
  const client = new pg.Client({ connectionString })
  await client.connect()
  try {
    const result = await client.query(`
      SELECT
        tv.value_code,
        tv.value_name,
        tv.value_text,
        tv.value_number
      FROM type_value tv
      JOIN type_code tc ON tc.id = tv.type_code_id
      WHERE tc.code = $1
        AND tv.status <> -4
      ORDER BY tv.sort_order NULLS LAST, tv.value_name ASC
    `, [typeCode])

    return result.rows
      .map((row) => ({
        value: valueColumn === 'value_number' ? nullableNumber(row.value_number) : row.value_text,
        label: row.value_name || row.value_code,
      }))
      .filter((option) => option.value !== null && option.value !== undefined && option.value !== '')
  } finally {
    await client.end()
  }
}

async function loadFood(connectionString, foodId) {
  const client = new pg.Client({ connectionString })
  await client.connect()
  try {
    const foodResult = await client.query(`
      SELECT id, function_code, name, advance_for, image_url, status
      FROM food_library
      WHERE id = $1
    `, [foodId])
    const ingredientResult = await client.query(`
      SELECT food_ing_id, amount_per_serving, unit, status, description
      FROM food_library_ingredients
      WHERE food_lib_id = $1
      ORDER BY id ASC
    `, [foodId])

    const food = foodResult.rows[0]
    if (!food) {
      return null
    }

    return {
      id: Number(food.id),
      functionCode: food.function_code ?? '',
      name: food.name ?? '',
      advanceFor: food.advance_for ?? '',
      imageUrl: food.image_url ?? '',
      status: food.status ?? DEFAULT_STATUS,
      ingredients: ingredientResult.rows.map((row) => ({
        ingredientId: Number(row.food_ing_id),
        amount: toNumber(row.amount_per_serving),
        unit: row.unit || DEFAULT_UNIT,
        status: row.status ?? DEFAULT_STATUS,
        description: row.description || '',
      })),
    }
  } finally {
    await client.end()
  }
}

async function saveFoodLibrary(connectionString, payload, context, foodId = null) {
  validatePayload(payload)

  const client = new pg.Client({ connectionString })
  await client.connect()
  try {
    await client.query('BEGIN')
    const actor = context.currentAdmin?.email || 'admin'
    const savedFoodId = foodId
      ? await updateFood(client, foodId, payload, actor)
      : await insertFood(client, payload, actor)

    await client.query('DELETE FROM food_library_ingredients WHERE food_lib_id = $1', [savedFoodId])
    for (const ingredient of payload.ingredients) {
      await client.query(`
        INSERT INTO food_library_ingredients
          (food_lib_id, food_ing_id, amount_per_serving, unit, status, description)
        VALUES ($1, $2, $3, $4, $5, $6)
      `, [
        savedFoodId,
        ingredient.ingredientId,
        ingredient.amount,
        ingredient.unit || DEFAULT_UNIT,
        ingredient.status ?? DEFAULT_STATUS,
        ingredient.description || null,
      ])
    }

    const summary = await calculateSummary(client, payload.ingredients)
    await client.query('DELETE FROM food_nutrition_summary WHERE food_id = $1', [savedFoodId])
    await client.query(`
      INSERT INTO food_nutrition_summary (
        food_id,
        total_calories, total_calories_unit,
        total_protein, total_protein_unit,
        total_carbs, total_carbs_unit,
        total_fat, total_fat_unit,
        total_fiber, total_fiber_unit,
        total_sugar, total_sugar_unit,
        total_sodium, total_sodium_unit
      )
      VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15)
    `, [
      savedFoodId,
      summary.total_calories,
      SUMMARY_UNITS.total_calories_unit,
      summary.total_protein,
      SUMMARY_UNITS.total_protein_unit,
      summary.total_carbs,
      SUMMARY_UNITS.total_carbs_unit,
      summary.total_fat,
      SUMMARY_UNITS.total_fat_unit,
      summary.total_fiber,
      SUMMARY_UNITS.total_fiber_unit,
      summary.total_sugar,
      SUMMARY_UNITS.total_sugar_unit,
      summary.total_sodium,
      SUMMARY_UNITS.total_sodium_unit,
    ])

    await client.query('COMMIT')
    const record = await context.resource.findOne(String(savedFoodId), context)

    return {
      foodId: savedFoodId,
      record: record.toJSON(context.currentAdmin),
    }
  } catch (error) {
    await client.query('ROLLBACK')
    throw error
  } finally {
    await client.end()
  }
}

async function insertFood(client, payload, actor) {
  const result = await client.query(`
    INSERT INTO food_library
      (function_code, name, advance_for, image_url, created_at, updated_at, created_by, updated_by, status)
    VALUES ($1, $2, $3, $4, NOW(), NOW(), $5, $5, $6)
    RETURNING id
  `, [
    nullableNumber(payload.functionCode),
    payload.name,
    payload.advanceFor || null,
    payload.imageUrl || null,
    actor,
    payload.status ?? DEFAULT_STATUS,
  ])

  return Number(result.rows[0].id)
}

async function updateFood(client, foodId, payload, actor) {
  await client.query(`
    UPDATE food_library
    SET function_code = $1,
        name = $2,
        advance_for = $3,
        image_url = $4,
        updated_at = NOW(),
        updated_by = $5,
        status = $6
    WHERE id = $7
  `, [
    nullableNumber(payload.functionCode),
    payload.name,
    payload.advanceFor || null,
    payload.imageUrl || null,
    actor,
    payload.status ?? DEFAULT_STATUS,
    foodId,
  ])

  return Number(foodId)
}

async function calculateSummary(client, ingredients) {
  const ids = ingredients.map((ingredient) => ingredient.ingredientId)
  const result = await client.query(`
    SELECT
      food_ingredient_id,
      COALESCE(NULLIF(base_amount, 0), 100) AS base_amount,
      COALESCE(calories, 0) AS calories,
      COALESCE(protein, 0) AS protein,
      COALESCE(carbs, 0) AS carbs,
      COALESCE(fat, 0) AS fat,
      COALESCE(fiber, 0) AS fiber,
      COALESCE(sugar, 0) AS sugar,
      COALESCE(sodium, 0) AS sodium
    FROM ingredient_nutrition
    WHERE food_ingredient_id = ANY($1::bigint[])
  `, [ids])
  const nutritionByIngredient = new Map(result.rows.map((row) => [Number(row.food_ingredient_id), row]))

  return roundSummary(ingredients.reduce((summary, ingredient) => {
    const nutrition = nutritionByIngredient.get(ingredient.ingredientId)
    if (!nutrition) {
      return summary
    }

    const ratio = ingredient.amount / toNumber(nutrition.base_amount, 100)
    summary.total_calories += toNumber(nutrition.calories) * ratio
    summary.total_protein += toNumber(nutrition.protein) * ratio
    summary.total_carbs += toNumber(nutrition.carbs) * ratio
    summary.total_fat += toNumber(nutrition.fat) * ratio
    summary.total_fiber += toNumber(nutrition.fiber) * ratio
    summary.total_sugar += toNumber(nutrition.sugar) * ratio
    summary.total_sodium += toNumber(nutrition.sodium) * ratio
    return summary
  }, {
    total_calories: 0,
    total_protein: 0,
    total_carbs: 0,
    total_fat: 0,
    total_fiber: 0,
    total_sugar: 0,
    total_sodium: 0,
  }))
}

function parsePayload(payload = {}) {
  const ingredients = Array.isArray(payload.ingredients)
    ? payload.ingredients
    : JSON.parse(payload.ingredients || '[]')

  return {
    name: String(payload.name || '').trim(),
    functionCode: payload.functionCode,
    advanceFor: String(payload.advanceFor || '').trim(),
    imageUrl: String(payload.imageUrl || '').trim(),
    status: nullableNumber(payload.status) ?? DEFAULT_STATUS,
    ingredients: ingredients
      .map((ingredient) => ({
        ingredientId: nullableNumber(ingredient.ingredientId),
        amount: toNumber(ingredient.amount),
        unit: String(ingredient.unit || DEFAULT_UNIT).trim(),
        status: nullableNumber(ingredient.status) ?? DEFAULT_STATUS,
        description: String(ingredient.description || '').trim(),
      }))
      .filter((ingredient) => ingredient.ingredientId && ingredient.amount > 0),
  }
}

function validatePayload(payload) {
  if (!payload.name) {
    throw new Error('Tên món ăn là bắt buộc.')
  }

  if (!payload.ingredients.length) {
    throw new Error('Cần chọn ít nhất một nguyên liệu.')
  }
}

function safeReturnTo(returnTo) {
  if (typeof returnTo !== 'string' || !returnTo.startsWith('/admin/resources/food_library')) {
    return null
  }

  return returnTo
}

function nullableNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function toNumber(value, fallback = 0) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function roundSummary(summary) {
  return Object.fromEntries(
    Object.entries(summary).map(([key, value]) => [key, Math.round(value * 100) / 100]),
  )
}
