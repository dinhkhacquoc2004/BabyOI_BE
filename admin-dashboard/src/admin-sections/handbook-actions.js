import pg from 'pg'

const ACTIVE_STATUS = 2

export function handbookPostActions(component) {
  return {
    comments: {
      actionType: 'record',
      icon: 'MessageCircle',
      component,
      label: 'Binh luan',
      handler: async (request, response, context) => ({
        record: context.record.toJSON(context.currentAdmin),
        redirectUrl: handbookCommentsListUrl(context, context.record.id()),
      }),
    },
  }
}

export function handbookCommentActions(component, connectionString) {
  if (!component || !connectionString) {
    return {}
  }

  return {
    reply: {
      actionType: 'record',
      icon: 'Reply',
      label: 'Tra loi',
      component,
      handler: replyHandler(connectionString),
    },
  }
}

function replyHandler(connectionString) {
  return async (request, response, context) => {
    const recordJson = context.record.toJSON(context.currentAdmin)

    if (request.method === 'get') {
      return {
        record: recordJson,
      }
    }

    const content = String(request.payload?.content || '').trim()
    if (!content) {
      return {
        record: recordJson,
        notice: {
          message: 'Noi dung tra loi la bat buoc.',
          type: 'error',
        },
      }
    }

    const params = recordJson.params || {}
    const postId = Number(params.post_id)
    const parentId = Number(params.parent_id || recordJson.id)
    if (!Number.isFinite(postId) || !Number.isFinite(parentId)) {
      return {
        record: recordJson,
        notice: {
          message: 'Khong xac dinh duoc bai viet hoac binh luan goc.',
          type: 'error',
        },
      }
    }

    const savedReply = await insertReply(connectionString, {
      postId,
      parentId,
      content,
      userName: adminDisplayName(context.currentAdmin),
    })
    await insertReplyNotification(connectionString, savedReply)

    return {
      record: recordJson,
      redirectUrl: handbookCommentsListUrl(context, postId),
      notice: {
        message: 'Da tra loi binh luan.',
        type: 'success',
      },
    }
  }
}

async function insertReply(connectionString, reply) {
  const client = new pg.Client({ connectionString })
  await client.connect()
  try {
    const result = await client.query(`
      INSERT INTO handbook_comments (
        post_id,
        parent_id,
        user_id,
        user_name,
        avatar_url,
        content,
        like_count,
        admin_reply,
        created_at,
        status
      )
      VALUES ($1, $2, NULL, $3, NULL, $4, 0, TRUE, NOW(), $5)
      RETURNING id, post_id, parent_id, user_name, content
    `, [
      reply.postId,
      reply.parentId,
      reply.userName,
      reply.content,
      ACTIVE_STATUS,
    ])

    return result.rows[0]
  } finally {
    await client.end()
  }
}

async function insertReplyNotification(connectionString, reply) {
  if (!reply?.parent_id) {
    return
  }

  const client = new pg.Client({ connectionString })
  await client.connect()
  try {
    const contextResult = await client.query(`
      SELECT
        parent.user_id AS recipient_user_id,
        post.id AS post_id,
        post.title AS post_title
      FROM handbook_comments parent
      JOIN handbook_posts post ON post.id = parent.post_id
      WHERE parent.id = $1
        AND parent.user_id IS NOT NULL
    `, [reply.parent_id])
    const context = contextResult.rows[0]
    if (!context?.recipient_user_id) {
      return
    }

    const replierName = reply.user_name || 'Admin BabyOi'
    const postTitle = context.post_title || 'b\u00e0i c\u1ea9m nang'
    const shortTitle = truncate(postTitle, 48)
    const data = {
      screen: 'handbook_comment',
      route: `/camnang/comment?postId=${context.post_id}&commentId=${reply.parent_id}`,
      postId: Number(context.post_id),
      postTitle,
      commentId: Number(reply.parent_id),
      replyId: Number(reply.id),
      replierName,
    }

    const notificationResult = await client.query(`
      INSERT INTO "notification" (
        user_id,
        type,
        title,
        body,
        data_json,
        priority,
        source_type,
        source_id,
        status,
        created_at
      )
      VALUES ($1, $2, $3, $4, $5, 2, 'HANDBOOK_COMMENT', $6, -1, NOW())
      RETURNING id, user_id, type, title, body, data_json
    `, [
      context.recipient_user_id,
      'HANDBOOK_COMMENT_REPLY',
      `${replierName} \u0111\u00e3 tr\u1ea3 l\u1eddi b\u00ecnh lu\u1eadn c\u1ee7a b\u1ea1n`,
      `Trong b\u00e0i "${shortTitle}": ${truncate(reply.content || '', 120)}`,
      JSON.stringify(data),
      context.post_id,
    ])

    await sendImmediatePush(client, notificationResult.rows[0])
  } finally {
    await client.end()
  }
}

async function sendImmediatePush(client, notification) {
  if (!notification) {
    return
  }

  const tokensResult = await client.query(`
    SELECT dt.id, dt.token
    FROM device_token dt
    LEFT JOIN notification_setting ns ON ns.user_id = dt.user_id
    WHERE dt.user_id = $1
      AND dt.status = 2
      AND COALESCE(ns.push_enabled, TRUE) = TRUE
      AND COALESCE(ns.chat_enabled, TRUE) = TRUE
  `, [notification.user_id])

  for (const token of tokensResult.rows) {
    try {
      const response = await fetch('https://exp.host/--/api/v2/push/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          to: token.token,
          title: notification.title,
          body: notification.body,
          sound: 'default',
          data: {
            notificationId: String(notification.id),
            type: notification.type,
            dataJson: notification.data_json || '',
          },
        }),
      })

      if (response.ok) {
        await client.query('UPDATE device_token SET last_used_at = NOW() WHERE id = $1', [token.id])
      }
    } catch {
      // Notification list already has the message; push failure should not block admin reply.
    }
  }
}

function adminDisplayName(currentAdmin) {
  return currentAdmin?.title || currentAdmin?.name || currentAdmin?.email || 'Admin BabyOi'
}

function handbookCommentsListUrl(context, postId) {
  return context.h.resourceActionUrl({
    resourceId: 'handbook_comments',
    actionName: 'list',
    search: `?filters.post_id=${encodeURIComponent(postId)}`,
  })
}

function truncate(value, maxLength) {
  if (!value || value.length <= maxLength) {
    return value || ''
  }
  return `${value.slice(0, Math.max(0, maxLength - 3)).trim()}...`
}
