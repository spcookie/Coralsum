import http from './http'
import {useUserStore} from '@/stores/user'

export interface GenerateRequest {
    prompt: string
    systemPrompt?: string
    inputImages: File[]
    config: {
        modelType: 'Basic' | 'Pro'
        candidateRadio: 1 | 2 | 3 | 4
        aspectRatio: string
        topP: number
        temperature: number
        resolution: '1x' | '3x' | '2x' | '4x'
        outputFormat: 'PNG' | 'JPG'
        imageSize: '1K' | '2K' | '4K'
        mediaResolution: 'auto' | 'low' | 'medium' | 'high'
    }
}

export interface GenerateResponse {
    inputTokens: number
    outputTokens: number
    durationMs: number
    images: string[]
    text?: string
    linkImages: any
}

export async function sendEmailCode(email: string, purpose?: 'REGISTER' | 'RESET'): Promise<{
    ok: true
}> {
    const {data} = await http.post('/auth/send-code', {email, purpose})
    return data
}

export async function login(email: string, password: string) {
    const {data} = await http.post('/auth/login', {username: email, password})
    return data
}

export async function getProfile() {
    const {data} = await http.get('/web/profile', {headers: {'X-API-Version': 'v1'}})
    return {
        uid: data.uid,
        permissions: Array.isArray(data.permissions) ? data.permissions : [],
        name: data.nick_name,
        nickTag: typeof data.nick_tag === 'number' ? data.nick_tag : null,
        email: data.source_code,
        tier: data.tier,
        permanentPoints: Number(data.permanent_points || 0),
        subscribePoints: Number(data.subscribe_points || 0),
        subscribeExpireTime: typeof data.subscribe_expire_time === 'number' ? Number(data.subscribe_expire_time) : null,
        points: Number(data.permanent_points || 0) + Number(data.subscribe_points || 0),
    }
}

export async function refreshUserInfoByEmail() {
    return await getProfile()
}

export async function updateProfileName(name: string) {
    const headers: any = {'X-API-Version': 'v1'}
    const {data} = await http.put('/web/profile', {nick_name: name}, {headers})
    return {
        uid: data.uid,
        permissions: Array.isArray(data.permissions) ? data.permissions : [],
        name: data.nick_name,
        nickTag: typeof data.nick_tag === 'number' ? data.nick_tag : null,
        email: data.source_code,
        tier: data.tier,
        permanentPoints: Number(data.permanent_points || 0),
        subscribePoints: Number(data.subscribe_points || 0),
        subscribeExpireTime: typeof data.subscribe_expire_time === 'number' ? Number(data.subscribe_expire_time) : null,
        points: Number(data.permanent_points || 0) + Number(data.subscribe_points || 0),
    }
}

export async function changePassword(email: string, oldPassword: string, newPassword: string, code: string) {
    const {data} = await http.post('/user/change-password', {
        email,
        old_password: oldPassword,
        new_password: newPassword,
        code
    })
    return data
}

