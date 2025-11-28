<template>
  <div class="flex-1 p-4 space-y-4">
    <div v-if="!showOverlay" class="flex flex-wrap items-center gap-1 sm:gap-1.5 text-[10px] sm:text-[11px]">
      <div
          class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
        <Icon class="text-[0.75rem]" icon="ph:code"/>
        <span class="font-medium">输入</span>
        <span class="font-semibold tabular-nums">{{ formatTokens(result?.inputTokens ?? 0) }}</span>
      </div>
      <div
          class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-fuchsia-50 dark:bg-fuchsia-900/30 text-fuchsia-700 dark:text-fuchsia-300 border border-fuchsia-200 dark:border-fuchsia-800">
        <Icon class="text-[0.75rem]" icon="ph:sparkle"/>
        <span class="font-medium">输出</span>
        <span class="font-semibold tabular-nums">{{ formatTokens(result?.outputTokens ?? 0) }}</span>
      </div>
      <div
          class="flex items-center gap-0.5 px-1 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300">
        <Icon class="text-[0.75rem]" icon="ph:timer"/>
        <span class="font-medium">耗时</span>
        <span class="font-semibold tabular-nums">{{ result ? formatDuration(result.durationMs) : '-' }}</span>
      </div>
    </div>
    <div v-if="!loading && result?.text"
         class="p-3 border border-dashed rounded border-neutral-300 dark:border-neutral-700 min-h-16">
      <div>{{ result?.text }}</div>
    </div>
    <div v-if="allImagesLoaded"
         class="flex items-center gap-1.5 px-2 py-1 rounded-md bg-amber-50 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800 text-[11px]">
      <Icon class="text-[0.85rem]" icon="ph:warning"/>
      <span>图片预览链接有效时间为 30 分钟，且仅有 5 次有效预览次数，请注意保存图片！</span>
    </div>
    <div v-if="loading" class="flex flex-wrap justify-center gap-3">
      <div v-for="i in settings.candidateRadio" :key="i"
           class="rounded overflow-hidden bg-neutral-100 dark:bg-neutral-800 border border-dashed border-neutral-300 dark:border-neutral-700 w-full sm:w-[144px] md:w-[216px] lg:w-[288px] xl:w-[360px]">
        <div :style="skeletonAspectStyle" class="w-full">
          <div
              class="h-full w-full bg-gradient-to-r from-neutral-200 via-neutral-100 to-neutral-200 dark:from-neutral-700 dark:via-neutral-800 dark:to-neutral-700 animate-pulse"></div>
        </div>
      </div>
    </div>
    <div v-if="!loading && images.length > 0" class="flex flex-wrap justify-center gap-3">
      <div v-for="(img, i) in images" :key="i"
           class="rounded bg-neutral-100 dark:bg-neutral-800 w-full sm:w-[144px] md:w-[216px] lg:w-[288px] xl:w-[360px] relative">
        <div
            :style="document.documentElement.classList.contains('dark')
              ? 'background-color: rgba(59,130,246,0.85); color: #fff; border: 1px solid rgba(0,0,0,0.40)'
              : 'background-color: rgba(37,99,235,0.90); color: #fff; border: 1px solid rgba(255,255,255,0.70)'
            "
            class="img-badge"
        >
          {{ i + 1 }}
        </div>
        <div v-if="!loadedSet.has(i)" :style="skeletonAspectStyle" class="w-full">
          <div
              class="h-full w-full bg-gradient-to-r from-neutral-200 via-neutral-100 to-neutral-200 dark:from-neutral-700 dark:via-neutral-800 dark:to-neutral-700 animate-pulse"></div>
        </div>
        <img
            :class="loadedSet.has(i) ? 'opacity-100' : 'opacity-0'"
            :src="img"
            class="w-full h-auto object-contain cursor-zoom-in transition-opacity duration-200"
            @click="openPreview(img)"
            @error="onImgError($event, i)"
            @load="onImgLoad(i)"
        />
        <div class="absolute inset-x-0 bottom-0">
          <div class="h-12 bg-gradient-to-t from-black/65 to-transparent flex items-center justify-center">
            <button
                class="px-3 py-1.5 rounded-full bg-white/90 dark:bg-white/80 text-neutral-800 shadow-sm border border-white/60 hover:bg-white"
                title="复制分享链接" @click.stop="onShare(i)">
              <Icon class="text-[1rem]" icon="ph:share"/>
            </button>
          </div>
          <div v-if="shareIndex === i"
               class="absolute left-1/2 -translate-x-1/2 -translate-y-2 bottom-12 px-3 py-1.5 rounded bg-neutral-800 text-white text-xs shadow">
            {{ shareTip }}
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="showOverlay"
         class="flex items-center justify-center min-h-[50vh] sm:min-h-[56vh] lg:min-h-[60vh] p-6 text-center">
      <div class="typewriter">
        <span v-for="(c,i) in visibleChars" :key="i" class="fade-char tw-text">{{ c }}</span>
      </div>
    </div>
  </div>
  <ImagePreviewer v-model:modelValue="previewShow" :src="previewSrc"/>
