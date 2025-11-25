<template>
  <n-config-provider :theme="theme">
    <n-message-provider>
      <div class="min-h-screen flex tech-bg">
        <SideMenu/>
        <div class="flex-1 flex flex-col">
          <TopBar/>
          <router-view/>
        </div>
      </div>
    </n-message-provider>
  </n-config-provider>
</template>

<script lang="ts" setup>
import SideMenu from '@/components/SideMenu.vue'
import TopBar from '@/components/TopBar.vue'
import {darkTheme, NConfigProvider, NMessageProvider} from 'naive-ui'
import {computed} from 'vue'
import {useSettingsStore} from '@/stores/settings'

const settings = useSettingsStore()
const theme = computed(() => (settings.darkMode ? darkTheme : null))
</script>

<style scoped></style>