export async function generate(req: GenerateRequest): Promise<GenerateResponse> {
    const fd = new FormData()
    if (Array.isArray(req.inputImages) && req.inputImages.length > 0) {
        const files = req.inputImages.filter(Boolean)
        let sid: string | undefined
        for (const f of files) {
            sid = await uploadImage(f, sid)
        }
        if (sid) fd.append('sid', sid)
    }
    fd.append('text', req.prompt)
    if (req.systemPrompt) fd.append('system', req.systemPrompt)
    const arMap: Record<string, string> = {
        '1:1': 'R1_1',
        '2:3': 'R2_3',
        '3:2': 'R3_2',
        '3:4': 'R3_4',
        '4:3': 'R4_3',
        '9:16': 'R9_16',
        '16:9': 'R16_9',
        '21:9': 'R21_9'
    }
    const ar = arMap[req.config.aspectRatio]
    if (ar) fd.append('aspectRatio', ar)
    const mtMap: Record<string, string> = {Basic: 'BASIC', Pro: 'PRO'}
    const mt = mtMap[req.config.modelType]
    if (mt) fd.append('modelType', mt)
    fd.append('candidateCount', String(req.config.candidateRadio))
    const temp = Math.max(0, Math.min(2, req.config.temperature))
    const topp = Math.max(0, Math.min(1, req.config.topP))
    fd.append('temperature', String(temp))
    fd.append('topP', String(topp))
    fd.append('format', req.config.outputFormat)
    const scaleMap: Record<string, string> = {'1x': 'X1', '2x': 'X2', '3x': 'X3', '4x': 'X4'}
    const sc = scaleMap[req.config.resolution]
    if (sc) fd.append('upscaylScale', sc)
    const sizeMap: Record<string, string> = {'1K': 'X1', '2K': 'X2', '4K': 'X4'}
    const sz = sizeMap[req.config.imageSize]
    if (sz) fd.append('imageSize', sz)
    const mrMap: Record<string, string> = {auto: 'AUTO', low: 'LOW', medium: 'MEDIUM', high: 'HIGH'}
    const mr = mrMap[req.config.mediaResolution]
    if (mr) fd.append('mediaResolution', mr)
    
    const user = useUserStore()
    const headers: any = {'X-API-Version': 'v1'}
    if (user?.token) headers.Authorization = `Bearer ${user.token}`
    await http.post('/generative-image/submit-task', fd, {headers})
    const intervalMs = 1500
    const maxAttempts = Math.ceil((1000 * 60 * 5) / intervalMs)
    for (let i = 0; i < maxAttempts; i++) {
        await new Promise((r) => setTimeout(r, intervalMs))
        const {data} = await http.get('/generative-image/get-task-result', {headers})
        const status = (data as any)?.status as string
        if (status === 'COMPLETED') {
            const result = (data as any)?.result || {}
            return {
                durationMs: result.duration_ms ?? result.durationMs,
                inputTokens: result.input_tokens ?? result.inputTokens,
                outputTokens: result.output_tokens ?? result.outputTokens,
                images: result.images || [],
                text: result.text,
                linkImages: result.link_images ?? result.linkImages ?? []
            }
        }
        if (status === 'FAILED') {
            throw {message: '生成任务失败', status: 500}
        }
        if (status === 'NONE') {
            throw {message: '任务不存在或已失效', status: 400}
        }
    }
    throw {message: '生成超时，请稍后重试', status: 504}
}

export async function uploadImage(file: File, sid?: string): Promise<string> {
    const fd = new FormData()
    fd.append('image', file, (file as any).name || 'image')
    if (sid) fd.append('sid', sid)
    const headers: any = {'X-API-Version': 'v1'}
    const user = useUserStore()
    if (user?.token) headers.Authorization = `Bearer ${user.token}`
    const {data} = await http.post('/generative-image/upload', fd, {headers})
    const sessionId = (data?.data ?? data?.sid ?? data) as any
    return String(sessionId || '')
}

export async function assessIntent(text: string): Promise<{ generateIntent: boolean; guideMessage: string }> {
    const headers: any = {'X-API-Version': 'v1', 'Content-Type': 'text/plain'}
    const user = useUserStore()
    if (user?.token) headers.Authorization = `Bearer ${user.token}`
    const {data} = await http.post('/generative-image/assess-intent', text, {headers})
    return {
        generateIntent: data.generate_intent,
        guideMessage: data.guide_message
    }
}

export async function getImageBytes(ref: string, pt?: string): Promise<Blob> {
    const headers: any = {'X-API-Version': 'v1'}
    const params: any = {ref}
    if (pt) params.pt = pt
    const {data} = await http.get('/generative-image/bytes', {params, headers, responseType: 'blob'})
    return data as Blob
}

export async function getPreviewBytes(ref: string, pt?: string): Promise<Blob> {
    const headers: any = {'X-API-Version': 'v1'}
    const params: any = {ref}
    if (pt) params.pt = pt
    const {data} = await http.get('/generative-image', {params, headers, responseType: 'blob'})
    return data as Blob
}

