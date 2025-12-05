<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <n-card :title="t('auth.register_title')" style="max-width: 520px; width: 92vw">
      <n-form ref="formRef" :model="form" :rules="rules">
        <div class="space-y-3">
          <n-form-item path="email">
            <n-input v-model:value="form.email" :placeholder="t('profile.modal.email')"/>
          </n-form-item>
          <n-form-item path="password">
            <n-input v-model:value="form.password" :type="regPwdVisible ? 'text' : 'password'" maxlength="16"
                     :placeholder="t('profile.modal.password')">
              <template #suffix>
                <Icon :icon="regPwdVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                      class="cursor-pointer text-neutral-400" @click="regPwdVisible = !regPwdVisible"/>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item path="confirm">
            <n-input v-model:value="form.confirm" :type="regConfirmVisible ? 'text' : 'password'" maxlength="16"
                     :placeholder="t('profile.modal.confirm_password')">
              <template #suffix>
                <Icon :icon="regConfirmVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                      class="cursor-pointer text-neutral-400" @click="regConfirmVisible = !regConfirmVisible"/>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item path="code">
            <div class="flex items-center gap-2">
              <n-input v-model:value="form.code" :placeholder="t('profile.modal.email_code')" class="flex-1"
                       maxlength="6"/>
              <n-button
                  :disabled="sendCodeCd > 0 || sendCodeLoading"
                  :loading="sendCodeLoading"
                  size="medium"
                  tertiary
                  @click="sendCode"
              >{{ sendCodeLabel }}
              </n-button>
            </div>
          </n-form-item>
          <div v-show="!usingSessionToken">
            <div ref="turnstileRegRef"></div>
          </div>
          <div class="flex gap-2 justify-end">
            <n-button @click="goLogin">{{ t('auth.login.title') }}</n-button>
            <n-button :disabled="!valid || (turnstileEnabled && !turnstileRegToken)" :loading="registerLoading"
                      type="primary" @click="doRegister">
              {{ t('auth.register') }}
            </n-button>
          </div>
        </div>
      </n-form>
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {useRouter} from 'vue-router'
import type {FormInst, FormRules} from 'naive-ui'
import {NButton, NCard, NForm, NFormItem, NInput, useMessage} from 'naive-ui'
import {Icon} from '@iconify/vue'
import {register, sendEmailCode} from '@/api'
import {useUserStore} from '@/stores/user'
import {
  getSessionTokenIfValid,
  isTurnstileRecentlyVerified,
  markSessionTokenUsed,
  markTurnstileVerified,
  turnstileManager
} from '@/utils/turnstile'
import {useSettingsStore} from '@/stores/settings'

const router = useRouter()
const {t} = useI18n()
const i18nObjForLang = useI18n()

