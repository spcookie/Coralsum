<template>
  <div class="flex flex-col md:flex-row h-auto md:h-[calc(100vh-56px)] md:overflow-hidden">
    <LeftConfigPanel :generating="btnLoading" @generate="doGenerate"/>
    <div class="flex-1 flex flex-col md:h-full h-auto glass bg-white/40 dark:bg-black/30">
      <div class="flex-1 overflow-y-auto">
        <RightResultPanel :loading="loading" :result="result"/>
      </div>
      <div class="shrink-0 py-2">
        <div class="flex flex-col items-center justify-center text-sm text-neutral-600 dark:text-neutral-300">
          <div class="flex items-center justify-center">
            <span class="mr-1">Powered by</span>
            <svg aria-hidden="true" class="mx-1 w-5 h-5 text-red-500 animate-heart-bounce" fill="currentColor"
                 viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path
                  d="M12 21l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41.81 4.5 2.09C12.09 4.81 13.76 4 15.5 4 18 4 20 6 20 8.5c0 3.78-3.4 6.86-8.55 11.18L12 21z"/>
            </svg>
            <span class="ml-1 font-medium">Vue.js | Micronaut</span>
          </div>
          <div class="mt-1 w-full px-4 text-center break-words text-xs text-neutral-500 dark:text-neutral-400">
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

async function convertImagesToBlobUrls(imgs: string[]): Promise<string[]> {
  const out: string[] = []
  for (const u of imgs) {
    try {
      if (!u) {
        out.push('')
        continue
      }
      const refParam = (() => {
        try {
          const url = new URL(u, window.location.origin)
          const q = url.searchParams.get('ref')
          if (q) return q
          const path = url.pathname.split('/').pop() || ''
          return path
        } catch {
          const idxQ = u.indexOf('ref=')
          if (idxQ >= 0) return decodeURIComponent(u.slice(idxQ + 4))
          const noQuery = u.split('?')[0]
          return noQuery.split('/').pop() || ''
        }
      })()
      const proxyUrl = refParam ? `/api/generative-image/bytes?ref=${encodeURIComponent(refParam)}` : u
      const res = await fetch(proxyUrl)
      const b = await res.blob()
      const url = URL.createObjectURL(b)
      out.push(url)
    } catch {
      out.push(u || '')
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

async function doGenerate(payload: { prompt: string; systemPrompt?: string; files: File[] }) {
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
    if (result.value) {
      try {
        await addHistoryFromResult(user.email || '', {
          prompt: payload.prompt,
          systemPrompt: payload.systemPrompt,
          config: {
            candidateRadio: settings.candidateRadio,
            aspectRatio: settings.aspectRatio,
            topP: settings.topP,
            temperature: settings.temperature,
            resolution: settings.resolution,
            outputFormat: settings.outputFormat,
            imageSize: settings.imageSize,
            mediaResolution: settings.mediaResolution
          },
          result: result.value
        })
      } catch {
      }
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
        if (result.value) {
          try {
            await addHistoryFromResult(user.email || '', {
              prompt: '',
              systemPrompt: undefined,
              config: {
                candidateRadio: settings.candidateRadio,
                aspectRatio: settings.aspectRatio,
                topP: settings.topP,
                temperature: settings.temperature,
                resolution: settings.resolution,
                outputFormat: settings.outputFormat,
                imageSize: settings.imageSize,
                mediaResolution: settings.mediaResolution
              },
              result: result.value
            })
          } catch {
          }
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
