<template>
  <div class="space-y-3">
    <div class="flex flex-wrap items-center gap-2">
      <div class="inline-flex">
        <n-upload :show-file-list="false" accept="image/*" @change="onUpload">
          <n-button secondary size="small">
            <Icon class="mr-1" icon="mdi:upload"/>
            {{ t('tools.meme_slicer.upload') }}
          </n-button>
        </n-upload>
      </div>
      <n-button secondary size="small" @click="showPicker = true">
        <Icon class="mr-1" icon="mdi:history"/>
        {{ t('tools.meme_slicer.pick_history') }}
      </n-button>
      <n-button :disabled="!imgSrc" size="small" type="primary" @click="doSlice">
        <Icon class="mr-1" icon="mdi:eye"/>
        {{ t('tools.meme_slicer.preview') }}
      </n-button>
      <n-button :disabled="slices.length===0" size="small" tertiary type="primary" @click="downloadZip">
        <Icon class="mr-1" icon="mdi:archive-arrow-down"/>
        {{ t('tools.meme_slicer.download_zip') }}
      </n-button>
      <n-button size="small" type="primary" @click="resetGuides">
        <Icon class="mr-1" icon="mdi:refresh"/>
        {{ t('tools.meme_slicer.reset') }}
      </n-button>
    </div>
    <div class="flex flex-col sm:flex-row sm:flex-wrap items-center gap-3 mt-2">
      <div class="flex items-center gap-2">
        <span class="inline-block w-10 text-sm text-neutral-600 dark:text-neutral-300">{{
            t('tools.meme_slicer.rows')
          }}</span>
        <n-input-number v-model:value="rows" :max="maxRows" :min="1" size="small" style="width: 96px"
                        @update:value="onGridChange"/>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-block w-10 text-sm text-neutral-600 dark:text-neutral-300">{{
            t('tools.meme_slicer.cols')
          }}</span>
        <n-input-number v-model:value="cols" :max="maxCols" :min="1" size="small" style="width: 96px"
                        @update:value="onGridChange"/>
      </div>
      <div class="flex items-center gap-2 sm:ml-auto">
        <n-tag size="small" type="info">{{ t('tools.meme_slicer.max_rows', {count: maxRows}) }}</n-tag>
        <n-tag size="small" type="info">{{ t('tools.meme_slicer.max_cols', {count: maxCols}) }}</n-tag>
      </div>
    </div>

    <div v-if="imgSrc" class="relative overflow-auto">
      <div ref="wrap" class="inline-block relative w-full max-w-full">
        <img ref="imgEl" :src="imgSrc" class="max-w-full h-auto object-contain select-none" @load="onImgLoad"/>
        <svg v-if="imgLoaded" :height="displayH" :viewBox="`0 0 ${naturalW} ${naturalH}`"
             :width="displayW" class="absolute inset-0 pointer-events-none">
          <defs>
            <pattern id="mask" height="10" patternUnits="userSpaceOnUse" width="10" x="0" y="0">
              <rect fill="black" height="10" opacity="0.2" width="10" x="0" y="0"/>
            </pattern>
          </defs>
          <rect :height="naturalH" :width="naturalW" fill="url(#mask)" x="0" y="0"/>
          <rect :height="crop.h" :stroke="haloColor" :stroke-width="lineWidth + 2" :width="crop.w" :x="crop.x" :y="crop.y"
                fill="none" stroke-dasharray="6 6"/>
          <rect :height="crop.h" :stroke="primaryColor" :stroke-width="lineWidth" :width="crop.w" :x="crop.x" :y="crop.y"
                fill="none" stroke-dasharray="6 6"/>
          <g>
            <line v-for="(x,i) in xGuides" :key="'xh'+i" :stroke="haloColor" :stroke-width="lineWidth + 2" :x1="crop.x + x"
                  :x2="crop.x + x" :y1="crop.y" :y2="crop.y + crop.h" stroke-dasharray="6 6"/>
            <line v-for="(x,i) in xGuides" :key="'x'+i" :stroke="primaryColor" :stroke-width="lineWidth" :x1="crop.x + x"
                  :x2="crop.x + x" :y1="crop.y" :y2="crop.y + crop.h" stroke-dasharray="6 6"/>
            <line v-for="(y,i) in yGuides" :key="'yh'+i" :stroke="haloColor" :stroke-width="lineWidth + 2" :x1="crop.x"
                  :x2="crop.x + crop.w" :y1="crop.y + y" :y2="crop.y + y" stroke-dasharray="6 6"/>
            <line v-for="(y,i) in yGuides" :key="'y'+i" :stroke="primaryColor" :stroke-width="lineWidth" :x1="crop.x"
                  :x2="crop.x + crop.w" :y1="crop.y + y" :y2="crop.y + y" stroke-dasharray="6 6"/>
          </g>
        </svg>
        <div v-if="imgLoaded" class="absolute inset-0" @pointerdown="onPointerDown" @pointermove="onPointerMove"
             @pointerup="onPointerUp"></div>
      </div>
    </div>

    <div v-if="slices.length>0" class="mt-2">
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2">
        <div v-for="(s,i) in slices" :key="i" class="rounded bg-neutral-100 dark:bg-neutral-800">
          <img :src="s" class="w-full h-auto object-contain"/>
          <div class="p-2 flex justify-end">
            <a :download="`slice_${i+1}.png`" :href="s" class="text-xs underline">{{ t('preview.download') }}</a>
          </div>
        </div>
      </div>
      <div v-if="zipping" class="mt-2">
        <n-progress :percentage="zipProgress" type="line"/>
      </div>
    </div>

    <HistoryPicker v-model:modelValue="showPicker" @pick="onPick"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {NButton, NInputNumber, NProgress, NTag, NUpload, useMessage, useThemeVars} from 'naive-ui'
