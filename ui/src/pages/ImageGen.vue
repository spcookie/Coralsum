<template>
  <div class="flex flex-col md:flex-row md:h-[calc(100vh-56px)]">
    <LeftConfigPanel :generating="btnLoading" @generate="doGenerate"/>
    <div class="relative z-0 isolate flex-1 flex flex-col md:h-full glass bg-white/40 dark:bg-black/30">
      <div class="flex-1 overflow-y-auto">
        <RightResultPanel :loading="loading" :result="result" @save-history="onSaveHistory"/>
      </div>
      <div class="shrink-0 py-2 md:pb-0 pb-[env(safe-area-inset-bottom)]">
        <div class="flex flex-col items-center justify-center text-sm text-neutral-600 dark:text-neutral-300">
          <div class="flex items-center justify-center text-sm font-medium text-neutral-700 dark:text-neutral-200">
            <span class="mr-1">Powered by</span>
            <svg aria-hidden="true" class="mx-1 w-5 h-5 text-red-500 animate-heart-bounce" fill="currentColor"
                 viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path
                  d="M12 21l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41.81 4.5 2.09C12.09 4.81 13.76 4 15.5 4 18 4 20 6 20 8.5c0 3.78-3.4 6.86-8.55 11.18L12 21z"/>
            </svg>
            <span class="ml-1">Vue.js | Micronaut | Kotlin</span>
          </div>
          <div class="mt-1 text-xs text-neutral-500 dark:text-neutral-400">Model Crafted by Nano Banana Pro 🍌</div>
          <div class="mt-2 w-full px-4 text-center break-words text-xs text-neutral-500 dark:text-neutral-400">
            Copyright © 2025 Spcookie. 项目开源，采用
            <a class="underline" href="https://opensource.org/licenses/MIT" rel="noopener noreferrer" target="_blank">MIT
              许可证</a>。
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, onUnmounted, ref, watch} from 'vue'
import LeftConfigPanel from '@/components/LeftConfigPanel.vue'
import RightResultPanel from '@/components/RightResultPanel.vue'
import {useSettingsStore} from '@/stores/settings'
import {assessIntent, generate, getGenerateTaskResult, refreshUserInfoByEmail} from '@/api'
import {useUserStore} from '@/stores/user'
import {useMessage} from 'naive-ui'
import {addHistoryFromResult} from '@/utils/indexedDb'

const settings = useSettingsStore()
const user = useUserStore()
const result = ref<any>()
const loading = ref(false)
const btnLoading = ref(false)
const message = useMessage()
const polling = ref(false)
const currentBlobUrls = ref<string[]>([])

function ratioWH(baseW = 512) {
  const ar = settings.aspectRatio
  const map: Record<string, [number, number]> = {
    '1:1': [1, 1],
    '2:3': [2, 3],
    '3:2': [3, 2],
    '3:4': [3, 4],
    '4:3': [4, 3],
    '9:16': [9, 16],
    '16:9': [16, 9],
    '21:9': [21, 9]
  }
  const [rw, rh] = map[ar] ?? map['1:1']
  const w = baseW
  const h = Math.round(baseW * (rh / rw))
  return {w, h}
}

