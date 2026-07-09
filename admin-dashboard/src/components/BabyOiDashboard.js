import { ApiClient } from 'adminjs'
import React, { useEffect, useMemo, useState } from 'react'

const chartWidth = 720
const chartHeight = 220
const chartPadding = 34

const styles = {
  page: {
    minHeight: 'calc(100vh - 64px)',
    background: '#fef9f3',
    padding: '28px clamp(18px, 4vw, 42px)',
    color: '#334650',
    fontFamily: '"Segoe UI", system-ui, -apple-system, BlinkMacSystemFont, sans-serif',
  },
  header: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    gap: 16,
    marginBottom: 20,
  },
  eyebrow: {
    margin: 0,
    color: '#ff8fab',
    fontSize: 13,
    fontWeight: 900,
    textTransform: 'uppercase',
    letterSpacing: 0,
  },
  title: {
    margin: '6px 0 0',
    color: '#334650',
    fontSize: 28,
    lineHeight: 1.15,
    fontWeight: 850,
    letterSpacing: 0,
  },
  meta: {
    margin: 0,
    color: '#5c7a8a',
    fontSize: 13,
    fontWeight: 650,
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
    gap: 12,
    marginBottom: 14,
  },
  statCard: {
    background: '#ffffff',
    border: '1px solid #f0d7df',
    borderRadius: 18,
    padding: 18,
    boxShadow: '0 10px 28px rgba(255, 182, 193, 0.18)',
  },
  statLabel: {
    margin: 0,
    color: '#5c7a8a',
    fontSize: 13,
    fontWeight: 750,
  },
  statValue: {
    margin: '8px 0 2px',
    color: '#334650',
    fontSize: 26,
    fontWeight: 850,
    lineHeight: 1.05,
    overflowWrap: 'anywhere',
  },
  statHint: {
    margin: 0,
    color: '#7c91a0',
    fontSize: 12,
    fontWeight: 650,
  },
  panel: {
    background: '#ffffff',
    border: '1px solid #f0d7df',
    borderRadius: 18,
    padding: 18,
    boxShadow: '0 10px 28px rgba(255, 182, 193, 0.18)',
  },
  panelTitle: {
    margin: 0,
    color: '#334650',
    fontSize: 17,
    fontWeight: 850,
  },
  panelSubtle: {
    margin: '4px 0 0',
    color: '#5c7a8a',
    fontSize: 13,
    fontWeight: 650,
  },
  chartWrap: {
    width: '100%',
    overflow: 'hidden',
    marginTop: 14,
  },
  empty: {
    margin: '18px 0 0',
    color: '#7c91a0',
    fontSize: 13,
    fontWeight: 700,
  },
  featureChartGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
    gap: 14,
    marginTop: 14,
  },
  featureChartCard: {
    background: '#ffffff',
    border: '1px solid #f0d7df',
    borderRadius: 18,
    padding: 16,
    boxShadow: '0 8px 22px rgba(255, 182, 193, 0.14)',
  },
  featureChartHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    gap: 12,
    alignItems: 'flex-start',
  },
  featureChartName: {
    margin: 0,
    color: '#334650',
    fontSize: 16,
    fontWeight: 850,
  },
  featureChartMetric: {
    margin: '4px 0 0',
    color: '#5c7a8a',
    fontSize: 12,
    fontWeight: 700,
  },
  trendBadge: {
    borderRadius: 999,
    background: '#fff1f5',
    color: '#ff5f93',
    padding: '6px 10px',
    fontSize: 12,
    fontWeight: 850,
    whiteSpace: 'nowrap',
  },
  selectedPoint: {
    margin: '10px 0 0',
    borderRadius: 8,
    background: '#fff1f5',
    border: '1px solid #ffd5dd',
    color: '#5c7a8a',
    padding: '9px 11px',
    fontSize: 12,
    fontWeight: 800,
  },
}

function formatNumber(value) {
  return new Intl.NumberFormat('vi-VN').format(Number(value || 0))
}

