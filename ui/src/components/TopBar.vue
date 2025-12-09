<template>
  <div
      class="h-14 border-b border-transparent px-3 sm:px-4 flex items-center justify-between glass bg-white/40 dark:bg-black/30">
    <div class="font-medium text-neutral-700 dark:text-neutral-200 flex items-center gap-2">
      <Icon class="text-xl" icon="solar:stars-bold-duotone"/>
      <span>{{ t('app.title') }}</span>
    </div>
    <div class="flex items-center gap-2 sm:gap-3 flex-wrap text-sm">
      <div class="hidden sm:flex items-center gap-2">
        <Icon class="text-lg" icon="mdi:account"/>
        <span class="font-bold text-neutral-700 dark:text-neutral-200">{{ nameParts.base }}<span v-if="nameParts.tag"
                                                                                                 class="font-medium text-neutral-400 dark:text-neutral-500">#{{
            nameParts.tag
          }}</span></span>
        <n-tooltip v-if="user.profileReady" placement="bottom" trigger="hover">
          <template #trigger>
            <div class="flex items-center gap-1">
              <Icon class="text-yellow-500 text-base" icon="material-symbols:bolt-rounded"/>
              <span class="inline-block underline underline-offset-2 decoration-[1px] decoration-solid">
                <n-number-animation :active="true" :duration="800" :from="prevPoints" :precision="0" :to="user.points"/>
              </span>
            </div>
          </template>
          <div class="text-xs text-white space-y-1">
            <div>{{ t('points.subscribe') }}：{{ user.subscribePoints }}</div>
            <div>{{ t('points.permanent') }}：{{ user.permanentPoints }}</div>
            <div v-if="typeof user.giftPoints === 'number' && user.giftPoints > 0">{{
                t('points.gift')
              }}：{{ user.giftPoints }}
            </div>
            <div v-if="user.tier==='PRO' && typeof user.subscribeExpireTime === 'number'">
              {{ t('points.expire') }}：{{
                formatDate(user.subscribeExpireTime as number, {
                  year: 'numeric',
                  month: '2-digit',
                  day: '2-digit'
                })
              }}
            </div>
            <div v-if="typeof user.giftExpireTime === 'number' && user.giftExpireTime">
              {{ t('points.gift_expire') }}：{{
                formatDate(user.giftExpireTime as number, {
                  year: 'numeric',
                  month: '2-digit',
                  day: '2-digit'
                })
              }}
            </div>
          </div>
        </n-tooltip>
        <n-tag v-if="user.profileReady" :color="tierTagStyle"
               class="inline-flex items-center justify-center leading-none" size="small">
          {{ user.tier }}
        </n-tag>
      </div>
      <div class="flex items-center gap-2">
        <n-button v-if="user.profileReady" circle class="sm:hidden" quaternary size="small"
                  @click="user.requireProfile()">
          <Icon icon="mdi:account-cog"/>
        </n-button>

        <n-button v-if="user.profileReady" circle class="sm:hidden" quaternary size="small"
                  @click="showRedeemMobile = true">
          <Icon icon="mdi:key"/>
        </n-button>
        <n-badge v-if="user.profileReady" :show="historyCount>0" :value="historyCount" class="sm:hidden">
          <n-button circle quaternary size="small" @click="openHistory">
            <Icon icon="mdi:history"/>
          </n-button>
        </n-badge>
        <n-button v-if="!user.profileReady" circle class="sm:hidden" quaternary size="small"
                  @click="user.requireLogin()">
          <Icon icon="mdi:account-arrow-right"/>
        </n-button>
        <n-button v-if="user.profileReady" circle class="sm:hidden" quaternary size="small"
                  @click="showLogout = true">
          <Icon icon="mdi:logout"/>
        </n-button>
        <n-switch :value="settings.darkMode" @update:value="onToggleDark">
          <template #checked>
            <Icon icon="mdi:weather-night"/>
          </template>
          <template #unchecked>
            <Icon icon="mdi:white-balance-sunny"/>
          </template>
        </n-switch>
        <LanguageSwitcher/>
      </div>

    </div>
  </div>
  <div
      class="sm:hidden w-full px-3 py-2 flex items-center justify-between glass bg-white/60 dark:bg-black/30 border-b border-neutral-200 dark:border-neutral-800">
    <div class="flex items-center gap-2 min-w-0">
      <Icon class="text-base" icon="mdi:account"/>
      <span class="truncate max-w-[40vw] font-bold text-neutral-700 dark:text-neutral-200">{{ nameParts.base }}<span
          v-if="nameParts.tag" class="font-medium text-neutral-400 dark:text-neutral-500">#{{
          nameParts.tag
        }}</span></span>
      <n-tooltip v-if="user.profileReady" placement="bottom" trigger="click">
        <template #trigger>
          <div class="flex items-center gap-1">
            <Icon class="text-yellow-500" icon="material-symbols:bolt-rounded"/>
            <span class="inline-block underline underline-offset-2 decoration-[1px] decoration-solid">
              <n-number-animation :active="true" :duration="800" :from="prevPoints" :precision="0" :to="user.points"/>
            </span>
          </div>
        </template>
        <div class="text-xs text-white space-y-1">
          <div>{{ t('points.subscribe') }}：{{ user.subscribePoints }}</div>
          <div>{{ t('points.permanent') }}：{{ user.permanentPoints }}</div>
          <div v-if="typeof user.giftPoints === 'number' && user.giftPoints > 0">{{
              t('points.gift')
            }}：{{ user.giftPoints }}
          </div>
          <div v-if="user.tier==='PRO' && typeof user.subscribeExpireTime === 'number'">
            {{ t('points.expire') }}：{{
              formatDate(user.subscribeExpireTime as number, {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
              })
            }}
          </div>
          <div v-if="typeof user.giftExpireTime === 'number' && user.giftExpireTime">
            {{ t('points.gift_expire') }}：{{
              formatDate(user.giftExpireTime as number, {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
              })
            }}
          </div>
        </div>
      </n-tooltip>
      <n-tag v-if="user.profileReady" :color="tierTagStyle" class="inline-flex items-center justify-center leading-none"
             size="small">
        {{ user.tier }}
      </n-tag>
    </div>
  </div>
  <n-modal v-model:show="user.showProfileModal" :style="{ width: '520px', maxWidth: '92vw', margin: '0 auto' }"
           preset="card"
           :title="t('profile.modal.title')">
    <n-form ref="profileFormRef" :model="profileForm" :rules="profileRules">
      <div class="space-y-3">
        <div class="text-sm text-neutral-500">{{ t('profile.modal.email') }}：{{ user.email }}</div>
        <n-form-item path="name">
          <n-input v-model:value="profileForm.name" :placeholder="t('profile.modal.name')"/>
        </n-form-item>
        <n-collapse>
          <n-collapse-item :title="t('profile.modal.change_password')" name="pwd">
            <div class="space-y-2">
              <n-form-item path="oldPwd">
                <n-input v-model:value="profileForm.oldPwd" :placeholder="t('profile.modal.old_password')"
                         type="password"/>
              </n-form-item>
              <n-form-item path="newPwd">
                <n-input v-model:value="profileForm.newPwd" :placeholder="t('profile.modal.new_password')"
                         type="password"/>
              </n-form-item>
              <n-form-item path="confirmPwd">
                <n-input v-model:value="profileForm.confirmPwd" :placeholder="t('profile.modal.confirm_password')"
                         type="password"/>
              </n-form-item>
              <n-form-item path="code">
                <div class="flex items-center gap-2">
                  <n-input v-model:value="profileForm.emailCode" :placeholder="t('profile.modal.email_code')"
                           class="flex-1"/>
                  <n-button :disabled="sendCodeCd > 0 || sendCodeLoading" :loading="sendCodeLoading" size="small"
                            tertiary @click="sendCode">{{ sendCodeLabel }}
                  </n-button>
                </div>
              </n-form-item>

            </div>
          </n-collapse-item>
        </n-collapse>
        <div class="flex gap-2 justify-end">
          <n-button @click="user.showProfileModal=false">{{ t('profile.modal.cancel') }}</n-button>
          <n-button type="primary" @click="saveProfile">
            {{ t('profile.modal.save') }}
          </n-button>
        </div>
      </div>
    </n-form>
  </n-modal>
  <n-modal v-model:show="showRedeemMobile" :style="{ width: '420px', maxWidth: '92vw', margin: '0 auto' }" preset="card"
           :title="t('redeem.title')">
    <div class="space-y-3">
      <n-input v-model:value="redeemKeyMobile" :placeholder="t('redeem.placeholder')"/>
      <div class="flex gap-2 justify-end">
        <n-button @click="showRedeemMobile=false">{{ t('redeem.cancel') }}</n-button>
        <n-button :disabled="redeemingMobile || !redeemKeyMobile.trim()" type="primary" @click="redeemMobile">
          {{ redeemingMobile ? t('redeem.loading') : t('redeem.ok') }}
        </n-button>
      </div>
    </div>
  </n-modal>
  <n-modal v-model:show="user.showLoginModal" :style="{ width: '520px', maxWidth: '92vw', margin: '0 auto' }"
           :title="t('auth.login.title')" preset="card">
    <n-form ref="formRef" :model="loginForm" :rules="rules">
      <div class="space-y-3">
        <n-form-item path="email">
          <n-input v-model:value="loginForm.email" :placeholder="t('profile.modal.email')"
                   @keydown.enter.prevent="login"/>
        </n-form-item>
        <n-form-item path="password">
          <n-input v-model:value="loginForm.password" :type="loginPwdVisible ? 'text' : 'password'" maxlength="16"
                   :placeholder="t('profile.modal.password')" @keydown.enter.prevent="login">
            <template #suffix>
              <Icon :icon="loginPwdVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                    class="cursor-pointer text-neutral-400" @click="loginPwdVisible = !loginPwdVisible"/>
            </template>
          </n-input>
        </n-form-item>

        <div class="flex gap-2 justify-between items-center">
          <div class="flex gap-2">
            <n-button quaternary @click="goRegister">{{ t('auth.register') }}</n-button>
            <n-button quaternary @click="goForgot">{{ t('auth.forgot') }}</n-button>
          </div>
          <div class="flex gap-2">
            <n-button :disabled="!loginValid" :loading="loginLoading" type="primary" @click="login">
              {{ t('auth.login.title') }}
            </n-button>
          </div>
        </div>
      </div>
    </n-form>
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
                    class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800 shrink-0 whitespace-nowrap">
                  <Icon class="text-[0.75rem]" icon="ph:code"/>
                  <span class="font-medium hidden sm:inline">{{ t('metrics.input') }}</span>
                  <span class="font-medium sm:hidden">{{ t('metrics_short.input') }}</span>
                  <span class="font-semibold tabular-nums">{{ formatTokens(r.inputTokens) }}</span>
                </div>
                <div
                    class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-fuchsia-50 dark:bg-fuchsia-900/30 text-fuchsia-700 dark:text-fuchsia-300 border border-fuchsia-200 dark:border-fuchsia-800 shrink-0 whitespace-nowrap">
                  <Icon class="text-[0.75rem]" icon="ph:sparkle"/>
                  <span class="font-medium hidden sm:inline">{{ t('metrics.output') }}</span>
                  <span class="font-medium sm:hidden">{{ t('metrics_short.output') }}</span>
                  <span class="font-semibold tabular-nums">{{ formatTokens(r.outputTokens) }}</span>
                </div>
                <div
                    class="flex items-center gap-0.5 px-1 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 shrink-0 whitespace-nowrap">
                  <Icon class="text-[0.75rem]" icon="ph:timer"/>
                  <span class="font-medium hidden sm:inline">{{ t('metrics.duration') }}</span>
                  <span class="font-medium sm:hidden">{{ t('metrics_short.duration') }}</span>
                  <span class="font-semibold tabular-nums">{{ formatDuration(r.durationMs) }}</span>
                </div>
                <div
                    class="flex items-center gap-0.5 px-1 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300 shrink-0 whitespace-nowrap">
                  <Icon class="text-[0.75rem]" icon="ph:image"/>
                  <span class="font-medium hidden sm:inline">{{ t('metrics.images') }}</span>
                  <span class="font-medium sm:hidden">{{ t('metrics_short.images') }}</span>
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
                    <img :src="img || placeholder(i)" class="w-full h-auto object-contain"
                         @click="openPreview(img || placeholder(i))"/>
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
</template>

