<template>
  <div
      class="hidden sm:flex w-14 border-r border-neutral-200 dark:border-neutral-800 flex-col items-center py-3 gap-4 glass bg-white/40 dark:bg-black/30">
    <n-tooltip placement="right">
      <template #trigger>
        <n-button circle quaternary @click="openProfile">
          <Icon class="text-xl" icon="mdi:account-cog"/>
        </n-button>
      </template>
      用户信息
    </n-tooltip>
    <n-tooltip placement="right">
      <template #trigger>
        <n-button circle quaternary @click="openHistory">
          <Icon class="text-xl" icon="mdi:history"/>
        </n-button>
      </template>
      历史记录
    </n-tooltip>
    <n-tooltip placement="right">
      <template #trigger>
        <n-button circle quaternary @click="showRedeem = true">
          <Icon class="text-xl" icon="mdi:key"/>
        </n-button>
      </template>
      兑换密钥
    </n-tooltip>

    <n-modal v-model:show="showRedeem" :style="{ width: '420px', maxWidth: '92vw' }" preset="card" title="兑换积分密钥">
      <div class="space-y-3">
        <n-input v-model:value="redeemKey" placeholder="输入密钥"/>
        <div class="flex gap-2 justify-end">
          <n-button @click="showRedeem=false">取消</n-button>
          <n-button type="primary" @click="redeem">兑换</n-button>
        </div>
      </div>
    </n-modal>
    <n-modal v-model:show="showHistory" :style="{ width: 'min(96vw, 980px)' }" preset="card">
      <template #header>
        <div class="flex items-center gap-2">
          <span>历史记录</span>
          <n-tooltip placement="top">
            <template #trigger>
              <Icon class="text-amber-500 dark:text-amber-400 text-[1rem]" icon="ph:warning"/>
            </template>
            请注意：历史记录保存在您的浏览器本地，不会上传到服务器。更换设备或清理浏览器数据可能会丢失历史。
          </n-tooltip>
        </div>
      </template>
      <div class="flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <div class="text-xs text-neutral-500">共 {{ records.length }} 条</div>
          <div class="flex items-center gap-2">
            <n-button quaternary size="small" @click="loadHistory">
              <div class="flex items-center gap-1">
                <Icon icon="mdi:refresh"/>
                <span>刷新</span></div>
            </n-button>
          </div>
        </div>
        <n-spin :show="loadingHistory">
          <div v-if="records.length === 0" class="p-6">
            <div class="text-center text-neutral-500">暂无历史记录</div>
          </div>
          <div v-else class="flex flex-col gap-3">
            <div v-for="r in viewRecords" :key="r.id"
                 class="rounded border border-neutral-200 dark:border-neutral-800 bg-white/60 dark:bg-black/30 overflow-hidden">
              <div class="p-3 flex flex-wrap items-center gap-2">
                <div
                    class="flex items-center gap-1 px-1.5 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                  <Icon class="text-[0.9rem]" icon="ph:code"/>
                  <span class="font-medium">输入</span>
                  <span class="font-semibold tabular-nums">{{ formatTokens(r.inputTokens) }}</span>
                </div>
                <div
                    class="flex items-center gap-1 px-1.5 py-0.5 rounded-full bg-fuchsia-50 dark:bg-fuchsia-900/30 text-fuchsia-700 dark:text-fuchsia-300 border border-fuchsia-200 dark:border-fuchsia-800">
                  <Icon class="text-[0.9rem]" icon="ph:sparkle"/>
                  <span class="font-medium">输出</span>
                  <span class="font-semibold tabular-nums">{{ formatTokens(r.outputTokens) }}</span>
                </div>
                <div
                    class="flex items-center gap-1 px-1.5 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300">
                  <Icon class="text-[0.9rem]" icon="ph:timer"/>
                  <span class="font-medium">耗时</span>
                  <span class="font-semibold tabular-nums">{{ formatDuration(r.durationMs) }}</span>
                </div>
                <div
                    class="flex items-center gap-1 px-1.5 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300">
                  <Icon class="text-[0.9rem]" icon="ph:image"/>
                  <span class="font-medium">图片</span>
                  <span class="font-semibold tabular-nums">{{ r.imageCount }}</span>
                </div>
                <div class="ml-auto text-xs text-neutral-500">{{ formatTime(r.createdAt) }}</div>
              </div>
              <div v-if="r.urls?.length" class="px-3 pb-3">
                <n-button quaternary size="small" @click="toggle(r.id)">
                  <div class="flex items-center gap-1">
                    <Icon :icon="expanded.has(r.id) ? 'mdi:chevron-up' : 'mdi:chevron-down'"/>
                    <span>展开图片</span></div>
                </n-button>
                <div v-if="expanded.has(r.id)" class="mt-2 flex flex-wrap justify-center gap-3">
                  <div v-for="(img,i) in r.urls" :key="i"
                       class="rounded bg-neutral-100 dark:bg-neutral-800 w-full sm:w-[260px] md:w-[320px] lg:w-[380px] cursor-zoom-in">
                    <img :src="img || placeholder(i)" class="w-full h-auto object-contain" @click="openPreview(img)"/>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </n-spin>
      </div>
      <ImagePreviewer v-model:modelValue="previewShow" :src="previewSrc"/>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import {ref, watch} from 'vue'
