(function () {
  var buttonId = 'babyoi-return-to-list'
  var storagePrefix = 'babyoi.admin.returnTo.'
  var syncScheduled = false

  function resourceFromPath(pathname) {
    var match = pathname.match(/\/resources\/([^/?#]+)/)
    return match ? decodeURIComponent(match[1]) : null
  }

  function storageKey(resource) {
    return storagePrefix + resource
  }

  function resourceListPath(resource) {
    return '/admin/resources/' + encodeURIComponent(resource)
  }

  function isListPath(pathname) {
    return Boolean(resourceFromPath(pathname)) && pathname.indexOf('/records/') === -1
  }

  function isRecordPage(pathname) {
    return Boolean(resourceFromPath(pathname)) && pathname.indexOf('/records/') !== -1
  }

  function currentRelativeUrl() {
    return window.location.pathname + window.location.search
  }

  function safeRelativeUrl(value, resource) {
    if (typeof value !== 'string' || value.charAt(0) !== '/') {
      return null
    }

    var listPath = resourceListPath(resource)
    if (value === listPath || value.indexOf(listPath + '?') === 0) {
      return value
    }

    return null
  }

  function getReturnTo(resource) {
    var params = new URLSearchParams(window.location.search)
    return (
      safeRelativeUrl(params.get('returnTo'), resource) ||
      safeRelativeUrl(window.sessionStorage.getItem(storageKey(resource)), resource) ||
      resourceListPath(resource)
    )
  }

  function setReturnToOnCurrentUrl(returnTo) {
    if (!returnTo || !isRecordPage(window.location.pathname)) {
      return
    }

    var params = new URLSearchParams(window.location.search)
    if (params.get('returnTo') === returnTo) {
      return
    }

    params.set('returnTo', returnTo)
    window.history.replaceState(
      window.history.state,
      '',
      window.location.pathname + '?' + params.toString(),
    )
  }

  function decorateRecordUrl(href) {
    try {
      var url = new URL(href, window.location.origin)
      var resource = resourceFromPath(url.pathname)
      if (!resource || !isRecordPage(url.pathname)) {
        return href
      }

      var returnTo = getReturnTo(resource)
      url.searchParams.set('returnTo', returnTo)
      return url.pathname + url.search + url.hash
    } catch (error) {
      return href
    }
  }

  function rememberListUrl() {
    var resource = resourceFromPath(window.location.pathname)
    if (resource && isListPath(window.location.pathname)) {
      window.sessionStorage.setItem(storageKey(resource), currentRelativeUrl())
    }
  }

  function ensureReturnButton() {
    var existing = document.getElementById(buttonId)
    var resource = resourceFromPath(window.location.pathname)

    if (!resource || !isRecordPage(window.location.pathname)) {
      if (existing) {
        existing.remove()
      }
      return
    }

    var returnTo = getReturnTo(resource)
    setReturnToOnCurrentUrl(returnTo)

    if (existing) {
      existing.setAttribute('href', returnTo)
      placeReturnButton(existing)
      return
    }

    var anchor = document.createElement('a')
    anchor.id = buttonId
    anchor.href = returnTo
    anchor.textContent = 'Quay lại danh sách'
    anchor.style.display = 'inline-flex'
    anchor.style.alignItems = 'center'
    anchor.style.justifyContent = 'center'
    anchor.style.minHeight = '42px'
    anchor.style.padding = '0 18px'
    anchor.style.margin = '0 0 18px 0'
    anchor.style.borderRadius = '999px'
    anchor.style.background = '#ffffff'
    anchor.style.border = '1px solid #f0d7df'
    anchor.style.color = '#ff8fab'
    anchor.style.fontWeight = '900'
    anchor.style.textDecoration = 'none'
    anchor.style.boxShadow = '0 10px 24px rgba(255, 182, 193, 0.18)'

    placeReturnButton(anchor)
  }

  function placeReturnButton(anchor) {
    var main = document.querySelector('main') || document.body
    var titleAnchor = findTitleAnchor(main, anchor)

    if (titleAnchor && titleAnchor.parentNode) {
      if (anchor.parentNode === titleAnchor.parentNode && anchor.previousElementSibling === titleAnchor) {
        return
      }

      titleAnchor.parentNode.insertBefore(anchor, titleAnchor.nextSibling)
      return
    }

    var detailPanel = findDetailPanel(main, anchor)

    if (detailPanel && detailPanel.parentNode) {
      if (anchor.parentNode === detailPanel.parentNode && anchor.nextElementSibling === detailPanel) {
        return
      }

      detailPanel.parentNode.insertBefore(anchor, detailPanel)
      return
    }

    if (anchor.parentNode === main && anchor.nextElementSibling === null) {
      return
    }

    main.appendChild(anchor)
  }

  function findTitleAnchor(main, anchor) {
    var headings = Array.prototype.slice.call(main.querySelectorAll('h1, h2, h3'))
    var title = headings.find(function (heading) {
      if (heading === anchor || heading.contains(anchor)) {
        return false
      }

      var text = heading.textContent.trim()
      return text === 'Chi tiết' || text === 'Chỉnh sửa' || text === 'Thêm mới'
    })

    if (!title) {
      return null
    }

    var parentStyle = window.getComputedStyle(title.parentElement)
    if (parentStyle.display.indexOf('flex') !== -1 || parentStyle.display.indexOf('grid') !== -1) {
      return title.parentElement
    }

    return title
  }

  function findDetailPanel(main, anchor) {
    var candidates = Array.prototype.slice
      .call(main.querySelectorAll('section, article, [data-css="box"]'))
      .filter(function (element) {
        if (element === anchor || element.contains(anchor)) {
          return false
        }

        var rect = element.getBoundingClientRect()
        return rect.width > 420 && rect.height > 160
      })
      .sort(function (first, second) {
        return second.getBoundingClientRect().bottom - first.getBoundingClientRect().bottom
      })

    return candidates[0] || null
  }

  function sync() {
    rememberListUrl()
    ensureReturnButton()
  }

  function scheduleSync() {
    if (syncScheduled) {
      return
    }

    syncScheduled = true
    window.requestAnimationFrame(function () {
      syncScheduled = false
      sync()
    })
  }

  document.addEventListener(
    'click',
    function (event) {
      var anchor = event.target.closest && event.target.closest('a[href]')
      if (!anchor) {
        return
      }

      rememberListUrl()
      anchor.setAttribute('href', decorateRecordUrl(anchor.getAttribute('href')))
    },
    true,
  )

  var originalPushState = window.history.pushState
  var originalReplaceState = window.history.replaceState

  window.history.pushState = function () {
    originalPushState.apply(this, arguments)
    scheduleSync()
  }

  window.history.replaceState = function () {
    originalReplaceState.apply(this, arguments)
    scheduleSync()
  }

  window.addEventListener('popstate', scheduleSync)
  new MutationObserver(scheduleSync).observe(document.documentElement, { childList: true, subtree: true })
  sync()
})()
