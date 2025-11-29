<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <n-card style="max-width: 520px; width: 92vw" title="忘记密码">
      <n-form ref="formRef" :model="form" :rules="rules">
        <div class="space-y-3">
          <n-form-item path="email">
            <n-input v-model:value="form.email" placeholder="邮箱"/>
          </n-form-item>
          <n-form-item path="newPassword">
            <n-input v-model:value="form.newPassword" :type="newPwdVisible ? 'text' : 'password'" maxlength="16"
                     placeholder="新密码">
              <template #suffix>
                <Icon :icon="newPwdVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                      class="cursor-pointer text-neutral-400" @click="newPwdVisible = !newPwdVisible"/>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item path="confirm">
            <n-input v-model:value="form.confirm" :type="confirmVisible ? 'text' : 'password'" maxlength="16"
                     placeholder="确认密码">
              <template #suffix>
                <Icon :icon="confirmVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'"
                      class="cursor-pointer text-neutral-400" @click="confirmVisible = !confirmVisible"/>
              </template>
            </n-input>
          </n-form-item>
          <div class="flex items-center gap-2">
            <n-form-item class="flex-1" path="code">
              <n-input v-model:value="form.code" maxlength="6" placeholder="邮箱验证码"/>
            </n-form-item>
            <n-button size="small" tertiary @click="sendCode">发送验证码</n-button>
          </div>
          <div class="flex gap-2 justify-end">
            <n-button @click="goLogin">返回登录</n-button>
            <n-button :disabled="!valid" :loading="resetLoading" type="primary" @click="doReset">重置密码</n-button>
          </div>
        </div>
      </n-form>
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import type {FormInst, FormRules} from 'naive-ui'
import {NButton, NCard, NForm, NFormItem, NInput, useMessage} from 'naive-ui'
import {Icon} from '@iconify/vue'
import {resetPassword, sendEmailCode} from '@/api'
import {useUserStore} from '@/stores/user'

const router = useRouter()
const message = useMessage()
const user = useUserStore()
const formRef = ref<FormInst | null>(null)
const form = reactive({email: '', newPassword: '', confirm: '', code: ''})
const resetLoading = ref(false)
const newPwdVisible = ref(false)
const confirmVisible = ref(false)
const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const pwdRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,16}$/
const codeRegex = /^\d{4,6}$/
const rules: FormRules = {
  email: [
    {required: true, message: '请输入邮箱', trigger: ['input', 'blur']},
    {validator: (_, v) => emailRegex.test(String(v || '')), message: '邮箱格式不正确', trigger: ['input', 'blur']}
  ],
  newPassword: [
    {required: true, message: '请输入新密码', trigger: ['input', 'blur']},
    {
      validator: (_, v) => pwdRegex.test(String(v || '')),
      message: '密码需6-16位且含字母与数字',
      trigger: ['input', 'blur']
    }
  ],
  confirm: [
    {required: true, message: '请确认密码', trigger: ['input', 'blur']},
    {validator: (_, v) => String(v || '') === form.newPassword, message: '两次输入不一致', trigger: ['input', 'blur']}
  ],
  code: [
    {required: true, message: '请输入验证码', trigger: ['input', 'blur']},
    {validator: (_, v) => codeRegex.test(String(v || '')), message: '验证码为4-6位数字', trigger: ['input', 'blur']}
  ]
}
const valid = computed(() => emailRegex.test(form.email.trim()) && pwdRegex.test(form.newPassword.trim()) && form.confirm === form.newPassword && codeRegex.test(form.code.trim()))

async function sendCode() {
  const email = form.email.trim()
  if (!emailRegex.test(email)) {
    message.error('请先填写正确邮箱');
    return
  }
  await sendEmailCode(email, 'RESET')
}

async function doReset() {
  try {
    if (resetLoading.value) return
    if (!valid.value) {
      message.error('请检查输入项');
      return
    }
    resetLoading.value = true
    await resetPassword(form.email.trim(), form.newPassword.trim(), form.code.trim())
    message.success('重置成功，请登录')
    user.requireLogin()
    router.push('/')
  } catch (e: any) {
    message.error(e?.message || '重置失败')
  } finally {
    resetLoading.value = false
  }
}

function goLogin() {
  user.requireLogin()
  router.push('/')
}
</script>

<style scoped></style>
