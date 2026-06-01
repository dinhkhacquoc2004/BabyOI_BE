import React, { useEffect, useMemo, useState } from 'react'
import { ApiClient } from 'adminjs'

const api = new ApiClient()
const emptyIngredient = {
  ingredientId: '',
  amount: '',
  unit: 'g',
  status: 1,
  description: '',
}

const styles = {
  panel: {
    background: '#ffffff',
    border: '1px solid #f0d7df',
    borderRadius: 18,
    padding: 24,
    boxShadow: '0 10px 28px rgba(255, 182, 193, 0.14)',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
    gap: 16,
  },
  group: {
    marginBottom: 16,
  },
  label: {
    display: 'block',
    marginBottom: 8,
    color: '#334650',
    fontSize: 13,
    fontWeight: 900,
  },
  input: {
    width: '100%',
    minHeight: 42,
    border: '1px solid #f0d7df',
    borderRadius: 14,
    padding: '0 12px',
    color: '#334650',
    fontSize: 14,
    fontWeight: 700,
    boxSizing: 'border-box',
    background: '#ffffff',
  },
  textarea: {
    width: '100%',
    minHeight: 76,
    border: '1px solid #f0d7df',
    borderRadius: 14,
    padding: 12,
    color: '#334650',
    fontSize: 14,
    fontWeight: 700,
    boxSizing: 'border-box',
    background: '#ffffff',
    resize: 'vertical',
  },
  sectionTitle: {
    margin: '22px 0 12px',
    color: '#334650',
    fontSize: 18,
    fontWeight: 900,
  },
  row: {
    display: 'grid',
    gridTemplateColumns: 'minmax(220px, 2fr) 120px 90px minmax(180px, 1fr) 44px',
    gap: 10,
    alignItems: 'start',
    marginBottom: 10,
  },
  buttonRow: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 10,
    marginTop: 18,
  },
  primaryButton: {
    minHeight: 42,
    border: '1px solid #ff8fab',
    borderRadius: 999,
    padding: '0 20px',
    background: '#ff8fab',
    color: '#ffffff',
    fontWeight: 900,
    cursor: 'pointer',
  },
  secondaryButton: {
    minHeight: 42,
    border: '1px solid #f0d7df',
    borderRadius: 999,
    padding: '0 18px',
    background: '#ffffff',
    color: '#ff8fab',
    fontWeight: 900,
    cursor: 'pointer',
  },
  iconButton: {
    width: 42,
    height: 42,
    border: '1px solid #f0d7df',
    borderRadius: 14,
    background: '#ffffff',
    color: '#d64545',
    fontWeight: 900,
    cursor: 'pointer',
  },
  summary: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
    gap: 10,
  },
  summaryItem: {
    border: '1px solid #f0d7df',
    borderRadius: 14,
    padding: 12,
    background: '#fff7fa',
  },
  summaryLabel: {
    margin: 0,
    color: '#7c91a0',
    fontSize: 12,
    fontWeight: 800,
  },
  summaryValue: {
    margin: '6px 0 0',
    color: '#334650',
    fontSize: 16,
    fontWeight: 900,
  },
  message: {
    margin: '0 0 16px',
    padding: '12px 14px',
    borderRadius: 14,
    fontWeight: 800,
  },
  error: {
    background: '#ffebee',
    color: '#d64545',
    border: '1px solid #ffcdd2',
  },
  success: {
    background: '#e8f9ed',
    color: '#2e7d32',
    border: '1px solid #c8f0d1',
  },
}

