<template>
  <div class="p-4 border-t border-neutral-200 dark:border-neutral-800">
    <div v-if="showHeader" class="text-sm mb-2 flex items-center gap-2">
      <Icon icon="ph:clock-counter-clockwise"/>
      历史记录
    </div>
    <div class="space-y-2">
      <div v-for="item in items" :key="item.id" class="border rounded p-3 border-neutral-200 dark:border-neutral-800">
        <div class="flex justify-between items-center">
          <div class="flex flex-wrap items-center gap-1.5 sm:gap-2 text-[11px] sm:text-xs">
            <div
                class="flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-neutral-50 dark:bg-neutral-800/60 text-neutral-700 dark:text-neutral-300 border border-neutral-200 dark:border-neutral-700">
              <Icon class="text-[0.85rem]" icon="ph:calendar"/>
              <span class="font-medium">时间</span>
              <span class="tabular-nums">{{ new Date(item.createdAt).toLocaleString() }}</span>
            </div>
            <div
                class="flex items-center gap-1 px-1.5 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
              <Icon class="text-[0.85rem]" icon="ph:code"/>
              <span class="font-medium">输入</span>
              <span class="font-semibold tabular-nums">{{ formatTokens(item.inputTokens) }}</span>
            </div>
            <div
                class="flex items-center gap-1 px-1.5 py-0.5 rounded-full bg-fuchsia-50 dark:bg-fuchsia-900/30 text-fuchsia-700 dark:text-fuchsia-300 border border-fuchsia-200 dark:border-fuchsia-800">
              <Icon class="text-[0.85rem]" icon="ph:sparkle"/>
              <span class="font-medium">输出</span>
              <span class="font-semibold tabular-nums">{{ formatTokens(item.outputTokens) }}</span>
            </div>
            <div
                class="flex items-center gap-1 px-1.5 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300">
              <Icon class="text-[0.85rem]" icon="ph:timer"/>
              <span class="font-medium">耗时</span>
              <span class="font-semibold tabular-nums">{{ formatDuration(item.durationMs) }}</span>
            </div>
          </div>
          <n-button quaternary size="small" @click="view(item.id)">
            <div class="flex items-center gap-1.5 text-[11px] sm:text-xs">
              <Icon :class="item.loading ? 'animate-spin' : ''"
                    :icon="item.loading ? 'mdi:loading' : (item.expanded ? 'mdi:chevron-up' : 'mdi:chevron-down')"/>
              <span>查看</span>
            </div>
          </n-button>
        </div>
        <div v-if="item.expanded" class="mt-3 relative">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-2">
            <img v-for="(img,i) in item.images" :key="i" :src="img"
                 class="w-full h-28 object-cover rounded cursor-zoom-in" @click="openPreview(img)"
                 @error="onImgError($event, i)"/>
          </div>
          <n-alert v-if="item.error" class="mt-2" closable type="error">{{ item.error }}</n-alert>
        </div>
      </div>
    </div>
  </div>
  <ImagePreviewer v-model:modelValue="previewShow" :src="previewSrc"/>
</template>

<script lang="ts" setup>
import {ref} from 'vue'
import {Icon} from '@iconify/vue'
import {storeToRefs} from 'pinia'
import {useHistoryStore} from '@/stores/history'
import {NButton} from 'naive-ui'
import {fetchHistoryImages} from '@/api'
import ImagePreviewer from '@/components/ImagePreviewer.vue'
import {useUserStore} from '@/stores/user'
import {useMessage} from 'naive-ui'
import {useSettingsStore} from '@/stores/settings'

const history = useHistoryStore()
const settings = useSettingsStore()
const {showHeader = true} = defineProps<{ showHeader?: boolean }>()
const {items} = storeToRefs(history)
const user = useUserStore()
const message = useMessage()

async function view(id: string) {
  history.items.forEach(i => {
    if (i.id !== id) i.expanded = false
  })
  const it = history.items.find(i => i.id === id)
  if (!it) return
  if (it.expanded) {
    it.expanded = false;
    return
  }
  if (it.images && it.images.length > 0) {
    it.expanded = true;
    return
  }
  try {
    history.setLoading(id, true)
    history.setError(id, '')
    it.images = await fetchHistoryImages(id)
    it.expanded = true
  } catch (e: any) {
    const status = e?.status
    if (status === 401 || status === 403) {
      message.error('未登录或未授权，请先登录')
      user.requireLogin()
    } else if (status === 500) {
      message.error('服务异常，请稍后重试')
    } else {
      message.error(e?.message || '加载历史图片失败')
    }
  } finally {
    history.setLoading(id, false)
  }
}

const previewShow = ref(false)
const previewSrc = ref('')

function openPreview(src: string) {
  previewSrc.value = src;
  previewShow.value = true
}

function themedPlaceholder(width: number, height: number, label: string) {
  const dark = document.documentElement.classList.contains('dark')
  const bg = dark ? '#1f2937' : '#f3f4f6'
  const border = dark ? '#374151' : '#d1d5db'
  const text = dark ? '#9ca3af' : '#6b7280'
  const g1 = dark ? '#0ea5e9' : '#93c5fd'
  const g2 = dark ? '#8b5cf6' : '#a5b4fc'
  const fs = Math.max(12, Math.floor(height * 0.18))
  const sub = Math.max(10, Math.floor(height * 0.14))
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='${width}' height='${height}' viewBox='0 0 ${width} ${height}'>
<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0' stop-color='${g1}'/><stop offset='1' stop-color='${g2}'/></linearGradient></defs>
<rect width='100%' height='100%' fill='${bg}'/>
<rect x='0.5' y='0.5' width='${width - 1}' height='${height - 1}' fill='none' stroke='${border}' stroke-dasharray='5 5' rx='6'/>
<rect width='100%' height='100%' fill='url(#g)' opacity='0.08'/>
<g fill='${text}' font-family='system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif' text-anchor='middle'>
<text x='${width / 2}' y='${height / 2 - 4}' font-size='${fs}'>${label}</text>
<text x='${width / 2}' y='${height / 2 + fs / 2 + 10}' font-size='${sub}'>图片预览已失效</text>
</g>
</svg>`
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg)
}

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

function placeholderImage(index: number) {
  const {w, h} = ratioWH(512);
  return themedPlaceholder(w, h, `IMG${index + 1}`)
}

function onImgError(e: Event, i: number) {
  const t = e.target as HTMLImageElement;
  t.src = placeholderImage(i)
}

function formatDuration(ms: number) {
  const totalSeconds = ms / 1000
  const h = Math.floor(totalSeconds / 3600)
  const m = Math.floor((totalSeconds % 3600) / 60)
  const s = totalSeconds % 60
  const parts: string[] = []
  if (h > 0) parts.push(`${h}h`)
  if (m > 0 || h > 0) parts.push(`${m}m`)
  parts.push(`${s.toFixed(2)}s`)
  return parts.join('')
}

function formatTokens(n: number) {
  const abs = Math.abs(n)
  if (abs < 1000) return `${n} tokens`
  let val = n
  let suf = ''
  if (abs >= 1000000000) {
    val = n / 1000000000;
    suf = 'B'
  } else if (abs >= 1000000) {
    val = n / 1000000;
    suf = 'M'
  } else {
    val = n / 1000;
    suf = 'k'
  }
  const digits = val >= 100 ? 0 : val >= 10 ? 1 : 2
  const str = Number(val.toFixed(digits)).toString()
  return `${str}${suf} tokens`
}
</script>

<style scoped></style>