const BASE = window.location.hostname === 'localhost'
  ? '/api'
  : 'https://daycarelog-production-0a50.up.railway.app/api'

function getToken() {
  return localStorage.getItem('dcl_token')
}

async function request(path, options = {}) {
  const token = getToken()
  const res = await fetch(`${BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...options,
  })

  if (!res.ok) {
    let msg = `Request failed (${res.status})`
    let code = null
    try {
      const body = await res.json()
      msg = body.message ?? msg
      code = body.code ?? null
    } catch (_) {}

    if (code === 'EMAIL_NOT_VERIFIED' && !/^\/(check-email|verify-email)/.test(window.location.pathname)) {
      window.location.href = '/check-email'
    }

    const err = new Error(msg)
    err.code = code
    throw err
  }

  if (res.status === 204) return null
  return res.json()
}

export const api = {
  auth: {
    login:    (email, password)              => request('/auth/login',    { method: 'POST', body: JSON.stringify({ email, password }) }),
    register: (email, password, firstName, lastName, middleName, suffix) => request('/auth/register', { method: 'POST', body: JSON.stringify({ email, password, firstName, lastName, middleName, suffix }) }),
    verifyByToken:        token       => request('/auth/verify-email', { method: 'POST', body: JSON.stringify({ token }) }),
    verifyByCode:         (email, code) => request('/auth/verify-email', { method: 'POST', body: JSON.stringify({ email, code }) }),
    resendVerification:   email       => request('/auth/resend-verification', { method: 'POST', body: JSON.stringify({ email }) }),
    forgotPassword:        email       => request('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
    resetPasswordByToken: (token, newPassword) => request('/auth/reset-password', { method: 'POST', body: JSON.stringify({ token, newPassword }) }),
    resetPasswordByCode:  (email, code, newPassword) => request('/auth/reset-password', { method: 'POST', body: JSON.stringify({ email, code, newPassword }) }),
    checkEmail:            email       => request(`/auth/check-email?email=${encodeURIComponent(email)}`),
    me:                   ()          => request('/auth/me'),
    refreshToken:         ()          => request('/auth/refresh-token', { method: 'POST' }),
    logout:               ()          => request('/auth/logout', { method: 'POST' }),
  },
  children: {
    list:   ()          => request('/children'),
    mine:   ()          => request('/children/mine'),
    get:    id          => request(`/children/${id}`),
    create: data        => request('/children', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data)  => request(`/children/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: id          => request(`/children/${id}`, { method: 'DELETE' }),
  },
  attendance: {
    getByDate:  date    => request(`/attendance?date=${date}`),
    getByChild: childId => request(`/attendance/child/${childId}`),
    getRange:   (s, e)  => request(`/attendance/range?start=${s}&end=${e}`),
    mine:       ()      => request('/attendance/mine'),
    saveBulk:   records => request('/attendance/bulk', { method: 'POST', body: JSON.stringify(records) }),
  },
  health: {
    list:            ()  => request('/health-records'),
    getByChild: childId   => request(`/health-records/child/${childId}`),
    mine:            ()  => request('/health-records/mine'),
    create:        data  => request('/health-records', { method: 'POST', body: JSON.stringify(data) }),
    delete:          id  => request(`/health-records/${id}`, { method: 'DELETE' }),
    trash:           ()  => request('/health-records/trash'),
    restore:         id  => request(`/health-records/${id}/restore`, { method: 'PUT' }),
    permanentDelete: id  => request(`/health-records/${id}/permanent`, { method: 'DELETE' }),
  },
  immunizations: {
    schedule:        ()  => request('/immunizations/schedule'),
    list:            ()  => request('/immunizations'),
    getByChild: childId   => request(`/immunizations/child/${childId}`),
    mine:            ()  => request('/immunizations/mine'),
    create:        data  => request('/immunizations', { method: 'POST', body: JSON.stringify(data) }),
    delete:          id  => request(`/immunizations/${id}`, { method: 'DELETE' }),
    trash:           ()  => request('/immunizations/trash'),
    restore:         id  => request(`/immunizations/${id}/restore`, { method: 'PUT' }),
    permanentDelete: id  => request(`/immunizations/${id}/permanent`, { method: 'DELETE' }),
  },
  guardians: {
    list:          childId          => request(`/children/${childId}/guardians`),
    add:           (childId, data)  => request(`/children/${childId}/guardians`, { method: 'POST', body: JSON.stringify(data) }),
    delete:        (childId, id)    => request(`/children/${childId}/guardians/${id}`, { method: 'DELETE' }),
    listAccounts:  ()               => request('/guardians'),
    removeAccount: userId           => request(`/guardians/user/${userId}`, { method: 'DELETE' }),
  },
  reports: {
    monthly: month => request(`/reports/monthly?month=${month}`),
  },
  users: {
    list:           ()          => request('/users'),
    create:         data        => request('/users', { method: 'POST', body: JSON.stringify(data) }),
    updateRole:     (id, role)  => request(`/users/${id}/role`, { method: 'PUT', body: JSON.stringify({ role }) }),
    updateProfile:  (id, data)  => request(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    deactivate:     id          => request(`/users/${id}/deactivate`, { method: 'PUT' }),
    reactivate:     id          => request(`/users/${id}/reactivate`, { method: 'PUT' }),
    resetPassword:  id          => request(`/users/${id}/reset-password`, { method: 'PUT' }),
    delete:         id          => request(`/users/${id}`, { method: 'DELETE' }),
  },
  activity: {
    recent:  (size = 8) => request(`/activity-logs/recent?size=${size}`),
    search:  (params = {}) => {
      const qs = new URLSearchParams()
      Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') qs.set(k, v) })
      return request(`/activity-logs?${qs.toString()}`)
    },
    childHistory: (childId, page = 0, size = 20) => request(`/children/${childId}/history?page=${page}&size=${size}`),
  },
}
