<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <n-card style="max-width: 520px; width: 92vw" title="注册">
      <n-form ref="formRef" :model="form" :rules="rules">
        <div class="space-y-3">
          <n-form-item path="email">
            <n-input v-model:value="form.email" placeholder="邮箱"/>
          </n-form-item>
          <n-form-item path="password">
            <n-input v-model:value="form.password" :type="regPwdVisible ? 'text' : 'password'" maxlength="16"
                     placeholder="密码">
              <template #suffix>
                <Icon :icon="regPwdVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                      class="cursor-pointer text-neutral-400" @click="regPwdVisible = !regPwdVisible"/>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item path="confirm">
            <n-input v-model:value="form.confirm" :type="regConfirmVisible ? 'text' : 'password'" maxlength="16"
                     placeholder="确认密码">
              <template #suffix>
                <Icon :icon="regConfirmVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                      class="cursor-pointer text-neutral-400" @click="regConfirmVisible = !regConfirmVisible"/>
              </template>
            </n-input>
          </n-form-item>
          <div class="flex items-center gap-2">
            <n-form-item class="flex-1" path="code">
              <n-input v-model:value="form.code" maxlength="6" placeholder="邮箱验证码"/>
            </n-form-item>
            <n-button
                :disabled="sendCodeCd > 0 || sendCodeLoading"
                :loading="sendCodeLoading"
                size="small"
                tertiary
                @click="sendCode"
            >{{ sendCodeLabel }}
            </n-button>
          </div>
          <div class="flex gap-2 justify-end">
            <n-button @click="goLogin">已有账号？去登录</n-button>
            <n-button :disabled="!valid" :loading="registerLoading" type="primary" @click="doRegister">注册</n-button>
          </div>
        </div>
      </n-form>
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onUnmounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import type {FormInst, FormRules} from 'naive-ui'
import {NButton, NCard, NForm, NFormItem, NInput, useMessage} from 'naive-ui'
import {Icon} from '@iconify/vue'
import {register, sendEmailCode} from '@/api'
import {useUserStore} from '@/stores/user'

const router = useRouter()
const message = useMessage()
const user = useUserStore()
const formRef = ref<FormInst | null>(null)
const form = reactive({email: '', password: '', confirm: '', code: ''})
const regPwdVisible = ref(false)
const regConfirmVisible = ref(false)
const registerLoading = ref(false)
const sendCodeLoading = ref(false)
const sendCodeCd = ref(0)
const sendCodeLabel = computed(() => sendCodeCd.value > 0 ? `重新发送(${sendCodeCd.value}s)` : '发送验证码')
let sendCodeTimer: any = null
const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const pwdRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[A-Za-z\d]{8,}$/
const codeRegex = /^\d{4,6}$/
const rules: FormRules = {
  email: [
    {required: true, message: '请输入邮箱', trigger: ['input', 'blur']},
    {validator: (_, v) => emailRegex.test(String(v || '')), message: '邮箱格式不正确', trigger: ['input', 'blur']}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: ['input', 'blur']},
    {
      validator: (_, v) => pwdRegex.test(String(v || '')),
      message: '至少8位且含大小写字母和数字',
      trigger: ['input', 'blur']
    }
  ],
  confirm: [
    {required: true, message: '请确认密码', trigger: ['input', 'blur']},
    {validator: (_, v) => String(v || '') === form.password, message: '两次输入不一致', trigger: ['input', 'blur']}
  ],
  code: [
    {required: true, message: '请输入验证码', trigger: ['input', 'blur']},
    {validator: (_, v) => codeRegex.test(String(v || '')), message: '验证码为4-6位数字', trigger: ['input', 'blur']}
  ]
}
const valid = computed(() => emailRegex.test(form.email.trim()) && pwdRegex.test(form.password.trim()) && form.confirm === form.password && codeRegex.test(form.code.trim()))

async function sendCode() {
  const email = form.email.trim()
  if (!emailRegex.test(email)) {
    message.error('请先填写正确邮箱');
    return
  }
  if (sendCodeLoading.value || sendCodeCd.value > 0) return
  sendCodeLoading.value = true
  try {
    await sendEmailCode(email)
    message.success('验证码已发送，请注意查收')
    sendCodeCd.value = 5
    if (sendCodeTimer) clearInterval(sendCodeTimer)
    sendCodeTimer = setInterval(() => {
      sendCodeCd.value = Math.max(0, sendCodeCd.value - 1)
      if (sendCodeCd.value === 0) {
        clearInterval(sendCodeTimer)
        sendCodeTimer = null
      }
    }, 1000)
  } catch (e: any) {
    message.error(e?.message || '发送失败')
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
      message.error('请检查输入项');
      return
    }
    registerLoading.value = true
    await register(form.email.trim(), form.password.trim(), form.code.trim())
    message.success('注册成功，请登录')
    user.requireLogin()
    router.push('/')
  } catch (e: any) {
    message.error(e?.message || '注册失败')
  } finally {
    registerLoading.value = false
  }
}

function goLogin() {
  user.requireLogin()
  router.push('/')
}
</script>

<style scoped></style>