function formatGeneratedAt(value) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value))
}

function statCard(label, value, hint) {
  return React.createElement(
    'article',
    { style: styles.statCard },
    React.createElement('p', { style: styles.statLabel }, label),
    React.createElement('p', { style: styles.statValue }, value),
    hint ? React.createElement('p', { style: styles.statHint }, hint) : null,
  )
}

function pointCoordinates(series, item, index, maxValue) {
  const innerWidth = chartWidth - chartPadding * 2
  const innerHeight = chartHeight - chartPadding * 2
  const divisor = Math.max(series.length - 1, 1)
  const x = chartPadding + (index / divisor) * innerWidth
  const y = chartHeight - chartPadding - (Number(item.views || 0) / maxValue) * innerHeight
  return { x, y }
}

function buildPolyline(series, maxValue) {
  return series.map((item, index) => {
    const { x, y } = pointCoordinates(series, item, index, maxValue)
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

function selectedPointKey(featureKey, item) {
  return `${featureKey}:${item.date}`
}

function featureTrendChart(feature, selectedPoint, setSelectedPoint) {
  const series = feature.series || []
  if (!series.length) {
    return React.createElement('p', { style: styles.empty }, 'Chưa có dữ liệu sử dụng.')
  }

  const maxValue = Math.max(1, ...series.map((item) => Number(item.views || 0)))
  const viewLine = buildPolyline(series, maxValue)
  const selected = selectedPoint?.featureKey === feature.featureKey ? selectedPoint : null

  return React.createElement(
    'div',
    { style: styles.chartWrap },
    React.createElement(
      'svg',
      {
        viewBox: `0 0 ${chartWidth} ${chartHeight}`,
        role: 'img',
        'aria-label': `Biểu đồ lượt truy cập ${feature.featureName}`,
        style: { width: '100%', height: 210, display: 'block' },
      },
      React.createElement('rect', {
        x: 0,
        y: 0,
        width: chartWidth,
        height: chartHeight,
        rx: 18,
        fill: '#fff7fa',
      }),
      [0, 0.25, 0.5, 0.75, 1].map((ratio) => {
        const y = chartPadding + ratio * (chartHeight - chartPadding * 2)
        return React.createElement('line', {
          key: `grid-${ratio}`,
          x1: chartPadding,
          y1: y,
          x2: chartWidth - chartPadding,
          y2: y,
          stroke: '#f0d7df',
          strokeWidth: 1,
        })
      }),
      React.createElement('polyline', {
        points: viewLine,
        fill: 'none',
        stroke: '#ff8fab',
        strokeWidth: 4,
        strokeLinecap: 'round',
        strokeLinejoin: 'round',
      }),
      series.map((item, index) => {
        const { x, y } = pointCoordinates(series, item, index, maxValue)
        const isSelected = selectedPointKey(feature.featureKey, item) === selected?.key
        return React.createElement(
          'g',
          { key: `point-${feature.featureKey}-${item.date}` },
          React.createElement('circle', {
            cx: x,
            cy: y,
            r: isSelected ? 7 : 5,
            fill: '#ffffff',
            stroke: isSelected ? '#ff5f93' : '#ff8fab',
            strokeWidth: isSelected ? 4 : 3,
            style: { cursor: 'pointer' },
            onClick: () => setSelectedPoint({
              key: selectedPointKey(feature.featureKey, item),
              featureKey: feature.featureKey,
              featureName: feature.featureName,
              label: item.label,
              views: item.views,
            }),
          }),
          React.createElement('title', null, `${item.label}: ${formatNumber(item.views)} lượt truy cập`),
        )
      }),
      series.map((item, index) => {
        const divisor = Math.max(series.length - 1, 1)
        const x = chartPadding + (index / divisor) * (chartWidth - chartPadding * 2)
        return React.createElement(
          'text',
          {
            key: `label-${feature.featureKey}-${item.date}`,
            x,
            y: chartHeight - 8,
            textAnchor: 'middle',
            fill: '#7c91a0',
            fontSize: 12,
            fontWeight: 700,
          },
          item.label,
        )
      }),
    ),
    selected
      ? React.createElement(
        'p',
        { style: styles.selectedPoint },
        `${selected.featureName} ngày ${selected.label}: ${formatNumber(selected.views)} lượt truy cập`,
      )
      : React.createElement(
        'p',
        { style: styles.featureChartMetric },
        'Bấm vào từng điểm để xem số lượt truy cập của ngày đó.',
      ),
  )
}

function featureTrendGrid(features, selectedPoint, setSelectedPoint) {
  if (!features.length) {
    return React.createElement('p', { style: styles.empty }, 'Chưa có dữ liệu sử dụng chức năng.')
  }

  return React.createElement(
    'div',
    { style: styles.featureChartGrid },
    features.map((feature) => React.createElement(
      'article',
      { key: feature.featureKey, style: styles.featureChartCard },
      React.createElement(
        'div',
        { style: styles.featureChartHeader },
        React.createElement(
          'div',
          null,
          React.createElement('h3', { style: styles.featureChartName }, feature.featureName),
          React.createElement(
            'p',
            { style: styles.featureChartMetric },
            `${formatNumber(feature.totalViews)} lượt truy cập trong kỳ`,
          ),
        ),
        React.createElement('span', { style: styles.trendBadge }, `${formatNumber(feature.todayViews)} lượt hôm nay`),
      ),
      featureTrendChart(feature, selectedPoint, setSelectedPoint),
    )),
  )
}

const BabyOiDashboard = (props) => {
  const api = useMemo(() => new ApiClient(), [])
  const [dashboardData, setDashboardData] = useState(props?.data || null)
  const [selectedPoint, setSelectedPoint] = useState(null)
  const data = dashboardData || props?.data || {}
  const analytics = data.analytics || {}
  const tables = data.tables ?? '-'
  const database = data.database ?? 'BabyOi'
  const generatedAt = formatGeneratedAt(analytics.generatedAt)
  const featureMonthlySeries = analytics.featureMonthlySeries || []

  useEffect(() => {
    let mounted = true

    api.getDashboard()
      .then((response) => {
        if (mounted) {
          setDashboardData(response.data || {})
        }
      })
      .catch(() => undefined)

    return () => {
      mounted = false
    }
  }, [api])

  return React.createElement(
    'main',
    { style: styles.page },
    React.createElement(
      'header',
      { style: styles.header },
      React.createElement(
        'div',
        null,
        React.createElement('p', { style: styles.eyebrow }, 'BabyOi Analytics'),
        React.createElement('h1', { style: styles.title }, 'Thống kê sử dụng app'),
      ),
      React.createElement(
        'p',
        { style: styles.meta },
        `Cập nhật ${generatedAt} · ${tables} bảng · ${database}`,
      ),
    ),
    React.createElement(
      'section',
      { style: styles.statsGrid },
      statCard('User đã đăng ký', formatNumber(analytics.totalUsers), 'Không tính admin và tài khoản demo'),
      statCard('User dùng app hôm nay', formatNumber(analytics.activeUsersToday), 'Đếm duy nhất theo user trong ngày'),
      statCard('Lượt vào chức năng hôm nay', formatNumber(analytics.featureViewsToday), 'Tổng lượt truy cập các chức năng chính'),
      statCard('Chức năng có thống kê', formatNumber(featureMonthlySeries.length), ''),
    ),
    React.createElement(
      'section',
      { style: styles.panel },
      React.createElement('h2', { style: styles.panelTitle }, 'Biểu đồ lượt truy cập từng chức năng'),
      React.createElement('p', { style: styles.panelSubtle }, 'Dữ liệu hiển thị từ ngày 26/06, mỗi điểm là tổng lượt truy cập của một ngày.'),
      featureTrendGrid(featureMonthlySeries, selectedPoint, setSelectedPoint),
    ),
  )
}

export default BabyOiDashboard
