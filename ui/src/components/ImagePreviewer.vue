<template>
  <n-modal
      :closable="false"
      :mask-closable="true"
      :show="modelValue"
      :show-icon="false"
      :style="fullscreenModalStyle"
      :z-index="10000"
      to="body"
      :trap-focus="false"
      class="image-previewer-modal fullscreen"
      @update:show="onVisibleChange"
  >
    <div class="relative w-[100vw] h-[100vh] select-none">
      <div class="absolute top-4 right-4 z-10">
        <n-button circle class="close-btn" size="small" strong @click="close">
          <Icon icon="mdi:close"/>
        </n-button>
      </div>
      <div class="absolute top-8 left-1/2 -translate-x-1/2 z-10 pointer-events-none">
        <div class="scale-indicator px-3 py-1 rounded-md text-xs">{{ t('preview.zoom') }} {{
            (scale * 100).toFixed(0)
          }}%
        </div>
      </div>
      <div
          ref="canvasRef"
          class="absolute inset-0 overflow-hidden bg-transparent"
          @pointerdown="onPointerDown"
          @pointerleave="onPointerLeave"
          @pointermove="onPointerMove"
          @pointerup="onPointerUp"
          @wheel.prevent="onWheel"
      >
        <img
            ref="imgRef"
            :src="realSrc"
            :style="imgStyle"
            class="absolute top-1/2 left-1/2 will-change-transform"
            crossorigin="anonymous"
            draggable="false"
            @load="onImgLoad"
        />
        <n-spin v-if="imgLoading" class="absolute inset-0 grid place-items-center" size="large"/>
      </div>

      <div class="pointer-events-none absolute bottom-0 left-0 w-full p-4 pb-12">
        <div class="pointer-events-auto mx-auto max-w-4xl flex flex-wrap items-center justify-center gap-2">
          <n-button class="control-btn" size="small" strong @click="zoomOut">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:magnify-minus-outline"/>
              <span>{{ t('preview.zoom_out') }}</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="zoomIn">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:magnify-plus-outline"/>
              <span>{{ t('preview.zoom_in') }}</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="reset">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:restart"/>
              <span>{{ t('preview.reset') }}</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="rotateLeft">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:rotate-left"/>
              <span>{{ t('preview.rotate_left') }}</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="rotateRight">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:rotate-right"/>
              <span>{{ t('preview.rotate_right') }}</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="copyImage">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:content-copy"/>
              <span>{{ t('preview.copy') }}</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="downloadImage">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:download"/>
              <span>{{ t('preview.download') }}</span>
            </div>
          </n-button>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script lang="ts" setup>
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {Icon} from '@iconify/vue'
import {NButton, NModal, NSpin, useMessage} from 'naive-ui'
import {getPreview, savePreview} from '@/utils/indexedDb'

const props = defineProps<{ modelValue: boolean; src: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const message = useMessage()
const {t} = useI18n()
const canvasRef = ref<HTMLElement | null>(null)
const imgRef = ref<HTMLImageElement | null>(null)
const imgLoading = ref(true)
const realSrc = ref<string>(props.src)
const scale = ref(1)
const tx = ref(0)
const ty = ref(0)
const dragging = ref(false)
const moved = ref(false)
const rotation = ref(0)
const startX = ref(0)
const startY = ref(0)

const imgStyle = computed(() => ({
  transform: `translate(-50%, -50%) translate(${tx.value}px, ${ty.value}px) scale(${scale.value}) rotate(${rotation.value}deg)`,
  cursor: dragging.value ? 'grabbing' : 'grab',
  userSelect: 'none'
}))

const fullscreenModalStyle = {width: '100vw', height: '100vh', margin: '0', padding: '0', background: 'transparent'}

function clampScale(v: number) {
  return Math.min(6, Math.max(0.1, v))
}

function onVisibleChange(v: boolean) {
  if (!v) close()
}

function close() {
  emit('update:modelValue', false)
}

function onEsc(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.modelValue) close()
}

let escBound = false

function bindEsc() {
  if (escBound) return
  window.addEventListener('keydown', onEsc)
  escBound = true
}

function unbindEsc() {
  if (!escBound) return
  window.removeEventListener('keydown', onEsc)
  escBound = false
}

function reset() {
  scale.value = 1;
  tx.value = 0;
  ty.value = 0
  rotation.value = 0
}

function zoomIn() {
  scale.value = clampScale(scale.value + 0.2)
}

function zoomOut() {
  scale.value = clampScale(scale.value - 0.2)
}

function onWheel(e: WheelEvent) {
  const delta = e.deltaY > 0 ? -0.15 : 0.15;
  scale.value = clampScale(scale.value + delta)
}

function rotateLeft() {
  rotation.value -= 90
}

function rotateRight() {
  rotation.value += 90
}

function onPointerDown(e: PointerEvent) {
  dragging.value = true;
  startX.value = e.clientX;
  startY.value = e.clientY;
  moved.value = false;
  (e.target as HTMLElement).setPointerCapture?.(e.pointerId)
}

function onPointerMove(e: PointerEvent) {
  if (!dragging.value) return;
  const dx = e.clientX - startX.value;
  const dy = e.clientY - startY.value;
  startX.value = e.clientX;
  startY.value = e.clientY;
  tx.value += dx;
  ty.value += dy
  if (Math.abs(dx) > 2 || Math.abs(dy) > 2) moved.value = true
}

function onPointerUp(e: PointerEvent) {
  dragging.value = false
  const target = e.target as HTMLElement
  if (!moved.value && target === canvasRef.value) close()
}

