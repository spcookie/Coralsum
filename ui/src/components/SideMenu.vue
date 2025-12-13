<template>
  <div
      :class="[settings.sidebarCollapsed ? 'w-14' : 'w-56']"
      class="hidden sm:flex border-r border-neutral-200 dark:border-neutral-800 flex-col py-3 glass bg-white/40 dark:bg-black/30">
    <template v-if="settings.sidebarCollapsed">
      <div class="flex-1 flex flex-col items-center gap-3">
        <n-tooltip v-if="user.profileReady" placement="right">
          <template #trigger>
            <n-button circle quaternary @click="openProfile">
              <Icon class="text-xl" icon="mdi:account-cog"/>
            </n-button>
          </template>
          {{ t('profile.modal.title') }}
        </n-tooltip>

    <n-tooltip v-if="user.profileReady && (user.permissions || []).includes('CTL')" placement="right">
      <template #trigger>
        <n-button circle quaternary @click="goCtlKeys">
          <Icon class="text-xl" icon="mdi:key-chain"/>
        </n-button>
      </template>
      {{ t('menu.key_manage') }}
    </n-tooltip>
    <n-tooltip v-if="user.profileReady" placement="right">
      <template #trigger>
        <n-button circle quaternary @click="goTools">
          <Icon class="text-xl" icon="mdi:wrench"/>
        </n-button>
      </template>
      {{ t('tools.title') }}
    </n-tooltip>
        <n-tooltip v-if="user.profileReady && (user.permissions || []).includes('CTL')" placement="right">
          <template #trigger>
            <n-button circle quaternary @click="goIdeas">
              <Icon class="text-xl" icon="mdi:lightbulb-on-outline"/>
        </n-button>
      </template>
          {{ t('ideas.title') }}
    </n-tooltip>
        <n-tooltip v-if="user.profileReady" placement="right">
          <template #trigger>
            <n-button circle quaternary @click="showRedeem = true">
              <Icon class="text-xl" icon="mdi:key"/>
            </n-button>
          </template>
          {{ t('redeem.title') }}
        </n-tooltip>
        <n-tooltip v-if="user.profileReady" placement="right">
          <template #trigger>
            <n-button circle quaternary @click="openHistory">
              <Icon class="text-xl" icon="mdi:history"/>
            </n-button>
          </template>
          {{ t('profile.history.title') }}
        </n-tooltip>

        <n-tooltip v-if="!user.profileReady" placement="right">
          <template #trigger>
            <n-button circle quaternary @click="user.requireLogin()">
              <Icon class="text-xl" icon="mdi:account-arrow-right"/>
            </n-button>
          </template>
          {{ t('auth.login.title') }}
        </n-tooltip>

        <n-tooltip v-if="user.profileReady" placement="right">
          <template #trigger>
            <n-button circle quaternary @click="showLogout = true">
              <Icon class="text-xl" icon="mdi:logout"/>
            </n-button>
          </template>
          {{ t('profile.logout.title') }}
        </n-tooltip>
      </div>
      <div class="mt-auto w-full px-2 pt-2 border-t border-neutral-200 dark:border-neutral-800 flex justify-center">
        <n-button circle quaternary @click="toggleCollapse">
          <Icon :icon="settings.sidebarCollapsed ? 'mdi:menu' : 'mdi:menu-open'" class="text-xl"/>
        </n-button>
      </div>
    </template>
    <template v-else>
      <div class="flex-1 flex flex-col px-2">
        <div class="flex flex-col gap-1 py-2 pl-2">
          <div class="text-[0.95rem] font-bold tracking-wide uppercase">Coralsum</div>
          <div class="text-xs">by @spcookie</div>
        </div>
        <div class="flex flex-col gap-3 py-2">
          <n-button :class="['w-full h-10 px-2 justify-start', onHomePage ? activeItemClass : baseItemClass]" quaternary
                    size="medium"
                    @click="goHome">
            <div class="flex items-center gap-2 w-full">
              <Icon class="text-xl" icon="mdi:home"/>
              <span class="truncate">{{ t('menu.image_generate') }}</span>
            </div>
          </n-button>
          <n-button v-if="(user.permissions || []).includes('CTL')" :class="['w-full h-10 px-2 justify-start', onIdeasPage ? activeItemClass : baseItemClass]" quaternary
                    size="medium"
                    @click="goIdeas">
            <div class="flex items-center gap-2 w-full">
              <Icon class="text-xl" icon="mdi:lightbulb-on-outline"/>
              <span class="truncate">{{ t('ideas.title') }}</span>
            </div>
          </n-button>
          <n-button v-if="(user.permissions || []).includes('CTL')" :class="['w-full h-10 px-2 justify-start', onCtlPage ? activeItemClass : baseItemClass]" quaternary
                    size="medium"
                    @click="goCtlKeys">
            <div class="flex items-center gap-2 w-full">
              <Icon class="text-xl" icon="mdi:key-chain"/>
              <span class="truncate">{{ t('menu.key_manage') }}</span>
            </div>
          </n-button>
          <n-button :class="['w-full h-10 px-2 justify-start', onToolsPage ? activeItemClass : baseItemClass]" quaternary
                    size="medium"
                    @click="goTools">
            <div class="flex items-center gap-2 w-full">
              <Icon class="text-xl" icon="mdi:wrench"/>
              <span class="truncate">{{ t('tools.title') }}</span>
            </div>
          </n-button>
        </div>
      </div>
      <div class="mt-auto w-full px-2 pt-2">
        <div class="flex flex-col gap-2 py-2">
          <n-button v-if="user.profileReady" :class="['w-full h-10 px-2 justify-start', baseItemClass]" quaternary
                    size="medium" @click="openProfile">
            <div class="flex items-center gap-2 w-full">
              <Icon class="text-xl" icon="mdi:account-cog"/>
              <span class="truncate">{{ t('profile.modal.title') }}</span>
            </div>
          </n-button>
          <n-button v-if="user.profileReady" :class="['w-full h-10 px-2 justify-start', baseItemClass]" quaternary
                    size="medium" @click="openHistory">
            <div class="flex items-center gap-2 w-full">
              <Icon class="text-xl" icon="mdi:history"/>
              <span class="truncate">{{ t('profile.history.title') }}</span>
            </div>
          </n-button>
          <n-button v-if="user.profileReady" :class="['w-full h-10 px-2 justify-start', baseItemClass]" quaternary
                    size="medium" @click="showRedeem = true">
            <div class="flex items-center gap-2 w-full">
              <Icon class="text-xl" icon="mdi:key"/>
              <span class="truncate">{{ t('redeem.title') }}</span>
            </div>
          </n-button>
          <n-button v-if="!user.profileReady" :class="['w-full h-10 px-2 justify-start', baseItemClass]" quaternary
                    size="medium" @click="user.requireLogin()">
            <div class="flex items-center gap-2 w-full">
              <Icon class="text-xl" icon="mdi:account-arrow-right"/>
              <span class="truncate">{{ t('auth.login.title') }}</span>
            </div>
          </n-button>
          <n-button v-if="user.profileReady" :class="['w-full h-10 px-2 justify-start', baseItemClass]" quaternary
                    size="medium" @click="showLogout = true">
            <div class="flex items-center gap-2 w-full">
              <Icon class="text-xl" icon="mdi:logout"/>
              <span class="truncate">{{ t('profile.logout.title') }}</span>
            </div>
          </n-button>
        </div>
        <div class="pb-2 flex justify-center">
          <n-button circle quaternary @click="toggleCollapse">
            <Icon :icon="settings.sidebarCollapsed ? 'mdi:menu' : 'mdi:menu-open'" class="text-xl"/>
          </n-button>
        </div>
      </div>
    </template>

    <n-modal v-model:show="showRedeem" :style="{ width: '420px', maxWidth: '92vw', margin: '0 auto' }" preset="card"
             :title="t('redeem.title')">
      <div class="space-y-3">
        <n-input v-model:value="redeemKey" :placeholder="t('redeem.placeholder')"/>
        <div class="flex gap-2 justify-end">
          <n-button @click="showRedeem=false">{{ t('redeem.cancel') }}</n-button>
          <n-button :disabled="redeeming || !redeemKey.trim()" type="primary" @click="redeem">
            {{ redeeming ? t('redeem.loading') : t('redeem.ok') }}
          </n-button>
        </div>
      </div>
    </n-modal>
    <n-modal v-model:show="showHistory" :style="{ width: 'min(96vw, 980px)', margin: '0 auto' }" preset="card">
      <template #header>
        <div class="flex items-center gap-2">
          <span>{{ t('profile.history.title') }}</span>
          <n-tooltip placement="top">
            <template #trigger>
              <Icon class="text-amber-500 dark:text-amber-400 text-[1rem]" icon="ph:warning"/>
            </template>
            {{ t('profile.history.warning') }}
          </n-tooltip>
        </div>
      </template>
      <div class="flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <div class="text-xs text-neutral-500">{{ t('profile.history.count') }} {{ total }}</div>
          <div class="flex items-center gap-2">
            <n-button quaternary size="small" @click="loadHistory">
              <div class="flex items-center gap-1">
                <Icon icon="mdi:refresh"/>
                <span>{{ t('profile.history.refresh') }}</span></div>
            </n-button>
          </div>
        </div>
        <n-spin :show="loadingHistory">
          <div v-if="records.length === 0" class="p-6">
            <div class="text-center text-neutral-500">{{ t('profile.history.empty') }}</div>
          </div>
          <div v-else class="flex flex-col gap-3">
            <div v-for="r in viewRecords" :key="r.id"
                 class="rounded border border-neutral-200 dark:border-neutral-800 bg-white/60 dark:bg-black/30 overflow-hidden">
              <div class="p-3 flex items-center gap-2">
                <div class="flex-1 flex flex-wrap items-center gap-1.5 text-[10px] sm:text-[11px]">
                  <div
                      class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                    <Icon class="text-[0.75rem]" icon="ph:code"/>
                    <span class="font-medium">{{ t('metrics.input') }}</span>
                    <span class="font-semibold tabular-nums">{{ formatTokens(r.inputTokens) }}</span>
                  </div>
                  <div
                      class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-fuchsia-50 dark:bg-fuchsia-900/30 text-fuchsia-700 dark:text-fuchsia-300 border border-fuchsia-200 dark:border-fuchsia-800">
                    <Icon class="text-[0.75rem]" icon="ph:sparkle"/>
                    <span class="font-medium">{{ t('metrics.output') }}</span>
                    <span class="font-semibold tabular-nums">{{ formatTokens(r.outputTokens) }}</span>
                  </div>
                  <div
                      class="flex items-center gap-0.5 px-1 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300">
                    <Icon class="text-[0.75rem]" icon="ph:timer"/>
                    <span class="font-medium">{{ t('metrics.duration') }}</span>
                    <span class="font-semibold tabular-nums">{{ formatDuration(r.durationMs) }}</span>
                  </div>
                  <div
                      class="flex items-center gap-0.5 px-1 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300">
                    <Icon class="text-[0.75rem]" icon="ph:image"/>
                    <span class="font-medium">{{ t('metrics.images') }}</span>
                    <span class="font-semibold tabular-nums">{{ r.imageCount }}</span>
                  </div>
                  <div class="ml-auto text-[11px] text-neutral-500">{{ formatTime(r.createdAt) }}</div>
                </div>
                <div class="flex items-center gap-2">
                  <n-button quaternary size="small" @click="toggle(r.id)">
                    <div class="flex items-center gap-1">
                      <Icon :icon="expanded.has(r.id) ? 'mdi:chevron-up' : 'mdi:chevron-down'"/>
                      <span>{{ t('profile.history.expand_images') }}</span></div>
                  </n-button>
                  <n-popconfirm :negative-text="t('profile.modal.cancel')" :positive-text="t('profile.history.delete')"
                                :show-icon="false"
                                @positive-click="removeRecord(r)">
                    <template #trigger>
                      <n-button quaternary size="small">
                        <div class="flex items-center gap-1 text-red-600">
                          <Icon icon="mdi:delete-outline"/>
                          <span>{{ t('profile.history.delete') }}</span>
                        </div>
                      </n-button>
                    </template>
                    {{ t('profile.history.confirm_delete') }}
                  </n-popconfirm>
                </div>
              </div>
              <transition name="expand">
                <div v-if="expanded.has(r.id) && r.urls?.length" class="px-3 pb-3 overflow-hidden">
                  <div class="mt-2 flex flex-wrap justify-center gap-2">
                    <div v-for="(img,i) in r.urls" :key="i"
                         class="rounded bg-neutral-100 dark:bg-neutral-800 w-full sm:w-[200px] md:w-[280px] lg:w-[360px] cursor-zoom-in">
                      <img :src="img || placeholder(i)" class="w-full h-auto object-contain" @click="openPreview(img)"/>
                    </div>
                  </div>
                </div>
              </transition>
            </div>
          </div>
        </n-spin>
      </div>
      <div class="mt-2 flex justify-end">
        <n-pagination :item-count="total" :page="page" :page-size="pageSize" @update:page="onPageChange"
                      @update:page-size="onPageSizeChange"/>
      </div>
      <ImagePreviewer v-model:modelValue="previewShow" :src="previewSrc"/>
    </n-modal>
    <n-modal v-model:show="showLogout" :style="{ width: '420px', maxWidth: '92vw', margin: '0 auto' }" preset="card"
             :title="t('profile.logout.title')">
      <div class="space-y-3">
        <div>{{ t('profile.logout.confirm') }}</div>
        <div class="flex gap-2 justify-end">
          <n-button @click="showLogout=false">{{ t('profile.logout.cancel') }}</n-button>
          <n-button type="error" @click="doLogout">{{ t('profile.logout.ok') }}</n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import {computed, nextTick, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {Icon} from '@iconify/vue'
import {useUserStore} from '@/stores/user'
import {NButton, NInput, NModal, NPagination, NPopconfirm, NSpin, NTooltip, useMessage} from 'naive-ui'
import {getProfile, redeemPoints} from '@/api'
import {countHistory, deleteHistory, listHistory} from '@/utils/indexedDb'
import ImagePreviewer from '@/components/ImagePreviewer.vue'
import {useRoute, useRouter} from 'vue-router'
import {useSettingsStore} from '@/stores/settings'

const user = useUserStore()
const {t} = useI18n()
const message = useMessage()
const router = useRouter()
const route = useRoute()
const settings = useSettingsStore()
const activeItemClass = 'bg-orange-50 text-orange-600 dark:bg-orange-900/20 dark:text-orange-300'
const baseItemClass = 'text-neutral-700 dark:text-neutral-200 hover:bg-neutral-100 dark:hover:bg-neutral-800'

function toggleCollapse() {
  const fn = (settings as any).toggleSidebar
  if (typeof fn === 'function') fn.call(settings)
  else settings.sidebarCollapsed = !settings.sidebarCollapsed
}
const onCtlPage = computed(() => route.name === 'ctl-keys')
const onToolsPage = computed(() => route.name === 'tools')
const onIdeasPage = computed(() => route.name === 'ideas-admin')
const onHomePage = computed(() => route.path === '/' || route.name === 'home')

function openProfile() {
  user.requireProfile()
}

function goCtlKeys() {
  router.push('/ctl/keys')
}

function goHome() {
  router.push('/')
}

function goTools() {
  router.push('/tools')
}

function goIdeas() {
  router.push('/ideas')
}

const showRedeem = ref(false)
const redeemKey = ref('')
const redeeming = ref(false)
const showLogout = ref(false)


function redeem() {
  if (!user.profileReady) {
    message.error(t('messages.please_login'))
    user.requireLogin()
    return
  }
  if (redeemKey.value.trim()) {
    redeeming.value = true
    redeemPoints(user.email, redeemKey.value.trim())
        .then(async (res) => {
          const addedRaw = Number((res as any)?.pointsAdded ?? (res as any)?.points_added ?? 0)
          const addedView = Number.isFinite(addedRaw) ? addedRaw : 0
          try {
            const profile = await getProfile()
            user.setProfile({...profile})
          } catch {
          }
          message.success(t('redeem.success', {points: addedView.toFixed(0)}))
          redeemKey.value = ''
          nextTick(() => {
            showRedeem.value = false
          })
        })
        .catch((e: any) => {
          const status = e?.status
          if (status === 401 || status === 403) {
            const expired = !!(e?.__authExpired)
            message.error(expired ? t('messages.auth_expired') : t('messages.please_login'))
            user.requireLogin()
          } else if (status === 500) {
            message.error(t('messages.service_error'))
          } else {
            message.error(e?.message || t('redeem.failed'))
          }
        })
        .finally(() => {
          redeeming.value = false
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
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

function openHistory() {
  if (!user.profileReady) {
    message.error(t('messages.please_login'))
    user.requireLogin()
    return
  }
  showHistory.value = true
  page.value = 1
  nextTick(() => {
    loadHistory()
  })
}

async function loadHistory() {
  try {
    loadingHistory.value = true
    const [list, cnt] = await Promise.all([
      listHistory(user.email || '', pageSize.value, (page.value - 1) * pageSize.value),
      countHistory(user.email || '')
    ])
    records.value = list
    total.value = Number(cnt || 0)
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

function onPageChange(p: number) {
  page.value = p
  loadHistory()
}

function onPageSizeChange(s: number) {
  pageSize.value = s
  page.value = 1
  loadHistory()
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
  if (expanded.value.has(id)) {
    expanded.value.delete(id)
  } else {
    expanded.value.clear()
    expanded.value.add(id)
  }
  expanded.value = new Set(expanded.value)
}

function openPreview(src: string) {
  previewSrc.value = src
  previewShow.value = true
}

function doLogout() {
  try {
    user.logout()
    message.success(t('messages.logout_success'))
  } finally {
    nextTick(() => {
      showLogout.value = false
    })
  }
}

async function removeRecord(r: any) {
  try {
    if (r?.urls) {
      for (const u of r.urls) {
        if (u && typeof u === 'string' && u.startsWith('blob:')) URL.revokeObjectURL(u)
      }
    }
    await deleteHistory(r.id)
    const cnt = await countHistory(user.email || '')
    total.value = Number(cnt || 0)
    const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
    expanded.value.delete(r.id)
    await loadHistory()
    message.success(t('messages.delete_success'))
  } catch {
    message.error(t('messages.delete_failed'))
  }
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

<style scoped>
.expand-enter-active,
.expand-leave-active {
  transition: max-height .2s ease, opacity .2s ease, transform .2s ease;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-4px);
}

.expand-enter-to,
.expand-leave-from {
  max-height: 1000px;
  opacity: 1;
  transform: translateY(0);
}
</style>