import {Icon} from '@iconify/vue'
import {useUserStore} from '@/stores/user'
import {NButton, NInput, NModal, NSpin, NTooltip, useMessage} from 'naive-ui'
import {redeemPoints} from '@/api'
import {listHistory} from '@/utils/indexedDb'
import ImagePreviewer from '@/components/ImagePreviewer.vue'

const user = useUserStore()
const message = useMessage()

function openProfile() {
  user.requireProfile()
}

const showRedeem = ref(false)
const redeemKey = ref('')


function redeem() {
  if (!user.profileReady) {
    message.error('请先登录')
    user.requireLogin()
    return
  }
  if (redeemKey.value.trim()) {
    redeemPoints(user.email, redeemKey.value.trim())
        .then(res => {
          user.addPoints(res.pointsAdded)
          redeemKey.value = ''
          showRedeem.value = false
        })
        .catch((e: any) => {
          const status = e?.status
          if (status === 401 || status === 403) {
            message.error('未登录或未授权，请先登录')
            user.requireLogin()
          } else if (status === 500) {
            message.error('服务异常，请稍后重试')
          } else {
            message.error(e?.message || '兑换失败')
          }
        })
  }
}

const showHistory = ref(false)
const loadingHistory = ref(false)
const records = ref<any[]>([])
const viewRecords = ref<any[]>([])
const expanded = ref<Set<string>>(new Set())
const previewShow = ref(false)
const previewSrc = ref('')

function openHistory() {
  if (!user.profileReady) {
    message.error('请先登录')
    user.requireLogin()
    return
  }
  showHistory.value = true
  loadHistory()
}

async function loadHistory() {
  try {
    loadingHistory.value = true
    const list = await listHistory(user.email || '', 100, 0)
    records.value = list
    const urlsList: string[][] = list.map(r => (r.images || []).map((img: any) => {
      if (img && typeof img !== 'string') {
        try {
          return URL.createObjectURL(img as Blob)
        } catch {
          return ''
        }
      }
      return img || ''
    }))
    viewRecords.value = list.map((r, idx) => ({...r, urls: urlsList[idx]}))
  } catch {
    records.value = []
    viewRecords.value = []
  } finally {
    loadingHistory.value = false
  }
}

function cleanupUrls() {
  try {
    for (const r of viewRecords.value) {
      for (const u of (r.urls || [])) {
        if (u && u.startsWith('blob:')) URL.revokeObjectURL(u)
      }
    }
  } catch {
  }
}

watch(() => showHistory.value, (v) => {
  if (!v) cleanupUrls()
})

function toggle(id: string) {
  if (expanded.value.has(id)) expanded.value.delete(id)
  else expanded.value.add(id)
}

function openPreview(src: string) {
  previewSrc.value = src
  previewShow.value = true
}

function formatTime(ts: number) {
  const d = new Date(ts)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`
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
    val = n / 1000000000
    suf = 'B'
  } else if (abs >= 1000000) {
    val = n / 1000000
    suf = 'M'
  } else {
    val = n / 1000
    suf = 'k'
  }
  const digits = val >= 100 ? 0 : val >= 10 ? 1 : 2
  const str = Number(val.toFixed(digits)).toString()
  return `${str}${suf} tokens`
}

function placeholder(index: number) {
  const w = 512, h = 320
  return `https://placehold.co/${w}x${h}?text=IMG${index + 1}`
}
</script>

<style scoped></style>