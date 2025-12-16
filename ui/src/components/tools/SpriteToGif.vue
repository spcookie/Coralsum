<template>
  <div class="space-y-3">
    <div class="flex flex-wrap items-center gap-2">
      <div class="inline-flex">
        <n-upload :show-file-list="false" accept="image/*" @change="onUpload">
          <n-button secondary size="small">
            <Icon class="mr-1" icon="mdi:upload"/>
            <span class="hidden sm:inline">{{ t('tools.sprite_to_gif.upload') }}</span>
            <span class="sm:hidden">上传</span>
          </n-button>
        </n-upload>
      </div>
      <n-button secondary size="small" @click="showPicker = true">
        <Icon class="mr-1" icon="mdi:history"/>
        <span class="hidden sm:inline">{{ t('tools.sprite_to_gif.pick_history') }}</span>
        <span class="sm:hidden">历史</span>
      </n-button>
      <n-button secondary size="small" @click="showSlicerMode = !showSlicerMode">
        <Icon :icon="showSlicerMode ? 'mdi:grid' : 'mdi:scissors'" class="mr-1"/>
        <span class="hidden sm:inline">{{
            showSlicerMode ? t('tools.sprite_to_gif.grid_mode') : t('tools.sprite_to_gif.slicer_mode')
          }}</span>
        <span class="sm:hidden">{{
            showSlicerMode ? t('tools.sprite_to_gif.grid') : t('tools.sprite_to_gif.slicer')
          }}</span>
      </n-button>
    </div>

    <div v-if="!showSlicerMode" class="flex flex-wrap items-center gap-3">
      <div class="flex items-center gap-2">
        <span class="inline-block w-20 text-sm text-neutral-600 dark:text-neutral-300">{{
            t('tools.sprite_to_gif.frame_width')
          }}</span>
        <n-input-number v-model:value="frameWidth" :max="maxFrameSize" :min="1" size="small" style="width: 100px"
                        @update:value="onConfigChange"/>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-block w-20 text-sm text-neutral-600 dark:text-neutral-300">{{
            t('tools.sprite_to_gif.frame_height')
          }}</span>
        <n-input-number v-model:value="frameHeight" :max="maxFrameSize" :min="1" size="small" style="width: 100px"
                        @update:value="onConfigChange"/>
      </div>
    </div>

    <div v-if="showSlicerMode && imgSrc" class="space-y-2">
      <div class="flex flex-wrap items-center gap-3">
        <div class="flex items-center gap-2">
          <span class="inline-block w-10 text-sm text-neutral-600 dark:text-neutral-300">{{
              t('tools.meme_slicer.rows')
            }}</span>
          <n-input-number v-model:value="rows" :max="maxRows" :min="1" size="small" style="width: 80px"
                          @update:value="onGridChange"/>
        </div>
        <div class="flex items-center gap-2">
          <span class="inline-block w-10 text-sm text-neutral-600 dark:text-neutral-300">{{
              t('tools.meme_slicer.cols')
            }}</span>
          <n-input-number v-model:value="cols" :max="maxCols" :min="1" size="small" style="width: 80px"
                          @update:value="onGridChange"/>
        </div>
      </div>

      <div class="relative overflow-hidden max-h-[500px] flex justify-center items-center">
        <div ref="wrap" class="inline-block relative max-w-full max-h-full">
          <img ref="imgEl" :src="imgSrc" class="max-w-full max-h-[500px] object-contain select-none" @load="onImgLoad"/>
          <svg v-if="imgLoaded" :height="displayH" :viewBox="`0 0 ${naturalW} ${naturalH}`"
               :width="displayW" class="absolute inset-0 pointer-events-none">
            <defs>
              <pattern id="mask-sprite" height="10" patternUnits="userSpaceOnUse" width="10" x="0" y="0">
                <rect fill="black" height="10" opacity="0.2" width="10" x="0" y="0"/>
              </pattern>
            </defs>
            <rect :height="naturalH" :width="naturalW" fill="url(#mask-sprite)" x="0" y="0"/>
            <rect :height="crop.h" :stroke="haloColor" :stroke-width="lineWidth + 2" :width="crop.w" :x="crop.x"
                  :y="crop.y"
                  fill="none" stroke-dasharray="6 6"/>
            <rect :height="crop.h" :stroke="primaryColor" :stroke-width="lineWidth" :width="crop.w" :x="crop.x"
                  :y="crop.y"
                  fill="none" stroke-dasharray="6 6"/>
            <g>
              <line v-for="(x,i) in xGuides" :key="'xh'+i" :stroke="haloColor" :stroke-width="lineWidth + 2"
                    :x1="crop.x + x"
                    :x2="crop.x + x" :y1="crop.y" :y2="crop.y + crop.h" stroke-dasharray="6 6"/>
              <line v-for="(x,i) in xGuides" :key="'x'+i" :stroke="primaryColor" :stroke-width="lineWidth"
                    :x1="crop.x + x"
                    :x2="crop.x + x" :y1="crop.y" :y2="crop.y + crop.h" stroke-dasharray="6 6"/>
              <line v-for="(y,i) in yGuides" :key="'yh'+i" :stroke="haloColor" :stroke-width="lineWidth + 2"
                    :x1="crop.x"
                    :x2="crop.x + crop.w" :y1="crop.y + y" :y2="crop.y + y" stroke-dasharray="6 6"/>
              <line v-for="(y,i) in yGuides" :key="'y'+i" :stroke="primaryColor" :stroke-width="lineWidth" :x1="crop.x"
                    :x2="crop.x + crop.w" :y1="crop.y + y" :y2="crop.y + y" stroke-dasharray="6 6"/>
            </g>
          </svg>
          <div v-if="imgLoaded" class="absolute inset-0" @pointerdown="onPointerDown" @pointermove="onPointerMove"
               @pointerup="onPointerUp"></div>
        </div>
      </div>
    </div>

    <!-- Direct mode image display -->
    <div v-if="imgSrc && !showSlicerMode"
         class="relative overflow-hidden max-h-[500px] flex justify-center items-center">
      <div ref="wrap" class="inline-block relative max-w-full max-h-full">
        <img ref="imgEl" :src="imgSrc" class="max-w-full max-h-[500px] object-contain select-none" @load="onImgLoad"/>
      </div>
    </div>

    <div v-if="gridInfo" class="flex flex-wrap items-center gap-2">
      <n-tag size="small" type="info">{{ t('tools.sprite_to_gif.total_frames', {count: gridInfo.totalFrames}) }}</n-tag>
      <n-tag size="small" type="info">{{ gridInfo.cols }} × {{ gridInfo.rows }}</n-tag>
    </div>

    <div class="flex flex-wrap items-center gap-3">
      <div class="flex items-center gap-2">
        <span class="inline-block w-20 text-sm text-neutral-600 dark:text-neutral-300">{{
            t('tools.sprite_to_gif.start_frame')
          }}</span>
        <n-input-number v-model:value="startFrame" :max="maxFrameIndex" :min="0" size="small" style="width: 100px"/>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-block w-20 text-sm text-neutral-600 dark:text-neutral-300">{{
            t('tools.sprite_to_gif.end_frame')
          }}</span>
        <n-input-number v-model:value="endFrame" :max="maxFrameIndex" :min="startFrame" size="small"
                        style="width: 100px"/>
      </div>
    </div>

    <div class="flex flex-wrap items-center gap-3">
      <div class="flex items-center gap-2">
        <span class="inline-block w-20 text-sm text-neutral-600 dark:text-neutral-300">{{
            t('tools.sprite_to_gif.fps')
          }}</span>
        <n-input-number v-model:value="fps" :max="60" :min="1" size="small" style="width: 100px"/>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-block w-20 text-sm text-neutral-600 dark:text-neutral-300">{{
            t('tools.sprite_to_gif.loop_count')
          }}</span>
        <n-input-number v-model:value="loopCount" :max="100" :min="0" size="small" style="width: 100px"/>
        <span class="text-xs text-neutral-500">({{ t('tools.sprite_to_gif.infinite_loop') }})</span>
      </div>
    </div>

    <div class="flex flex-wrap items-center gap-2">
      <n-button :disabled="!imgSrc" size="small" type="primary" @click="extractAndPreview">
        <Icon class="mr-1" icon="mdi:eye"/>
        <span class="hidden sm:inline">{{ t('tools.sprite_to_gif.extract_preview') }}</span>
        <span class="sm:hidden">{{ t('tools.sprite_to_gif.preview') }}</span>
      </n-button>
      <n-button :disabled="frames.length === 0" :loading="encoding" size="small" type="primary" @click="generateGif">
        <Icon class="mr-1" icon="mdi:file-gif-box"/>
        <span class="hidden sm:inline">{{ t('tools.sprite_to_gif.generate_gif') }}</span>
        <span class="sm:hidden">{{ t('tools.sprite_to_gif.generate') }}</span>
      </n-button>
      <n-button :disabled="!gifUrl" size="small" tertiary type="primary" @click="downloadGif">
        <Icon class="mr-1" icon="mdi:download"/>
        <span class="hidden sm:inline">{{ t('tools.sprite_to_gif.download_gif') }}</span>
        <span class="sm:hidden">{{ t('tools.sprite_to_gif.download') }}</span>
      </n-button>
    </div>

    <div v-if="encoding" class="mt-2">
      <n-progress :percentage="encodeProgress" type="line">
        {{ t('tools.sprite_to_gif.encoding') }}
      </n-progress>
    </div>

    <div v-if="frames.length > 0" class="mt-4 space-y-2">
      <div class="text-sm font-medium text-neutral-700 dark:text-neutral-200">
        {{ t('tools.sprite_to_gif.extracted_frames', {count: frames.length}) }}
      </div>
      <div class="grid grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-2 max-h-96 overflow-y-auto">
        <div v-for="(f,i) in frames" :key="i"
             class="rounded bg-neutral-100 dark:bg-neutral-800 p-1 flex flex-col items-center">
          <img :src="f.canvas.toDataURL()" class="w-full h-auto object-contain"/>
          <span class="text-xs text-neutral-500 mt-1">{{ f.index }}</span>
        </div>
      </div>
    </div>

    <div v-if="gifUrl" class="mt-4 space-y-2">
      <div class="text-sm font-medium text-neutral-700 dark:text-neutral-200">{{
          t('tools.sprite_to_gif.gif_preview')
        }}
      </div>
      <div class="flex justify-center">
        <div class="inline-block border-2 border-neutral-300 dark:border-neutral-700 rounded p-2">
          <img :src="gifUrl" class="max-w-full h-auto"/>
        </div>
      </div>
      <div class="flex items-center gap-2 text-xs text-neutral-500">
        <span>{{ t('tools.sprite_to_gif.file_size', {size: gifSize}) }}</span>
        <span>·</span>
        <span>{{ t('tools.sprite_to_gif.frame_count', {count: frames.length}) }}</span>
        <span>·</span>
        <span>{{ t('tools.sprite_to_gif.fps_value', {fps: fps}) }}</span>
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
import {type Frame, useSpriteExtractor} from '@/composables/useSpriteExtractor'
import {useGifEncoder} from '@/composables/useGifEncoder'
import {useImageGuides} from '@/composables/useImageGuides'

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

