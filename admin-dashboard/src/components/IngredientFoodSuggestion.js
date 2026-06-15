import React, { useEffect, useMemo, useState } from 'react'
import { ApiClient } from 'adminjs'

const api = new ApiClient()

const styles = {
  page: {
    minHeight: 'calc(100vh - 64px)',
    background: '#fef9f3',
    padding: '28px clamp(16px, 4vw, 48px)',
    color: '#334650',
    fontFamily: '"Segoe UI", system-ui, -apple-system, BlinkMacSystemFont, sans-serif',
  },
  header: {
    marginBottom: 18,
  },
  eyebrow: {
    margin: 0,
    color: '#ff8fab',
    fontSize: 13,
    fontWeight: 900,
    textTransform: 'uppercase',
  },
  title: {
    margin: '6px 0 0',
    color: '#334650',
    fontSize: 30,
    lineHeight: 1.2,
    fontWeight: 900,
  },
  panel: {
    background: '#ffffff',
    border: '1px solid #f0d7df',
    borderRadius: 8,
    padding: 20,
    boxShadow: '0 10px 28px rgba(255, 182, 193, 0.14)',
  },
  formGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
    gap: 18,
    alignItems: 'start',
  },
  field: {
    marginBottom: 14,
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
    borderRadius: 8,
    padding: '0 12px',
    color: '#334650',
    fontSize: 14,
    fontWeight: 700,
    boxSizing: 'border-box',
    background: '#ffffff',
  },
  textarea: {
    width: '100%',
    minHeight: 104,
    border: '1px solid #f0d7df',
    borderRadius: 8,
    padding: 12,
    color: '#334650',
    fontSize: 14,
    fontWeight: 700,
    boxSizing: 'border-box',
    background: '#ffffff',
    resize: 'vertical',
  },
  captureBox: {
    border: '1px dashed #ff8fab',
    borderRadius: 8,
    background: '#fff7fa',
    padding: 14,
  },
  preview: {
    width: '100%',
    maxHeight: 280,
    objectFit: 'cover',
    borderRadius: 8,
    border: '1px solid #f0d7df',
    marginTop: 12,
  },
  buttonRow: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 10,
    marginTop: 14,
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
  message: {
    margin: '14px 0 0',
    padding: '12px 14px',
    borderRadius: 8,
    fontWeight: 800,
  },
  error: {
    background: '#ffebee',
    color: '#d64545',
    border: '1px solid #ffcdd2',
  },
  warning: {
    background: '#fff4d6',
    color: '#8a5a00',
    border: '1px solid #ffe0a3',
  },
  resultGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
    gap: 14,
    marginTop: 18,
  },
  card: {
    background: '#ffffff',
    border: '1px solid #f0d7df',
    borderRadius: 8,
    overflow: 'hidden',
    boxShadow: '0 8px 20px rgba(255, 182, 193, 0.12)',
  },
  foodImage: {
    width: '100%',
    height: 150,
    objectFit: 'cover',
    background: '#fff7fa',
  },
  cardBody: {
    padding: 14,
  },
  cardTitle: {
    margin: 0,
    color: '#334650',
    fontSize: 18,
    fontWeight: 900,
  },
  score: {
    margin: '8px 0',
    color: '#ff8fab',
    fontSize: 13,
    fontWeight: 900,
  },
  text: {
    margin: '8px 0 0',
    color: '#5c7a8a',
    fontSize: 13,
    lineHeight: 1.5,
    fontWeight: 700,
  },
  tagList: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 6,
    marginTop: 10,
  },
  tag: {
    borderRadius: 999,
    background: '#fff1f5',
    color: '#d95076',
    padding: '5px 9px',
    fontSize: 12,
    fontWeight: 900,
  },
  empty: {
    marginTop: 18,
    padding: 18,
    border: '1px solid #f0d7df',
    borderRadius: 8,
    background: '#ffffff',
    color: '#5c7a8a',
    fontWeight: 800,
  },
}

