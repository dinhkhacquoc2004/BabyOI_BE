import { SPRING_API_BASE_URL } from './config.js'

export async function authenticate(username, password) {
  if (!username || !password) {
    return null
  }

  try {
    const response = await fetch(`${SPRING_API_BASE_URL}/api/admin/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.trim(),
        password,
      }),
    })

    if (!response.ok) {
      return null
    }

    const admin = await response.json()

    return {
      id: admin.id,
      email: admin.email,
      title: admin.userName || admin.email,
      role: admin.role,
    }
  } catch (error) {
    console.warn('Spring admin authentication failed:', error.message)
    return null
  }
}