function themedPlaceholder(width: number, height: number, label: string) {
  const dark = typeof document !== 'undefined' && document.documentElement.classList.contains('dark')
  const bg = dark ? '#1f2937' : '#f3f4f6'
  const border = dark ? '#374151' : '#d1d5db'
  const text = dark ? '#9ca3af' : '#6b7280'
  const g1 = dark ? '#0ea5e9' : '#93c5fd'
  const g2 = dark ? '#8b5cf6' : '#a5b4fc'
  const fs = Math.max(12, Math.floor(height * 0.08))
  const sub = Math.max(10, Math.floor(height * 0.06))
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='${width}' height='${height}' viewBox='0 0 ${width} ${height}'>
<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0' stop-color='${g1}'/><stop offset='1' stop-color='${g2}'/></linearGradient></defs>
<rect width='100%' height='100%' fill='${bg}'/>
<rect x='0.5' y='0.5' width='${width - 1}' height='${height - 1}' fill='none' stroke='${border}' stroke-dasharray='6 6' rx='8'/>
<rect width='100%' height='100%' fill='url(#g)' opacity='0.08'/>
<g fill='${text}' font-family='system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif' text-anchor='middle'>
<text x='${width / 2}' y='${height / 2 - 6}' font-size='${fs}'>${label}</text>
<text x='${width / 2}' y='${height / 2 + fs / 2 + 16}' font-size='${sub}'>图片预览已失效</text>
</g>
</svg>`
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg)
}

function themedPlaceholderImage(index: number) {
  const {w, h} = ratioWH(512)
  return themedPlaceholder(w, h, `IMG${index + 1}`)
}

async function convertImagesToBlobUrls(imgs: string[]): Promise<string[]> {
  const out: string[] = []
  for (const u of imgs) {
    try {
      if (!u) {
        out.push('')
        continue
      }
      const {refParam, ptParam} = (() => {
        try {
          const url = new URL(u, window.location.origin)
          const q = url.searchParams.get('ref')
          const pt = url.searchParams.get('pt') || undefined
          if (q) return {refParam: q, ptParam: pt}
          const path = url.pathname.split('/').pop() || ''
          return {refParam: path, ptParam: pt}
        } catch {
          const idxQ = u.indexOf('ref=')
          const idxPt = u.indexOf('&pt=')
          const pt = idxPt >= 0 ? decodeURIComponent(u.slice(idxPt + 4)) : undefined
          if (idxQ >= 0) return {refParam: decodeURIComponent(u.slice(idxQ + 4)), ptParam: pt}
          const noQuery = u.split('?')[0]
          return {refParam: noQuery.split('/').pop() || '', ptParam: pt}
        }
      })()
      if (refParam) {
        const url = `/api/generative-image?ref=${encodeURIComponent(refParam)}${ptParam ? `&pt=${encodeURIComponent(ptParam)}` : ''}`
        out.push(url)
        continue
      }
      out.push(u)
    } catch {
      // 失败使用占位图，避免不可用直链
      out.push(themedPlaceholderImage(out.length))
    }
  }
  return out
}

async function buildDataUrls(imgs: string[]): Promise<string[]> {
  const out: string[] = []
  for (const u of imgs) {
    try {
      if (!u) {
        out.push('');
        continue
      }
      const dataUrlViaCanvas = await (async () => {
        const img = new Image()
        img.crossOrigin = 'anonymous'
        const p = new Promise<void>((resolve, reject) => {
          img.onload = () => resolve()
          img.onerror = () => reject(new Error('img load failed'))
        })
        img.src = u
        await p
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        if (!ctx) throw new Error('no canvas ctx')
        canvas.width = img.naturalWidth || img.width
        canvas.height = img.naturalHeight || img.height
        ctx.drawImage(img, 0, 0)
        return canvas.toDataURL('image/png')
      })()
      out.push(dataUrlViaCanvas)
    } catch {
      const {refParam, ptParam} = (() => {
        try {
          const url = new URL(u, window.location.origin)
          const q = url.searchParams.get('ref')
          const pt = url.searchParams.get('pt') || undefined
          if (q) return {refParam: q, ptParam: pt}
          const path = url.pathname.split('/').pop() || ''
          return {refParam: path, ptParam: pt}
        } catch {
          const idxQ = u.indexOf('ref=')
          const idxPt = u.indexOf('&pt=')
          const pt = idxPt >= 0 ? decodeURIComponent(u.slice(idxPt + 4)) : undefined
          if (idxQ >= 0) return {refParam: decodeURIComponent(u.slice(idxQ + 4)), ptParam: pt}
          const noQuery = u.split('?')[0]
          return {refParam: noQuery.split('/').pop() || '', ptParam: pt}
        }
      })()
      if (refParam) {
        try {
          const b = await getImageBytes(refParam, ptParam)
          const dataUrl = await new Promise<string>((resolve, reject) => {
            const r = new FileReader()
            r.onload = () => resolve(String(r.result || ''))
            r.onerror = () => reject(r.error)
            r.readAsDataURL(b)
          })
          out.push(dataUrl)
          continue
        } catch {
        }
      }
      out.push('')
    }
  }
  return out
}

function revokeCurrentBlobUrls() {
  try {
    for (const u of currentBlobUrls.value) {
      if (u && u.startsWith('blob:')) URL.revokeObjectURL(u)
    }
  } catch {
  }
  currentBlobUrls.value = []
}

const lastGenerateAt = ref(0)

async function doGenerate(payload: { prompt: string; systemPrompt?: string; files: File[] }) {
  const now = Date.now()
  if (btnLoading.value) return
  if (now - lastGenerateAt.value < 500) return
  lastGenerateAt.value = now
  result.value = undefined
  btnLoading.value = true
  try {
    const intent = await assessIntent(payload.prompt)
    if (!intent.generateIntent) {
      result.value = {images: [], text: intent.guideMessage, inputTokens: 0, outputTokens: 0, durationMs: 0}
      return
    }
    if (!user.profileReady) {
      message.error('请先登录')
      user.requireLogin()
      return
    }
    loading.value = true
    const r = await generate({
      prompt: payload.prompt,
      systemPrompt: payload.systemPrompt,
      inputImages: payload.files,
      config: {
        candidateRadio: settings.candidateRadio,
        aspectRatio: settings.aspectRatio,
        topP: settings.topP,
        temperature: settings.temperature,
        resolution: settings.resolution,
        outputFormat: settings.outputFormat,
        imageSize: settings.imageSize,
        mediaResolution: settings.mediaResolution
      }
    })
    try {
      revokeCurrentBlobUrls()
      const urls = await convertImagesToBlobUrls(r.images || [])
      currentBlobUrls.value = urls
      result.value = {...r, images: urls}
    } catch {
      result.value = r
    }
    if (user.email) {
      const u = await refreshUserInfoByEmail()
      user.setProfile({...u, token: user.token})
    }

  } catch (e: any) {
    const msg = e?.message || '服务异常'
    // 模拟状态码处理
    const status = e?.status
    if (status === 401 || status === 403) {
      message.error('未登录或未授权，请先登录')
      user.requireLogin()
    } else if (status === 500) {
      message.error('服务异常，请稍后重试')
    } else {
      message.error(msg)
    }
  } finally {
    loading.value = false
    btnLoading.value = false
  }
}

// 历史记录模块已移除

async function syncGenerateStatus() {
  try {
    if (!user.profileReady) return
    const res = await getGenerateTaskResult()
    if (res.status === 'PROCESSING') {
      if (!polling.value) {
        polling.value = true
        await pollTaskResultLoop()
        polling.value = false
      }
      return
    }
    loading.value = false
    if (res.status === 'COMPLETED' && res.result) {
      try {
        revokeCurrentBlobUrls()
        const urls = await convertImagesToBlobUrls(res.result.images || [])
        currentBlobUrls.value = urls
        result.value = {...res.result, images: urls}
      } catch {
        result.value = res.result
      }
    }
    if (res.status === 'FAILED' || res.status === 'FAIL') {
      message.error('生成任务失败')
    }
  } catch {
  }
}

async function pollTaskResultLoop() {
  try {
    loading.value = true
    const intervalMs = 1500
    const maxAttempts = Math.ceil((1000 * 60 * 5) / intervalMs)
    for (let i = 0; i < maxAttempts; i++) {
      await new Promise(r => setTimeout(r, intervalMs))
      const res = await getGenerateTaskResult()
      if (res.status === 'COMPLETED' && res.result) {
        try {
          revokeCurrentBlobUrls()
          const urls = await convertImagesToBlobUrls(res.result.images || [])
          currentBlobUrls.value = urls
          result.value = {...res.result, images: urls}
        } catch {
          result.value = res.result
        }
        loading.value = false
        try {
          const u = await refreshUserInfoByEmail()
          user.setProfile({...u, token: user.token})
        } catch {
        }

        return
      }
      if (res.status === 'FAILED' || res.status === 'FAIL') {
        loading.value = false
        message.error('生成任务失败')
        return
      }
      if (res.status === 'NONE') {
        loading.value = false
        return
      }
    }
    loading.value = false
    message.error('生成超时，请稍后重试')
  } catch (e: any) {
    loading.value = false
    const status = e?.status
    if (status === 401 || status === 403) {
      message.error('未登录或未授权，请先登录')
      user.requireLogin()
    } else if (status === 500) {
      message.error('服务异常，请稍后重试')
    }
  }
}

onMounted(() => {
  syncGenerateStatus()
})

function onSaveHistory(dataUrls: string[]) {
  if (!result.value) return
  const cfg = {
    candidateRadio: settings.candidateRadio,
    aspectRatio: settings.aspectRatio,
    topP: settings.topP,
    temperature: settings.temperature,
    resolution: settings.resolution,
    outputFormat: settings.outputFormat,
    imageSize: settings.imageSize,
    mediaResolution: settings.mediaResolution
  }
  addHistoryFromResult(user.email || '', {
    prompt: '',
    systemPrompt: undefined,
    config: cfg,
    result: {...result.value, images: dataUrls}
  })
}

onUnmounted(() => {
  revokeCurrentBlobUrls()
})

watch(() => user.profileReady, (ready) => {
  if (ready) syncGenerateStatus()
})

</script>

<style scoped>
@keyframes heart-bounce {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  10% {
    transform: translateY(-6px) scaleX(1.06) scaleY(0.94);
  }
  20% {
    transform: translateY(0) scaleX(0.95) scaleY(1.05);
  }
  28% {
    transform: translateY(-3px) scaleX(1.03) scaleY(0.97);
  }
  34% {
    transform: translateY(0) scaleX(0.98) scaleY(1.02);
  }
  40%, 100% {
    transform: translateY(0) scale(1);
  }
}

.animate-heart-bounce {
  animation: heart-bounce 1.6s cubic-bezier(0.22, 0.61, 0.36, 1) infinite;
  will-change: transform;
}
</style>
