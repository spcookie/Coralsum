<template>
  <n-config-provider :date-locale="naiveDateLocale" :locale="naiveLocale" :theme="theme">
    <n-message-provider>
      <n-dialog-provider>
        <div v-if="!isStandalone" class="md:h-screen md:overflow-hidden flex tech-bg">
          <SideMenu/>
          <div class="flex-1 flex flex-col md:overflow-hidden">
            <TopBar/>
            <router-view/>
          </div>
        </div>
        <div v-else class="min-h-screen tech-bg">
          <router-view/>
        </div>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script lang="ts" setup>
import {computed, defineAsyncComponent, onMounted, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {
  darkTheme,
  dateZhCN,
  dateEnUS,
  dateZhTW,
  enUS,
  zhCN,
  zhTW,
  NConfigProvider,
  NDialogProvider,
  NMessageProvider
} from 'naive-ui'
import {useRoute} from 'vue-router'
import {useSettingsStore} from '@/stores/settings'
import {useUserStore} from '@/stores/user'
import {useWsStore} from '@/stores/ws'

const SideMenu = defineAsyncComponent(() => import('@/components/SideMenu.vue'))
const TopBar = defineAsyncComponent(() => import('@/components/TopBar.vue'))

const settings = useSettingsStore()
const i18nObj = useI18n()
const theme = computed(() => (settings.darkMode ? darkTheme : null))
const route = useRoute()
const isStandalone = computed(() => route.name === 'register' || route.name === 'forgot')

const user = useUserStore()
const ws = useWsStore()

onMounted(() => {
  if (user.token) ws.connect(user.token)
})

watch(() => user.token, (nv) => {
  if (nv && nv.length > 0) ws.connect(nv)
  else ws.disconnect()
})

const naiveLocale = computed(() => {
  const lang = String((i18nObj as any).locale.value || 'en')
  if (lang === 'zh-CN') return zhCN
  if (lang === 'zh-TW') return zhTW
  return enUS
})
const naiveDateLocale = computed(() => {
  const lang = String((i18nObj as any).locale.value || 'en')
  if (lang === 'zh-CN') return dateZhCN
  if (lang === 'zh-TW') return dateZhTW
  return dateEnUS
})
</script>

<style>
html, body, #app {
  font-family: "Geist Mono", "JetBrains Mono", monospace;
  font-weight: 500;
}
</style>