const showSlicerMode = ref(false)
const rows = ref(3)
const cols = ref(3)
const maxRows = 20
const maxCols = 20

const {xGuides, yGuides, init, clampGuideX, clampGuideY, rects} = useImageGuides()

const frameWidth = ref(64)
const frameHeight = ref(64)
const startFrame = ref(0)
const endFrame = ref(0)
const fps = ref(10)
const loopCount = ref(0)

const frames = ref<Frame[]>([])
const encoding = ref(false)
const encodeProgress = ref(0)
const gifUrl = ref<string>('')
const gifSize = ref<string>('')

const {extractFrames, extractFramesFromRects, calculateGridInfo} = useSpriteExtractor()
const {encodeGif} = useGifEncoder()

const maxFrameSize = computed(() => Math.max(naturalW.value, naturalH.value))

const gridInfo = computed(() => {
  if (!imgSrc.value || !imgLoaded.value) return null
  if (showSlicerMode.value) {
    const totalFrames = rows.value * cols.value
    return {cols: cols.value, rows: rows.value, totalFrames}
  }
  return calculateGridInfo(naturalW.value, naturalH.value, frameWidth.value, frameHeight.value)
})

const maxFrameIndex = computed(() => {
  if (!gridInfo.value) return 0
  return Math.max(0, gridInfo.value.totalFrames - 1)
})

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

