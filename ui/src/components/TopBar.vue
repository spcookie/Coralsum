<template>
  <div
      class="h-14 border-b border-transparent px-3 sm:px-4 flex items-center justify-between glass bg-white/40 dark:bg-black/30">
    <div class="font-medium text-neutral-700 dark:text-neutral-200 flex items-center gap-2">
      <Icon class="text-xl" icon="solar:stars-bold-duotone"/>
      <span>Coralsum 生图</span>
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
            <div>订阅积分：{{ user.subscribePoints }}</div>
            <div>永久积分：{{ user.permanentPoints }}</div>
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
      <div v-if="user.profileReady" class="flex items-center gap-1">
        <Icon class="text-yellow-500" icon="material-symbols:bolt-rounded"/>
        <span class="inline-block underline underline-offset-2 decoration-[1px] decoration-solid">
          <n-number-animation :active="true" :duration="800" :from="prevPoints" :precision="0" :to="user.points"/>
        </span>
      </div>
      <n-tag v-if="user.profileReady" :color="tierTagStyle" class="inline-flex items-center justify-center leading-none"
             size="small">
        {{ user.tier }}
      </n-tag>
    </div>
  </div>
  <n-modal v-model:show="user.showProfileModal" :style="{ width: '520px', maxWidth: '92vw', margin: '0 auto' }"
           preset="card"
           title="用户信息">
    <div class="space-y-3">
      <div class="text-sm text-neutral-500">邮箱：{{ user.email }}</div>
      <n-input v-model:value="nameMobile" placeholder="名称"/>
      <n-collapse>
        <n-collapse-item name="pwd" title="修改密码">
          <div class="space-y-2">
            <n-input v-model:value="oldPwd" placeholder="原密码" type="password"/>
            <n-input v-model:value="newPwd" placeholder="新密码" type="password"/>
            <n-input v-model:value="confirmPwd" placeholder="确认密码" type="password"/>
            <div class="flex items-center gap-2">
              <n-input v-model:value="emailCode" placeholder="邮箱验证码"/>
              <n-button size="small" tertiary @click="sendCode">发送验证码</n-button>
            </div>
          </div>
        </n-collapse-item>
      </n-collapse>
      <div class="flex gap-2 justify-end">
        <n-button @click="user.showProfileModal=false">取消</n-button>
        <n-button type="primary" @click="saveProfile">保存</n-button>
      </div>
    </div>
  </n-modal>
  <n-modal v-model:show="showRedeemMobile" :style="{ width: '420px', maxWidth: '92vw', margin: '0 auto' }" preset="card"
           title="兑换积分密钥">
    <div class="space-y-3">
      <n-input v-model:value="redeemKeyMobile" placeholder="输入密钥"/>
      <div class="flex gap-2 justify-end">
        <n-button @click="showRedeemMobile=false">取消</n-button>
        <n-button type="primary" @click="redeemMobile">兑换</n-button>
      </div>
    </div>
  </n-modal>
  <n-modal v-model:show="user.showLoginModal" :style="{ width: '520px', maxWidth: '92vw', margin: '0 auto' }"
           preset="card" title="登录">
    <n-form ref="formRef" :model="loginForm" :rules="rules">
      <div class="space-y-3">
        <n-form-item path="email">
          <n-input v-model:value="loginForm.email" placeholder="邮箱"/>
        </n-form-item>
        <n-form-item path="password">
          <n-input v-model:value="loginForm.password" :type="loginPwdVisible ? 'text' : 'password'" maxlength="16"
                   placeholder="密码">
            <template #suffix>
              <Icon :icon="loginPwdVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                    class="cursor-pointer text-neutral-400" @click="loginPwdVisible = !loginPwdVisible"/>
            </template>
          </n-input>
        </n-form-item>
        <div class="flex gap-2 justify-between items-center">
          <div class="flex gap-2">
            <n-button quaternary @click="goRegister">注册</n-button>
            <n-button quaternary @click="goForgot">忘记密码</n-button>
          </div>
          <div class="flex gap-2">
            <n-button :disabled="!loginValid" :loading="loginLoading" type="primary" @click="login">登录</n-button>
          </div>
        </div>
      </div>
    </n-form>
  </n-modal>
  <n-modal v-model:show="showHistory" :style="{ width: 'min(96vw, 980px)', margin: '0 auto' }" preset="card">
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
            <div class="p-3 flex items-center gap-2">
              <div class="flex-1 flex flex-wrap items-center gap-1.5 text-[10px] sm:text-[11px]">
                <div
                    class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                  <Icon class="text-[0.75rem]" icon="ph:code"/>
                  <span class="font-medium">输入</span>
                  <span class="font-semibold tabular-nums">{{ formatTokens(r.inputTokens) }}</span>
                </div>
                <div
                    class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-fuchsia-50 dark:bg-fuchsia-900/30 text-fuchsia-700 dark:text-fuchsia-300 border border-fuchsia-200 dark:border-fuchsia-800">
                  <Icon class="text-[0.75rem]" icon="ph:sparkle"/>
                  <span class="font-medium">输出</span>
                  <span class="font-semibold tabular-nums">{{ formatTokens(r.outputTokens) }}</span>
                </div>
                <div
                    class="flex items-center gap-0.5 px-1 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300">
                  <Icon class="text-[0.75rem]" icon="ph:timer"/>
                  <span class="font-medium">耗时</span>
                  <span class="font-semibold tabular-nums">{{ formatDuration(r.durationMs) }}</span>
                </div>
                <div
                    class="flex items-center gap-0.5 px-1 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300">
                  <Icon class="text-[0.75rem]" icon="ph:image"/>
                  <span class="font-medium">图片</span>
                  <span class="font-semibold tabular-nums">{{ r.imageCount }}</span>
                </div>
                <div class="ml-auto text-[11px] text-neutral-500">{{ formatTime(r.createdAt) }}</div>
              </div>
              <div class="flex items-center gap-2">
                <n-button quaternary size="small" @click="toggle(r.id)">
                  <div class="flex items-center gap-1">
                    <Icon :icon="expanded.has(r.id) ? 'mdi:chevron-up' : 'mdi:chevron-down'"/>
                    <span>展开图片</span></div>
                </n-button>
                <n-popconfirm :show-icon="false" negative-text="取消" positive-text="删除"
                              @positive-click="removeRecord(r)">
                  <template #trigger>
                    <n-button quaternary size="small">
                      <div class="flex items-center gap-1 text-red-600">
                        <Icon icon="mdi:delete-outline"/>
                        <span>删除</span>
                      </div>
                    </n-button>
                  </template>
                  确认删除该条历史记录？
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
    <ImagePreviewer v-model:modelValue="previewShow" :src="previewSrc"/>
  </n-modal>
  <n-modal v-model:show="showLogout" :style="{ width: '420px', maxWidth: '92vw', margin: '0 auto' }" preset="card"
           title="退出登录">
    <div class="space-y-3">
      <div>确定要退出登录吗？</div>
      <div class="flex gap-2 justify-end">
        <n-button @click="showLogout=false">取消</n-button>
        <n-button type="error" @click="doLogout">确认退出</n-button>
      </div>
    </div>
  </n-modal>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {Icon} from '@iconify/vue'
