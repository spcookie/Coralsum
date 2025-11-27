<template>
  <div
      class="w-full md:w-[420px] md:h-full h-auto overflow-y-auto p-4 md:p-5 md:border-r border-b md:border-b-0 border-neutral-200 dark:border-neutral-800 space-y-5 glass bg-white/40 dark:bg-black/30">
    <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
        <span>提示词</span>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <Icon class="text-neutral-400" icon="ph:info"/>
          </template>
          <div class="text-xs text-white">
            输入自然语言描述你想要生成或修改的内容。建议包含主体、风格、构图与色调，明确限制条件与细节，避免歧义。
          </div>
        </n-tooltip>
      </div>
      <n-input v-model:value="prompt" :autosize="{ minRows: 6, maxRows: 12 }"
               placeholder="请输入提示词（必填）：描述主体、风格、构图与色调"
               type="textarea"/>
    </div>
    <n-collapse :default-expanded-names="[]">
      <n-collapse-item name="img">
        <template #header>
          <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
            <span>输入图片</span>
            <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
              <template #trigger>
                <Icon class="text-neutral-400" icon="ph:info"/>
              </template>
              <div class="text-xs text-white">可拖拽、选择或粘贴一到多张参考图，辅助生成或进行图像编辑。建议使用
                PNG/JPG，体积适中。
              </div>
            </n-tooltip>
          </div>
        </template>
        <n-upload :default-file-list="uploadList" multiple @change="onUploadChange">
          <n-upload-dragger @paste.prevent="handlePaste">
            <div
                class="flex flex-col items-center justify-center py-10 border border-dashed rounded-xl border-neutral-300 dark:border-neutral-700 text-neutral-500 bg-neutral-50/60 dark:bg-neutral-900/30">
              <Icon class="text-2xl" icon="mdi:cloud-upload"/>
              <div>拖拽、点击选择或粘贴图片</div>
            </div>
          </n-upload-dragger>
        </n-upload>
        <div v-if="uploadList.length > 0" class="mt-3 grid grid-cols-2 md:grid-cols-3 gap-2">
          <n-image-group>
            <n-image
                v-for="(it,i) in uploadList"
                :key="i"
                :class="selectedIndex===i ? 'ring-2 ring-emerald-500 rounded' : ''"
                :img-props="{ class: 'rounded bg-neutral-100 dark:bg-neutral-800' }"
                :src="it.url"
                class="w-full"
                @click="selectedIndex=i"
            />
          </n-image-group>
        </div>
      </n-collapse-item>
      <n-collapse-item name="sys">
        <template #header>
          <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
            <span>系统提示词</span>
            <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
              <template #trigger>
                <Icon class="text-neutral-400" icon="ph:info"/>
              </template>
              <div class="text-xs text-white">
                面向模型的高级指令，用于约束整体风格、行为与安全策略。建议与提示词分离，保持客观、简洁。
              </div>
            </n-tooltip>
          </div>
        </template>
        <n-input v-model:value="systemPrompt" :autosize="{ minRows: 4, maxRows: 10 }"
                 placeholder="系统提示（可选）：约束整体风格、行为与安全策略"
                 type="textarea"/>
      </n-collapse-item>
    </n-collapse>
    <div class="grid grid-cols-2 gap-3">
      <n-button class="w-full justify-center" tertiary type="warning" @click="onReset">
        <div class="flex items-center gap-2">
          <Icon icon="mdi:reload"/>
          <span>重置</span>
        </div>
      </n-button>
      <n-button :disabled="generating || promptEmpty" class="w-full justify-center" type="primary" @click="onGenerate">
        <div class="flex items-center gap-2">
          <Icon :class="generating ? 'animate-spin' : ''" :icon="generating ? 'mdi:loading' : 'mdi:magic-staff'"/>
          <span>生成</span>
        </div>
      </n-button>
    </div>
    <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
        <span>候选图片数量</span>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <Icon class="text-neutral-400" icon="ph:info"/>
          </template>
          <div class="text-xs text-white">一次返回的候选结果数量。数量越多，耗时与消耗越高，建议按需选择 1–4。</div>
        </n-tooltip>
      </div>
      <div class="segmented radio-center">
        <n-radio-group v-model:value="settings.candidateRadio">
          <n-radio-button :value="1">1</n-radio-button>
          <n-radio-button :value="2">2</n-radio-button>
          <n-radio-button :value="3">3</n-radio-button>
          <n-radio-button :value="4">4</n-radio-button>
        </n-radio-group>
      </div>
    </div>
    <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
        <span>图片宽高比</span>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <Icon class="text-neutral-400" icon="ph:info"/>
          </template>
          <div class="text-xs text-white">自动：沿用输入图片的原始比例。也可选择固定比例（1:1、16:9
            等）以适配社交/海报等场景。
          </div>
        </n-tooltip>
      </div>
      <div class="grid grid-cols-3 sm:grid-cols-4 gap-2">
        <n-button :type="settings.aspectRatio==='auto'?'primary':'default'" class="w-full" size="small"
                  @click="settings.aspectRatio='auto'">自动
        </n-button>
        <n-button :type="settings.aspectRatio==='1:1'?'primary':'default'" class="w-full" size="small"
                  @click="settings.aspectRatio='1:1'">1:1
        </n-button>
        <n-button :type="settings.aspectRatio==='2:3'?'primary':'default'" class="w-full" size="small"
                  @click="settings.aspectRatio='2:3'">2:3
        </n-button>
        <n-button :type="settings.aspectRatio==='3:2'?'primary':'default'" class="w-full" size="small"
                  @click="settings.aspectRatio='3:2'">3:2
        </n-button>
        <n-button :type="settings.aspectRatio==='3:4'?'primary':'default'" class="w-full" size="small"
                  @click="settings.aspectRatio='3:4'">3:4
        </n-button>
        <n-button :type="settings.aspectRatio==='4:3'?'primary':'default'" class="w-full" size="small"
                  @click="settings.aspectRatio='4:3'">4:3
        </n-button>
        <n-button :type="settings.aspectRatio==='9:16'?'primary':'default'" class="w-full" size="small"
                  @click="settings.aspectRatio='9:16'">9:16
        </n-button>
        <n-button :type="settings.aspectRatio==='16:9'?'primary':'default'" class="w-full" size="small"
                  @click="settings.aspectRatio='16:9'">16:9
        </n-button>
        <n-button :type="settings.aspectRatio==='21:9'?'primary':'default'" class="w-full" size="small"
                  @click="settings.aspectRatio='21:9'">21:9
        </n-button>
      </div>
    </div>
    <div class="space-y-3">
      <div>
        <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center justify-between">
          <div class="flex items-center gap-1">
            <span>核采样</span>
            <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
              <template #trigger>
                <Icon class="text-neutral-400" icon="ph:info"/>
              </template>
              <div class="text-xs text-white">
                核采样阈值，控制输出多样性；值越大越丰富，越小越保守。可按需微调以平衡质量与多样性。
              </div>
            </n-tooltip>
          </div>
          <div class="text-[11px] text-neutral-500 dark:text-neutral-300">{{ settings.topP.toFixed(1) }}</div>
        </div>
        <n-config-provider :theme="settings.darkMode ? darkTheme : undefined" :theme-overrides="sliderThemeOverrides">
          <n-slider v-model:value="settings.topP" :max="1.0" :min="0.0" :step="0.1"/>
        </n-config-provider>
      </div>
      <div>
        <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center justify-between">
          <div class="flex items-center gap-1">
            <span>温度</span>
            <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
              <template #trigger>
                <Icon class="text-neutral-400" icon="ph:info"/>
              </template>
              <div class="text-xs text-white">温度控制随机性；低温更稳定、可控，高温更有创意、发散。推荐 0.6–0.9。</div>
            </n-tooltip>
          </div>
          <div class="text-[11px] text-neutral-500 dark:text-neutral-300">{{ settings.temperature.toFixed(1) }}</div>
        </div>
        <n-config-provider :theme="settings.darkMode ? darkTheme : undefined" :theme-overrides="sliderThemeOverrides">
          <n-slider v-model:value="settings.temperature" :max="2.0" :min="0.0" :step="0.1"/>
        </n-config-provider>
      </div>
    </div>
    <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
        <span>输出格式</span>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <Icon class="text-neutral-400" icon="ph:info"/>
          </template>
          <div class="text-xs text-white">
            选择最终图片编码格式：PNG（无损、支持透明）、JPG（体积更小）、WEBP（高压缩比与较好质量）。
          </div>
        </n-tooltip>
      </div>
      <div class="segmented radio-center">
        <n-radio-group v-model:value="settings.outputFormat">
          <n-radio-button value="PNG">PNG</n-radio-button>
          <n-radio-button value="JPG">JPG</n-radio-button>
        </n-radio-group>
      </div>
    </div>
    <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
        <span>图片分辨率</span>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <Icon class="text-neutral-400" icon="ph:info"/>
          </template>
          <div class="text-xs text-white">选择目标输出的基础分辨率尺寸，数值越大越清晰，耗时与消耗越高。</div>
        </n-tooltip>
      </div>
      <div class="segmented radio-center">
        <n-radio-group v-model:value="settings.imageSize">
          <n-radio-button value="1K">1K</n-radio-button>
          <n-radio-button value="2K">2K</n-radio-button>
          <n-radio-button value="4K">4K</n-radio-button>
        </n-radio-group>
      </div>
    </div>
    <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
        <span>超分辨率</span>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <Icon class="text-neutral-400" icon="ph:info"/>
          </template>
          <div class="text-xs text-white">使用 AI 对图像进行超分与细节重建，使画面更清晰锐利；倍率越高耗时与消耗越大。
          </div>
        </n-tooltip>
      </div>
      <div class="segmented radio-center">
        <n-radio-group v-model:value="settings.resolution">
          <n-radio-button value="1x">1x</n-radio-button>
          <n-radio-button value="2x">2x</n-radio-button>
          <n-radio-button value="3x">3x</n-radio-button>
          <n-radio-button value="4x">4x</n-radio-button>
        </n-radio-group>
      </div>
    </div>
    <n-modal v-model:show="confirmReset" content="是否重置所有配置？" negative-text="取消" positive-text="确认"
             preset="dialog" title="确认重置" @positive-click="doReset"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue'