const IngredientFoodSuggestion = (props) => {
  const data = props?.data || {}
  const [apiBaseUrl, setApiBaseUrl] = useState(data.apiBaseUrl || 'http://localhost:8085')
  const [profiles, setProfiles] = useState(data.profiles || [])
  const [profileId, setProfileId] = useState(data.profiles?.[0]?.id ? String(data.profiles[0].id) : '')
  const [ingredientNames, setIngredientNames] = useState('')
  const [limit, setLimit] = useState(5)
  const [imageFile, setImageFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [result, setResult] = useState(null)

  useEffect(() => {
    let mounted = true

    async function loadActionData() {
      if (!props?.resource?.id || !props?.action?.name) {
        return
      }

      try {
        const response = await api.resourceAction({
          resourceId: props.resource.id,
          actionName: props.action.name,
        })
        const actionData = response.data?.data || {}
        if (!mounted) {
          return
        }
        if (actionData.apiBaseUrl) {
          setApiBaseUrl(actionData.apiBaseUrl)
        }
        if (Array.isArray(actionData.profiles)) {
          setProfiles(actionData.profiles)
          setProfileId((current) => current || (actionData.profiles[0]?.id ? String(actionData.profiles[0].id) : ''))
        }
      } catch (error) {
        if (mounted) {
          setMessage({ type: 'warning', text: 'Khong tai duoc danh sach profile, ban co the nhap profileId thu cong.' })
        }
      }
    }

    loadActionData()
    return () => {
      mounted = false
    }
  }, [props?.action?.name, props?.resource?.id])

  useEffect(() => {
    if (!imageFile) {
      setPreviewUrl('')
      return undefined
    }

    const objectUrl = URL.createObjectURL(imageFile)
    setPreviewUrl(objectUrl)
    return () => URL.revokeObjectURL(objectUrl)
  }, [imageFile])

  const detectedNames = useMemo(() => (
    result?.detectedIngredients?.map((item) => item.name).filter(Boolean) || []
  ), [result])

  async function submit(event) {
    event.preventDefault()
    setLoading(true)
    setMessage(null)
    setResult(null)

    try {
      const imageBase64 = imageFile ? await fileToBase64(imageFile) : null

      const response = await fetch(`${apiBaseUrl}/api/foods/suggestions/from-ingredients-json`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          profileId: profileId ? Number(profileId) : null,
          ingredientNames: splitIngredients(ingredientNames),
          imageBase64,
          imageMimeType: imageFile?.type || null,
          imageFilename: imageFile?.name || null,
          limit: Number(limit || 5),
        }),
      })
      const body = await safeJson(response)
      if (!response.ok) {
        throw new Error(body?.detail || body?.message || body?.error || 'Khong phan tich duoc nguyen lieu.')
      }

      setResult(body)
      if (!body?.suggestions?.length) {
        setMessage({ type: 'warning', text: 'Chua tim thay mon phu hop tu nguyen lieu nay.' })
      }
    } catch (error) {
      setMessage({ type: 'error', text: error.message || 'Co loi khi goi y mon.' })
    } finally {
      setLoading(false)
    }
  }

  function resetForm() {
    setImageFile(null)
    setIngredientNames('')
    setResult(null)
    setMessage(null)
  }

  return React.createElement(
    'main',
    { style: styles.page },
    React.createElement(
      'header',
      { style: styles.header },
      React.createElement('p', { style: styles.eyebrow }, 'Dinh duong'),
      React.createElement('h1', { style: styles.title }, 'AI goi y mon tu anh nguyen lieu'),
    ),
    React.createElement(
      'section',
      { style: styles.panel },
      React.createElement(
        'form',
        { onSubmit: submit },
        React.createElement(
          'div',
          { style: styles.formGrid },
          React.createElement(
            'div',
            null,
            field('Ho so', profileSelect(profileId, profiles, setProfileId)),
            field('So mon goi y', input('number', limit, (value) => setLimit(Number(value)), { min: 1, max: 20 })),
            field('Nhap them nguyen lieu', textarea(ingredientNames, setIngredientNames, 'Vi du: trung ga, ca rot, thit ga')),
          ),
          React.createElement(
            'div',
            { style: styles.captureBox },
            field(
              'Chup hoac chon anh nguyen lieu',
              React.createElement('input', {
                type: 'file',
                accept: 'image/*',
                capture: 'environment',
                style: styles.input,
                onChange: (event) => setImageFile(event.target.files?.[0] || null),
              }),
            ),
            previewUrl
              ? React.createElement('img', { src: previewUrl, alt: 'Anh nguyen lieu', style: styles.preview })
              : React.createElement('p', { style: styles.text }, 'Bam vao o tren de mo camera tren dien thoai hoac chon anh co san.'),
          ),
        ),
        React.createElement(
          'div',
          { style: styles.buttonRow },
          React.createElement(
            'button',
            { type: 'submit', style: styles.primaryButton, disabled: loading },
            loading ? 'Dang phan tich...' : 'Phan tich va goi y mon',
          ),
          React.createElement(
            'button',
            { type: 'button', style: styles.secondaryButton, onClick: resetForm, disabled: loading },
            'Lam moi',
          ),
        ),
      ),
      message
        ? React.createElement(
          'div',
          { style: { ...styles.message, ...(message.type === 'error' ? styles.error : styles.warning) } },
          message.text,
        )
        : null,
      result ? resultSummary(result, detectedNames) : null,
    ),
    result?.suggestions?.length
      ? React.createElement(
        'section',
        { style: styles.resultGrid },
        result.suggestions.map((suggestion) => suggestionCard(suggestion)),
      )
      : result
        ? React.createElement('div', { style: styles.empty }, 'Khong co mon nao trong thu vien trung voi nguyen lieu da gui.')
        : null,
  )
}