import {useSettingsStore} from '@/stores/settings'
import {useUserStore} from '@/stores/user'
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
  NPopconfirm,
  NSpin,
  NSwitch,
  NTag,
  NTooltip,
  useMessage
} from 'naive-ui'
import {
  changePassword,
  getProfile,
  login as apiLogin,
  redeemPoints,
  refreshUserInfoByEmail,
  sendEmailCode,
  updateProfileName
} from '@/api'
import {deleteHistory, listHistory} from '@/utils/indexedDb'
import ImagePreviewer from '@/components/ImagePreviewer.vue'

const settings = useSettingsStore()
const router = useRouter()
const user = useUserStore()
const message = useMessage()
// 使用全局用户 Store 控制登录弹窗
const loginForm = reactive({email: '', password: ''})
const formRef = ref<FormInst | null>(null)
const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const pwdRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,16}$/
const rules: FormRules = {
  email: [
    {required: true, message: '请输入邮箱', trigger: ['input', 'blur']},
    {validator: (_, v) => emailRegex.test(String(v || '')), message: '邮箱格式不正确', trigger: ['input', 'blur']}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: ['input', 'blur']},
    {
      validator: (_, v) => pwdRegex.test(String(v || '')),
      message: '密码需6-16位且含字母与数字',
      trigger: ['input', 'blur']
    }
  ],
}
const loginValid = computed(() => emailRegex.test(loginForm.email.trim()) && pwdRegex.test(loginForm.password.trim()))
const displayName = computed(() => user.name || user.email || '未登录')
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
const redeemKeyMobile = ref('')
const showHistory = ref(false)
const loadingHistory = ref(false)
const records = ref<any[]>([])
const viewRecords = ref<any[]>([])
const expanded = ref<Set<string>>(new Set())
const previewShow = ref(false)
const previewSrc = ref('')
const historyCount = ref(0)
const showLogout = ref(false)

