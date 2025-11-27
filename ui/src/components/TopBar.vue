<template>
  <div
      class="h-14 border-b border-transparent px-3 sm:px-4 flex items-center justify-between glass bg-white/40 dark:bg-black/30">
    <div class="font-medium text-neutral-700 dark:text-neutral-200 flex items-center gap-2">
      <Icon class="text-xl" icon="solar:stars-bold-duotone"/>
      <span>Coralsum AI 生图</span>
    </div>
    <div class="flex items-center gap-2 sm:gap-3 flex-wrap text-sm">
      <div class="hidden sm:flex items-center gap-2">
        <Icon class="text-lg" icon="mdi:account"/>
        <span>{{ displayName }}</span>
        <n-tooltip placement="bottom" trigger="hover">
          <template #trigger>
            <div class="flex items-center gap-1">
              <Icon class="text-yellow-500 text-base" icon="material-symbols:bolt-rounded"/>
              <span>{{ user.points }}</span>
            </div>
          </template>
          <div class="text-xs text-white space-y-1">
            <div>订阅积分：{{ user.subscribePoints }}</div>
            <div>永久积分：{{ user.permanentPoints }}</div>
          </div>
        </n-tooltip>
        <n-tag :color="tierTagStyle" class="inline-flex items-center justify-center leading-none" size="small">
          {{ user.tier }}
        </n-tag>
      </div>
      <div class="flex items-center gap-2">
        <n-button circle class="sm:hidden" quaternary size="small" @click="user.requireProfile()">
          <Icon icon="mdi:account-cog"/>
        </n-button>
        <n-button circle class="sm:hidden" quaternary size="small" @click="showRedeemMobile = true">
          <Icon icon="mdi:key"/>
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
      <span class="truncate max-w-[40vw]">{{ displayName }}</span>
      <div class="flex items-center gap-1">
        <Icon class="text-yellow-500" icon="material-symbols:bolt-rounded"/>
        <span>{{ user.points }}</span>
      </div>
      <n-tag :color="tierTagStyle" class="inline-flex items-center justify-center leading-none" size="small">
        {{ user.tier }}
      </n-tag>
    </div>
  </div>
  <n-modal v-model:show="user.showProfileModal" :style="{ width: '520px', maxWidth: '92vw' }" preset="card"
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
  <n-modal v-model:show="showRedeemMobile" :style="{ width: '420px', maxWidth: '92vw' }" preset="card"
           title="兑换积分密钥">
    <div class="space-y-3">
      <n-input v-model:value="redeemKeyMobile" placeholder="输入密钥"/>
      <div class="flex gap-2 justify-end">
        <n-button @click="showRedeemMobile=false">取消</n-button>
        <n-button type="primary" @click="redeemMobile">兑换</n-button>
      </div>
    </div>
  </n-modal>
  <n-modal v-model:show="user.showLoginModal" :style="{ width: '520px', maxWidth: '92vw' }" preset="card" title="登录">
    <n-form ref="formRef" :model="loginForm" :rules="rules">
      <div class="space-y-3">
        <n-form-item path="email">
          <n-input v-model:value="loginForm.email" placeholder="邮箱"/>
        </n-form-item>
        <n-form-item path="password">
          <n-input v-model:value="loginForm.password" maxlength="16" placeholder="密码" type="password"/>
        </n-form-item>
        <div class="flex gap-2 justify-between items-center">
          <n-button quaternary @click="goRegister">注册</n-button>
          <div class="flex gap-2">
            <n-button :disabled="!loginValid" type="primary" @click="login">登录</n-button>
          </div>
        </div>
      </div>
    </n-form>
  </n-modal>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {Icon} from '@iconify/vue'
import {useSettingsStore} from '@/stores/settings'
import {useUserStore} from '@/stores/user'
import type {FormInst, FormRules} from 'naive-ui'
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NForm,
  NFormItem,
  NInput,
  NModal,
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
// 绑定相关已移除
const nameMobile = ref(user.name)
const oldPwd = ref('')
const newPwd = ref('')
const confirmPwd = ref('')
const emailCode = ref('')
const redeemKeyMobile = ref('')

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
  await sendEmailCode(targetEmail)
}

async function login() {
  try {
    if (!loginValid.value) {
      message.error('请检查输入项');
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
  }
}

function goRegister() {
  user.showLoginModal = false
  router.push('/register')
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
      const latest = await updateProfileName(newName)
      user.setProfile({...latest})
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
  try {
    const profile = await getProfile()
    user.setProfile({...profile, token: user.token})
  } catch {
    if (!user.profileReady) user.requireLogin()
  }
})
</script>

<style scoped></style>
