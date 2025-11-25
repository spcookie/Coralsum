<template>
  <div class="flex flex-col md:flex-row h-auto md:h-[calc(100vh-56px)] md:overflow-hidden">
    <LeftConfigPanel :generating="loading" @generate="doGenerate"/>
    <div class="flex-1 flex flex-col md:h-full h-auto overflow-y-auto glass bg-white/40 dark:bg-black/30">
      <RightResultPanel :loading="loading" :result="result"/>
      <div class="mt-2">
        <div class="sticky bottom-0 z-10">
          <div
              class="flex items-center justify-between px-3 py-2 glass bg-white/40 dark:bg-black/30 border-t border-neutral-200 dark:border-neutral-800">
            <div class="flex items-center gap-2 text-sm">
              <Icon icon="ph:clock-counter-clockwise"/>
              <span>历史记录</span>
            </div>
            <n-button quaternary size="small" @click="onToggleHistory">
              <div class="flex items-center gap-1.5 text-[11px] sm:text-xs">
                <Icon :class="historyLoading ? 'animate-spin' : ''"
                      :icon="historyLoading ? 'mdi:loading' : (showHistory ? 'mdi:chevron-down' : 'mdi:chevron-up')"/>
                <span>{{ showHistory ? '收起' : '展开' }}</span>
              </div>
            </n-button>
          </div>
        </div>
        <HistoryList v-if="showHistory" :showHeader="false"/>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref, watch} from 'vue'
import {Icon} from '@iconify/vue'
import LeftConfigPanel from '@/components/LeftConfigPanel.vue'
import RightResultPanel from '@/components/RightResultPanel.vue'
import HistoryList from '@/components/HistoryList.vue'
import {useSettingsStore} from '@/stores/settings'
import {useHistoryStore} from '@/stores/history'
import {fetchHistoryList, generate, refreshUserInfoByEmail, getGenerateTaskResult} from '@/api'
import {useUserStore} from '@/stores/user'
import {NButton, useMessage} from 'naive-ui'

const settings = useSettingsStore()
const history = useHistoryStore()
const user = useUserStore()
const result = ref<any>()
const loading = ref(false)
const message = useMessage()
const showHistory = ref(false)
const historyLoading = ref(false)
const polling = ref(false)

async function doGenerate(payload: { prompt: string; systemPrompt?: string; files: File[] }) {
  result.value = undefined
  loading.value = true
  try {
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
        outputFormat: settings.outputFormat
      }
    })
    const list = await fetchHistoryList()
    history.setItems(list.map(it => ({...it, expanded: false, loading: false, error: ''})))
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

async function onToggleHistory() {
  if (!showHistory.value) {
    try {
      historyLoading.value = true
      const list = await fetchHistoryList()
      history.setItems(list.map(it => ({...it, expanded: false, loading: false, error: ''})))
    } catch (e: any) {
      const status = e?.status
      if (status === 401 || status === 403) {
        message.error('未登录或未授权，请先登录')
        user.requireLogin()
      } else if (status === 500) {
        message.error('服务异常，请稍后重试')
      } else {
        message.error(e?.message || '加载历史记录失败')
      }
    } finally {
      historyLoading.value = false
    }
  }
  showHistory.value = !showHistory.value
}

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
    const maxAttempts = Math.ceil((1000 * 60 * 3) / intervalMs)
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