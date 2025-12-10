<template>
  <n-modal v-model:show="show" :style="{ width: 'min(96vw, 880px)', margin: '0 auto' }" preset="card">
    <template #header>
      <div class="flex items-center gap-2">
        <span>{{ t('tools.meme_slicer.pick_history') }}</span>
      </div>
    </template>
    <div class="flex flex-col gap-3">
      <div class="flex items-center justify-between">
        <div class="text-xs text-neutral-500">{{ t('profile.history.count') }} {{ total }}</div>
        <div class="flex items-center gap-2">
          <n-button quaternary size="small" @click="loadHistory">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:refresh"/>
              <span>{{ t('profile.history.refresh') }}</span></div>
          </n-button>
        </div>
      </div>
      <n-spin :show="loading">
        <div v-if="records.length === 0" class="p-6">
          <div class="text-center text-neutral-500">{{ t('profile.history.empty') }}</div>
        </div>
        <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2">
          <div v-for="(img,i) in allImages" :key="i" class="rounded bg-neutral-100 dark:bg-neutral-800 cursor-pointer"
               @click="pick(img)">
            <img :src="img" class="w-full h-auto object-contain"/>
          </div>
        </div>
      </n-spin>
      <div class="mt-2 flex justify-end">
        <n-pagination :item-count="total" :page="page" :page-size="pageSize" @update:page="onPageChange"
                      @update:page-size="onPageSizeChange"/>
      </div>
    </div>
  </n-modal>
</template>

<script lang="ts" setup>
import {onMounted, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {Icon} from '@iconify/vue'
import {NButton, NModal, NPagination, NSpin} from 'naive-ui'
import {countHistory, listHistory} from '@/utils/indexedDb'
import {useUserStore} from '@/stores/user'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'pick', src: string): void }>()

const {t} = useI18n()
const user = useUserStore()
const show = ref(false)
const loading = ref(false)
const records = ref<any[]>([])
const allImages = ref<string[]>([])
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

onMounted(() => {
  show.value = !!props.modelValue
  loadHistory()
})

watch(() => props.modelValue, (v) => {
  show.value = !!v
})
watch(() => show.value, (v) => emit('update:modelValue', v))

async function loadHistory() {
  try {
    loading.value = true
    const [list, cnt] = await Promise.all([
      listHistory(user.email || '', pageSize.value, (page.value - 1) * pageSize.value),
      countHistory(user.email || '')
    ])
    records.value = list
    total.value = Number(cnt || 0)
    const imgs: string[] = []
    for (const r of list) {
      for (const u of (r.images || [])) {
        imgs.push(u)
      }
    }
    allImages.value = imgs
  } catch {
    records.value = []
    allImages.value = []
  } finally {
    loading.value = false
  }
}

function onPageChange(p: number) {
  page.value = p
  loadHistory()
}

function onPageSizeChange(s: number) {
  pageSize.value = s
  page.value = 1
  loadHistory()
}

function pick(src: string) {
  emit('pick', src)
  show.value = false
}
</script>

<style scoped>
</style>