function onConfigChange() {
  if (imgLoaded.value && gridInfo.value) {
    endFrame.value = Math.min(endFrame.value, gridInfo.value.totalFrames - 1)
  }
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

  if (showSlicerMode.value) {
    init(crop.value.w, crop.value.h, cols.value, rows.value)
  }

  nextTickMeasure()
  attachResizeObserver()

  if (gridInfo.value) {
    endFrame.value = Math.max(0, Math.min(gridInfo.value.totalFrames - 1, 10))
  }
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
  if (!showSlicerMode.value) return
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
}

function onPointerMove(ev: PointerEvent) {
  if (!dragging || !showSlicerMode.value) return
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

function extractAndPreview() {
  try {
    const img = imgEl.value
    if (!img) return

    if (showSlicerMode.value) {
      const rowsRects = rects()
      const flat = rowsRects.flat().map(r => ({x: r.x + crop.value.x, y: r.y + crop.value.y, w: r.w, h: r.h}))
      frames.value = extractFramesFromRects(img, flat, startFrame.value, endFrame.value)
    } else {
      frames.value = extractFrames(img, {
        frameWidth: frameWidth.value,
        frameHeight: frameHeight.value,
        startFrame: startFrame.value,
        endFrame: endFrame.value
      })
    }

    message.success(`成功提取 ${frames.value.length} 帧`)
  } catch (err) {
    message.error('提取失败: ' + (err as Error).message)
  }
}

async function generateGif() {
  if (frames.value.length === 0) {
    message.warning('请先提取帧')
    return
  }

  try {
    encoding.value = true
    encodeProgress.value = 0

    const canvases = frames.value.map(f => f.canvas)
    const blob = await encodeGif(canvases, {
      fps: fps.value,
      loop: loopCount.value,
      quality: 10
    }, (p) => {
      encodeProgress.value = p
    })

    const url = URL.createObjectURL(blob)
    gifUrl.value = url
    gifSize.value = formatSize(blob.size)

    message.success('GIF 生成成功')
  } catch (err) {
    message.error('GIF 生成失败: ' + (err as Error).message)
  } finally {
    encoding.value = false
  }
}

function downloadGif() {
  if (!gifUrl.value) return
  const a = document.createElement('a')
  a.href = gifUrl.value
  a.download = `sprite-animation-${Date.now()}.gif`
  a.click()
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
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
    if (gifUrl.value) {
      URL.revokeObjectURL(gifUrl.value)
    }
  } catch {
  }
  window.removeEventListener('resize', nextTickMeasure)
})

</script>

<style scoped>
</style>