import {Icon} from '@iconify/vue'
import HistoryPicker from '@/components/tools/HistoryPicker.vue'
import {useImageGuides} from '@/composables/useImageGuides'
import {sliceImageToDataUrls} from '@/composables/useCanvasSlicer'

const {t} = useI18n()
const message = useMessage()
const showPicker = ref(false)
const themeVars = useThemeVars()
const primaryColor = computed(() => themeVars.value?.primaryColor || '#18a058')
const lineWidth = computed(() => (document?.documentElement?.classList?.contains('dark') ? 3 : 2))
const haloColor = computed(() => (document?.documentElement?.classList?.contains('dark') ? '#ffffffaa' : '#00000055'))

const imgEl = ref<HTMLImageElement | null>(null)
const wrap = ref<HTMLElement | null>(null)
const imgSrc = ref<string>('')
const imgLoaded = ref(false)
const naturalW = ref(0)
const naturalH = ref(0)
const displayW = ref(0)
const displayH = ref(0)
const crop = ref<{ x: number; y: number; w: number; h: number }>({x: 0, y: 0, w: 0, h: 0})

const rows = ref(3)
const cols = ref(3)
const slices = ref<string[]>([])
const zipping = ref(false)
const zipProgress = ref(0)
const maxRows = 20
const maxCols = 20
const pieceCount = computed(() => rows.value * cols.value)

const {xGuides, yGuides, init, clampGuideX, clampGuideY, rects} = useImageGuides()

function onUpload(opts: any) {
  try {
    const file = Array.isArray(opts.fileList) ? opts.fileList[0]?.file : opts.file?.file || opts.file
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => {
      imgSrc.value = String(reader.result || '')
    }
    reader.readAsDataURL(file as File)
  } catch {
  }
}

function onPick(src: string) {
  imgSrc.value = src
}

function onGridChange() {
  if (!imgLoaded.value) return
  init(crop.value.w, crop.value.h, cols.value, rows.value)
}

function onImgLoad(e: Event) {
  const img = e.target as HTMLImageElement
  naturalW.value = img.naturalWidth || img.width
  naturalH.value = img.naturalHeight || img.height
  imgLoaded.value = true
  crop.value = {x: 0, y: 0, w: naturalW.value, h: naturalH.value}
  init(crop.value.w, crop.value.h, cols.value, rows.value)
  nextTickMeasure()
  attachResizeObserver()
}

function nextTickMeasure() {
  setTimeout(() => {
    const root = wrap.value
    const img = imgEl.value
    if (!root || !img) return
    const r = img.getBoundingClientRect()
    displayW.value = Math.round(r.width)
    displayH.value = Math.round(r.height)
  }, 0)
}

let ro: ResizeObserver | null = null

function attachResizeObserver() {
  try {
    const img = imgEl.value
    if (!img) return
    if (ro) ro.disconnect()
    ro = new ResizeObserver(() => {
      nextTickMeasure()
    })
    ro.observe(img)
  } catch {
  }
}

