import React from 'react'

const styles = {
  page: {
    minHeight: 'calc(100vh - 64px)',
    background: '#fef9f3',
    padding: '40px clamp(24px, 5vw, 72px)',
    color: '#5c7a8a',
    fontFamily: '"Segoe UI", system-ui, -apple-system, BlinkMacSystemFont, sans-serif',
  },
  hero: {
    minHeight: 420,
    borderRadius: 32,
    background: '#ffffff',
    boxShadow: '0 24px 60px rgba(255, 143, 171, 0.18)',
    border: '1px solid #f0d7df',
    display: 'grid',
    placeItems: 'center',
    textAlign: 'center',
    padding: '44px 28px',
  },
  logo: {
    width: 132,
    height: 132,
    objectFit: 'contain',
    marginBottom: 24,
    filter: 'drop-shadow(0 14px 24px rgba(255, 143, 171, 0.22))',
  },
  eyebrow: {
    margin: 0,
    color: '#ff8fab',
    fontSize: 15,
    fontWeight: 900,
    textTransform: 'uppercase',
    letterSpacing: 0,
  },
  title: {
    margin: '10px 0 12px',
    color: '#ff8fab',
    fontSize: 'clamp(32px, 4vw, 52px)',
    lineHeight: 1.08,
    fontWeight: 900,
    letterSpacing: 0,
  },
  subtitle: {
    margin: '0 auto',
    maxWidth: 720,
    color: '#5c7a8a',
    fontSize: 18,
    lineHeight: 1.7,
    fontWeight: 700,
  },
  stats: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))',
    gap: 16,
    marginTop: 22,
  },
  stat: {
    background: '#ffffff',
    border: '1px solid #f0d7df',
    borderRadius: 22,
    padding: 20,
    boxShadow: '0 12px 28px rgba(255, 182, 193, 0.14)',
  },
  statLabel: {
    margin: 0,
    color: '#7c91a0',
    fontSize: 13,
    fontWeight: 800,
  },
  statValue: {
    margin: '8px 0 0',
    color: '#334650',
    fontSize: 24,
    fontWeight: 900,
  },
}

function statCard(label, value) {
  return React.createElement(
    'article',
    { style: styles.stat },
    React.createElement('p', { style: styles.statLabel }, label),
    React.createElement('p', { style: styles.statValue }, value),
  )
}

const BabyOiDashboard = (props) => {
  const data = props?.data || {}
  const tables = data.tables ?? '-'
  const database = data.database ?? 'BabyOi'

  return React.createElement(
    'main',
    { style: styles.page },
    React.createElement(
      'section',
      { style: styles.hero },
      React.createElement(
        'div',
        null,
        React.createElement('img', {
          src: '/admin-assets/babyoi-logo.png',
          alt: 'BabyOi',
          style: styles.logo,
        }),
        React.createElement('p', { style: styles.eyebrow }, 'BabyOi Admin'),
        React.createElement('h1', { style: styles.title }, 'Chào mừng trở lại'),
        React.createElement(
          'p',
          { style: styles.subtitle },
          'Chúc bạn một ngày làm việc hiệu quả. Đây là khu vực quản trị dữ liệu BabyOi, nơi theo dõi nội dung dinh dưỡng, tiêm chủng, cẩm nang và hồ sơ người dùng.',
        ),
      ),
    ),
    React.createElement(
      'section',
      { style: styles.stats },
      statCard('Bảng đang quản trị', tables),
      statCard('Cơ sở dữ liệu', database),
      statCard('Trạng thái', 'Sẵn sàng'),
    ),
  )
}

export default BabyOiDashboard