function onPointerLeave() {
  dragging.value = false
}

function fitToContainer() {
  const el = canvasRef.value
  const img = imgRef.value
  if (!el || !img) return
  const rect = el.getBoundingClientRect()
  const iw = img.naturalWidth || 1
  const ih = img.naturalHeight || 1
  const sw = rect.width / iw
  const sh = rect.height / ih
  scale.value = clampScale(Math.min(1, Math.min(sw, sh)))
  tx.value = 0
  ty.value = 0
}

function onImgLoad() {
  imgLoading.value = false
  try {
    if (!realSrc.value.startsWith('data:')) {
      const img = imgRef.value
      if (!img || !img.complete || img.naturalWidth === 0 || img.naturalHeight === 0) return
      const canvas = document.createElement('canvas')
      canvas.width = img.naturalWidth
      canvas.height = img.naturalHeight
      const ctx = canvas.getContext('2d')
      if (!ctx) return
      ctx.drawImage(img, 0, 0)
      const dataUrl = canvas.toDataURL('image/png')
      if (dataUrl && dataUrl.startsWith('data:')) {
        savePreview(props.src, dataUrl)
      }
    }
  } catch {
  }
  fitToContainer()
}

async function dataUrlToBlob(url: string) {
  const parts = url.split(',')
  const mime = parts[0].match(/data:(.*);base64/)?.[1] || 'image/png'
  const bstr = atob(parts[1])
  const n = bstr.length
  const u8 = new Uint8Array(n)
  for (let i = 0; i < n; i++) u8[i] = bstr.charCodeAt(i)
  return new Blob([u8], {type: mime})
}

async function canvasToBlob(): Promise<Blob | null> {
  const img = imgRef.value
  if (!img || !img.complete || img.naturalWidth === 0 || img.naturalHeight === 0) return null
  const canvas = document.createElement('canvas')
  canvas.width = img.naturalWidth
  canvas.height = img.naturalHeight
  const ctx = canvas.getContext('2d')
  if (!ctx) return null
  try {
    ctx.drawImage(img, 0, 0)
  } catch {
    return null
  }
  return await new Promise(resolve => {
    try {
      canvas.toBlob(b => resolve(b), 'image/png')
    } catch {
      resolve(null)
    }
  })
}

async function copyImage() {
  try {
    let blob: Blob | null = null
    if (props.src.startsWith('data:')) {
      blob = await dataUrlToBlob(props.src)
    } else {
      blob = await canvasToBlob()
    }
    if (blob && navigator.clipboard && 'write' in navigator.clipboard && (window as any).ClipboardItem) {
      await (navigator.clipboard as any).write([new (window as any).ClipboardItem({[blob.type]: blob})])
      message?.success(t('preview.copy_clipboard'))
    } else {
      await copyText(props.src)
      message?.success(t('preview.copy_link'))
    }
  } catch (e) {
    message?.error(t('share.copy_failed'))
  }
}

async function copyText(text: string) {
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return
    }
  } catch {
    // fall through to execCommand
  }
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
  } catch {}
  try {
    document.execCommand('copy')
  } finally {
    document.body.removeChild(ta)
  }
}

async function downloadImage() {
  try {
    let blob: Blob | null = null
    if (props.src.startsWith('data:')) {
      blob = await dataUrlToBlob(props.src)
    } else {
      blob = await canvasToBlob()
    }
    const a = document.createElement('a')
    const nameFromUrl = props.src.split('?')[0].split('/').pop() || 'image'
    if (blob) {
      const url = URL.createObjectURL(blob)
      a.href = url
      a.download = nameFromUrl
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } else {
      a.href = props.src
      a.download = nameFromUrl
      document.body.appendChild(a)
      a.click()
      a.remove()
    }
  } catch (e) {
    message?.error(t('preview.download_failed'))
  }
}

watch(() => props.modelValue, v => {
  if (v) {
    imgLoading.value = true
    reset();
    resolveSrc()
    bindEsc()
  } else {
    unbindEsc()
  }
})
watch(() => props.src, () => {
  imgLoading.value = true
  realSrc.value = props.src
  resolveSrc()
})
onMounted(() => {
  if (props.modelValue) {
    reset();
    resolveSrc()
    bindEsc()
  }
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  unbindEsc()
  window.removeEventListener('resize', onResize)
})

async function resolveSrc() {
  try {
    const cached = await getPreview(props.src)
    if (cached) {
      realSrc.value = cached
    } else {
      realSrc.value = props.src
    }
  } catch {
    realSrc.value = props.src
  }
}

function onResize() {
  if (props.modelValue) fitToContainer()
}
</script>

<style scoped>
:deep(.n-modal-mask) {
  background-color: rgba(0, 0, 0, 0.15);
}

:deep(.n-modal) {
  width: 100vw !important;
  height: 100vh !important;
  margin: 0 !important;
  background-color: transparent !important;
  box-shadow: none !important;
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

:deep(.close-btn.n-button) {
  background-color: rgba(128, 128, 128, 0.3) !important;
  color: #fff !important;
  border: none !important;
  box-shadow: none !important;
}

:deep(.close-btn.n-button:hover) {
  background-color: rgba(128, 128, 128, 0.4) !important;
}

:deep(.control-btn.n-button .n-button__border),
:deep(.close-btn.n-button .n-button__border) {
  display: none !important;
}

.scale-indicator {
  background-color: rgba(0, 0, 0, 0.9);
  color: rgba(255, 255, 255, 0.85);
}
</style>