let dragging: {
  type: 'x' | 'y' | 'crop-move' | 'crop-left' | 'crop-right' | 'crop-top' | 'crop-bottom';
  idx?: number;
  startX?: number;
  startY?: number;
  cropStart?: { x: number; y: number; w: number; h: number };
  baselineX?: number[];
  baselineY?: number[]
} | null = null

function scaleGuidesFromBaseline(bx: number[] | undefined, by: number[] | undefined, ow: number, oh: number, nw: number, nh: number) {
  if (!bx || !by) return
  if (ow <= 0 || oh <= 0 || nw <= 0 || nh <= 0) return
  const rx = bx.map(v => v / ow)
  const ry = by.map(v => v / oh)
  xGuides.value = rx.map(r => Math.round(r * nw))
  yGuides.value = ry.map(r => Math.round(r * nh))
}

function imageCoordFromClient(ev: PointerEvent): { x: number; y: number } {
  const img = imgEl.value
  if (!img) return {x: 0, y: 0}
  const rect = img.getBoundingClientRect()
  const rx = (ev.clientX - rect.left) / rect.width
  const ry = (ev.clientY - rect.top) / rect.height
  return {x: Math.round(rx * naturalW.value), y: Math.round(ry * naturalH.value)}
}

function findNearbyHandle(ev: PointerEvent): { type: 'x' | 'y'; idx: number } | null {
  const {x, y} = imageCoordFromClient(ev)
  const tol = Math.max(6, Math.round(naturalW.value * 0.01))
  let best: { type: 'x' | 'y'; idx: number } | null = null
  let bestDist = Infinity
  xGuides.value.forEach((gx, i) => {
    const d = Math.abs((crop.value.x + gx) - x)
    if (d < tol && d < bestDist) {
      best = {type: 'x', idx: i};
      bestDist = d
    }
  })
  yGuides.value.forEach((gy, i) => {
    const d = Math.abs((crop.value.y + gy) - y)
    if (d < tol && d < bestDist) {
      best = {type: 'y', idx: i};
      bestDist = d
    }
  })
  return best
}

function onPointerDown(ev: PointerEvent) {
  const {x, y} = imageCoordFromClient(ev)
  const tol = Math.max(6, Math.round(naturalW.value * 0.01))
  const cx = crop.value.x, cy = crop.value.y, cw = crop.value.w, ch = crop.value.h
  const insideX = x >= cx && x <= cx + cw
  const insideY = y >= cy && y <= cy + ch
  const h = findNearbyHandle(ev)
  if (h) {
    dragging = h;
    return
  }
  if (insideY && Math.abs(x - cx) < tol) {
    dragging = {
      type: 'crop-left',
      startX: x,
      startY: y,
      cropStart: {...crop.value},
      baselineX: [...xGuides.value],
      baselineY: [...yGuides.value]
    };
    return
  }
  if (insideY && Math.abs(x - (cx + cw)) < tol) {
    dragging = {
      type: 'crop-right',
      startX: x,
      startY: y,
      cropStart: {...crop.value},
      baselineX: [...xGuides.value],
      baselineY: [...yGuides.value]
    };
    return
  }
  if (insideX && Math.abs(y - cy) < tol) {
    dragging = {
      type: 'crop-top',
      startX: x,
      startY: y,
      cropStart: {...crop.value},
      baselineX: [...xGuides.value],
      baselineY: [...yGuides.value]
    };
    return
  }
  if (insideX && Math.abs(y - (cy + ch)) < tol) {
    dragging = {
      type: 'crop-bottom',
      startX: x,
      startY: y,
      cropStart: {...crop.value},
      baselineX: [...xGuides.value],
      baselineY: [...yGuides.value]
    };
    return
  }
  if (insideX && insideY) {
    dragging = {type: 'crop-move', startX: x, startY: y, cropStart: {...crop.value}};
    return
  }
  // attach baselines for crop resizing types
  if (dragging && dragging.cropStart && dragging.type !== 'crop-move') {
    dragging.baselineX = [...xGuides.value]
    dragging.baselineY = [...yGuides.value]
  }
}

