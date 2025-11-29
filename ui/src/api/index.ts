import http from './http'
import {useUserStore} from '@/stores/user'

export interface GenerateRequest {
    prompt: string
    systemPrompt?: string
    inputImages: File[]
    config: {
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
}

export async function sendEmailCode(email: string, purpose?: 'REGISTER' | 'RESET'): Promise<{
    ok: true;
    code: string
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
        email: data.source_code,
        tier: data.tier,
        permanentPoints: Number(data.permanent_points || 0),
        subscribePoints: Number(data.subscribe_points || 0),
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
        email: data.source_code,
        tier: data.tier,
        permanentPoints: Number(data.permanent_points || 0),
        subscribePoints: Number(data.subscribe_points || 0),
        points: Number(data.permanent_points || 0) + Number(data.subscribe_points || 0),
    }
}

export async function changePassword(email: string, oldPassword: string, newPassword: string, code: string) {
    const {data} = await http.post('/user/change-password', {email, oldPassword, newPassword, code})
    return data
}

export async function generate(req: GenerateRequest): Promise<GenerateResponse> {
    const fd = new FormData()
    if (Array.isArray(req.inputImages) && req.inputImages.length > 0) {
        for (const f of req.inputImages) {
            if (f) fd.append('image', f, (f as any).name || 'image')
        }
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

export async function assessIntent(text: string): Promise<{ generateIntent: boolean; guideMessage: string }> {
    const headers: any = {'X-API-Version': 'v1', 'Content-Type': 'text/plain'}
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

export async function getImageShareLink(ref: string): Promise<string> {
    const headers: any = {'X-API-Version': 'v1'}
    const params: any = {ref}
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
    const {data} = await http.post('/auth/reset-password', {email, newPassword, code})
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
    subscribeDurationDays?: number
    validFrom?: string
    validTo?: string
}) {
    const {data} = await http.post('/ctl/points-keys/config', payload)
    return data
}

export async function listPointsKeyConfigs(params?: {
    page?: number
    size?: number
    name?: string
    sortBy?: 'id' | 'start' | 'end'
    order?: 'asc' | 'desc'
}) {
    const {data} = await http.get('/ctl/points-keys/configs', {params})
    return data
}

export async function generatePointsKeys(configId: number, count: number) {
    const {data} = await http.post('/ctl/points-keys/generate', {configId, count})
    return data
}

export async function listPointsKeys(params?: {
    page?: number
    size?: number
    key?: string
    order?: 'asc' | 'desc'
}) {
    const {data} = await http.get('/ctl/points-keys/keys', {params})
    return data
}

export async function togglePointsKeys(ids: number[], enabled: boolean) {
    const {data} = await http.post('/ctl/points-keys/toggle', {ids, enabled})
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
