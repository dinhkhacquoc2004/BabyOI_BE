import React from 'react'

const COLORS = {
  pink: '#ff8fab',
  green: '#72c39a',
  ink: '#334650',
  muted: '#7c91a0',
  border: '#f0d7df',
  grid: '#edf1f3',
  paper: '#ffffff',
  background: '#fef9f3',
}

const styles = {
  page: {
    minHeight: 'calc(100vh - 64px)',
    background: COLORS.background,
    padding: '32px clamp(20px, 4vw, 56px) 48px',
    color: COLORS.ink,
    fontFamily: '"Segoe UI", system-ui, -apple-system, BlinkMacSystemFont, sans-serif',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: 18,
    marginBottom: 24,
  },
  logo: {
    width: 72,
    height: 72,
    objectFit: 'contain',
    filter: 'drop-shadow(0 10px 18px rgba(255, 143, 171, 0.22))',
  },
  eyebrow: {
    margin: 0,
    color: COLORS.pink,
    fontSize: 13,
    fontWeight: 900,
    textTransform: 'uppercase',
    letterSpacing: 1.2,
  },
  title: {
    margin: '4px 0 0',
    color: COLORS.ink,
    fontSize: 'clamp(28px, 4vw, 42px)',
    lineHeight: 1.1,
    fontWeight: 900,
  },
  stats: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
    gap: 16,
    marginBottom: 22,
  },
  stat: {
    background: COLORS.paper,
    border: `1px solid ${COLORS.border}`,
    borderRadius: 22,
    padding: 22,
    boxShadow: '0 12px 28px rgba(255, 182, 193, 0.12)',
  },
  statLabel: {
    margin: 0,
    color: COLORS.muted,
    fontSize: 14,
    fontWeight: 800,
  },
  statValue: {
    margin: '8px 0 4px',
    color: COLORS.ink,
    fontSize: 34,
    fontWeight: 900,
  },
  statHint: {
    margin: 0,
    color: '#9aaab3',
    fontSize: 12,
    fontWeight: 600,
  },
  chartCard: {
    background: COLORS.paper,
    border: `1px solid ${COLORS.border}`,
    borderRadius: 26,
    padding: '24px 22px 18px',
    boxShadow: '0 16px 36px rgba(255, 182, 193, 0.13)',
  },
  chartHeader: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 14,
    marginBottom: 12,
  },
  chartTitle: {
    margin: 0,
    color: COLORS.ink,
    fontSize: 20,
    fontWeight: 900,
  },
  chartSubtitle: {
    margin: '5px 0 0',
    color: COLORS.muted,
    fontSize: 13,
    fontWeight: 600,
  },
  legend: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 16,
    color: COLORS.muted,
    fontSize: 13,
    fontWeight: 700,
  },
  legendItem: {
    display: 'flex',
    alignItems: 'center',
    gap: 7,
  },
  legendDot: {
    width: 10,
    height: 10,
    borderRadius: 999,
  },
  chartViewport: {
    width: '100%',
    overflowX: 'auto',
  },
  empty: {
    minHeight: 260,
    display: 'grid',
    placeItems: 'center',
    color: COLORS.muted,
    fontWeight: 700,
  },
}

const numberFormatter = new Intl.NumberFormat('vi-VN')
const shortDateFormatter = new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' })

function formatNumber(value) {
  return numberFormatter.format(Number(value) || 0)
}

function formatDate(value) {
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? value : shortDateFormatter.format(date)
}

function statCard(label, value, hint, accent) {
  return React.createElement(
    'article',
    { style: { ...styles.stat, borderTop: `4px solid ${accent}` } },
    React.createElement('p', { style: styles.statLabel }, label),
    React.createElement('p', { style: { ...styles.statValue, color: accent } }, formatNumber(value)),
    React.createElement('p', { style: styles.statHint }, hint),
  )
}

function legendItem(color, label) {
  return React.createElement(
    'span',
    { style: styles.legendItem },
    React.createElement('span', { style: { ...styles.legendDot, background: color } }),
    label,
  )
}