import {Icon} from '@iconify/vue'
import {useSettingsStore} from '@/stores/settings'
import {useUserStore} from '@/stores/user'
import {
  darkTheme,
  NButton,
  NCollapse,
  NCollapseItem,
  NConfigProvider,
  NImage,
  NImageGroup,
  NInput,
  NModal,
  NRadioButton,
  NRadioGroup,
  NSlider,
  NTooltip,
  NUpload,
  NUploadDragger,
  useMessage
} from 'naive-ui'

const emit = defineEmits<{
  (e: 'generate', payload: { prompt: string; systemPrompt?: string; files: File[] }): void
}>()
const p = defineProps<{ generating?: boolean }>()
const generating = computed(() => p.generating)
const message = useMessage()

const settings = useSettingsStore()
const user = useUserStore()
const prompt = ref('')
const promptEmpty = computed(() => prompt.value.trim().length === 0)
const systemPrompt = ref('')
const files = ref<File[]>([])
const uploadList = ref<any[]>([])
const confirmReset = ref(false)
const selectedIndex = ref<number | null>(null)

function onUploadChange(list: any) {
  const fl = list?.fileList ?? []
  for (const it of fl) {
    if (!it.url && it.file) it.url = URL.createObjectURL(it.file)
  }
  uploadList.value = fl
  files.value = fl.map((f: any) => f.file).filter(Boolean)
}

