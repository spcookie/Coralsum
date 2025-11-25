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
        resolution: '1x' | '2x' | '4x'
        outputFormat: 'PNG' | 'JPG' | 'WEBP'
    }
}

export interface GenerateResponse {
    inputTokens: number
    outputTokens: number
    durationMs: number
    images: string[]
    text?: string
}

export async function sendEmailCode(email: string): Promise<{ ok: true; code: string }> {
    const {data} = await http.post('/auth/send-code', {email})
    return data
}

export async function login(email: string, password: string) {
    const {data} = await http.post('/auth/login', {username: email, password})
    return data
}

export async function getProfile() {
    const {data} = await http.get('/web/profile', {headers: {'X-API-Version': 'v1'}})
    return {
        name: data.nick_name,
        email: data.source_code,
        tier: data.tier,
        points: data.permanent_points + data.subscribe_points,
    }
}

export async function refreshUserInfoByEmail() {
    return await getProfile()
}

export async function updateProfileName(email: string, name: string) {
    const {data} = await http.post('/user/update-name', {email, name})
    return data
}

export async function changePassword(email: string, oldPassword: string, newPassword: string, code: string) {
    const {data} = await http.post('/user/change-password', {email, oldPassword, newPassword, code})
    return data
}

export async function generate(req: GenerateRequest): Promise<GenerateResponse> {
    const fd = new FormData()
    const file = Array.isArray(req.inputImages) && req.inputImages.length > 0 ? req.inputImages[0] : null
    if (file) fd.append('image', file)
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
    const scaleMap: Record<string, string> = {'1x': 'X1', '2x': 'X2', '4x': 'X4'}
    const sc = scaleMap[req.config.resolution]
    if (sc) fd.append('upscaylScale', sc)
    const user = useUserStore()
    const headers: any = {'X-API-Version': 'v1'}
    if (user?.token) headers.Authorization = `Bearer ${user.token}`
    await http.post('/generative-image/submit-task', fd, {headers})
    const intervalMs = 1500
    const maxAttempts = Math.ceil((1000 * 60 * 3) / intervalMs)
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
                text: result.text
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

export async function register(email: string, password: string, code: string) {
    const {data} = await http.post('/auth/register', {email, password, code})
    return data
}

export async function fetchHistoryList() {
    const {data} = await http.get('/history/list')
    if (Array.isArray(data)) return data
    if (data && Array.isArray((data as any).list)) return (data as any).list
    return []
}

export async function fetchHistoryImages(id: string): Promise<string[]> {
    const {data} = await http.get('/history/images', {params: {id}})
    return Array.isArray(data) ? data : []
}

export async function redeemPoints(email: string, key: string): Promise<{ pointsAdded: number; points: number }> {
    const {data} = await http.post('/points/redeem', {email, key})
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
                text: r.text
            }
            : undefined
    }
}