function ActivityChart({ daily }) {
  if (!Array.isArray(daily) || daily.length === 0) {
    return React.createElement('div', { style: styles.empty }, 'Chưa có dữ liệu hoạt động.')
  }

  const width = 960
  const height = 320
  const padding = { top: 24, right: 24, bottom: 54, left: 58 }
  const plotWidth = width - padding.left - padding.right
  const plotHeight = height - padding.top - padding.bottom
  const maximum = Math.max(
    1,
    ...daily.map((item) => Math.max(item.activeUsers || 0, item.nutritionPageViews || 0)),
  )
  const xAt = (index) => padding.left + (daily.length === 1 ? plotWidth / 2 : (index * plotWidth) / (daily.length - 1))
  const yAt = (value) => padding.top + plotHeight - ((Number(value) || 0) / maximum) * plotHeight
  const pointsFor = (key) => daily.map((item, index) => `${xAt(index)},${yAt(item[key])}`).join(' ')
  const labelStep = Math.max(1, Math.ceil(daily.length / 6))
  const children = []

  for (let line = 0; line <= 4; line += 1) {
    const value = Math.round((maximum * (4 - line)) / 4)
    const y = padding.top + (plotHeight * line) / 4
    children.push(React.createElement('line', {
      key: `grid-${line}`,
      x1: padding.left,
      y1: y,
      x2: width - padding.right,
      y2: y,
      stroke: COLORS.grid,
      strokeWidth: 1,
    }))
    children.push(React.createElement('text', {
      key: `grid-label-${line}`,
      x: padding.left - 12,
      y: y + 4,
      textAnchor: 'end',
      fill: COLORS.muted,
      fontSize: 11,
      fontWeight: 700,
    }, formatNumber(value)))
  }

  children.push(React.createElement('polyline', {
    key: 'active-line',
    points: pointsFor('activeUsers'),
    fill: 'none',
    stroke: COLORS.pink,
    strokeWidth: 4,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
  }))
  children.push(React.createElement('polyline', {
    key: 'nutrition-line',
    points: pointsFor('nutritionPageViews'),
    fill: 'none',
    stroke: COLORS.green,
    strokeWidth: 4,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
  }))

  daily.forEach((item, index) => {
    if (index % labelStep === 0 || index === daily.length - 1) {
      children.push(React.createElement('text', {
        key: `date-${item.date}`,
        x: xAt(index),
        y: height - 22,
        textAnchor: 'middle',
        fill: COLORS.muted,
        fontSize: 11,
        fontWeight: 700,
      }, formatDate(item.date)))
    }

    ;[
      ['activeUsers', COLORS.pink, 'Active user'],
      ['nutritionPageViews', COLORS.green, 'Lượt dinh dưỡng'],
    ].forEach(([key, color, label]) => {
      children.push(React.createElement(
        'circle',
        {
          key: `${key}-${item.date}`,
          cx: xAt(index),
          cy: yAt(item[key]),
          r: 4,
          fill: COLORS.paper,
          stroke: color,
          strokeWidth: 3,
        },
        React.createElement('title', null, `${formatDate(item.date)} · ${label}: ${formatNumber(item[key])}`),
      ))
    })
  })

  return React.createElement(
    'div',
    { style: styles.chartViewport },
    React.createElement('svg', {
      viewBox: `0 0 ${width} ${height}`,
      role: 'img',
      'aria-label': 'Biểu đồ người dùng hoạt động và lượt truy cập trang dinh dưỡng trong 30 ngày',
      style: { display: 'block', width: '100%', minWidth: 680, height: 'auto' },
    }, children),
  )
}

const BabyOiDashboard = (props) => {
  const data = props?.data || {}
  const summary = data.summary || {}
  const daily = data.daily || []

  return React.createElement(
    'main',
    { style: styles.page },
    React.createElement(
      'header',
      { style: styles.header },
      React.createElement('img', {
        src: '/admin-assets/babyoi-logo.png',
        alt: 'BabyOi',
        style: styles.logo,
      }),
      React.createElement(
        'div',
        null,
        React.createElement('p', { style: styles.eyebrow }, 'BabyOi Admin'),
        React.createElement('h1', { style: styles.title }, 'Tổng quan hoạt động'),
      ),
    ),
    React.createElement(
      'section',
      { style: styles.stats },
      statCard('Tổng người đã đăng ký', summary.registeredUsers, 'Không bao gồm tài khoản quản trị', COLORS.pink),
      statCard('Active user hôm nay', summary.activeUsersToday, 'Người dùng app duy nhất trong ngày', '#6aa9d8'),
      statCard('Lượt truy cập dinh dưỡng', summary.nutritionPageViews, 'Tổng lượt mở trang từ khi bắt đầu ghi nhận', COLORS.green),
    ),
    React.createElement(
      'section',
      { style: styles.chartCard },
      React.createElement(
        'div',
        { style: styles.chartHeader },
        React.createElement(
          'div',
          null,
          React.createElement('h2', { style: styles.chartTitle }, 'Hoạt động 30 ngày gần nhất'),
          React.createElement('p', { style: styles.chartSubtitle }, 'Dữ liệu được chốt theo múi giờ Việt Nam'),
        ),
        React.createElement(
          'div',
          { style: styles.legend },
          legendItem(COLORS.pink, 'Active user'),
          legendItem(COLORS.green, 'Lượt vào trang dinh dưỡng'),
        ),
      ),
      React.createElement(ActivityChart, { daily }),
    ),
  )
}

export default BabyOiDashboard