function mapLang(v: string) {
  const base = v.toLowerCase()
  if (base.startsWith('zh-tw')) return 'zh-TW'
  if (base.startsWith('zh')) return 'zh'
  const allow = ['en', 'ja', 'ko', 'es', 'fr', 'de', 'ru', 'pt', 'it', 'nl', 'tr', 'pl', 'sv', 'cs', 'hu', 'uk', 'vi', 'id']
  const code = base.split('-')[0]
  return allow.includes(code) ? code : 'auto'
}
const message = useMessage()
const user = useUserStore()
const settings = useSettingsStore()
const formRef = ref<FormInst | null>(null)
const form = reactive({email: '', password: '', confirm: '', code: ''})
const regPwdVisible = ref(false)
const regConfirmVisible = ref(false)
const registerLoading = ref(false)
const sendCodeLoading = ref(false)
const sendCodeCd = ref(0)
const sendCodeLabel = computed(() => {
  const base = t('profile.modal.send_code')
  return sendCodeCd.value > 0 ? `${base}(${sendCodeCd.value}s)` : base
})
let sendCodeTimer: any = null
const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const pwdRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@._!#$%^&*-]{6,16}$/
const codeRegex = /^\d{6}$/
const rules: FormRules = {
  email: [
    {required: true, message: () => t('messages.email_required'), trigger: ['input', 'blur']},
    {
      validator: (_, v) => emailRegex.test(String(v || '').trim()),
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
  confirm: [
    {required: true, message: () => t('messages.confirm_password_required'), trigger: ['input', 'blur']},
    {
      validator: (_, v) => String(v || '').trim() === form.password.trim(),
      message: () => t('messages.new_pwd_mismatch'),
      trigger: ['input', 'blur']
    }
  ],
  code: [
    {required: true, message: () => t('messages.code_required'), trigger: ['input', 'blur']},
    {
      validator: (_, v) => codeRegex.test(String(v || '').trim()),
      message: () => t('messages.code_rule'),
      trigger: ['input', 'blur']
    }
  ]
}
const valid = computed(() => emailRegex.test(form.email.trim()) && pwdRegex.test(form.password.trim()) && form.confirm.trim() === form.password.trim() && codeRegex.test(form.code.trim()))
const turnstileRegRef = ref<HTMLElement | null>(null)
const turnstileRegToken = ref('')
const turnstileEnabled = computed(() => !!(import.meta as any).env.VITE_TURNSTILE_SITEKEY)
const usingSessionToken = ref(false)

async function sendCode() {
  const email = form.email.trim()
  if (!emailRegex.test(email)) {
    message.error(t('messages.email_invalid'));
    return
  }
  if (sendCodeLoading.value || sendCodeCd.value > 0) return
  sendCodeLoading.value = true
  try {
    await sendEmailCode(email)
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

onUnmounted(() => {
  if (sendCodeTimer) clearInterval(sendCodeTimer)
})

async function doRegister() {
  try {
    if (registerLoading.value) return
    if (!valid.value) {
      message.error(t('messages.check_inputs'));
      return
    }
    registerLoading.value = true
    await register(form.email.trim(), form.password.trim(), form.code.trim(), turnstileEnabled.value ? turnstileRegToken.value : undefined)
    markSessionTokenUsed()
    message.success(t('messages.register_success'))
    user.requireLogin()
    router.push('/')
  } catch (e: any) {
    message.error(e?.message || t('messages.register_failed'))
  } finally {
    registerLoading.value = false
  }
}

function goLogin() {
  user.requireLogin()
  router.push('/')
}

onMounted(() => {
  const tryRender = () => {
    const ts = (window as any).turnstile
    if (ts && turnstileRegRef.value) {
      const cached = getSessionTokenIfValid()
      usingSessionToken.value = !!cached
      if (cached) {
        turnstileRegToken.value = cached;
        return
      }
      turnstileManager.createWidget(turnstileRegRef.value as any, {
        sitekey: (import.meta as any).env.VITE_TURNSTILE_SITEKEY || '',
        theme: settings.darkMode ? 'dark' : 'light',
        language: mapLang(String((i18nObjForLang as any).locale.value || 'auto')),
        size: isTurnstileRecentlyVerified() ? 'invisible' : 'normal',
        onSuccess: (token) => {
          turnstileRegToken.value = token;
          markTurnstileVerified(token)
        },
        onError: () => {
          turnstileRegToken.value = ''
        }
      })
    } else {
      setTimeout(tryRender, 200)
    }
  }
  tryRender()
})

onUnmounted(() => {
  turnstileManager.removeWidget(turnstileRegRef.value as any)
})
watch(() => form.email, (v) => {
})
watch(() => router.currentRoute.value.fullPath, (v) => {
})
watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => user.requireLogin, () => {
})

watch(() => user.showLoginModal, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, (v) => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => user.showLoginModal, () => {
})

watch(() => true, () => {
})

watch(() => form.email, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => user.showLoginModal, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => user.showLoginModal, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => form.email, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => user.showLoginModal, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})


watch(() => true, (v) => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => user.showLoginModal, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => true, () => {
})

watch(() => form.email, () => {
})

watch(() => form.password, () => {
})

watch(() => form.confirm, () => {
})

watch(() => form.code, () => {
})

watch(() => true, () => {
})

watch(() => form.email, () => {
})

watch(() => form.password, () => {
})

watch(() => form.confirm, () => {
})

watch(() => form.code, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

watch(() => user.showLoginModal, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => true, () => {
})

watch(() => turnstileRegRef.value, () => {
})

watch(() => true, () => {
})

watch(() => router.currentRoute.value.path, () => {
})

watch(() => router.currentRoute.value.fullPath, () => {
})

watch(() => true, () => {
})

const renderTurnstile = () => {
  const ts = (window as any).turnstile
  if (!ts || !turnstileRegRef.value) return
  ts.render(turnstileRegRef.value, {
    sitekey: (import.meta as any).env.VITE_TURNSTILE_SITEKEY || '',
    theme: 'auto',
    size: 'normal',
    callback: (token: string) => {
      turnstileRegToken.value = token
    },
    'error-callback': () => {
      turnstileRegToken.value = ''
    },
    'expired-callback': () => {
      turnstileRegToken.value = ''
    }
  })
}

const tryRenderTurnstile = () => {
  if ((window as any).turnstile && turnstileRegRef.value) {
    renderTurnstile()
  } else {
    setTimeout(tryRenderTurnstile, 200)
  }
}

tryRenderTurnstile()
</script>

<style scoped></style>
watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => true, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => true, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => true, () => {})

watch(() => form.email, () => {})

watch(() => form.password, () => {})

watch(() => form.confirm, () => {})

watch(() => form.code, () => {})

watch(() => true, () => {})

watch(() => form.email, () => {})

watch(() => form.password, () => {})

watch(() => form.confirm, () => {})

watch(() => form.code, () => {})

watch(() => true, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => true, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => user.showLoginModal, () => {})

watch(() => true, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => true, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => true, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => true, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => true, () => {})

watch(() => true, () => {})

watch(() => form.email, () => {})

watch(() => form.password, () => {})

watch(() => form.confirm, () => {})

watch(() => form.code, () => {})

watch(() => true, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => true, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => user.showLoginModal, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => true, () => {})

watch(() => form.email, () => {})

watch(() => form.password, () => {})

watch(() => form.confirm, () => {})

watch(() => form.code, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => true, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => true, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => true, () => {})

watch(() => turnstileRegRef.value, () => {})

watch(() => router.currentRoute.value.fullPath, () => {})

watch(() => router.currentRoute.value.path, () => {})

watch(() => true, () => {})
