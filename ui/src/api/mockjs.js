import Mock from 'mockjs'

Mock.setup({timeout: '300-900'})

const mem = {
    usersByEmail: new Map(),
    emailCodes: new Map(),
    passwords: new Map()
}

function genPoints() {
    return Math.floor(500 + Math.random() * 1500)
}

function genTier() {
    return ['FREE', 'PLUS', 'PRO'][Math.floor(Math.random() * 3)]
}

function genEmailCode() {
    return String(Math.floor(100000 + Math.random() * 900000))
}

function genToken() {
    return Math.random().toString(36).slice(2)
}

function placeholderImage(index) {
    const w = 512, h = 320;
    return `https://placehold.co/${w}x${h}?text=IMG${index + 1}`
}

Mock.mock(/\/api\/auth\/send-code$/, 'post', (opts) => {
    const {email} = JSON.parse(opts.body || '{}')
    const code = genEmailCode()
    mem.emailCodes.set(email, {code, expiresAt: Date.now() + 10 * 60 * 1000})
    return {code: 'SUCCESS', message: '', data: {ok: true, code}}
})

Mock.mock(/\/api\/auth\/register$/, 'post', (opts) => {
    const {email, password, code} = JSON.parse(opts.body || '{}')
    const rec = mem.emailCodes.get(email)
    if (!email || !password || !code || !rec || rec.expiresAt < Date.now() || rec.code !== code) {
        return {status: 400, message: '注册信息无效或验证码错误'}
    }
    mem.passwords.set(email, password)
    let info = mem.usersByEmail.get(email)
    if (!info) {
        info = {email, name: email.split('@')[0], points: genPoints(), tier: genTier(), token: genToken()}
        mem.usersByEmail.set(email, info)
    }
    return {code: 'SUCCESS', message: '', data: {ok: true}}
})

// Mock.mock(/\/api\/auth\/login$/, 'post', (opts) => {
//   const { username, password } = JSON.parse(opts.body || '{}')
//   const stored = mem.passwords.get(username)
//   if (!username || !password || !stored || stored !== password) {
//     return { status: 401, message: '用户名或密码错误' }
//   }
//   const token = genToken()
//   return { code: 'SUCCESS', message: '', data: { access_token: token } }
// })

// Mock.mock(/\/api\/auth\/login$/, 'post', (opts) => {
//   const { email, password, code } = JSON.parse(opts.body || '{}')
//   const rec = mem.emailCodes.get(email)
//   if (!email || !password || !rec || rec.expiresAt < Date.now() || rec.code !== code) {
//     return { status: 401, message: '登录信息无效' }
//   }
//   let info = mem.usersByEmail.get(email)
//   if (!info) {
//     info = { email, name: email.split('@')[0], points: genPoints(), tier: genTier(), token: genToken() }
//     mem.usersByEmail.set(email, info)
//   }
//   return { code: 'SUCCESS', message: '', data: info }
// })

// Mock.mock(/\/api\/user\/profile$/, 'get', (opts) => {
//   const url = new URL(opts.url, location.origin)
//   const email = url.searchParams.get('email')
//   if (!email) {
//     return { status: 401, message: '未登录' }
//   }
//   let info = mem.usersByEmail.get(email)
//   if (!info) {
//     const name = email.includes('@') ? email.split('@')[0] : email
//     info = { email, name, points: genPoints(), tier: genTier(), token: genToken() }
//     mem.usersByEmail.set(email, info)
//   }
//   return { code: 'SUCCESS', message: '', data: info }
// })

Mock.mock(/\/api\/user\/update-name$/, 'post', (opts) => {
    const {email, name} = JSON.parse(opts.body || '{}')
    let info = mem.usersByEmail.get(email)
    if (!info) {
        info = {email, name, points: genPoints(), tier: genTier(), token: genToken()}
        mem.usersByEmail.set(email, info)
    } else {
        info.name = name
    }
    return {code: 'SUCCESS', message: '', data: info}
})

Mock.mock(/\/api\/user\/change-password$/, 'post', (opts) => {
    const {email, oldPassword, newPassword, code} = JSON.parse(opts.body || '{}')
    const rec = mem.emailCodes.get(email)
    const stored = mem.passwords.get(email)
    if (!email || !oldPassword || !newPassword || !code || !rec || rec.expiresAt < Date.now() || rec.code !== code) {
        return {status: 400, message: '信息不完整或验证码无效'}
    }
    if (stored && stored !== oldPassword) {
        return {status: 403, message: '原密码错误'}
    }
    mem.passwords.set(email, newPassword)
    return {code: 'SUCCESS', message: '', data: {ok: true}}
})

// Mock.mock(/\/api\/generate$/, 'post', (opts) => {
//   const req = JSON.parse(opts.body || '{}')
//   const n = Math.max(1, Math.min(req?.config?.candidateRadio || 1, 4))
//   const images = Array.from({ length: n }, (_, i) => placeholderImage(i))
//   const inputTokens = 64 + Math.min(2048, (req?.prompt || '').length)
//   const outputTokens = 128 * n
//   const text = req?.prompt ? `为「${req.prompt}」生成了 ${n} 张图（${req?.config?.outputFormat || 'PNG'}）` : ''
//   const durationMs = 800 + Math.floor(Math.random() * 400)
//   return { code: 'SUCCESS', message: '', data: { inputTokens, outputTokens, durationMs, images, text } }
// })

Mock.mock(/\/api\/history\/list$/, 'get', () => {
    const now = Date.now()
    const list = Array.from({length: 5}, (_, i) => ({
        id: String(now - i * 60000),
        createdAt: now - i * 60000,
        inputTokens: 64 + i * 10,
        outputTokens: 256 + i * 64,
        durationMs: 800 + i * 120,
        images: [],
        text: `历史记录 #${i + 1}`
    }))
    return {code: 'SUCCESS', message: '', data: list}
})

Mock.mock(/\/api\/history\/images$/, 'get', (opts) => {
    return {
        code: 'SUCCESS',
        message: '',
        data: [placeholderImage(0), placeholderImage(1), placeholderImage(2), placeholderImage(3)]
    }
})

Mock.mock(/\/api\/points\/redeem$/, 'post', (opts) => {
    const {email, key} = JSON.parse(opts.body || '{}')
    if (!email || !key) {
        return {status: 400, message: '信息不完整'}
    }
    let info = mem.usersByEmail.get(email)
    if (!info) {
        return {status: 401, message: '未登录'}
    }
    const add = 100
    info.points += add
    return {code: 'SUCCESS', message: '', data: {pointsAdded: add, points: info.points}}
})