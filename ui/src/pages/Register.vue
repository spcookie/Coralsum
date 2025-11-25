<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <n-card style="max-width: 520px; width: 92vw" title="注册">
      <n-form ref="formRef" :model="form" :rules="rules">
        <div class="space-y-3">
          <n-form-item path="email">
            <n-input v-model:value="form.email" placeholder="邮箱"/>
          </n-form-item>
          <n-form-item path="password">
            <n-input v-model:value="form.password" maxlength="16" placeholder="密码" type="password"/>
          </n-form-item>
          <n-form-item path="confirm">
            <n-input v-model:value="form.confirm" maxlength="16" placeholder="确认密码" type="password"/>
          </n-form-item>
          <div class="flex items-center gap-2">
            <n-form-item class="flex-1" path="code">
              <n-input v-model:value="form.code" maxlength="6" placeholder="邮箱验证码"/>
            </n-form-item>
            <n-button size="small" tertiary @click="sendCode">发送验证码</n-button>
          </div>
          <div class="flex gap-2 justify-end">
            <n-button @click="goLogin">已有账号？去登录</n-button>
            <n-button :disabled="!valid" type="primary" @click="doRegister">注册</n-button>
          </div>
        </div>
      </n-form>
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import {ref, reactive, computed} from 'vue'
import {useRouter} from 'vue-router'
import {NCard, NForm, NFormItem, NInput, NButton, useMessage} from 'naive-ui'
import type {FormRules, FormInst} from 'naive-ui'
import {sendEmailCode, register} from '@/api'
import {useUserStore} from '@/stores/user'

const router = useRouter()
const message = useMessage()
const user = useUserStore()
const formRef = ref<FormInst | null>(null)
const form = reactive({email: '', password: '', confirm: '', code: ''})
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
  await sendEmailCode(email)
}

async function doRegister() {
  try {
    if (!valid.value) {
      message.error('请检查输入项');
      return
    }
    await register(form.email.trim(), form.password.trim(), form.code.trim())
    message.success('注册成功，请登录')
    user.requireLogin()
    router.push('/')
  } catch (e: any) {
    message.error(e?.message || '注册失败')
  }
}

function goLogin() {
  user.requireLogin()
  router.push('/')
}
</script>

<style scoped></style>