export async function getImageShareLink(ref: string, darkMode?: boolean): Promise<string> {
    const headers: any = {'X-API-Version': 'v1'}
    try {
        const user = useUserStore()
        if (user?.token) headers.Authorization = `Bearer ${user.token}`
    } catch {
    }
    const params: any = {ref}
    if (typeof darkMode === 'boolean') params.darkMode = darkMode
    const {data} = await http.get('/generative-image/link', {params, headers})
    if (data && typeof data === 'object') {
        const url = (data as any).url ?? (data as any).link ?? ''
        return String(url || '')
    }
    return String(data || '')
}

export async function getExternalBlob(url: string): Promise<Blob> {
    const {data} = await http.get(url, {responseType: 'blob'})
    return data as Blob
}

export async function register(email: string, password: string, code: string) {
    const {data} = await http.post('/auth/register', {email, password, code})
    return data
}

export async function resetPassword(email: string, newPassword: string, code: string) {
    const {data} = await http.post('/auth/reset-password', {email, new_password: newPassword, code})
    return data
}

// 历史记录模块已移除

export async function redeemPoints(email: string, key: string): Promise<{ pointsAdded: number; points: number }> {
    const {data} = await http.post('/points/redeem', {email, key})
    return data
}

// CTL 管理接口
export async function createPointsKeyConfig(payload: {
    name: string
    permanentPoints?: number
    subscribePoints?: number
    subscribeType?: string
    periodUnit?: string
    periodCount?: number
}) {
    const body: any = {
        name: payload.name,
        permanent_points: payload.permanentPoints,
        subscribe_points: payload.subscribePoints,
        subscribe_type: payload.subscribeType,
        period_unit: payload.periodUnit,
        period_count: payload.periodCount
    }
    const {data} = await http.post('/ctl/points-keys/config', body)
    return data
}

export async function listPointsKeyConfigs(params?: {
    page?: number
    size?: number
    name?: string
    sortBy?: 'id' | 'status'
    order?: 'asc' | 'desc'
}) {
    const {data} = await http.get('/ctl/points-keys/configs', {params})
    return data
}

export async function generatePointsKeys(configId: number, count: number) {
    const {data} = await http.post('/ctl/points-keys/generate', {config_id: configId, count})
    return data
}

export async function listPointsKeys(params?: {
    page?: number
    size?: number
    key?: string
    sortBy?: 'status' | 'used'
    order?: 'asc' | 'desc'
}) {
    const {data} = await http.get('/ctl/points-keys/keys', {params})
    return data
}

export async function togglePointsKeys(ids: number[], enabled: boolean) {
    const {data} = await http.post('/ctl/points-keys/toggle', {ids, enabled})
    return data
}

export async function togglePointsKeyConfig(id: number, disabled: boolean) {
    const {data} = await http.post('/ctl/points-keys/config/toggle', {id, disabled})
    return data
}

export async function getGenerateTaskResult(): Promise<{
    status: 'COMPLETED' | 'PROCESSING' | 'NONE' | 'FAILED' | 'FAIL';
    result?: GenerateResponse
}> {
    const headers: any = {'X-API-Version': 'v1'}
    const {data} = await http.get('/generative-image/get-task-result', {headers})
    const status = (data as any)?.status as 'COMPLETED' | 'PROCESSING' | 'NONE' | 'FAILED' | 'FAIL'
    const r = (data as any)?.result
    return {
        status,
        result: r
            ? {
                durationMs: r.duration_ms ?? r.durationMs,
                inputTokens: r.input_tokens ?? r.inputTokens,
                outputTokens: r.output_tokens ?? r.outputTokens,
                images: r.images || [],
                text: r.text,
                linkImages: r.link_images ?? r.linkImages ?? []
            }
            : undefined
    }
}

