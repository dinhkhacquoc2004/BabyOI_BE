import React from 'react'

const styles = {
  group: {
    marginBottom: 28,
  },
  label: {
    display: 'block',
    marginBottom: 10,
    color: '#334650',
    fontSize: 13,
    fontWeight: 900,
  },
  required: {
    color: '#ff8fab',
    marginRight: 4,
  },
  track: {
    display: 'inline-flex',
    gap: 6,
    padding: 6,
    borderRadius: 999,
    border: '1px solid #f0d7df',
    background: '#fff7fa',
    boxShadow: 'inset 0 1px 0 rgba(255, 255, 255, 0.8)',
  },
  option: {
    minWidth: 104,
    height: 38,
    padding: '0 18px',
    border: 0,
    borderRadius: 999,
    background: 'transparent',
    color: '#5c7a8a',
    cursor: 'pointer',
    fontSize: 14,
    fontWeight: 900,
    transition: 'background 160ms ease, color 160ms ease, box-shadow 160ms ease',
  },
  selected: {
    background: '#ff8fab',
    color: '#ffffff',
    boxShadow: '0 10px 22px rgba(255, 143, 171, 0.28)',
  },
  error: {
    marginTop: 8,
    color: '#d64545',
    fontSize: 13,
    fontWeight: 800,
  },
}

const ProfileSegmentProperty = ({ property, record, onChange }) => {
  const value = record?.params?.[property.path] ?? ''
  const error = record?.errors?.[property.path]
  const options = property.availableValues || property.custom?.options || []

  return React.createElement(
    'div',
    { style: styles.group },
    React.createElement(
      'label',
      { style: styles.label },
      property.isRequired ? React.createElement('span', { style: styles.required }, '*') : null,
      property.label,
    ),
    React.createElement(
      'div',
      { style: styles.track, role: 'radiogroup', 'aria-label': property.label },
      options.map((option) => {
        const selected = String(value) === String(option.value)

        return React.createElement(
          'button',
          {
            key: option.value,
            type: 'button',
            role: 'radio',
            'aria-checked': selected,
            style: {
              ...styles.option,
              ...(selected ? styles.selected : {}),
            },
            onClick: () => onChange(property.path, option.value),
          },
          option.label || option.value,
        )
      }),
    ),
    error?.message ? React.createElement('div', { style: styles.error }, error.message) : null,
  )
}

export default ProfileSegmentProperty