function onPointerMove(ev: PointerEvent) {
  if (!dragging) return
  const {x, y} = imageCoordFromClient(ev)
  if (dragging.type === 'x') {
    clampGuideX(dragging.idx!, x - crop.value.x)
  } else if (dragging.type === 'y') {
    clampGuideY(dragging.idx!, y - crop.value.y)
  } else {
    const start = dragging.cropStart!
    const minSize = 10
    if (dragging.type === 'crop-move') {
      const dx = x - (dragging.startX || 0)
      const dy = y - (dragging.startY || 0)
      let nx = Math.max(0, Math.min(naturalW.value - start.w, start.x + dx))
      let ny = Math.max(0, Math.min(naturalH.value - start.h, start.y + dy))
      crop.value = {x: nx, y: ny, w: start.w, h: start.h}
    } else if (dragging.type === 'crop-left') {
      let nx = Math.max(0, Math.min(start.x + start.w - minSize, x))
      const nw = start.x + start.w - nx
      crop.value = {x: nx, y: start.y, w: nw, h: start.h}
      scaleGuidesFromBaseline(dragging.baselineX, dragging.baselineY, start.w, start.h, nw, start.h)
    } else if (dragging.type === 'crop-right') {
      let rx = Math.max(start.x + minSize, Math.min(naturalW.value, x))
      const nw = rx - start.x
      crop.value = {x: start.x, y: start.y, w: nw, h: start.h}
      scaleGuidesFromBaseline(dragging.baselineX, dragging.baselineY, start.w, start.h, nw, start.h)
    } else if (dragging.type === 'crop-top') {
      let ny = Math.max(0, Math.min(start.y + start.h - minSize, y))
      const nh = start.y + start.h - ny
      crop.value = {x: start.x, y: ny, w: start.w, h: nh}
      scaleGuidesFromBaseline(dragging.baselineX, dragging.baselineY, start.w, start.h, start.w, nh)
    } else if (dragging.type === 'crop-bottom') {
      let by = Math.max(start.y + minSize, Math.min(naturalH.value, y))
      const nh = by - start.y
      crop.value = {x: start.x, y: start.y, w: start.w, h: nh}
      scaleGuidesFromBaseline(dragging.baselineX, dragging.baselineY, start.w, start.h, start.w, nh)
    }
  }
}

function onPointerUp() {
  dragging = null
}

async function doSlice() {
  try {
    const img = imgEl.value
    if (!img) return
    const rowsRects = rects()
    const flat = rowsRects.flat().map(r => ({x: r.x + crop.value.x, y: r.y + crop.value.y, w: r.w, h: r.h}))
    const urls = await sliceImageToDataUrls(img, flat)
    slices.value = urls
  } catch {
    message.error('切片失败')
  }
}

function resetGuides() {
  if (imgLoaded.value) {
    crop.value = {x: 0, y: 0, w: naturalW.value, h: naturalH.value}
  }
  init(crop.value.w, crop.value.h, cols.value, rows.value)
}

async function downloadZip() {
  try {
    if (slices.value.length === 0) return
    zipping.value = true
    zipProgress.value = 0
    const {zipSync, strToU8, zip} = await import('fflate') as any
    const files: Record<string, Uint8Array> = {}
    for (let i = 0; i < slices.value.length; i++) {
      const s = slices.value[i]
      const name = `slice_${i + 1}.png`
      const b64 = s.split(',')[1] || ''
      const bin = Uint8Array.from(atob(b64), c => c.charCodeAt(0))
      files[name] = bin
      zipProgress.value = Math.round(((i + 1) / slices.value.length) * 100)
    }
    const zipped = zipSync(files)
    const blob = new Blob([zipped], {type: 'application/zip'})
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'slices.zip'
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch {
    message.error('打包失败')
  } finally {
    zipping.value = false
  }
}

onMounted(() => {
  window.addEventListener('paste', (e: ClipboardEvent) => {
    try {
      const items = e.clipboardData?.items || []
      for (const it of items) {
        if (it.type.startsWith('image/')) {
          const f = it.getAsFile()
          if (f) {
            const r = new FileReader()
            r.onload = () => {
              imgSrc.value = String(r.result || '')
            }
            r.readAsDataURL(f)
            break
          }
        }
      }
    } catch {
    }
  })
  window.addEventListener('resize', nextTickMeasure)
})

onUnmounted(() => {
  try {
    if (ro) ro.disconnect()
  } catch {
  }
  window.removeEventListener('resize', nextTickMeasure)
})

</script>

<style scoped>
</style>
