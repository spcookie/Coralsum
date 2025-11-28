<template>
  <n-modal
      :closable="false"
      :mask-closable="true"
      :show="modelValue"
      :show-icon="false"
      :style="fullscreenModalStyle"
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
        <div class="scale-indicator px-3 py-1 rounded-md text-xs">缩放 {{ (scale * 100).toFixed(0) }}%</div>
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
            :src="src"
            :style="imgStyle"
            class="absolute top-1/2 left-1/2 will-change-transform"
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
              <span>缩小</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="zoomIn">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:magnify-plus-outline"/>
              <span>放大</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="reset">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:restart"/>
              <span>重置</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="rotateLeft">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:rotate-left"/>
              <span>左旋</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="rotateRight">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:rotate-right"/>
              <span>右旋</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="copyImage">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:content-copy"/>
              <span>复制</span>
            </div>
          </n-button>
          <n-button class="control-btn" size="small" strong @click="downloadImage">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:download"/>
              <span>下载</span>
            </div>
          </n-button>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue'
import {Icon} from '@iconify/vue'
import {NButton, NModal, NSpin, useMessage} from 'naive-ui'

const props = defineProps<{ modelValue: boolean; src: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const message = useMessage()
const canvasRef = ref<HTMLElement | null>(null)
const imgRef = ref<HTMLImageElement | null>(null)
const imgLoading = ref(true)
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
  scale.value = clampScale(Math.min(sw, sh))
  tx.value = 0
  ty.value = 0
}

function onImgLoad() {
  fitToContainer()
  imgLoading.value = false
}

async function fetchBlob(url: string) {
  if (url.startsWith('data:')) {
    const parts = url.split(',')
    const mime = parts[0].match(/data:(.*);base64/)?.[1] || 'image/png'
    const bstr = atob(parts[1])
    const n = bstr.length
    const u8 = new Uint8Array(n)
    for (let i = 0; i < n; i++) u8[i] = bstr.charCodeAt(i)
    return new Blob([u8], {type: mime})
  }
  const res = await fetch(url)
  const blob = await res.blob()
  return blob
}

async function copyImage() {
  try {
    const blob = await fetchBlob(props.src)
    // @ts-ignore
    if (navigator.clipboard && 'write' in navigator.clipboard && window.ClipboardItem) {
      // @ts-ignore
      await navigator.clipboard.write([new ClipboardItem({[blob.type]: blob})])
      message?.success('已复制到剪贴板')
      return
    }
    await navigator.clipboard.writeText(props.src)
    message?.success('已复制图片链接')
  } catch (e) {
    message?.error('复制失败')
  }
}

async function downloadImage() {
  try {
    const blob = await fetchBlob(props.src)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const nameFromUrl = props.src.split('?')[0].split('/').pop() || 'image'
    a.href = url
    a.download = nameFromUrl
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    message?.error('下载失败')
  }
}

watch(() => props.modelValue, v => {
  if (v) {
    imgLoading.value = true
    reset();
    fitToContainer()
  }
})
watch(() => props.src, () => {
  imgLoading.value = true
})
onMounted(() => {
  if (props.modelValue) {
    reset();
    fitToContainer()
  }
})
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