const FoodLibraryForm = ({ action, resource, record }) => {
  const isEdit = action.name === 'edit'
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState(null)
  const [ingredients, setIngredients] = useState([])
  const [foodFunctionCodes, setFoodFunctionCodes] = useState([])
  const [adviceTargets, setAdviceTargets] = useState([])
  const [form, setForm] = useState({
    name: '',
    functionCode: '',
    advanceFor: '',
    imageUrl: '',
    status: 1,
    ingredients: [{ ...emptyIngredient }],
  })

  useEffect(() => {
    let mounted = true

    async function load() {
      try {
        const response = isEdit
          ? await api.recordAction({
            resourceId: resource.id,
            recordId: record.id,
            actionName: action.name,
          })
          : await api.resourceAction({
            resourceId: resource.id,
            actionName: action.name,
          })

        if (!mounted) {
          return
        }

        const data = response.data.data || {}
        setIngredients(data.ingredients || [])
        setFoodFunctionCodes(data.foodFunctionCodes || [])
        setAdviceTargets(data.adviceTargets || [])

        if (data.food) {
          setForm({
            name: data.food.name || '',
            functionCode: data.food.functionCode || '',
            advanceFor: data.food.advanceFor || '',
            imageUrl: data.food.imageUrl || '',
            status: data.food.status ?? 1,
            ingredients: data.food.ingredients?.length ? data.food.ingredients : [{ ...emptyIngredient }],
          })
        }
      } catch (error) {
        setMessage({ type: 'error', text: error.response?.data?.message || error.message || 'Không tải được dữ liệu.' })
      } finally {
        if (mounted) {
          setLoading(false)
        }
      }
    }

    load()
    return () => {
      mounted = false
    }
  }, [action.name, isEdit, record?.id, resource.id])

  const summary = useMemo(() => calculateSummary(form.ingredients, ingredients), [form.ingredients, ingredients])

  function changeField(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function changeIngredient(index, field, value) {
    setForm((current) => ({
      ...current,
      ingredients: current.ingredients.map((ingredient, ingredientIndex) =>
        ingredientIndex === index ? { ...ingredient, [field]: value } : ingredient,
      ),
    }))
  }

  function addIngredient() {
    setForm((current) => ({
      ...current,
      ingredients: [...current.ingredients, { ...emptyIngredient }],
    }))
  }

  function removeIngredient(index) {
    setForm((current) => ({
      ...current,
      ingredients: current.ingredients.filter((_, ingredientIndex) => ingredientIndex !== index),
    }))
  }

  async function submit(event) {
    event.preventDefault()
    setSaving(true)
    setMessage(null)

    try {
      const payload = {
        ...form,
        ingredients: form.ingredients.filter((ingredient) => ingredient.ingredientId && Number(ingredient.amount) > 0),
      }
      const response = isEdit
        ? await api.recordAction({
          resourceId: resource.id,
          recordId: record.id,
          actionName: action.name,
          data: payload,
          params: new URLSearchParams(window.location.search),
        })
        : await api.resourceAction({
          resourceId: resource.id,
          actionName: action.name,
          data: payload,
        })

      setMessage({ type: 'success', text: response.data.notice?.message || 'Lưu món ăn thành công.' })
      if (response.data.redirectUrl) {
        if (isEdit) {
          window.history.replaceState(window.history.state, '', response.data.redirectUrl)
        } else {
          window.location.assign(response.data.redirectUrl)
        }
      }
    } catch (error) {
      setMessage({ type: 'error', text: error.response?.data?.message || error.message || 'Không lưu được món ăn.' })
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return React.createElement('div', { style: styles.panel }, 'Đang tải dữ liệu...')
  }

  return React.createElement(
    'form',
    { style: styles.panel, onSubmit: submit },
    message
      ? React.createElement(
        'div',
        { style: { ...styles.message, ...(message.type === 'error' ? styles.error : styles.success) } },
        message.text,
      )
      : null,
    React.createElement(
      'div',
      { style: styles.grid },
      field('Tên món ăn', input('name', form.name, (value) => changeField('name', value), true)),
      field('Function Code', selectOption(form.functionCode, foodFunctionCodes, (value) => changeField('functionCode', value), true)),
      field('Advance For', selectOption(form.advanceFor, adviceTargets, (value) => changeField('advanceFor', value))),
      field('Status', input('status', form.status, (value) => changeField('status', value), false, 'number')),
    ),
    field('Image URL', input('imageUrl', form.imageUrl, (value) => changeField('imageUrl', value))),
    React.createElement('h3', { style: styles.sectionTitle }, 'Nguyên liệu'),
    form.ingredients.map((ingredient, index) =>
      React.createElement(
        'div',
        { key: index, style: styles.row },
        selectIngredient(ingredient.ingredientId, ingredients, (value) => changeIngredient(index, 'ingredientId', value)),
        input('amount', ingredient.amount, (value) => changeIngredient(index, 'amount', value), true, 'number', 'Khối lượng'),
        input('unit', ingredient.unit, (value) => changeIngredient(index, 'unit', value), false, 'text', 'Đơn vị'),
        textarea(ingredient.description, (value) => changeIngredient(index, 'description', value), 'Mô tả'),
        React.createElement(
          'button',
          {
            type: 'button',
            style: styles.iconButton,
            onClick: () => removeIngredient(index),
            disabled: form.ingredients.length === 1,
            title: 'Xóa nguyên liệu',
          },
          'x',
        ),
      ),
    ),
    React.createElement(
      'button',
      { type: 'button', style: styles.secondaryButton, onClick: addIngredient },
      '+ Thêm nguyên liệu',
    ),
    React.createElement('h3', { style: styles.sectionTitle }, 'Dinh dưỡng tự tính'),
    React.createElement(
      'div',
      { style: styles.summary },
      summaryItem('Calories', `${summary.calories} kcal`),
      summaryItem('Protein', `${summary.protein} g`),
      summaryItem('Carbs', `${summary.carbs} g`),
      summaryItem('Fat', `${summary.fat} g`),
      summaryItem('Fiber', `${summary.fiber} g`),
      summaryItem('Sugar', `${summary.sugar} g`),
      summaryItem('Sodium', `${summary.sodium} mg`),
    ),
    React.createElement(
      'div',
      { style: styles.buttonRow },
      React.createElement(
        'button',
        { type: 'submit', style: styles.primaryButton, disabled: saving },
        saving ? 'Đang lưu...' : 'Lưu món ăn',
      ),
    ),
  )
}

function field(label, child) {
  return React.createElement(
    'div',
    { style: styles.group },
    React.createElement('label', { style: styles.label }, label),
    child,
  )
}

function input(name, value, onChange, required = false, type = 'text', placeholder = '') {
  return React.createElement('input', {
    name,
    value,
    required,
    type,
    placeholder,
    style: styles.input,
    onChange: (event) => onChange(event.target.value),
  })
}

function textarea(value, onChange, placeholder = '') {
  return React.createElement('textarea', {
    value,
    placeholder,
    style: styles.textarea,
    onChange: (event) => onChange(event.target.value),
  })
}

function selectOption(value, options, onChange, required = false) {
  return React.createElement(
    'select',
    {
      value,
      required,
      style: styles.input,
      onChange: (event) => onChange(event.target.value),
    },
    React.createElement('option', { value: '' }, 'Chá»n giÃ¡ trá»‹'),
    options.map((option) =>
      React.createElement(
        'option',
        { key: option.value, value: option.value },
        option.label,
      ),
    ),
  )
}

function selectIngredient(value, ingredients, onChange) {
  return React.createElement(
    'select',
    {
      value,
      required: true,
      style: styles.input,
      onChange: (event) => onChange(event.target.value),
    },
    React.createElement('option', { value: '' }, 'Chọn nguyên liệu'),
    ingredients.map((ingredient) =>
      React.createElement(
        'option',
        { key: ingredient.id, value: ingredient.id },
        `${ingredient.name} (${ingredient.baseAmount}${ingredient.baseUnit})`,
      ),
    ),
  )
}

function summaryItem(label, value) {
  return React.createElement(
    'div',
    { style: styles.summaryItem },
    React.createElement('p', { style: styles.summaryLabel }, label),
    React.createElement('p', { style: styles.summaryValue }, value),
  )
}

function calculateSummary(selectedIngredients, ingredientCatalog) {
  const byId = new Map(ingredientCatalog.map((ingredient) => [String(ingredient.id), ingredient]))
  const summary = selectedIngredients.reduce((current, selected) => {
    const ingredient = byId.get(String(selected.ingredientId))
    const amount = Number(selected.amount)
    if (!ingredient || !Number.isFinite(amount) || amount <= 0) {
      return current
    }

    const ratio = amount / Number(ingredient.baseAmount || 100)
    current.calories += Number(ingredient.calories || 0) * ratio
    current.protein += Number(ingredient.protein || 0) * ratio
    current.carbs += Number(ingredient.carbs || 0) * ratio
    current.fat += Number(ingredient.fat || 0) * ratio
    current.fiber += Number(ingredient.fiber || 0) * ratio
    current.sugar += Number(ingredient.sugar || 0) * ratio
    current.sodium += Number(ingredient.sodium || 0) * ratio
    return current
  }, {
    calories: 0,
    protein: 0,
    carbs: 0,
    fat: 0,
    fiber: 0,
    sugar: 0,
    sodium: 0,
  })

  return Object.fromEntries(
    Object.entries(summary).map(([key, value]) => [key, Math.round(value * 100) / 100]),
  )
}

export default FoodLibraryForm
