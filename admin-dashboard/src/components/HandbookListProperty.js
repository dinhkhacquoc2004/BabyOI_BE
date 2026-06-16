import React from 'react'

const CATEGORY_STYLES = {
  'Dinh dưỡng': { background: '#fff3d8', color: '#9a5b00', border: '#ffd58a' },
  'Sức khỏe': { background: '#e8f8ee', color: '#247344', border: '#b7e7c8' },
  'Giáo dục': { background: '#eaf1ff', color: '#2859b7', border: '#c5d7ff' },
  'Tiêm chủng': { background: '#e7f8f8', color: '#177276', border: '#b7e7e9' },
  'Quảng cáo': { background: '#fff0f5', color: '#b9285d', border: '#ffc6da' },
}

const STATUS_STYLES = {
  '-4': { label: 'DELETED', background: '#f5f5f5', color: '#777777', border: '#dddddd' },
  2: { label: 'ACTIVE', background: '#e8f8ee', color: '#247344', border: '#b7e7c8' },
}

const styles = {
  cell: {
    maxWidth: 260,
    overflow: 'hidden',
  },
  text: {
    display: '-webkit-box',
    WebkitLineClamp: 2,
    WebkitBoxOrient: 'vertical',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'normal',
    lineHeight: '18px',
    maxHeight: 38,
    color: '#334650',
  },
  smallText: {
    display: 'block',
    maxWidth: 120,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    color: '#334650',
  },
  badge: {
    display: 'inline-flex',
    alignItems: 'center',
    maxWidth: 132,
    minHeight: 24,
    borderRadius: 999,
    border: '1px solid',
    padding: '2px 10px',
    fontSize: 12,
    fontWeight: 900,
    lineHeight: '18px',
    whiteSpace: 'nowrap',
  },
  imageWrap: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    maxWidth: 180,
    overflow: 'hidden',
  },
  image: {
    width: 44,
    height: 32,
    borderRadius: 10,
    objectFit: 'cover',
    border: '1px solid #f0d7df',
    flex: '0 0 auto',
  },
  replyMarker: {
    display: 'inline-flex',
    alignItems: 'center',
    minHeight: 22,
    borderRadius: 999,
    border: '1px solid #d9e4f5',
    padding: '1px 8px',
    background: '#f3f7ff',
    color: '#486486',
    fontSize: 12,
    fontWeight: 800,
  },
}

const HandbookListProperty = ({ property, record }) => {
  const path = property.path
  const value = record?.params?.[path]

  if (path === 'category') {
    return renderBadge(String(value || 'Khác'), CATEGORY_STYLES[value] || {
      background: '#f6f7fb',
      color: '#607080',
      border: '#dce3ec',
    })
  }

  if (path === 'status') {
    const status = STATUS_STYLES[String(value)] || STATUS_STYLES[value]
    return renderBadge(status?.label || String(value ?? ''), status || {
      background: '#f6f7fb',
      color: '#607080',
      border: '#dce3ec',
    })
  }

  if (path === 'admin_reply') {
    return renderBadge(value ? 'Admin' : 'User', value ? {
      background: '#fff3d8',
      color: '#9a5b00',
      border: '#ffd58a',
    } : {
      background: '#eaf1ff',
      color: '#2859b7',
      border: '#c5d7ff',
    })
  }

  if (path === 'parent_id') {
    return value
      ? React.createElement('span', { style: styles.replyMarker }, `Reply #${value}`)
      : React.createElement('span', { style: styles.replyMarker }, 'Comment gốc')
  }

  if (path === 'image_url') {
    return value
      ? React.createElement(
        'div',
        { style: styles.imageWrap, title: String(value) },
        React.createElement('img', { src: value, alt: '', style: styles.image }),
        React.createElement('span', { style: styles.smallText }, String(value)),
      )
      : null
  }

  return React.createElement(
    'div',
    { style: styles.cell, title: value === null || value === undefined ? '' : String(value) },
    React.createElement('span', { style: styles.text }, value === null || value === undefined ? '' : String(value)),
  )
}

function renderBadge(label, style) {
  return React.createElement(
    'span',
    {
      style: {
        ...styles.badge,
        background: style.background,
        color: style.color,
        borderColor: style.border,
      },
      title: label,
    },
    label,
  )
}

export default HandbookListProperty