</template>

<script lang="ts" setup>
import {computed, onUnmounted, ref, watch} from 'vue'
import {Icon} from '@iconify/vue'
import ImagePreviewer from '@/components/ImagePreviewer.vue'
import {useSettingsStore} from '@/stores/settings'

export interface Result {
  inputTokens: number
  outputTokens: number
  durationMs: number
  images: string[]
  linkImages?: string[]
  text?: string
}

const props = defineProps<{ result?: Result; loading?: boolean }>()
const settings = useSettingsStore()
const images = computed(() => {
  const arr = (props.result?.images ?? []).slice()
  for (let i = 0; i < arr.length; i++) {
    if (!arr[i]) arr[i] = themedPlaceholderImage(i)
  }
  return arr
})

const loadedSet = ref<Set<number>>(new Set())
const allImagesLoaded = computed(() => !props.loading && images.value.length > 0 && loadedSet.value.size >= images.value.length)

function placeholderImage(index: number) {
  const w = 512, h = 320;
  return `https://placehold.co/${w}x${h}?text=IMG${index + 1}`
}

function themedPlaceholder(width: number, height: number, label: string) {
  const dark = document.documentElement.classList.contains('dark')
  const bg = dark ? '#1f2937' : '#f3f4f6'
  const border = dark ? '#374151' : '#d1d5db'
  const text = dark ? '#9ca3af' : '#6b7280'
  const g1 = dark ? '#0ea5e9' : '#93c5fd'
  const g2 = dark ? '#8b5cf6' : '#a5b4fc'
  const fs = Math.max(12, Math.floor(height * 0.08))
  const sub = Math.max(10, Math.floor(height * 0.06))
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='${width}' height='${height}' viewBox='0 0 ${width} ${height}'>
<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop offset='0' stop-color='${g1}'/><stop offset='1' stop-color='${g2}'/></linearGradient></defs>
<rect width='100%' height='100%' fill='${bg}'/>
<rect x='0.5' y='0.5' width='${width - 1}' height='${height - 1}' fill='none' stroke='${border}' stroke-dasharray='6 6' rx='8'/>
<rect width='100%' height='100%' fill='url(#g)' opacity='0.08'/>
<g fill='${text}' font-family='system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif' text-anchor='middle'>
<text x='${width / 2}' y='${height / 2 - 6}' font-size='${fs}'>${label}</text>
<text x='${width / 2}' y='${height / 2 + fs / 2 + 16}' font-size='${sub}'>图片预览已失效</text>
</g>
</svg>`
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg)
}

function ratioWH(baseW = 512) {
  const ar = settings.aspectRatio
  const map: Record<string, [number, number]> = {
    '1:1': [1, 1],
    '2:3': [2, 3],
    '3:2': [3, 2],
    '3:4': [3, 4],
    '4:3': [4, 3],
    '9:16': [9, 16],
    '16:9': [16, 9],
    '21:9': [21, 9]
  }
  const [rw, rh] = map[ar] ?? map['1:1']
  const w = baseW
  const h = Math.round(baseW * (rh / rw))
  return {w, h}
}

function themedPlaceholderImage(index: number) {
  const {w, h} = ratioWH(512);
  return themedPlaceholder(w, h, `IMG${index + 1}`)
}


const previewShow = ref(false)
const previewSrc = ref('')
const shareIndex = ref<number | null>(null)
const shareTip = ref('')

function openPreview(src: string) {
  previewSrc.value = src;
  previewShow.value = true
}

function onImgError(e: Event, i: number) {
  const t = e.target as HTMLImageElement;
  t.src = themedPlaceholderImage(i);
  loadedSet.value.add(i)
}

function onImgLoad(i: number) {
  loadedSet.value.add(i)
}

async function onShare(i: number) {
  const link = props.result?.linkImages?.[i] ?? images.value[i]
  if (!link) return
  try {
    await navigator.clipboard.writeText(link)
    shareTip.value = '分享链接已复制，可直接发送'
  } catch {
    shareTip.value = '复制失败，请手动复制链接'
  }
  shareIndex.value = i
  setTimeout(() => {
    if (shareIndex.value === i) shareIndex.value = null
  }, 1600)
}

watch(images, () => {
  loadedSet.value = new Set()
})

const showOverlay = computed(() => {
  const hasImg = images.value.length > 0
  const hasText = !!props.result?.text
  const isLoading = !!props.loading
  return !hasImg && !hasText && !isLoading
})
const twText = '用文字驱动图像创作与编辑：生图、局部重绘、风格迁移与扩图一应俱全。Coralsum 保持角色一致与场景连贯，速度快、细节稳。'
const twChars = computed(() => Array.from(twText))
const typeIndex = ref(0)
const visibleChars = computed(() => twChars.value.slice(0, typeIndex.value))
let timer: any

function schedule() {
  const speed = 115
  timer = setTimeout(() => {
    if (typeIndex.value < twChars.value.length) {
      typeIndex.value++
      schedule()
    } else {
      clearTimeout(timer)
      timer = null
    }
  }, speed)
}

watch(showOverlay, (v) => {
  if (timer) {
    clearTimeout(timer);
    timer = null
  }
  if (v) {
    typeIndex.value = 0
    schedule()
  }
}, {immediate: true})
onUnmounted(() => {
  if (timer) clearTimeout(timer)
})

function formatDuration(ms: number) {
  const totalSeconds = ms / 1000
  const h = Math.floor(totalSeconds / 3600)
  const m = Math.floor((totalSeconds % 3600) / 60)
  const s = totalSeconds % 60
  const parts: string[] = []
  if (h > 0) parts.push(`${h}h`)
  if (m > 0 || h > 0) parts.push(`${m}m`)
  parts.push(`${s.toFixed(2)}s`)
  return parts.join('')
}

function formatTokens(n: number) {
  const abs = Math.abs(n)
  if (abs < 1000) return `${n} tokens`
  let val = n
  let suf = ''
  if (abs >= 1000000000) {
    val = n / 1000000000;
    suf = 'B'
  } else if (abs >= 1000000) {
    val = n / 1000000;
    suf = 'M'
  } else {
    val = n / 1000;
    suf = 'k'
  }
  const digits = val >= 100 ? 0 : val >= 10 ? 1 : 2
  const str = Number(val.toFixed(digits)).toString()
  return `${str}${suf} tokens`
}

const skeletonAspectStyle = computed(() => {
  const ar = settings.aspectRatio
  const map: Record<string, [number, number]> = {
    '1:1': [1, 1],
    '2:3': [2, 3],
    '3:2': [3, 2],
    '3:4': [3, 4],
    '4:3': [4, 3],
    '9:16': [9, 16],
    '16:9': [16, 9],
    '21:9': [21, 9]
  }
  const [rw, rh] = map[ar] ?? map['1:1']
  return {aspectRatio: `${rw} / ${rh}`}
})

</script>

<style scoped>
.typewriter {
  display: inline-block;
  line-height: 2.1;
  font-size: clamp(1.2rem, 2.6vw, 2rem);
  font-weight: 700;
  letter-spacing: 0.02em
}

.tw-text {
  background: linear-gradient(135deg, #93c5fd, #a5b4fc, #c4b5fd);
  background-size: 100% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: saturate(0.9) brightness(0.95)
}

:deep(.dark) .tw-text {
  background: linear-gradient(135deg, #60a5fa, #818cf8, #a78bfa);
  background-size: 100% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: saturate(0.85) brightness(0.9)
}

.fade-char {
  opacity: 0;
  animation: fadeIn 320ms ease-out forwards
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(2px)
  }
  to {
    opacity: 1;
    transform: translateY(0)
  }
}
</style>
