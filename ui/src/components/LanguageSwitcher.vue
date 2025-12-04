<template>
  <n-dropdown :options="options" trigger="hover" @select="onSelect">
    <n-button quaternary size="small">{{ currentLabel }}</n-button>
  </n-dropdown>
</template>

<script lang="ts" setup>
import {computed} from 'vue'
import {useI18n} from 'vue-i18n'
import {NButton, NDropdown} from 'naive-ui'
import http from '@/api/http'
import {useUserStore} from '@/stores/user'

const {locale} = useI18n()
const user = useUserStore()

const options = [
  {label: 'English', key: 'en'},
  {label: '简体中文', key: 'zh-CN'},
  {label: '繁體中文', key: 'zh-TW'}
]

const currentLabel = computed(() => {
  const found = options.find(o => o.key === locale.value)
  return found?.label || 'Lang'
})

function persist(lang: string) {
  try {
    localStorage.setItem('lang', lang)
    document.cookie = `lang=${lang};path=/;max-age=31536000`
  } catch {
  }
}

async function syncServer(lang: string) {
  try {
    if (user?.token) {
      await http.put('/web/profile/language', {language: lang})
    }
  } catch {
  }
}

async function onSelect(key: string) {
  const lang = String(key)
  locale.value = lang
  persist(lang)
  await syncServer(lang)
  try {
    const rawHash = location.hash || '#/'
    const parts = rawHash.split('?')
    const pathPart = parts[0]
    const usp = new URLSearchParams(parts[1] || '')
    usp.set('lang', lang)
    location.hash = `${pathPart}?${usp.toString()}`
    location.reload()
  } catch {
    location.reload()
  }
}
</script>

<style scoped></style>
