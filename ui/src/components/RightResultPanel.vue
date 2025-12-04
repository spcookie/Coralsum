<template>
  <div class="p-4 space-y-4">
    <div v-if="!loading && !showOverlay && (images.length === 0 || allImagesLoaded)"
         class="flex flex-wrap items-center gap-1 sm:gap-1.5 text-[10px] sm:text-[11px]">
      <div
          class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
        <Icon class="text-[0.75rem]" icon="ph:code"/>
        <span class="font-medium">{{ t('metrics.input') }}</span>
        <span class="font-semibold tabular-nums">{{ formatTokens(result?.inputTokens ?? 0) }}</span>
      </div>
      <div
          class="flex items-center gap-0.5 px-1 py-0.5 rounded-full bg-fuchsia-50 dark:bg-fuchsia-900/30 text-fuchsia-700 dark:text-fuchsia-300 border border-fuchsia-200 dark:border-fuchsia-800">
        <Icon class="text-[0.75rem]" icon="ph:sparkle"/>
        <span class="font-medium">{{ t('metrics.output') }}</span>
        <span class="font-semibold tabular-nums">{{ formatTokens(result?.outputTokens ?? 0) }}</span>
      </div>
      <div
          class="flex items-center gap-0.5 px-1 py-0.5 rounded-md border border-dashed border-neutral-300 dark:border-neutral-700 text-neutral-700 dark:text-neutral-300">
        <Icon class="text-[0.75rem]" icon="ph:timer"/>
        <span class="font-medium">{{ t('metrics.duration') }}</span>
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
      <span>{{ t('share.tip') }}</span>
    </div>
    <div class="relative">
      <div v-if="images.length > 0" class="flex flex-wrap justify-center gap-4">
        <div v-for="(img, i) in images" :key="i"
             class="group rounded bg-neutral-100 dark:bg-neutral-800 w-full sm:w-[240px] md:w-[320px] lg:w-[400px] xl:w-[480px] relative">
          <div :style="skeletonAspectStyle" class="w-full relative">
            <img
                :ref="el => setImgRef(el, i)"
                :class="loadedSet.has(i) ? 'opacity-100' : 'opacity-0'"
                :src="img"
                class="absolute inset-0 w-full h-full object-contain cursor-zoom-in transition-opacity duration-200"
                crossorigin="anonymous"
                @click="openPreview(img)"
                @error="onImgError($event, i)"
                @load="onImgLoad(i)"
            />
            <div v-if="!loadedSet.has(i)" class="absolute inset-0 pointer-events-none">
              <div
                  class="h-full w-full bg-gradient-to-r from-neutral-200 via-neutral-100 to-neutral-200 dark:from-neutral-700 dark:via-neutral-800 dark:to-neutral-700 animate-pulse"></div>
            </div>
          </div>
          <div
              class="absolute inset-x-0 bottom-0 opacity-0 pointer-events-none transition-opacity duration-200 group-hover:opacity-100 group-hover:pointer-events-auto">
            <div class="h-12 bg-gradient-to-t from-black/65 to-transparent flex items-center justify-center">
              <n-tooltip placement="top" trigger="hover">
                <template #trigger>
                  <n-button class="share-btn" size="small" strong @click.stop="onShare(i)">
                    <Icon class="text-[1rem]" icon="ph:share"/>
                  </n-button>
                </template>
                {{ t('share.copy') }}
              </n-tooltip>
            </div>

          </div>
        </div>
      </div>
      <transition name="fade">
        <div v-if="loading && images.length > 0" class="absolute inset-0 z-10 pointer-events-none">
          <div class="flex flex-wrap justify-center gap-4">
            <div v-for="i in settings.candidateRadio" :key="i"
                 class="rounded overflow-hidden bg-neutral-100/80 dark:bg-neutral-800/80 border border-dashed border-neutral-300 dark:border-neutral-700 w-full sm:w-[240px] md:w-[320px] lg:w-[400px] xl:w-[480px] relative">
              <div :style="skeletonAspectStyle" class="w-full relative">
                <div class="h-full w-full skeleton-shimmer"></div>
                <div class="absolute inset-0 grid place-items-center">
                  <div class="flex items-center gap-2 text-neutral-700 dark:text-neutral-200">
                    <n-spin size="small"/>
                    <span>{{ t('loading.creating') }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>
    <transition name="fade">
      <div v-if="loading && images.length === 0" class="flex flex-wrap justify-center gap-4">
        <div v-for="i in settings.candidateRadio" :key="i"
             class="rounded overflow-hidden bg-neutral-100 dark:bg-neutral-800 border border-dashed border-neutral-300 dark:border-neutral-700 w-full sm:w-[240px] md:w-[320px] lg:w-[400px] xl:w-[480px] relative">
          <div :style="skeletonAspectStyle" class="w-full relative">
            <div class="h-full w-full skeleton-shimmer"></div>
            <div class="absolute inset-0 grid place-items-center">
              <div class="flex items-center gap-2 text-neutral-700 dark:text-neutral-200">
                <n-spin size="small"/>
                <span>{{ t('loading.creating') }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
    <div v-if="showOverlay"
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
import {useI18n} from 'vue-i18n'
import {Icon} from '@iconify/vue'
import ImagePreviewer from '@/components/ImagePreviewer.vue'
import {NButton, NSpin, NTooltip, useMessage} from 'naive-ui'
import {getImageShareLink} from '@/api'

import {useSettingsStore} from '@/stores/settings'
import {savePreview} from '@/utils/indexedDb'

const {t} = useI18n()

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
const imgRefs = ref<HTMLImageElement[]>([])
const emit = defineEmits<{ (e: 'save-history', dataUrls: string[]): void }>()
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
<text x='${width / 2}' y='${height / 2 + fs / 2 + 16}' font-size='${sub}'>${t('preview.invalid')}</text>
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
const message = useMessage()
const shareLinks = ref<Record<number, string>>({})

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
  const img = imgRefs.value[i]
  if (!img) return
  try {
    const canvas = document.createElement('canvas')
    const w = img.naturalWidth || img.width
    const h = img.naturalHeight || img.height
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    ctx.drawImage(img, 0, 0)
    const dataUrl = canvas.toDataURL('image/png')
    if (dataUrl && dataUrl.startsWith('data:')) {
      savePreview(img.src, dataUrl)
    }
  } catch {
  }
}

function setImgRef(el: HTMLImageElement | null, i: number) {
  if (el) imgRefs.value[i] = el
}

function onShare(i: number) {
  const raw = props.result?.linkImages?.[i] || props.result?.images?.[i]
  if (!raw) {
    message?.error(t('share.none'), {placement: 'top'})
    return
  }
  const cached = shareLinks.value[i]
  const abs = cached ?? (() => {
    try {
      return new URL(raw, window.location.origin).toString()
    } catch {
      return raw
    }
  })()
  const ok = copyTextSync(abs)
  if (ok) {
    message?.success(cached ? t('share.copied_ready') : t('share.copied_raw'), {placement: 'top'})
    return
  }
  copyTextAsync(abs)
      .then(() => message?.success(cached ? t('share.copied_ready') : t('share.copied_raw'), {placement: 'top'}))
      .catch((err: any) => {
        const msg = err?.message || t('share.copy_failed')
        message?.error(msg, {placement: 'top'})
      })
}

watch(images, () => {
  loadedSet.value = new Set()
  imgRefs.value = []
  shareLinks.value = {}
})

const showOverlay = computed(() => {
  const hasImg = images.value.length > 0
  const hasText = !!props.result?.text
  const isLoading = !!props.loading
  return !hasImg && !hasText && !isLoading
})
const twText = computed(() => t('overlay.text'))
const twChars = computed(() => Array.from(twText.value))
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
watch(twText, () => {
  if (showOverlay.value) {
    typeIndex.value = 0
    schedule()
  }
})
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

watch(allImagesLoaded, async (v) => {
  if (!v) return
  const out: string[] = []
  for (let i = 0; i < imgRefs.value.length; i++) {
    const img = imgRefs.value[i]
    if (!img) continue
    try {
      const canvas = document.createElement('canvas')
      const w = img.naturalWidth || img.width
      const h = img.naturalHeight || img.height
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      if (!ctx) continue
      ctx.drawImage(img, 0, 0)
      const dataUrl = canvas.toDataURL('image/png')
      out.push(dataUrl)
    } catch {
      out.push('')
    }
  }
  if (out.length > 0) emit('save-history', out)
  try {
    const dark = document.documentElement.classList.contains('dark')
    for (let i = 0; i < images.value.length; i++) {
      const raw = props.result?.linkImages?.[i] || props.result?.images?.[i]
      if (!raw) continue
      const refParam = (() => {
        try {
          const url = new URL(raw, window.location.origin)
          const q = url.searchParams.get('ref')
          if (q) return q
          const path = url.pathname.split('/').pop() || ''
          return path
        } catch {
          const idxQ = raw.indexOf('ref=')
          if (idxQ >= 0) return decodeURIComponent(raw.slice(idxQ + 4))
          const noQuery = raw.split('?')[0]
          return noQuery.split('/').pop() || ''
        }
      })()
      if (!refParam) continue
      try {
        const link = await getImageShareLink(refParam, dark)
        const abs = (() => {
          try {
            return new URL(link, window.location.origin).toString()
          } catch {
            return link
          }
        })()
        if (abs) shareLinks.value[i] = abs
      } catch {}
    }
  } catch {}
})

function isIOS() {
  if (typeof navigator === 'undefined') return false
  return /iphone|ipad|ipod/i.test(navigator.userAgent)
}

function copyTextSync(text: string): boolean {
  try {
    if (!isIOS()) return false
    const ta = document.createElement('textarea')
    ta.value = text
    ta.readOnly = true
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    ta.style.left = '0'
    ta.style.top = '0'
    document.body.appendChild(ta)
    ta.focus()
    ta.select()
    try {
      ta.setSelectionRange(0, ta.value.length)
    } catch {
    }
    const ok = document.execCommand('copy')
    document.body.removeChild(ta)
    return ok
  } catch {
    return false
  }
}

function copyTextAsync(text: string): Promise<void> {
  if (navigator?.clipboard?.writeText) {
    return navigator.clipboard.writeText(text)
  }
  return new Promise<void>((resolve, reject) => {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      ta.style.left = '0'
      ta.style.top = '0'
      document.body.appendChild(ta)
      ta.focus()
      ta.select()
      try {
        ta.setSelectionRange(0, ta.value.length)
      } catch {
      }
      const ok = document.execCommand('copy')
      document.body.removeChild(ta)
      ok ? resolve() : reject(new Error(t('share.copy_failed')))
    } catch (e) {
      reject(e as any)
    }
  })
}

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
.fade-enter-active, .fade-leave-active { transition: opacity 200ms ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
.img-badge {
position: absolute;
top: 8px;
left: 8px;
width: 24px;
height: 24px;
border-radius: 9999px;
display: grid;
place-items: center;
font-size: 12px;
}

:deep(.control-btn.n-button) {
background-color: rgba(128, 128, 128, 0.3) !important;
color: #fff !important;
border: none !important;
box-shadow: none !important;
}

:deep(.control-btn.n-button:hover) {
background-color: rgba(128, 128, 128, 0.4) !important;
}

:deep(.control-btn.n-button .n-button__content) {
color: #fff !important;
}

:deep(.control-btn.n-button .n-button__border) {
display: none !important;
}

.share-tip {
min-width: 220px;
max-width: 80%;
}

:deep(.n-tooltip .n-tooltip__content) {
background-color: rgba(0, 0, 0, 0.9) !important;
color: #fff !important;
}
.img-badge {
position: absolute;
top: 8px;
left: 8px;
width: 24px;
height: 24px;
border-radius: 9999px;
display: grid;
place-items: center;
font-size: 12px;
}

:deep(.control-btn.n-button) {
background-color: rgba(128, 128, 128, 0.3) !important;
color: #fff !important;
border: none !important;
box-shadow: none !important;
}

:deep(.control-btn.n-button:hover) {
background-color: rgba(128, 128, 128, 0.4) !important;
}

:deep(.control-btn.n-button .n-button__content) {
color: #fff !important;
}

:deep(.control-btn.n-button .n-button__border) {
display: none !important;
}
:deep(.share-btn.n-button) {
  background-color: rgba(255, 255, 255, 0.7) !important;
  color: #333 !important;
  border: none !important;
  box-shadow: none !important;
}

:deep(.share-btn.n-button:hover) {
  background-color: rgba(255, 255, 255, 0.8) !important;
}

:deep(.share-btn.n-button .n-button__content) {
  color: inherit !important;
}

:deep(.dark) .share-btn.n-button {
  background-color: rgba(255, 255, 255, 0.18) !important;
  color: #fff !important;
}

:deep(.dark) .share-btn.n-button:hover {
  background-color: rgba(255, 255, 255, 0.24) !important;
}
.skeleton-shimmer {
position: relative;
overflow: hidden;
background-color: rgba(229, 231, 235, 0.9);
}

:deep(.dark) .skeleton-shimmer {
background-color: rgba(55, 65, 81, 0.9);
}

.skeleton-shimmer::after {
content: '';
position: absolute;
top: 0;
left: -40%;
width: 40%;
height: 100%;
background: linear-gradient(90deg, transparent, rgba(255,255,255,0.7), transparent);
animation: skeletonShimmer 1200ms ease-in-out infinite;
}

:deep(.dark) .skeleton-shimmer::after {
background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
}

@keyframes skeletonShimmer {
0% { left: -40%; }
50% { left: 80%; }
100% { left: 100%; }
}
