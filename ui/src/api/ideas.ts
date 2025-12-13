import http, {uploadHttp} from './http'

export interface IdeaCategory {
    id: number;
    name: string;
    description?: string;
    parentId?: number
}

export interface CategoryNode {
    id: number;
    name: string;
    children?: CategoryNode[]
}

export interface IdeaTag {
    id: number;
    name: string
}

export interface IdeaTemplate {
    id: number
    name: string
    description?: string
    promptText: string
    categoryId?: number
    tipsNeedImage: boolean
    tipsNeedProModel: boolean
    tipsNeedEditPrompt: boolean
    originalImageRefs?: string
    effectImageRef?: string
    originalImageUris?: string[]
    effectImageUri?: string
    originalImageUrls?: string[]
    effectImageUrl?: string
    usageCount: number
    lastUsedTime?: string
    createTime?: string
    updateTime?: string
    isNew?: boolean
    isHot?: boolean
    categoryName?: string
    categoryPath?: string[]
    tagIds?: number[]
    tagNames?: string[]
}

export async function listIdeaCategories(): Promise<IdeaCategory[]> {
    const {data} = await http.get('/ideas/categories')
    const arr = ((data as any)?.data ?? data) as any[]
    return (arr || []).map((x: any) => ({
        id: Number(x.id ?? 0),
        name: String(x.name ?? ''),
        description: x.description ?? undefined,
        parentId: x.parent_id ?? x.parentId ?? undefined,
    }))
}

export async function listIdeaCategoryTree(): Promise<CategoryNode[]> {
    const {data} = await http.get('/ideas/categories/tree')
    return (data as any)?.data ?? data
}

export async function createIdeaCategory(name: string, description?: string, parentId?: number): Promise<IdeaCategory> {
    const body: any = {name, description}
    if (parentId != null) body.parent_id = parentId
    const {data} = await http.post('/ideas/category', body, {headers: {'Content-Type': 'application/json'}})
    const x = (data as any)?.data ?? data
    return {
        id: Number(x.id ?? 0),
        name: String(x.name ?? ''),
        description: x.description ?? undefined,
        parentId: x.parent_id ?? x.parentId ?? undefined,
    }
}

export async function updateIdeaCategory(id: number, name: string, description?: string, parentId?: number): Promise<IdeaCategory> {
    const body: any = {id, name, description}
    if (parentId != null) body.parent_id = parentId
    const {data} = await http.put('/ideas/category', body, {headers: {'Content-Type': 'application/json'}})
    const x = (data as any)?.data ?? data
    return {
        id: Number(x.id ?? id),
        name: String(x.name ?? name),
        description: x.description ?? description,
        parentId: x.parent_id ?? x.parentId ?? parentId,
    }
}

export async function deleteIdeaCategory(id: number): Promise<boolean> {
    const {data} = await http.delete(`/ideas/category/${id}`)
    return (data as any)?.data ?? data
}

export async function listIdeaTags(): Promise<IdeaTag[]> {
    const {data} = await http.get('/ideas/tags')
    return (data as any)?.data ?? data
}

export async function createIdeaTag(name: string): Promise<IdeaTag> {
    const {data} = await http.post('/ideas/tag', {name}, {headers: {'Content-Type': 'application/json'}})
    return (data as any)?.data ?? data
}

export async function updateIdeaTag(id: number, name: string): Promise<IdeaTag> {
    const {data} = await http.put('/ideas/tag', {id, name}, {headers: {'Content-Type': 'application/json'}})
    return (data as any)?.data ?? data
}

export async function deleteIdeaTag(id: number): Promise<boolean> {
    const {data} = await http.delete(`/ideas/tag/${id}`)
    return (data as any)?.data ?? data
}

export async function listIdeaTemplates(params?: {
    categoryIds?: number[];
    categoryId?: number;
    tagIds?: number[];
    sortBy?: 'updated' | 'hot';
    order?: 'asc' | 'desc'
}): Promise<IdeaTemplate[]> {
    const q: any = {}
    if (params?.categoryIds && params.categoryIds.length) q.categoryIds = params.categoryIds.join(',')
    else if (params?.categoryId) q.categoryId = params.categoryId
    if (params?.tagIds && params.tagIds.length) q.tagIds = params.tagIds.join(',')
    if (params?.sortBy) q.sortBy = params.sortBy
    if (params?.order) q.order = params.order
    const {data} = await http.get('/ideas/templates', {params: q})
    const arr = ((data as any)?.data ?? data) as any[]
    return (arr || []).map((x: any) => ({
        id: Number(x.id ?? 0),
        name: String(x.name ?? ''),
        description: x.description ?? undefined,
        promptText: String(x.prompt_text ?? x.promptText ?? ''),
        categoryId: x.category_id ?? x.categoryId ?? undefined,
        tipsNeedImage: !!(x.tips_need_image ?? x.tipsNeedImage),
        tipsNeedProModel: !!(x.tips_need_pro_model ?? x.tipsNeedProModel),
        tipsNeedEditPrompt: !!(x.tips_need_edit_prompt ?? x.tipsNeedEditPrompt),
        originalImageRefs: x.original_image_refs ?? x.originalImageRefs ?? undefined,
        effectImageRef: x.effect_image_ref ?? x.effectImageRef ?? undefined,
        usageCount: Number(x.usage_count ?? x.usageCount ?? 0),
        lastUsedTime: x.last_used_time ?? x.lastUsedTime ?? undefined,
        createTime: x.create_time ?? x.createTime ?? undefined,
        updateTime: x.update_time ?? x.updateTime ?? undefined,
        isNew: x.is_new ?? x.isNew ?? undefined,
        isHot: x.is_hot ?? x.isHot ?? undefined,
        categoryName: x.category_name ?? x.categoryName ?? undefined,
        categoryPath: Array.isArray(x.category_path ?? x.categoryPath) ? (x.category_path ?? x.categoryPath) : undefined,
        tagIds: Array.isArray(x.tag_ids ?? x.tagIds) ? (x.tag_ids ?? x.tagIds).map((v: any) => Number(v)) : [],
        tagNames: Array.isArray(x.tag_names ?? x.tagNames) ? (x.tag_names ?? x.tagNames) : [],
        originalImageUris: (() => {
            try {
                const raw = x.original_image_refs ?? x.originalImageRefs
                if (!raw) return []
                const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
                return Array.isArray(arr) ? arr.filter((s: any) => typeof s === 'string') : []
            } catch {
                return []
            }
        })(),
        effectImageUri: x.effect_image_ref ?? x.effectImageRef ?? undefined,
    }))
}