function field(label, child) {
  return React.createElement(
    'div',
    { style: styles.field },
    React.createElement('label', { style: styles.label }, label),
    child,
  )
}

function input(type, value, onChange, extraProps = {}) {
  return React.createElement('input', {
    type,
    value,
    style: styles.input,
    onChange: (event) => onChange(event.target.value),
    ...extraProps,
  })
}

function textarea(value, onChange, placeholder) {
  return React.createElement('textarea', {
    value,
    placeholder,
    style: styles.textarea,
    onChange: (event) => onChange(event.target.value),
  })
}

function profileSelect(value, profiles, onChange) {
  if (!profiles.length) {
    return input('number', value, onChange, { placeholder: 'Nhap profileId' })
  }

  return React.createElement(
    'select',
    {
      value,
      style: styles.input,
      onChange: (event) => onChange(event.target.value),
    },
    React.createElement('option', { value: '' }, 'Khong chon profile'),
    profiles.map((profile) =>
      React.createElement(
        'option',
        { key: profile.id, value: profile.id },
        `${profile.name || 'Profile'} #${profile.id}${profile.profileType ? ` - ${profile.profileType}` : ''}`,
      ),
    ),
  )
}

function resultSummary(result, detectedNames) {
  const matchedDbNames = result?.matchedDbIngredients?.map((ingredient) => ingredient.ingredientName).filter(Boolean) || []
  return React.createElement(
    'div',
    null,
    detectedNames.length
      ? tagLine('AI nhan dien', detectedNames)
      : null,
    matchedDbNames.length
      ? tagLine('Da match trong DB', matchedDbNames)
      : null,
    result.warnings?.map((warning) =>
      React.createElement('div', { key: warning, style: { ...styles.message, ...styles.warning } }, warning),
    ),
    result.disclaimer
      ? React.createElement('p', { style: styles.text }, result.disclaimer)
      : null,
  )
}

function suggestionCard(suggestion) {
  const food = suggestion.food || {}
  return React.createElement(
    'article',
    { key: food.id || food.name, style: styles.card },
    food.imageUrl
      ? React.createElement('img', { src: food.imageUrl, alt: food.name || 'Mon an', style: styles.foodImage })
      : React.createElement('div', { style: styles.foodImage }),
    React.createElement(
      'div',
      { style: styles.cardBody },
      React.createElement('h2', { style: styles.cardTitle }, food.name || 'Mon an'),
      React.createElement('p', { style: styles.score }, `Do phu hop: ${Math.round(Number(suggestion.matchScore || 0) * 100)}%`),
      tagLine('Trung nguyen lieu', suggestion.matchedIngredients),
      tagLine('Can bo sung', suggestion.missingIngredients),
      React.createElement('p', { style: styles.text }, suggestion.suitabilityNote || ''),
      React.createElement('p', { style: styles.text }, suggestion.nutritionNote || ''),
    ),
  )
}

function tagLine(label, values = []) {
  if (!values.length) {
    return null
  }
  return React.createElement(
    'div',
    null,
    React.createElement('p', { style: styles.text }, label),
    React.createElement(
      'div',
      { style: styles.tagList },
      values.slice(0, 8).map((value) => React.createElement('span', { key: value, style: styles.tag }, value)),
    ),
  )
}

function splitIngredients(value) {
  return String(value || '')
    .split(/[,;\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const value = typeof reader.result === 'string' ? reader.result : ''
      resolve(value.includes(',') ? value.split(',')[1] : value)
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

async function safeJson(response) {
  try {
    return await response.json()
  } catch (error) {
    return null
  }
}

export default IngredientFoodSuggestion
