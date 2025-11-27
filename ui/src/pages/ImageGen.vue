<template>
  <div class="flex flex-col md:flex-row h-auto md:h-[calc(100vh-56px)] md:overflow-hidden">
    <LeftConfigPanel :generating="loading" @generate="doGenerate"/>
    <div class="flex-1 flex flex-col md:h-full h-auto overflow-y-auto glass bg-white/40 dark:bg-black/30">
      <RightResultPanel :loading="loading" :result="result"/>
      <!-- 历史记录模块已移除 -->
    </div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref, watch} from 'vue'
import LeftConfigPanel from '@/components/LeftConfigPanel.vue'
import RightResultPanel from '@/components/RightResultPanel.vue'
import {useSettingsStore} from '@/stores/settings'
import {assessIntent, generate, getGenerateTaskResult, refreshUserInfoByEmail} from '@/api'
import {useUserStore} from '@/stores/user'
import {useMessage} from 'naive-ui'

const settings = useSettingsStore()
const user = useUserStore()
const result = ref<any>()
const loading = ref(false)
const message = useMessage()
const polling = ref(false)

async function doGenerate(payload: { prompt: string; systemPrompt?: string; files: File[] }) {
  result.value = undefined
  loading.value = true
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
    result.value = await generate({
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
        imageSize: settings.imageSize
      }
    })
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
      result.value = res.result
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
        result.value = res.result
        loading.value = false
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

watch(() => user.profileReady, (ready) => {
  if (ready) syncGenerateStatus()
})

</script>

<style scoped></style>