export async function getIdeaImageUrl(key: string): Promise<string> {
    const {data} = await http.get('/ideas/image-url', {params: {key}})
    const url = (data as any)?.data ?? data
    return String(url || '')
}

export async function createIdeaTemplate(payload: {
    name: string
    description?: string
    promptText: string
    categoryId?: number
    tipsNeedImage?: boolean
    tipsNeedProModel?: boolean
    tipsNeedEditPrompt?: boolean
    tagIds?: number[]
}): Promise<IdeaTemplate> {
    const body: any = {
        name: payload.name,
        description: payload.description,
        prompt_text: payload.promptText,
        category_id: payload.categoryId,
        tips_need_image: !!payload.tipsNeedImage,
        tips_need_pro_model: !!payload.tipsNeedProModel,
        tips_need_edit_prompt: !!payload.tipsNeedEditPrompt,
        tag_ids: (payload.tagIds || []).map((x) => Number(x))
    }
    const {data} = await http.post('/ideas/template', body, {headers: {'Content-Type': 'application/json'}})
    return (data as any)?.data ?? data
}

export async function updateIdeaTemplate(id: number, payload: {
    name: string
    description?: string
    promptText: string
    categoryId?: number
    tipsNeedImage?: boolean
    tipsNeedProModel?: boolean
    tipsNeedEditPrompt?: boolean
    tagIds?: number[]
}): Promise<IdeaTemplate> {
    const body: any = {
        name: payload.name,
        description: payload.description,
        prompt_text: payload.promptText,
        category_id: payload.categoryId,
        tips_need_image: !!payload.tipsNeedImage,
        tips_need_pro_model: !!payload.tipsNeedProModel,
        tips_need_edit_prompt: !!payload.tipsNeedEditPrompt,
        tag_ids: (payload.tagIds || []).map((x) => Number(x))
    }
    const {data} = await http.put(`/ideas/template/${id}`, body, {headers: {'Content-Type': 'application/json'}})
    return (data as any)?.data ?? data
}

export async function deleteIdeaTemplate(id: number): Promise<boolean> {
    const {data} = await http.delete(`/ideas/template/${id}`)
    return (data as any)?.data ?? data
}

export async function importIdeaTemplates(items: any[]): Promise<number> {
    const {data} = await http.post('/ideas/templates/import', items, {headers: {'Content-Type': 'application/json'}})
    return (data as any)?.data ?? data
}

export async function uploadIdeaOriginalImage(templateId: number, file: File): Promise<string> {
    const fd = new FormData()
    fd.append('image', file, (file as any).name || 'image')
    const {data} = await uploadHttp.post(`/ideas/template/${templateId}/upload-original`, fd)
    const uri = (data as any)?.data ?? data
    return String(uri || '')
}

export async function uploadIdeaEffectImage(templateId: number, file: File): Promise<string> {
    const fd = new FormData()
    fd.append('image', file, (file as any).name || 'image')
    const {data} = await uploadHttp.post(`/ideas/template/${templateId}/upload-effect`, fd)
    const uri = (data as any)?.data ?? data
    return String(uri || '')
}


export async function deleteIdeaOriginalImage(templateId: number, key: string): Promise<boolean> {
    const {data} = await http.delete(`/ideas/template/${templateId}/original`, {params: {key}})
    return (data as any)?.data ?? data
}

export async function deleteIdeaEffectImage(templateId: number): Promise<boolean> {
    const {data} = await http.delete(`/ideas/template/${templateId}/effect`)
    return (data as any)?.data ?? data
}

export async function markIdeaTemplateUsed(id: number): Promise<boolean> {
    const {data} = await http.post(`/ideas/template/${id}/use`)
    return (data as any)?.data ?? data
}
