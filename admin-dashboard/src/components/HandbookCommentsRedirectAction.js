import React, { useEffect } from 'react'

const styles = {
  panel: {
    background: '#ffffff',
    border: '1px solid #f0d7df',
    borderRadius: 18,
    padding: 24,
    color: '#334650',
    fontSize: 15,
    fontWeight: 800,
    boxShadow: '0 10px 28px rgba(255, 182, 193, 0.14)',
  },
}

const HandbookCommentsRedirectAction = ({ record }) => {
  const postId = record?.id || record?.params?.id

  useEffect(() => {
    if (!postId) {
      return
    }

    window.location.replace(`/admin/resources/handbook_comments/actions/list?filters.post_id=${encodeURIComponent(postId)}`)
  }, [postId])

  return React.createElement(
    'div',
    { style: styles.panel },
    postId ? 'Đang mở danh sách bình luận...' : 'Không xác định được bài viết.',
  )
}

export default HandbookCommentsRedirectAction
