<template>
  <n-config-provider :date-locale="dateZhCN" :locale="zhCN" :theme="theme">
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
import SideMenu from '@/components/SideMenu.vue'
import TopBar from '@/components/TopBar.vue'
import {darkTheme, dateZhCN, NConfigProvider, NDialogProvider, NMessageProvider, zhCN} from 'naive-ui'
import {computed} from 'vue'
import {useRoute} from 'vue-router'
import {useSettingsStore} from '@/stores/settings'

const settings = useSettingsStore()
const theme = computed(() => (settings.darkMode ? darkTheme : null))
const route = useRoute()
const isStandalone = computed(() => route.name === 'register' || route.name === 'forgot')
</script>

<style>
html, body, #app {
  font-family: "Geist Mono", "JetBrains Mono", monospace;
  font-weight: 500;
}
</style>