export async function getEstimateParams(): Promise<{
    usdToCny: number
    coefficient: number
    pointsPerRmb: number
    flashLiteTokensPerChar: number
    imagePreviewTokensPerMb: number
    flashLiteInputUsdPerMTokens: number
    flashLiteOutputUsdPerMTokens: number
    proInputUsdPerMTokens: number
    proOutputUsdPerMTokens: number
    imagePricePerResolutionUsd: Record<string, number>
    estimatedBytesPerImage: Record<string, number>
    ossBusyRmbPerGb: number
    ossIdleRmbPerGb: number
    trafficNatRmbPerGb: number
    trafficProxyRmbPerGb: number
    trafficVisitMultiplier: number
    upscaylEnabled: boolean
    upscaylChargeByScale: boolean
    basicInputUsdPerMTokens: number
    basicOutputUsdPerMTokens: number
    basicOutputPricePerImage1kUsd: number
}> {
    const headers: any = {'X-API-Version': 'v1'}
    const user = useUserStore()
    if (user?.token) headers.Authorization = `Bearer ${user.token}`
    const {data} = await http.get('/points/estimate-params', {headers})
    const payload: any = (data && typeof data === 'object' && 'data' in data) ? (data as any).data : data
    const imagePricePerResolutionUsd: Record<string, number> = {
        '1K': Number(payload.pro_price_per_image1k2k_usd ?? 0.134),
        '2K': Number(payload.pro_price_per_image1k2k_usd ?? 0.134),
        '4K': Number(payload.pro_price_per_image4k_usd ?? 0.24)
    }
    const estimatedBytesPerImage: Record<string, number> = {
        'PNG:1K': Number(payload.estimate_png_bytes1_k ?? 0),
        'PNG:2K': Number(payload.estimate_png_bytes2_k ?? 0),
        'PNG:4K': Number(payload.estimate_png_bytes4_k ?? 0),
        'JPG:1K': Number(payload.estimate_jpg_bytes1_k ?? 0),
        'JPG:2K': Number(payload.estimate_jpg_bytes2_k ?? 0),
        'JPG:4K': Number(payload.estimate_jpg_bytes4_k ?? 0)
    }
    return {
        usdToCny: Number(payload.usd_to_cny ?? 0),
        coefficient: Number(payload.coefficient ?? 1),
        pointsPerRmb: Number(payload.points_per_rmb ?? 100),
        flashLiteTokensPerChar: Number(payload.flash_lite_tokens_per_char ?? 2.5),
        imagePreviewTokensPerMb: Number(payload.image_preview_tokens_per_mb ?? 2500),
        flashLiteInputUsdPerMTokens: Number(payload.flash_lite_input_usd_per_mtokens ?? 0),
        flashLiteOutputUsdPerMTokens: Number(payload.flash_lite_output_usd_per_mtokens ?? 0),
        proInputUsdPerMTokens: Number(payload.pro_input_usd_per_mtokens ?? 0),
        proOutputUsdPerMTokens: Number(payload.pro_output_usd_per_mtokens ?? 0),
        imagePricePerResolutionUsd,
        estimatedBytesPerImage,
        ossBusyRmbPerGb: Number(payload.oss_busy_rmb_per_gb ?? 0),
        ossIdleRmbPerGb: Number(payload.oss_idle_rmb_per_gb ?? 0),
        trafficNatRmbPerGb: Number(payload.traffic_nat_rmb_per_gb ?? 0),
        trafficProxyRmbPerGb: Number(payload.traffic_proxy_rmb_per_gb ?? 0),
        trafficVisitMultiplier: Number(payload.traffic_visit_multiplier ?? 1),
        upscaylEnabled: !!payload.upscayl_enabled,
        upscaylChargeByScale: !!payload.upscayl_charge_by_scale,
        basicInputUsdPerMTokens: Number(payload.basic_input_usd_per_mtokens ?? 0.3),
        basicOutputUsdPerMTokens: Number(payload.basic_output_usd_per_mtokens ?? 0.4),
        basicOutputPricePerImage1kUsd: Number(payload.basic_output_price_per_image1k_usd ?? 0.039)
    }
}