// 绑定相关已移除

function onToggleDark(v: boolean) {
  settings.darkMode = v
  document.documentElement.classList.toggle('dark', v)
}

async function sendCode() {
  const targetEmail = user.email
  if (!targetEmail) return
  if (!emailRegex.test(targetEmail)) {
    message.error('邮箱格式不正确');
    return
  }
  await sendEmailCode(targetEmail, 'RESET')
}

async function login() {
  try {
    if (loginLoading.value) return
    if (!loginValid.value) {
      message.error('请检查输入项');
      return
    }
    loginLoading.value = true
    const tokenRes = await apiLogin(loginForm.email.trim(), loginForm.password.trim())
    user.token = tokenRes.access_token
    let profile: any
    try {
      profile = await getProfile()
    } catch {
      profile = await refreshUserInfoByEmail()
    }
    user.setProfile({...profile, token: tokenRes.access_token})
    user.showLoginModal = false
    loginForm.email = ''
    loginForm.password = ''
    message.success('登录成功')
    // 历史记录模块已移除
  } catch (e: any) {
    const status = e?.status
    if (status === 401 || status === 403) {
      message.error('未登录或未授权，请先登录')
      user.requireLogin()
    } else if (status === 500) {
      message.error('服务异常，请稍后重试')
    } else {
      message.error(e?.message || '登录失败')
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
    message.error('请先登录')
    user.requireLogin()
    return
  }
  const newName = nameMobile.value.trim()
  const doName = !!newName && newName !== user.name
  const hasPwdInput = !!(oldPwd.value || newPwd.value || confirmPwd.value || emailCode.value)

  try {
    if (hasPwdInput) {
      if (!oldPwd.value || !newPwd.value || !confirmPwd.value || !emailCode.value) {
        message.error('请完整填写原密码、新密码、确认密码与邮箱验证码')
        return
      }
      if (newPwd.value.length < 6) {
        message.error('新密码长度需至少 6 位')
        return
      }
      if (newPwd.value !== confirmPwd.value) {
        message.error('两次输入的新密码不一致')
        return
      }
      await changePassword(user.email, oldPwd.value.trim(), newPwd.value.trim(), emailCode.value.trim())
      oldPwd.value = ''
      newPwd.value = ''
      confirmPwd.value = ''
      emailCode.value = ''
    }

    if (doName) {
      await updateProfileName(newName)
      const refreshed = await getProfile()
      user.setProfile({...refreshed})
    }

    if (!hasPwdInput && !doName) {
      message.warning('未检测到可保存的更改')
      return
    }

    user.showProfileModal = false
    message.success('已保存用户信息')
  } catch (e: any) {
    const status = e?.status
    if (status === 401 || status === 403) {
      message.error('未登录或未授权，请先登录')
      user.requireLogin()
    } else if (status === 500) {
      message.error('服务异常，请稍后重试')
    } else {
      message.error(e?.message || '保存失败')
    }
  }
}


function redeemMobile() {
  if (!user.profileReady) {
    message.error('请先登录')
    user.requireLogin()
    return
  }
  if (redeemKeyMobile.value.trim()) {
    redeemPoints(user.email, redeemKeyMobile.value.trim())
        .then(res => {
          user.addPoints(res.pointsAdded)
          redeemKeyMobile.value = ''
          showRedeemMobile.value = false
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

// 绑定相关不再使用

onMounted(async () => {
  user.init()
  prevPoints.value = 0
  try {
    const profile = await getProfile()
    user.setProfile({...profile, token: user.token})
  } catch {
    if (!user.profileReady) user.requireLogin()
  }
  if (user.profileReady && user.email) {
    try {
      const list = await listHistory(user.email || '', 100, 0)
      historyCount.value = Array.isArray(list) ? list.length : 0
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
      const list = await listHistory(user.email || '', 100, 0)
      historyCount.value = Array.isArray(list) ? list.length : 0
    } catch {
      historyCount.value = 0
    }
  } else {
    historyCount.value = 0
  }
})

watch(() => user.showProfileModal, (v) => {
  if (v) nameMobile.value = nameParts.value.base
})

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
    historyCount.value = Array.isArray(list) ? list.length : 0
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
    message.success('已退出登录')
  } finally {
    showLogout.value = false
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
    records.value = records.value.filter((it) => it.id !== r.id)
    viewRecords.value = viewRecords.value.filter((it) => it.id !== r.id)
    expanded.value.delete(r.id)
    historyCount.value = records.value.length
    message.success('已删除该条记录')
  } catch {
    message.error('删除失败')
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

<style scoped></style>