function onReset() {
  confirmReset.value = true
}

function onGenerate() {
  if (promptEmpty.value) {
    message.error('请填写提示词')
    return
  }
  const pts = Number(user.points || 0)
  if (pts <= 0) {
    message.error('积分不足')
    return
  }
  emit('generate', {prompt: prompt.value, systemPrompt: systemPrompt.value, files: files.value})
}

function doReset() {
  settings.reset()
  uploadList.value.forEach((it: any) => {
    if (it?.url?.startsWith('blob:')) URL.revokeObjectURL(it.url)
  })
  uploadList.value = []
  files.value = []
  selectedIndex.value = null
}

function handlePaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items || []
  const pastedFiles: File[] = []
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item.kind === 'file') {
      const file = item.getAsFile()
      if (file) pastedFiles.push(file)
    }
  }
  if (pastedFiles.length) {
    for (const file of pastedFiles) {
      uploadList.value.push({name: file.name, status: 'finished', url: URL.createObjectURL(file)})
      files.value.push(file)
    }
  }
}

const sliderThemeOverrides = computed(() => ({
  common: settings.darkMode
      ? {primaryColor: '#2a947d', primaryColorSuppl: '#2a947d'}
      : {primaryColor: '#18a058', primaryColorSuppl: '#36ad6a'},
  Slider: {
    railColor: settings.darkMode ? 'rgba(255,255,255,0.2)' : '#e5e7eb',
    railColorHover: settings.darkMode ? 'rgba(255,255,255,0.3)' : '#d1d5db',
    railColorActive: settings.darkMode ? 'rgba(255,255,255,0.25)' : '#dbeafe',
    fillColor: settings.darkMode ? '#2a947d' : '#18a058',
    fillColorHover: settings.darkMode ? '#2a947d' : '#36ad6a',
    handleColor: settings.darkMode ? '#2a947d' : '#18a058',
    handleBorderColor: settings.darkMode ? '#2a947d' : '#18a058',
    dotColor: settings.darkMode ? '#2a947d' : '#36ad6a'
  }
}))

</script>

<style scoped>
.segmented :deep(.n-radio-group) {
  display: flex;
  width: 100%;
  gap: 0
}

.segmented :deep(.n-radio-button) {
  flex: 1 1 0;
  margin: 0;
  --n-button-padding: 8px 0;
  display: flex
}

.segmented :deep(.n-radio-button .n-radio-button__label),
.segmented :deep(.n-radio-button .n-radio-button__content) {
  width: 100%;
  display: grid;
  place-items: center;
  text-align: center;
}

.segmented :deep(.n-radio-button .n-radio__label) {
  flex: 1 1 0;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 0;
  text-align: center;
}

.radio-center :deep(.n-radio-button) {
  display: flex
}

.radio-center :deep(.n-radio-button .n-radio-button__content) {
  flex: 1;
  height: 100%;
  padding: 8px 0;
  padding-left: 0;
  padding-right: 0
}

.tooltip-xs :deep(.n-tooltip__content) {
  font-size: 0.75rem;
  line-height: 1rem;
  padding: 6px 8px
}

.segmented-wrap :deep(.n-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  width: 100%
}

.segmented-wrap :deep(.n-radio-button) {
  flex: 0 0 calc(33.333% - 4px);
  margin: 0;
  --n-button-padding: 8px 0;
  display: flex
}

/* removed class-based slider overrides to avoid conflicting colors */
</style>
