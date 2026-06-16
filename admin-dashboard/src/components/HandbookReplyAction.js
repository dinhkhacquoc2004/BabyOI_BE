import React, { useState } from 'react'
import { ApiClient } from 'adminjs'

const api = new ApiClient()

const styles = {
  panel: {
    maxWidth: 820,
    background: '#ffffff',
    border: '1px solid #f0d7df',
    borderRadius: 18,
    padding: 24,
    boxShadow: '0 10px 28px rgba(255, 182, 193, 0.14)',
  },
  heading: {
    margin: '0 0 16px',
    color: '#334650',
    fontSize: 22,
    fontWeight: 900,
  },
  meta: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 14,
  },
  pill: {
    display: 'inline-flex',
    alignItems: 'center',
    border: '1px solid #d9e4f5',
    borderRadius: 999,
    padding: '4px 10px',
    background: '#f3f7ff',
    color: '#486486',
    fontSize: 12,
    fontWeight: 800,
  },
  quote: {
    margin: '0 0 18px',
    padding: 16,
    border: '1px solid #f0d7df',
    borderRadius: 16,
    background: '#fff7fa',
    color: '#334650',
    lineHeight: '22px',
    whiteSpace: 'pre-wrap',
  },
  label: {
    display: 'block',
    marginBottom: 8,
    color: '#334650',
    fontSize: 13,
    fontWeight: 900,
  },
  textarea: {
    width: '100%',
    minHeight: 160,
    border: '1px solid #f0d7df',
    borderRadius: 14,
    padding: 12,
    color: '#334650',
    fontSize: 14,
    fontWeight: 700,
    boxSizing: 'border-box',
    resize: 'vertical',
  },
  buttonRow: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 10,
    marginTop: 16,
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
}

const HandbookReplyAction = ({ action, resource, record }) => {
  const [content, setContent] = useState('')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState(null)
  const params = record?.params || {}

  async function submit(event) {
    event.preventDefault()
    setSaving(true)
    setMessage(null)

    try {
      const response = await api.recordAction({
        resourceId: resource.id,
        recordId: record.id,
        actionName: action.name,
        method: 'post',
        data: { content },
      })

      if (response.data.redirectUrl) {
        window.location.assign(response.data.redirectUrl)
        return
      }

      setMessage({ type: 'success', text: response.data.notice?.message || 'Đã trả lời bình luận.' })
    } catch (error) {
      setMessage({ type: 'error', text: error.response?.data?.notice?.message || error.response?.data?.message || error.message || 'Không gửi được trả lời.' })
    } finally {
      setSaving(false)
    }
  }

  function goBack() {
    window.location.assign(`/admin/resources/${resource.id}/actions/list?filters.post_id=${encodeURIComponent(params.post_id || '')}`)
  }

  return React.createElement(
    'form',
    { style: styles.panel, onSubmit: submit },
    React.createElement('h2', { style: styles.heading }, 'Trả lời bình luận'),
    message
      ? React.createElement(
        'div',
        { style: { ...styles.message, ...(message.type === 'error' ? styles.error : {}) } },
        message.text,
      )
      : null,
    React.createElement(
      'div',
      { style: styles.meta },
      React.createElement('span', { style: styles.pill }, `Post #${params.post_id || '-'}`),
      React.createElement('span', { style: styles.pill }, `Comment #${record?.id || '-'}`),
      React.createElement('span', { style: styles.pill }, params.user_name || 'Người dùng'),
      params.parent_id ? React.createElement('span', { style: styles.pill }, `Reply của #${params.parent_id}`) : null,
    ),
    React.createElement('div', { style: styles.quote }, params.content || ''),
    React.createElement('label', { style: styles.label }, 'Nội dung admin trả lời'),
    React.createElement('textarea', {
      value: content,
      required: true,
      placeholder: 'Nhập câu trả lời cho bình luận này...',
      style: styles.textarea,
      onChange: (event) => setContent(event.target.value),
    }),
    React.createElement(
      'div',
      { style: styles.buttonRow },
      React.createElement(
        'button',
        { type: 'submit', style: styles.primaryButton, disabled: saving },
        saving ? 'Đang gửi...' : 'Gửi trả lời',
      ),
      React.createElement(
        'button',
        { type: 'button', style: styles.secondaryButton, onClick: goBack },
        'Quay lại list comment',
      ),
    ),
  )
}

export default HandbookReplyAction
