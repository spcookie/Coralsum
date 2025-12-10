<template>
  <div class="flex flex-col md:flex-row md:h-[calc(100vh-56px)]">
    <div
        class="shrink-0 w-full md:w-52 border-r border-neutral-200 dark:border-neutral-800 glass bg-white/40 dark:bg-black/30">
      <div class="p-3">
        <div class="text-sm font-medium text-neutral-700 dark:text-neutral-200">{{ t('tools.title') }}</div>
      </div>
      <div class="md:hidden px-3 pb-2">
        <n-select v-model:value="active" :options="selectOptions" size="small"/>
      </div>
      <div class="hidden md:block px-2 pb-2">
        <n-menu :options="menuOptions" :value="active" @update:value="onSelect"/>
      </div>
    </div>
    <div class="flex-1 md:h-full glass bg-white/40 dark:bg-black/30">
      <div class="h-full overflow-y-auto">
        <div v-if="active==='meme-slicer'" class="p-3">
          <MemeSlicer/>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {h, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {NMenu, NSelect} from 'naive-ui'
import {Icon} from '@iconify/vue'
import MemeSlicer from '@/components/tools/MemeSlicer.vue'

const {t} = useI18n()
const active = ref('meme-slicer')

const menuOptions = [
  {
    key: 'meme-slicer',
    label: t('tools.meme_slicer.title'),
    icon: () => h(Icon, {icon: 'mdi:scissors'})
  }
]

const selectOptions = menuOptions.map(it => ({label: it.label as string, value: it.key as string}))

function onSelect(k: string) {
  active.value = k
}
</script>

<style scoped>
</style>
