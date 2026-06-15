import React, { useState } from 'react'
import { useCurrentAdmin } from 'adminjs'

const styles = {
  wrapper: { marginBottom: 24 },
  label: { display: 'block', fontSize: 12, fontWeight: 700, marginBottom: 8, color: '#263238' },
  row: { display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' },
  input: { flex: '1 1 420px', minHeight: 40, border: '1px solid #cfd8dc', borderRadius: 8, padding: '8px 12px' },
  button: { minHeight: 40, border: 0, borderRadius: 8, padding: '0 16px', background: '#ff8fab', color: '#fff', fontWeight: 800, cursor: 'pointer' },
  preview: { display: 'block', width: 220, height: 140, objectFit: 'cover', borderRadius: 12, marginTop: 12, border: '1px solid #f1d5dd' },
  gallery: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))', gap: 10, marginTop: 12 },
  galleryButton: { border: '2px solid transparent', padding: 0, borderRadius: 10, overflow: 'hidden', cursor: 'pointer', background: '#fff' },
  galleryImage: { display: 'block', width: '100%', height: 90, objectFit: 'cover' },
  message: { marginTop: 8, fontSize: 12, color: '#c62828' },
}

const HandbookImageProperty = ({ property, record, onChange }) => {
  const [currentAdmin] = useCurrentAdmin()
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [images, setImages] = useState([])
  const [galleryOpen, setGalleryOpen] = useState(false)
  const value = record?.params?.[property.path] || ''

  async function upload(file) {
    if (!file) {
      return
    }

    setUploading(true)
    setError('')
    try {
      const formData = new FormData()
      formData.append('file', file)
      const response = await fetch(`${property.custom.apiBaseUrl}/api/handbook/images`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${currentAdmin?._auth?.accessToken || ''}`,
        },
        body: formData,
      })

      const payload = await response.json().catch(() => ({}))
      if (!response.ok || !payload.imageUrl) {
        throw new Error(payload.detail || payload.message || 'Không thể tải ảnh cẩm nang.')
      }

      onChange(property.path, payload.imageUrl)
    } catch (uploadError) {
      setError(uploadError.message || 'Không thể tải ảnh cẩm nang.')
    } finally {
      setUploading(false)
    }
  }

  async function toggleGallery() {
    if (galleryOpen) {
      setGalleryOpen(false)
      return
    }

    setError('')
    try {
      const response = await fetch(`${property.custom.apiBaseUrl}/api/handbook/images`, {
        headers: { Authorization: `Bearer ${currentAdmin?._auth?.accessToken || ''}` },
      })
      const payload = await response.json().catch(() => [])
      if (!response.ok) {
        throw new Error(payload.detail || payload.message || 'Không thể lấy thư viện ảnh.')
      }
      setImages(Array.isArray(payload) ? payload : [])
      setGalleryOpen(true)
    } catch (galleryError) {
      setError(galleryError.message || 'Không thể lấy thư viện ảnh.')
    }
  }

  return React.createElement(
    'div',
    { style: styles.wrapper },
    React.createElement('label', { style: styles.label }, property.label || 'Ảnh cẩm nang'),
    React.createElement(
      'div',
      { style: styles.row },
      React.createElement('input', {
        style: styles.input,
        value,
        placeholder: 'URL ảnh Cloudinary',
        onChange: (event) => onChange(property.path, event.target.value),
      }),
      React.createElement('label', { style: { ...styles.button, display: 'inline-flex', alignItems: 'center' } },
        uploading ? 'Đang tải...' : 'Chọn ảnh',
        React.createElement('input', {
          type: 'file',
          accept: 'image/*',
          disabled: uploading,
          style: { display: 'none' },
          onChange: (event) => upload(event.target.files?.[0]),
        }),
      ),
      React.createElement('button', {
        type: 'button',
        style: { ...styles.button, background: '#5c7a8a' },
        onClick: toggleGallery,
      }, galleryOpen ? 'Đóng thư viện' : 'Thư viện ảnh'),
    ),
    value ? React.createElement('img', { src: value, alt: 'Ảnh cẩm nang', style: styles.preview }) : null,
    galleryOpen
      ? React.createElement(
        'div',
        { style: styles.gallery },
        images.map((item) => React.createElement(
          'button',
          {
            key: item.publicId,
            type: 'button',
            title: item.publicId,
            style: { ...styles.galleryButton, borderColor: value === item.imageUrl ? '#ff8fab' : 'transparent' },
            onClick: () => onChange(property.path, item.imageUrl),
          },
          React.createElement('img', { src: item.imageUrl, alt: item.publicId, style: styles.galleryImage }),
        )),
      )
      : null,
    error ? React.createElement('div', { style: styles.message }, error) : null,
  )
}

export default HandbookImageProperty