<script lang="ts" setup>
import {computed, nextTick, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {useRouter} from 'vue-router'
import {Icon} from '@iconify/vue'
import {useSettingsStore} from '@/stores/settings'
import {useUserStore} from '@/stores/user'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import {useI18nFormat} from '@/utils/i18nFormat'
import type {FormInst, FormRules} from 'naive-ui'
import {
  NBadge,
  NButton,
  NCollapse,
  NCollapseItem,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NNumberAnimation,
  NPagination,
  NPopconfirm,
  NSpin,
  NSwitch,
  NTag,
  NTooltip,
  useMessage
} from 'naive-ui'
import {
  changePassword,
  getEstimateParams,
  getProfile,
  login as apiLogin,
  redeemPoints,
  refreshUserInfoByEmail,
  sendEmailCode,
  updateProfileName
} from '@/api'

import {countHistory, deleteHistory, listHistory} from '@/utils/indexedDb'
import ImagePreviewer from '@/components/ImagePreviewer.vue'

const settings = useSettingsStore()
const {t} = useI18n()
const i18nObjForLang = useI18n()
const {formatDate} = useI18nFormat()
const router = useRouter()
const user = useUserStore()
const message = useMessage()
// 使用全局用户 Store 控制登录弹窗
const loginForm = reactive({email: '', password: ''})
const formRef = ref<FormInst | null>(null)
const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const pwdRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@._!#$%^&*-]{6,16}$/
const codeRegex = /^\d{6}$/
const rules: FormRules = {
  email: [
    {required: true, message: () => t('messages.email_required'), trigger: ['input', 'blur']},
    {
      validator: (_, v) => emailRegex.test(String(v || '')),
      message: () => t('messages.email_invalid'),
      trigger: ['input', 'blur']
    }
  ],
  password: [
    {required: true, message: () => t('messages.password_required'), trigger: ['input', 'blur']},
    {
      validator: (_, v) => pwdRegex.test(String(v || '').trim()),
      message: () => t('messages.password_invalid'),
      trigger: ['input', 'blur']
    }
  ],
}
const loginValid = computed(() => emailRegex.test(loginForm.email.trim()) && pwdRegex.test(loginForm.password.trim()))

function mapLang(v: string) {
  const base = v.toLowerCase()
  if (base.startsWith('zh-tw') || base.startsWith('zh-hant')) return 'zh-TW'
  if (base.startsWith('zh-cn') || base.startsWith('zh-hans') || base === 'zh') return 'zh-CN'
  const allow = ['en', 'ja', 'ko', 'es', 'fr', 'de', 'ru', 'pt', 'it', 'nl', 'tr', 'pl', 'sv', 'cs', 'hu', 'uk', 'vi', 'id']
  const code = base.split('-')[0]
  return allow.includes(code) ? code : 'auto'
}

const tsLang = computed(() => mapLang(String((i18nObjForLang as any).locale.value || 'auto')))
const displayName = computed(() => user.name || user.email || t('messages.auth_required'))
const nameParts = computed(() => {
  const base = String(displayName.value || '')
  const tag = user.profileReady && typeof user.nickTag === 'number'
      ? String(user.nickTag).padStart(4, '0')
      : ''
  return {base, tag}
})
const tierTagStyle = computed(() => {
  const dark = settings.darkMode
  const t = user.tier
  if (t === 'FREE') {
    return dark
        ? {color: '#0000', textColor: '#e5e7eb', borderColor: 'rgba(255,255,255,0.24)'}
        : {color: '#0000', textColor: '#6b7280', borderColor: 'rgba(0,0,0,0.18)'}
  }
  if (t === 'PLUS') {
    return dark
        ? {color: 'rgba(16,185,129,0.18)', textColor: '#2dd4bf', borderColor: '#2dd4bf'}
        : {color: 'rgba(16,185,129,0.10)', textColor: '#10b981', borderColor: '#34d399'}
  }
  return dark
      ? {color: 'rgba(139,92,246,0.18)', textColor: '#a78bfa', borderColor: '#a78bfa'}
      : {color: 'rgba(139,92,246,0.10)', textColor: '#8b5cf6', borderColor: '#a78bfa'}
})
const showRedeemMobile = ref(false)
const loginPwdVisible = ref(false)
const loginLoading = ref(false)
const prevPoints = ref(0)
// 绑定相关已移除
const nameMobile = ref(nameParts.value.base)
const oldPwd = ref('')
const newPwd = ref('')
const confirmPwd = ref('')
const emailCode = ref('')
const profileFormRef = ref<FormInst | null>(null)
const profileForm = reactive({name: nameParts.value.base, oldPwd: '', newPwd: '', confirmPwd: '', emailCode: ''})

const profileRules: FormRules = {
  newPwd: [
    {required: true, message: () => t('messages.password_required'), trigger: ['input', 'blur']},
    {
      validator: (_, v) => pwdRegex.test(String(v || '').trim()),
      message: () => t('messages.password_invalid'),
      trigger: ['input', 'blur']
    },
  ],
  oldPwd: [
    {required: true, message: () => t('messages.password_required'), trigger: ['input', 'blur']},
  ],
  confirmPwd: [
    {required: true, message: () => t('messages.confirm_password_required'), trigger: ['input', 'blur']},
    {
      validator: (_, v) => String(v || '').trim() === String(profileForm.newPwd || '').trim(),
      message: () => t('messages.new_pwd_mismatch'),
      trigger: ['input', 'blur']
    },
  ],
  code: [
    {required: true, message: () => t('messages.code_required'), trigger: ['input', 'blur']},
    {
      validator: (_, v) => codeRegex.test(String(v || '').trim()),
      message: () => t('messages.code_rule'),
      trigger: ['input', 'blur']
    },
  ],
}
const sendCodeLoading = ref(false)
const sendCodeCd = ref(0)
const sendCodeLabel = computed(() => {
  const base = t('profile.modal.send_code')
  return sendCodeCd.value > 0 ? `${base}(${sendCodeCd.value}s)` : base
})
let sendCodeTimer: any = null
const redeemKeyMobile = ref('')
const redeemingMobile = ref(false)
const showHistory = ref(false)
const loadingHistory = ref(false)
const records = ref<any[]>([])
const viewRecords = ref<any[]>([])
const expanded = ref<Set<string>>(new Set())
const previewShow = ref(false)
const previewSrc = ref('')
const historyCount = ref(0)
const showLogout = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 绑定相关已移除

function onToggleDark(v: boolean) {
  settings.darkMode = v
  document.documentElement.classList.toggle('dark', v)
}

async function sendCode() {
  const targetEmail = user.email
  if (!targetEmail) return
  if (!emailRegex.test(targetEmail)) {
    message.error(t('messages.email_invalid'));
    return
  }
  if (sendCodeLoading.value || sendCodeCd.value > 0) return
  sendCodeLoading.value = true
  try {
    await sendEmailCode(targetEmail, 'RESET')
    message.success(t('messages.code_sent'))
    sendCodeCd.value = 60
    if (sendCodeTimer) clearInterval(sendCodeTimer)
    sendCodeTimer = setInterval(() => {
      sendCodeCd.value = Math.max(0, sendCodeCd.value - 1)
      if (sendCodeCd.value === 0) {
        clearInterval(sendCodeTimer)
        sendCodeTimer = null
      }
    }, 1000)
  } catch (e: any) {
    message.error(e?.message || t('messages.send_failed'))
  } finally {
    sendCodeLoading.value = false
  }
}

async function login() {
  try {
    if (loginLoading.value) return
    loginLoading.value = true
    if (!loginValid.value) {
      message.error(t('messages.check_inputs'));
      loginLoading.value = false
      return
    }

    const tokenRes = await apiLogin(loginForm.email.trim(), loginForm.password.trim())
    user.token = tokenRes.access_token
    let profile: any
    try {
      profile = await getProfile()
    } catch {
      profile = await refreshUserInfoByEmail()
    }
    user.setProfile({...profile, token: tokenRes.access_token})
    try { await getEstimateParams() } catch {}
    user.showLoginModal = false
    loginForm.email = ''
    loginForm.password = ''
    message.success(t('messages.login_success'))

    // 历史记录模块已移除
  } catch (e: any) {
    const status = e?.status
    if (status === 401 || status === 403) {
      const expired = !!(e?.__authExpired)
      message.error(expired ? t('messages.auth_expired') : t('messages.auth_required'))
      user.requireLogin()
    } else if (status === 500) {
      message.error(t('messages.service_error'))
    } else {
      message.error(e?.message || t('messages.login_failed'))
    }
  } finally {
    loginLoading.value = false
  }
}


function goRegister() {
  user.showLoginModal = false
  router.push('/register')
}

function goForgot() {
  user.showLoginModal = false
  router.push('/forgot')
}


async function saveProfile() {
  if (!user.profileReady) {
    message.error(t('messages.please_login'))
    user.requireLogin()
    return
  }
  const newName = String(profileForm.name || '').trim()
  const doName = !!newName && newName !== user.name
  const hasPwdInput = !!(profileForm.oldPwd || profileForm.newPwd || profileForm.confirmPwd || profileForm.emailCode)

  try {
    if (hasPwdInput) {
      await profileFormRef.value?.validate()
      await changePassword(
          user.email,
          String(profileForm.oldPwd || '').trim(),
          String(profileForm.newPwd || '').trim(),
          String(profileForm.emailCode || '').trim()
      )
      profileForm.oldPwd = ''
      profileForm.newPwd = ''
      profileForm.confirmPwd = ''
      profileForm.emailCode = ''

    }

    if (doName) {
      await updateProfileName(newName)
      const refreshed = await getProfile()
      user.setProfile({...refreshed})
    }

    if (!hasPwdInput && !doName) {
      message.warning(t('messages.no_changes'))
      return
    }

    user.showProfileModal = false
    message.success(t('messages.saved_user_info'))
  } catch (e: any) {
    const status = e?.status
    if (status === 401 || status === 403) {
      const expired = !!(e?.__authExpired)
      message.error(expired ? t('messages.auth_expired') : t('messages.auth_required'))
      user.requireLogin()
    } else if (status === 500) {
      message.error(t('messages.service_error'))
    } else {
      message.error(e?.message || t('messages.delete_failed'))
    }
  }
}


function redeemMobile() {
  if (!user.profileReady) {
    message.error(t('messages.please_login'))
    user.requireLogin()
    return
  }
  if (redeemKeyMobile.value.trim()) {
    redeemingMobile.value = true
    redeemPoints(user.email, redeemKeyMobile.value.trim())
        .then(async (res) => {
          const addedRaw = Number((res as any)?.pointsAdded ?? (res as any)?.points_added ?? 0)
          const addedView = Number.isFinite(addedRaw) ? addedRaw : 0
          try {
            const profile = await getProfile()
            user.setProfile({...profile})
          } catch {
          }
          message.success(t('redeem.success', {points: addedView.toFixed(0)}))
          redeemKeyMobile.value = ''
          showRedeemMobile.value = false
        })
        .catch((e: any) => {
          const status = e?.status
          if (status === 401 || status === 403) {
            const expired = !!(e?.__authExpired)
            message.error(expired ? t('messages.auth_expired') : t('messages.auth_required'))
            user.requireLogin()
          } else if (status === 500) {
            message.error(t('messages.service_error'))
          } else {
            message.error(e?.message || t('redeem.failed'))
          }
        })
        .finally(() => {
          redeemingMobile.value = false
        })
  }
}

// 绑定相关不再使用

onMounted(async () => {
  user.init()
  prevPoints.value = 0
  try {
    const profile = await getProfile()
    user.setProfile({...profile, token: user.token})
    try { await getEstimateParams() } catch {}
  } catch {
    if (!user.profileReady) user.requireLogin()
  }
  if (user.profileReady && user.email) {
    try {
      const cnt = await countHistory(user.email || '')
      historyCount.value = Number(cnt || 0)
    } catch {
      historyCount.value = 0
    }
  }
})
watch(() => user.points, (nv, ov) => {
  if (typeof ov === 'number') {
    prevPoints.value = ov
  }
})

watch(() => user.email, async (nv) => {
  if (user.profileReady && nv) {
    try {
      const cnt = await countHistory(user.email || '')
      historyCount.value = Number(cnt || 0)
    } catch {
      historyCount.value = 0
    }
  } else {
    historyCount.value = 0
  }
})

watch(() => user.showProfileModal, (v) => {
  if (v) profileForm.name = nameParts.value.base
})

watch(() => user.showLoginModal, (v) => {
})

function openHistory() {
  if (!user.profileReady) {
    message.error('请先登录')
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
    historyCount.value = total.value
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
    historyCount.value = 0
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
    showLogout.value = false
  }
}

onUnmounted(() => {
  if (sendCodeTimer) clearInterval(sendCodeTimer)
})

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
  if (abs < 1000) return `${n}`
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
  return `${str}${suf}`
}

function placeholder(index: number) {
  const w = 512, h = 320
  return `https://placehold.co/${w}x${h}?text=IMG${index + 1}`
}

async function waitToken(tokenRef: any, timeoutMs: number) {
  const start = Date.now()
  return await new Promise<boolean>((resolve) => {
    const tick = () => {
      if (tokenRef.value) return resolve(true)
      if (Date.now() - start >= timeoutMs) return resolve(false)
      setTimeout(tick, 120)
    }
    tick()
  })
}
</script>

<style scoped></style>
