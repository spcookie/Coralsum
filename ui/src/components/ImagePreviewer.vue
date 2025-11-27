<template>
  <n-modal :closable="false" :mask-closable="true" :show="modelValue" :show-icon="false" :style="modalStyle"
           :trap-focus="false" class="image-previewer-modal" preset="card" @update:show="onVisibleChange">
    <div class="flex flex-col gap-3 select-none">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <div class="flex items-center gap-2 flex-wrap">
          <n-button size="small" tertiary @click="zoomOut">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:magnify-minus-outline"/>
              <span>缩小</span></div>
          </n-button>
          <n-button size="small" tertiary @click="zoomIn">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:magnify-plus-outline"/>
              <span>放大</span></div>
          </n-button>
          <n-button size="small" tertiary @click="reset">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:restart"/>
              <span>重置</span></div>
          </n-button>
          <n-button size="small" tertiary @click="copyImage">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:content-copy"/>
              <span>复制</span></div>
          </n-button>
          <n-button size="small" tertiary @click="downloadImage">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:download"/>
              <span>下载</span></div>
          </n-button>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <div class="text-xs text-neutral-500">缩放 {{ (scale * 100).toFixed(0) }}%</div>
          <n-button quaternary size="small" @click="close">
            <div class="flex items-center gap-1">
              <Icon icon="mdi:close"/>
              <span>关闭预览</span></div>
          </n-button>
        </div>
      </div>
      <div ref="canvasRef" class="relative h-[70vh] w-full overflow-hidden bg-neutral-100 dark:bg-neutral-800 rounded"
           @pointerdown="onPointerDown" @pointerleave="onPointerUp" @pointermove="onPointerMove" @pointerup="onPointerUp"
           @wheel.prevent="onWheel">
        <img ref="imgRef" :src="src" :style="imgStyle" class="absolute top-1/2 left-1/2 will-change-transform"
             draggable="false" @load="onImgLoad"/>
        <n-spin v-if="imgLoading" class="absolute inset-0 grid place-items-center" size="large"/>
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
const startX = ref(0)
const startY = ref(0)

const imgStyle = computed(() => ({
  transform: `translate(-50%, -50%) translate(${tx.value}px, ${ty.value}px) scale(${scale.value})`,
  cursor: dragging.value ? 'grabbing' : 'grab',
  userSelect: 'none'
}))

const modalStyle = {width: 'min(96vw, 1100px)', margin: '24px auto'}

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

function onPointerDown(e: PointerEvent) {
  dragging.value = true;
  startX.value = e.clientX;
  startY.value = e.clientY;
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
}

function onPointerUp() {
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
.n-modal .n-card__content {
  height: auto;
}
</style>
