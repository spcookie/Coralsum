<template>
  <n-config-provider :theme="theme">
    <n-message-provider>
      <div v-if="!isStandalone" class="min-h-screen flex tech-bg">
        <SideMenu/>
        <div class="flex-1 flex flex-col">
          <TopBar/>
          <router-view/>
        </div>
      </div>
      <div v-else class="min-h-screen tech-bg">
        <router-view/>
      </div>
    </n-message-provider>
  </n-config-provider>
</template>

<script lang="ts" setup>
import SideMenu from '@/components/SideMenu.vue'
import TopBar from '@/components/TopBar.vue'
import {darkTheme, NConfigProvider, NMessageProvider} from 'naive-ui'
import {computed} from 'vue'
import {useRoute} from 'vue-router'
import {useSettingsStore} from '@/stores/settings'

const settings = useSettingsStore()
const theme = computed(() => (settings.darkMode ? darkTheme : null))
const route = useRoute()
const isStandalone = computed(() => route.name === 'register')
</script>

<style>
html, body, #app {
  font-family: "Geist Mono", "JetBrains Mono", monospace;
  font-weight: 500;
}
</style>