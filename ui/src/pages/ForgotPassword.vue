<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <n-card :title="t('auth.forgot_title')" style="max-width: 520px; width: 92vw">
      <n-form ref="formRef" :model="form" :rules="rules">
        <div class="space-y-3">
          <n-form-item path="email">
            <n-input v-model:value="form.email" :placeholder="t('profile.modal.email')"/>
          </n-form-item>
          <n-form-item path="newPassword">
            <n-input v-model:value="form.newPassword" :type="newPwdVisible ? 'text' : 'password'" maxlength="16"
                     :placeholder="t('profile.modal.new_password')">
              <template #suffix>
                <Icon :icon="newPwdVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                      class="cursor-pointer text-neutral-400" @click="newPwdVisible = !newPwdVisible"/>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item path="confirm">
            <n-input v-model:value="form.confirm" :type="confirmVisible ? 'text' : 'password'" maxlength="16"
                     :placeholder="t('profile.modal.confirm_password')">
              <template #suffix>
                <Icon :icon="confirmVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                      class="cursor-pointer text-neutral-400" @click="confirmVisible = !confirmVisible"/>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item path="code">
            <div class="flex items-center gap-2">
              <n-input v-model:value="form.code" :placeholder="t('profile.modal.email_code')" class="flex-1"
                       maxlength="6"/>
              <n-button :disabled="sendCodeCd > 0 || sendCodeLoading" :loading="sendCodeLoading" size="medium" tertiary
                        @click="sendCode">{{ sendCodeLabel }}
              </n-button>
            </div>
          </n-form-item>

          <div class="flex gap-2 justify-end">
            <n-button @click="goLogin">{{ t('auth.login.title') }}</n-button>
            <n-button :disabled="!valid" :loading="resetLoading"
                      type="primary" @click="doReset">{{
                t('auth.reset')
              }}
            </n-button>
          </div>
        </div>
      </n-form>
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, onUnmounted, reactive, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {useRouter} from 'vue-router'
import type {FormInst, FormRules} from 'naive-ui'
import {NButton, NCard, NForm, NFormItem, NInput, useMessage} from 'naive-ui'
import {Icon} from '@iconify/vue'
import {resetPassword, sendEmailCode} from '@/api'
import {useUserStore} from '@/stores/user'

import {useSettingsStore} from '@/stores/settings'

const router = useRouter()
const {t} = useI18n()
const i18nObjForLang = useI18n()

function mapLang(v: string) {
  const base = v.toLowerCase()
  if (base.startsWith('zh-tw') || base.startsWith('zh-hant')) return 'zh-TW'
  if (base.startsWith('zh-cn') || base.startsWith('zh-hans') || base === 'zh') return 'zh-CN'
  const allow = ['en', 'ja', 'ko', 'es', 'fr', 'de', 'ru', 'pt', 'it', 'nl', 'tr', 'pl', 'sv', 'cs', 'hu', 'uk', 'vi', 'id']
  const code = base.split('-')[0]
  return allow.includes(code) ? code : 'auto'
}
const message = useMessage()
const user = useUserStore()
const settings = useSettingsStore()
const formRef = ref<FormInst | null>(null)
const form = reactive({email: '', newPassword: '', confirm: '', code: ''})
const resetLoading = ref(false)
const newPwdVisible = ref(false)
const confirmVisible = ref(false)
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
      validator: (_, v) => emailRegex.test(String(v || '')),
      message: () => t('messages.email_invalid'),
      trigger: ['input', 'blur']
    }
  ],
  newPassword: [
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
      validator: (_, v) => String(v || '') === form.newPassword,
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
const valid = computed(() => emailRegex.test(form.email.trim()) && pwdRegex.test(form.newPassword.trim()) && form.confirm.trim() === form.newPassword.trim() && codeRegex.test(form.code.trim()))


async function sendCode() {
  const email = form.email.trim()
  if (!emailRegex.test(email)) {
    message.error(t('messages.email_invalid'));
    return
  }
  if (sendCodeLoading.value || sendCodeCd.value > 0) return
  sendCodeLoading.value = true
  try {
    await sendEmailCode(email, 'RESET')
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

async function doReset() {
  try {
    if (resetLoading.value) return
    if (!valid.value) {
      message.error(t('messages.check_inputs'));
      return
    }
    resetLoading.value = true
    await resetPassword(form.email.trim(), form.newPassword.trim(), form.code.trim())
    message.success(t('messages.reset_success') || '重置成功，请登录')
    user.requireLogin()
    router.push('/')
  } catch (e: any) {
    message.error(e?.message || t('auth.reset_failed') || '重置失败')
  } finally {
    resetLoading.value = false
  }
}

function goLogin() {
  user.requireLogin()
  router.push('/')
}

onUnmounted(() => {
  if (sendCodeTimer) clearInterval(sendCodeTimer)
})

onMounted(() => {
})

onUnmounted(() => {
})

</script>

<style scoped></style>
