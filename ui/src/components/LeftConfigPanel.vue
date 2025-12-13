<template>
  <div
      class="relative z-20 w-full md:w-[420px] md:flex-shrink-0 md:h-[calc(100vh-56px)] h-auto md:overflow-y-auto no-scrollbar p-4 md:p-5 md:border-r border-b md:border-b-0 border-neutral-200 dark:border-neutral-800 space-y-5 glass bg-white/40 dark:bg-black/30">
    <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1 justify-between">
        <div class="flex items-center gap-1">
          <span>{{ t('left.prompt.title') }}</span>
          <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
            <template #trigger>
              <Icon class="text-neutral-400" icon="ph:info"/>
            </template>
            <div class="text-xs text-white">{{ t('left.prompt.desc') }}</div>
          </n-tooltip>
        </div>
        <div class="flex items-center gap-1">
          <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
            <template #trigger>
              <button
                  aria-label="打开创意模版"
                  class="inline-flex items-center justify-center cursor-pointer w-6 h-6 p-1 rounded-full transition-colors duration-200 hover:bg-amber-100 dark:hover:bg-amber-900/40"
                  @click="openIdeaPicker"
              >
                <Icon class="text-amber-600 transition-colors duration-200 hover:text-amber-700"
                      icon="mdi:lightbulb-on-outline"/>
              </button>
            </template>
            <div class="text-xs text-white">创意模版</div>
          </n-tooltip>
          <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <button
                :aria-label="t('left.prompt.open_guide')"
                class="inline-flex items-center justify-center cursor-pointer w-6 h-6 p-1 rounded-full transition-colors duration-200 hover:bg-green-100 dark:hover:bg-green-900/40"
                @click="openPromptGuide"
            >
              <Icon class="text-green-600 transition-colors duration-200 hover:text-green-700"
                    icon="ph:book-open-text"/>
            </button>
          </template>
          <div class="text-xs text-white">{{ t('left.prompt.guide') }}</div>
          </n-tooltip>
        </div>
      </div>
      <n-input v-model:value="prompt" :autosize="{ minRows: 6, maxRows: 12 }" :disabled="generating"
               :placeholder="t('left.prompt.placeholder')"
               :maxlength="2000" clearable show-count type="textarea" @clear="clearPrompt"/>
    </div>
    <n-collapse :expanded-names="basicExpandedNames" @update:expanded-names="onBasicExpandChange">
      <n-collapse-item name="img">
        <template #header>
          <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
            <span>{{ t('left.image_input.title') }}</span>
            <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
              <template #trigger>
                <Icon class="text-neutral-400" icon="ph:info"/>
              </template>
              <div class="text-xs text-white">{{ t('left.image_input.tip') }}</div>
            </n-tooltip>
          </div>
        </template>
        <n-upload
            :default-file-list="uploadList"
            :disabled="generating"
            :on-before-upload="beforeUpload"
            accept="image/png,image/jpeg"
            multiple
            @change="onUploadChange">
          <n-upload-dragger @paste.prevent="handlePaste">
            <div
                class="flex flex-col items-center justify-center py-10 border border-dashed rounded-xl border-neutral-300 dark:border-neutral-700 text-neutral-500 bg-neutral-50/60 dark:bg-neutral-900/30">
              <Icon class="text-2xl" icon="mdi:cloud-upload"/>
              <div class="text-[13px] sm:text-sm">{{ t('left.image_input.drag') }}</div>
              <div class="text-[11px] sm:text-xs mt-1 text-neutral-500 dark:text-neutral-400">
                {{ t('left.image_input.only_png_jpg') }}
              </div>
            </div>
          </n-upload-dragger>
        </n-upload>
        <div v-if="uploadList.length > 0" class="mt-3 columns-2 md:columns-3 gap-2">
          <div
              v-for="(it,i) in uploadList"
              :key="i"
              class="mb-2 break-inside-avoid rounded bg-neutral-100 dark:bg-neutral-800 relative overflow-hidden">
            <img
                :src="it.url"
                class="w-full h-auto object-contain rounded cursor-pointer relative z-0"
                @click="onThumbClick(it.url, i)"
            />
            <div class="absolute top-0 right-0 w-8 h-8 pointer-events-none z-10">
              <div class="badge-fan"></div>
              <div class="badge-num">{{ i + 1 }}</div>
            </div>
          </div>
        </div>
      </n-collapse-item>
      <n-collapse-item name="sys">
        <template #header>
          <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
            <span>{{ t('left.system_prompt.title') }}</span>
            <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
              <template #trigger>
                <Icon class="text-neutral-400" icon="ph:info"/>
              </template>
              <div class="text-xs text-white">
                {{ t('left.system_prompt.tip') }}
              </div>
            </n-tooltip>
          </div>
        </template>
        <n-input v-model:value="systemPrompt" :autosize="{ minRows: 4, maxRows: 10 }" :disabled="generating"
                 :placeholder="t('left.system_prompt.placeholder')"
                 :maxlength="1000" clearable show-count type="textarea" @clear="clearSystemPrompt"/>
      </n-collapse-item>
    </n-collapse>
    <div class="grid grid-cols-2 gap-3">
      <n-button :disabled="generating" class="w-full justify-center" tertiary type="warning" @click="onReset">
        <div class="flex items-center gap-2">
          <Icon icon="mdi:reload"/>
          <span>{{ t('left.actions.reset') }}</span>
        </div>
      </n-button>
      <div class="relative w-full">
        <n-button :disabled="generating || promptEmpty" :loading="generating" class="w-full justify-center" type="primary"
                  @click="onGenerate">
          <div class="flex items-center gap-2">
            <Icon v-if="!generating" icon="ph:sparkle"/>
            <span>{{ t('left.actions.generate') }}</span>
          </div>
        </n-button>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover" :disabled="badgeHidden">
          <template #trigger>
            <transition name="bubble">
              <div v-if="!badgeHidden" :style="badgeStyle" class="absolute -top-2 -right-2 z-20 text-[11px] px-2 py-0.5 rounded-full shadow-md border pointer-events-auto flex items-center gap-1">
                -{{ estimatedPoints }}
                <Icon class="text-[12px] text-amber-500 dark:text-amber-700" icon="material-symbols:bolt-rounded"/>
              </div>
            </transition>
          </template>
          <div class="text-xs text-white">{{ t('left.estimate.tip') }}</div>
        </n-tooltip>
      </div>
    </div>
    <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
        <span>{{ t('left.model_type.title') }}</span>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <Icon class="text-neutral-400" icon="ph:info"/>
          </template>
          <div class="text-xs text-white">{{ t('left.model_type.tip') }}</div>
        </n-tooltip>
      </div>
      <div class="segmented radio-center">
        <n-radio-group v-model:value="settings.modelType" :disabled="generating">
          <n-radio-button value="Basic">{{ t('left.model_type.basic') }}</n-radio-button>
          <n-radio-button value="Pro">{{ t('left.model_type.pro') }}</n-radio-button>
        </n-radio-group>
      </div>
    </div>
    <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
        <span>{{ t('left.candidate.title') }}</span>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <Icon class="text-neutral-400" icon="ph:info"/>
          </template>
          <div class="text-xs text-white">{{ t('left.candidate.tip') }}</div>
        </n-tooltip>
      </div>
      <div class="segmented radio-center">
        <n-radio-group v-model:value="settings.candidateRadio" :disabled="generating">
          <n-radio-button :value="1">1</n-radio-button>
          <n-radio-button :value="2">2</n-radio-button>
          <n-radio-button :value="3">3</n-radio-button>
          <n-radio-button :value="4">4</n-radio-button>
        </n-radio-group>
      </div>
    </div>
    <n-collapse :expanded-names="advancedExpandedNames" @update:expanded-names="onAdvancedExpandChange">
      <n-collapse-item name="advanced">
        <template #header>
          <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
            <span>{{ t('left.advanced.title') }}</span>
            <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
              <template #trigger>
                <Icon class="text-neutral-400" icon="ph:info"/>
              </template>
              <div class="text-xs text-white">{{ t('left.advanced.tip') }}</div>
            </n-tooltip>
          </div>
        </template>
        <div class="space-y-6">
          <div class="space-y-2">
      <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
        <span>{{ t('left.aspect_ratio.title') }}</span>
        <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
          <template #trigger>
            <Icon class="text-neutral-400" icon="ph:info"/>
          </template>
          <div class="text-xs text-white">{{ t('left.aspect_ratio.tip') }}</div>
        </n-tooltip>
      </div>
      <div class="grid grid-cols-3 sm:grid-cols-4 gap-2">
        <n-button :disabled="generating" :type="settings.aspectRatio==='auto'?'primary':'default'" class="w-full"
                  size="small"
                  @click="settings.aspectRatio='auto'">{{ t('left.aspect_ratio.auto') }}
        </n-button>
        <n-button :disabled="generating" :type="settings.aspectRatio==='1:1'?'primary':'default'" class="w-full"
                  size="small"
                  @click="settings.aspectRatio='1:1'">1:1
        </n-button>
        <n-button :disabled="generating" :type="settings.aspectRatio==='2:3'?'primary':'default'" class="w-full"
                  size="small"
                  @click="settings.aspectRatio='2:3'">2:3
        </n-button>
        <n-button :disabled="generating" :type="settings.aspectRatio==='3:2'?'primary':'default'" class="w-full"
                  size="small"
                  @click="settings.aspectRatio='3:2'">3:2
        </n-button>
        <n-button :disabled="generating" :type="settings.aspectRatio==='3:4'?'primary':'default'" class="w-full"
                  size="small"
                  @click="settings.aspectRatio='3:4'">3:4
        </n-button>
        <n-button :disabled="generating" :type="settings.aspectRatio==='4:3'?'primary':'default'" class="w-full"
                  size="small"
                  @click="settings.aspectRatio='4:3'">4:3
        </n-button>
        <n-button :disabled="generating" :type="settings.aspectRatio==='9:16'?'primary':'default'" class="w-full"
                  size="small"
                  @click="settings.aspectRatio='9:16'">9:16
        </n-button>
        <n-button :disabled="generating" :type="settings.aspectRatio==='16:9'?'primary':'default'" class="w-full"
                  size="small"
                  @click="settings.aspectRatio='16:9'">16:9
        </n-button>
        <n-button :disabled="generating" :type="settings.aspectRatio==='21:9'?'primary':'default'" class="w-full"
                  size="small"
                  @click="settings.aspectRatio='21:9'">21:9
        </n-button>
      </div>
          </div>
          <div class="space-y-2">
            <div>
              <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center justify-between">
                <div class="flex items-center gap-1">
                  <span>{{ t('left.temperature.title') }}</span>
                  <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
                    <template #trigger>
                      <Icon class="text-neutral-400" icon="ph:info"/>
                    </template>
                    <div class="text-xs text-white">{{ t('left.temperature.tip') }}</div>
                  </n-tooltip>
                </div>
                <div class="text-[11px] text-neutral-500 dark:text-neutral-300">{{
                    settings.temperature.toFixed(1)
                  }}
                </div>
              </div>
              <n-config-provider :theme="settings.darkMode ? darkTheme : undefined"
                                 :theme-overrides="sliderThemeOverrides">
                <n-slider v-model:value="settings.temperature" :disabled="generating" :max="2.0" :min="0.0"
                          :step="0.1"/>
              </n-config-provider>
            </div>
          </div>
          <!--          <div class="space-y-2">-->
          <!--            <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">-->
          <!--              <span>媒体分辨率</span>-->
          <!--              <n-tooltip class="tooltip-xs" placement="top" trigger="hover">-->
          <!--                <template #trigger>-->
          <!--                  <Icon class="text-neutral-400" icon="ph:info"/>-->
          <!--                </template>-->
          <!--                <div class="text-xs text-white">-->
          <!--                  精细控制多模态视觉解析。分辨率越高，越有助于读取小字与识别细节，但会增加令牌消耗与生成时延。-->
          <!--                </div>-->
          <!--              </n-tooltip>-->
          <!--            </div>-->
          <!--            <div class="segmented radio-center">-->
          <!--              <n-radio-group v-model:value="settings.mediaResolution" :disabled="generating">-->
          <!--                <n-radio-button value="auto">自动</n-radio-button>-->
          <!--                <n-radio-button value="low">低</n-radio-button>-->
          <!--                <n-radio-button value="medium">中</n-radio-button>-->
          <!--                <n-radio-button value="high">高</n-radio-button>-->
          <!--              </n-radio-group>-->
          <!--            </div>-->
          <!--          </div>-->
          <div v-if="settings.modelType!=='Basic'" class="space-y-2">
            <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
              <span>{{ t('left.image_size.title') }}</span>
              <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
                <template #trigger>
                  <Icon class="text-neutral-400" icon="ph:info"/>
                </template>
                <div class="text-xs text-white">{{ t('left.image_size.tip') }}</div>
              </n-tooltip>
            </div>
            <div class="segmented radio-center">
              <n-radio-group v-model:value="settings.imageSize" :disabled="generating">
                <n-radio-button value="1K">1K</n-radio-button>
                <n-radio-button value="2K">2K</n-radio-button>
                <n-radio-button value="4K">4K</n-radio-button>
              </n-radio-group>
            </div>
          </div>
          <div class="space-y-2">
            <div>
              <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center justify-between">
                <div class="flex items-center gap-1">
                  <span>{{ t('left.top_p.title') }}</span>
                  <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
                    <template #trigger>
                      <Icon class="text-neutral-400" icon="ph:info"/>
                    </template>
                    <div class="text-xs text-white">{{ t('left.top_p.tip') }}</div>
                  </n-tooltip>
                </div>
                <div class="text-[11px] text-neutral-500 dark:text-neutral-300">{{ settings.topP.toFixed(2) }}</div>
              </div>
              <n-config-provider :theme="settings.darkMode ? darkTheme : undefined"
                                 :theme-overrides="sliderThemeOverrides">
                <n-slider v-model:value="settings.topP" :disabled="generating" :max="1.0" :min="0.0" :step="0.05"/>
              </n-config-provider>
            </div>
          </div>
          <div class="space-y-2">
            <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
              <span>{{ t('left.output_format.title') }}</span>
              <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
                <template #trigger>
                  <Icon class="text-neutral-400" icon="ph:info"/>
                </template>
                <div class="text-xs text-white">{{ t('left.output_format.tip') }}</div>
              </n-tooltip>
            </div>
            <div class="segmented radio-center">
              <n-radio-group v-model:value="settings.outputFormat" :disabled="generating">
                <n-radio-button value="PNG">PNG</n-radio-button>
                <n-radio-button value="JPG">JPG</n-radio-button>
              </n-radio-group>
            </div>
          </div>
          <div class="space-y-2">
            <div class="text-xs uppercase tracking-wide text-neutral-500 flex items-center gap-1">
              <span>{{ t('left.upscayl.title') }}</span>
              <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
                <template #trigger>
                  <Icon class="text-neutral-400" icon="ph:info"/>
                </template>
                <div class="text-xs text-white">{{ t('left.upscayl.tip') }}</div>
              </n-tooltip>
              <n-tooltip class="tooltip-xs" placement="top" trigger="hover">
                <template #trigger>
                  <Icon class="text-amber-500" icon="ph:warning"/>
                </template>
                <div class="text-xs text-white">{{ t('left.upscayl.warn') }}</div>
              </n-tooltip>
            </div>
            <div class="segmented radio-center">
              <n-radio-group v-model:value="settings.resolution" :disabled="generating">
                <n-radio-button value="1x">1x</n-radio-button>
                <n-radio-button value="2x">2x</n-radio-button>
                <n-radio-button value="3x">3x</n-radio-button>
                <n-radio-button value="4x">4x</n-radio-button>
              </n-radio-group>
            </div>
          </div>
        </div>
      </n-collapse-item>
    </n-collapse>
    <n-modal v-model:show="confirmReset" :content="t('left.reset.confirm_content')"
             :negative-text="t('profile.modal.cancel')" :positive-text="t('common.ok')"
             :title="t('left.reset.confirm_title')" preset="dialog" @positive-click="doReset"/>
    <n-modal v-model:show="promptGuideShow" :style="{ width: 'min(920px, 86vw)', margin: '20px auto' }"
             class="rounded-xl"
             :title="t('left.prompt.guide_title')" preset="card">
      <div class="max-h-[70vh] overflow-y-auto text-[13px] space-y-4">
        <div class="text-lg font-semibold">{{ t('left.guide.generate_title') }}</div>
        <ol class="list-decimal list-inside space-y-2">
          <li>
            <div class="font-medium">{{ t('left.guide.real_scene.title') }}</div>
            <div>{{ t('left.guide.real_scene.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.stickers.title') }}</div>
            <div>{{ t('left.guide.stickers.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.text_accuracy.title') }}</div>
            <div>{{ t('left.guide.text_accuracy.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.product_photography.title') }}</div>
            <div>{{ t('left.guide.product_photography.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.minimal_style.title') }}</div>
            <div>{{ t('left.guide.minimal_style.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.sequential_art.title') }}</div>
            <div>{{ t('left.guide.sequential_art.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.use_search.title') }}</div>
            <div>{{ t('left.guide.use_search.desc') }}</div>
          </li>
        </ol>
        <div class="text-lg font-semibold">{{ t('left.guide.edit_title') }}</div>
        <ol class="list-decimal list-inside space-y-2">
          <li>
            <div class="font-medium">{{ t('left.guide.add_remove.title') }}</div>
            <div>{{ t('left.guide.add_remove.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.inpaint.title') }}</div>
            <div>{{ t('left.guide.inpaint.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.style_transfer.title') }}</div>
            <div>{{ t('left.guide.style_transfer.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.advanced_composition.title') }}</div>
            <div>{{ t('left.guide.advanced_composition.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.high_fidelity.title') }}</div>
            <div>{{ t('left.guide.high_fidelity.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.bring_to_life.title') }}</div>
            <div>{{ t('left.guide.bring_to_life.desc') }}</div>
          </li>
          <li>
            <div class="font-medium">{{ t('left.guide.character_consistency.title') }}</div>
            <div>{{ t('left.guide.character_consistency.desc') }}</div>
          </li>
        </ol>
        <div class="text-lg font-semibold">{{ t('left.guide.best_title') }}</div>
        <ul class="list-disc list-inside space-y-2">
          <li>
            <span class="font-medium">{{ t('left.guide.concretize.title') }}</span>
            <span>{{ t('left.guide.concretize.desc') }}</span>
          </li>
          <li>
            <span class="font-medium">{{ t('left.guide.context_intent.title') }}</span>
            <span>{{ t('left.guide.context_intent.desc') }}</span>
          </li>
          <li>
            <span class="font-medium">{{ t('left.guide.iterate.title') }}</span>
            <span>{{ t('left.guide.iterate.desc') }}</span>
          </li>
          <li>
            <span class="font-medium">{{ t('left.guide.stepwise.title') }}</span>
            <span>{{ t('left.guide.stepwise.desc') }}</span>
          </li>
          <li>
            <span class="font-medium">{{ t('left.guide.negative_prompt.title') }}</span>
            <span>{{ t('left.guide.negative_prompt.desc') }}</span>
          </li>
          <li>
            <span class="font-medium">{{ t('left.guide.control_camera.title') }}</span>
            <span>{{ t('left.guide.control_camera.desc') }}</span>
          </li>
        </ul>
      </div>
    </n-modal>
    <n-modal v-model:show="ideaPickerShow" :style="{ width: 'min(920px, 92vw)', margin: '20px auto' }"
             :title="t('left.idea_picker.title')" display-directive="show"
             preset="card">
      <div class="space-y-3">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-2">
          <div class="filter-tags flex flex-wrap items-center gap-2">
            <span class="text-[12px] text-neutral-500">{{ t('left.idea_picker.filter_categories') }}</span>
            <n-tag v-for="opt in pickerCatOptions" :key="opt.value" :bordered="false" :checked="(pickerCategoryIds||[]).includes(opt.value)" checkable
                   round size="small" type="success"
                   @update:checked="(c)=>toggleCategory(opt.value, c)">{{ opt.label }}
            </n-tag>
          </div>
          <div class="filter-tags flex flex-wrap items-center gap-2">
            <span class="text-[12px] text-neutral-500">{{ t('left.idea_picker.filter_tags') }}</span>
            <n-tag v-for="opt in pickerTagOptions" :key="opt.value" :checked="(pickerTagIds||[]).includes(opt.value)" bordered checkable size="small"
                   type="info" @update:checked="(c)=>toggleTag(opt.value, c)">
              {{ opt.label }}
            </n-tag>
          </div>
          <n-select v-model:value="pickerSortBy" :options="pickerSortOptions" :placeholder="t('left.idea_picker.sort')"
                    size="small"/>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <div v-for="it in pickerTemplates" :key="it.id"
               class="rounded-lg border border-neutral-200 dark:border-neutral-800 bg-white/70 dark:bg-black/30 shadow-sm hover:shadow-md transition-shadow overflow-hidden">
            <div class="p-3 space-y-3">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <n-button class="apply-btn" size="tiny" tertiary type="success" @click="applyTemplate(it)">
                    <Icon icon="ph:check-circle"/>
                    <span>{{ t('left.idea_picker.apply') }}</span>
                  </n-button>
                  <div class="font-semibold text-sm text-neutral-800 dark:text-neutral-200">{{ it.name }}</div>
                </div>
                <div class="text-[10px] flex items-center gap-0.5">
                  <span v-if="newBadge(it)"
                        class="px-1 py-0 rounded bg-green-50 text-green-700 border border-green-200">{{
                      t('left.idea_picker.badge_new')
                    }}</span>
                  <span v-if="hotBadge(it)" class="px-1 py-0 rounded bg-red-50 text-red-700 border border-red-200">{{
                      t('left.idea_picker.badge_hot')
                    }}</span>
                </div>
              </div>
              <div class="chips-sm flex flex-wrap items-center gap-1">
                <n-tag v-if="it.categoryName" :bordered="false" round size="small" type="success">{{
                    it.categoryName
                  }}
                </n-tag>
                <n-tag v-for="tag in it.tagNames || []" :key="tag" bordered size="small" type="info">{{ tag }}</n-tag>
              </div>
              <div v-if="it.description"
                   class="text-[12px] text-neutral-600 dark:text-neutral-300 whitespace-pre-wrap line-clamp-3">
                {{ it.description }}
              </div>
              <div
                  class="text-[12px] text-neutral-700 dark:text-neutral-200 whitespace-pre-wrap rounded-md bg-neutral-50 dark:bg-neutral-900/40 border border-neutral-200 dark:border-neutral-800 p-2">
                {{ it.promptText }}
              </div>
              <div class="space-y-2">
                <div v-if="it.effectImageUrl" class="space-y-1">
                  <div class="text-[11px] text-neutral-500">{{ t('left.idea_picker.effect') }}</div>
                  <div
                      class="square-box rounded-md border border-neutral-200 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-900/40">
                    <img :src="it.effectImageUrl" class="absolute inset-0 w-full h-full object-contain" loading="lazy"/>
                  </div>
                </div>
                <div v-if="(it.originalImageUrls||[]).length" class="space-y-1">
                  <div class="text-[11px] text-neutral-500">{{ t('left.idea_picker.original') }}</div>
                  <div class="grid grid-cols-3 sm:grid-cols-4 gap-1">
                    <div v-for="(url,idx) in it.originalImageUrls" :key="idx"
                         class="square-box rounded border border-neutral-200 dark:border-neutral-800 bg-neutral-50 dark:bg-neutral-900/40 cursor-pointer"
                         @click="onThumbClick(url, idx)">
                      <img :src="url" class="absolute inset-0 w-full h-full object-contain" loading="lazy"/>
                    </div>
                  </div>
                </div>
              </div>
              <div class="text-[10px] flex flex-wrap gap-0.5">
                <span v-if="it.tipsNeedImage" class="px-1 py-0 rounded bg-blue-50 text-blue-700 border border-blue-200">IMG</span>
                <span v-if="it.tipsNeedProModel"
                      class="px-1 py-0 rounded bg-violet-50 text-violet-700 border border-violet-200">PRO</span>
                <span v-if="it.tipsNeedEditPrompt"
                      class="px-1 py-0 rounded bg-amber-50 text-amber-700 border border-amber-200">EDIT</span>
              </div>

            </div>
          </div>
        </div>
        <div v-if="pickerTemplates.length===0" class="p-6 text-center text-neutral-500">{{
            t('left.idea_picker.empty')
          }}
        </div>
      </div>
    </n-modal>
    <ImagePreviewer v-model:modelValue="previewShow" :src="previewSrc"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import {Icon} from '@iconify/vue'
import {useSettingsStore} from '@/stores/settings'
import {useUserStore} from '@/stores/user'
import {getEstimateParams, getPreviewUrl} from '@/api'
import {storeToRefs} from 'pinia'
import ImagePreviewer from '@/components/ImagePreviewer.vue'
import {
  darkTheme,
  NButton,
  NCollapse,
  NCollapseItem,
  NConfigProvider,
  NInput,
  NSelect,
  NModal,
  NTag,
  NRadioButton,
  NRadioGroup,
  NSlider,
  NTooltip,
  NUpload,
  NUploadDragger,
  useDialog,
  useMessage,
  useThemeVars
} from 'naive-ui'
import {
  listIdeaCategories,
  listIdeaTags,
  listIdeaTemplates,
  markIdeaTemplateUsed,
  IdeaTemplate,
  getIdeaImageUrl
} from '@/api/ideas'

const emit = defineEmits<{
  (e: 'generate', payload: { prompt: string; systemPrompt?: string; files: File[] }): void
}>()
const p = defineProps<{ generating?: boolean }>()
const generating = computed(() => p.generating)
const message = useMessage()
const dialog = useDialog()
const {t} = useI18n()

const settings = useSettingsStore()
const user = useUserStore()
const prompt = ref('')
const promptEmpty = computed(() => prompt.value.trim().length === 0)
const systemPrompt = computed({
  get: () => settings.systemPrompt,
  set: (v: string) => settings.setSystemPrompt(v)
})
const files = ref<File[]>([])
const uploadList = ref<any[]>([])
const {basicExpandedNames} = storeToRefs(settings)

onMounted(() => {
  try {
    const legacy = localStorage.getItem('coralsum.systemPrompt') || ''
    if (!settings.systemPrompt && legacy) {
      settings.setSystemPrompt(legacy)
      localStorage.removeItem('coralsum.systemPrompt')
    } else {
      settings.setSystemPrompt(settings.systemPrompt)
    }
  } catch {
  }
})
const confirmReset = ref(false)
const promptGuideShow = ref(false)
const ideaPickerShow = ref(false)
const selectedIndex = ref<number | null>(null)
const previewShow = ref(false)
const previewSrc = ref('')
const allowedTypes = new Set(['image/png', 'image/jpeg'])
const MAX_FILES = 10
const MAX_TOTAL_SIZE = 50 * 1024 * 1024

function onUploadChange(list: any) {
  if (generating.value) return
  const fl = list?.fileList ?? []
  const filtered: any[] = []
  let totalSize = 0
  for (const it of fl) {
    if (!it.url && it.file) it.url = URL.createObjectURL(it.file)
    const t = it?.file?.file?.type || it?.file?.type || it?.type
    if (allowedTypes.has(t)) {
      filtered.push(it)
      const f = it?.file?.file || it?.file
      if (f && typeof f.size === 'number') totalSize += f.size
    }
  }
  if (filtered.length > MAX_FILES) {
    dialog.error({title: t('upload.error.title'), content: t('upload.error.max_files')})
    return
  }
  if (totalSize > MAX_TOTAL_SIZE) {
    dialog.error({title: t('upload.error.title'), content: t('upload.error.max_total')})
    return
  }
  if (filtered.length < fl.length) message.error(t('left.image_input.only_png_jpg'))
  uploadList.value = filtered
  files.value = filtered.map((f: any) => f?.file?.file || f?.file).filter(Boolean)
}

function onReset() {
  confirmReset.value = true
}

function onGenerate() {
  const now = Date.now()
  if (generating.value) return
  if (now - lastClickAt.value < 400) return
  lastClickAt.value = now
  if (promptEmpty.value) {
    message.error(t('left.prompt.fill'))
    return
  }
  if (prompt.value.length > 2000) {
    message.error(t('left.prompt.maxlen', {count: 2000}))
    return
  }
  if (!user.profileReady) {
    message.error(t('messages.please_login'))
    user.requireLogin()
    return
  }
  const pts = Number(user.points || 0)
  if (pts <= 0) {
    message.error(t('points.insufficient'))
    return
  }
  const need = estimatedPoints.value == null ? 0 : Number(estimatedPoints.value || 0)
  if (need > 0 && pts < need) {
    message.error(`${t('points.insufficient')}，${t('left.estimate.tip')}：${need}`)
    return
  }
  emit('generate', {prompt: prompt.value, systemPrompt: systemPrompt.value, files: files.value})
}

const lastClickAt = ref(0)

const pricing = ref<any | null>(null)
async function loadPricing() {
  try {
    pricing.value = await getEstimateParams()
  } catch {}
}
loadPricing()

watch(() => user.profileReady, (ready) => {
  if (ready) loadPricing()
})

watch(() => user.token, (t) => {
  if (t && t.length > 0) loadPricing()
})

// 依赖 Pinia 的 setSystemPrompt 行为来维护 basicExpandedNames 与持久化

function onBasicExpandChange(names: Array<string | number>) {
  settings.setBasicExpandedNames(names)
}

const estimatedPoints = computed(() => {
  const p = pricing.value
  if (!p) return null
  const chars = prompt.value.length
  const tokensPerChar = Number(p.flashLiteTokensPerChar || 2.5)
  const inUsdEvalPerM = Number(p.flashLiteInputUsdPerMTokens || 0.1)
  const outUsdEvalPerM = Number(p.flashLiteOutputUsdPerMTokens || 0.4)
  const usdToCny = Number(p.usdToCny || 7.01)
  const coef = Number(p.coefficient || 1.5)
  const isBasic = settings.modelType === 'Basic'
  // 评估阶段：文本 + 输入图片体积转换为令牌
  const inputTextTokens = chars * tokensPerChar
  const totalInputBytes = files.value.reduce((s: number, f: File) => s + (f?.size || 0), 0)
  const mb = totalInputBytes / (1024 * 1024)
  const imageTokens = mb * Number(p.imagePreviewTokensPerMb || 2500)
  const evalInputTokens = inputTextTokens
  const evalOutputTokens = inputTextTokens * 2
  const evalUsd = evalInputTokens / 1_000_000 * inUsdEvalPerM + evalOutputTokens / 1_000_000 * outUsdEvalPerM
  const evalRmb = evalUsd * usdToCny

  // 生成阶段：模型令牌成本（预估与评估一致策略）
  const prevInUsdPerM = isBasic ? Number(p.basicInputUsdPerMTokens || 0.30) : Number(p.proInputUsdPerMTokens || 2.0)
  const previewInputTokens = inputTextTokens + imageTokens
  const previewUsd = previewInputTokens / 1_000_000 * prevInUsdPerM
  const previewRmbTokens = previewUsd * usdToCny

  // 图片单价（按分辨率）
  const perImageUsd = isBasic
      ? Number(p.basicOutputPricePerImage1kUsd ?? 0.039)
      : Number(p.imagePricePerResolutionUsd?.[settings.imageSize] ?? (settings.imageSize === '4K' ? 0.24 : 0.134))
  const images = Number(settings.candidateRadio || 1)
  const imageRmb = perImageUsd * usdToCny * images

  // 流量评估：按分辨率与格式估算图片体积，乘访问倍数与候选数量
  const key = `${settings.outputFormat}:${settings.imageSize}`
  const bytesPerImage = Number(p.estimatedBytesPerImage?.[key] || 0)
  const visitMultiplier = Number(p.trafficVisitMultiplier || 5)
  const totalBytes = bytesPerImage * images * visitMultiplier
  const gb = totalBytes / (1024 * 1024 * 1024)
  const hour = new Date().getHours()
  const startHour = Number(p.ossBusyStartHour || 8)
  const endHour = Number(p.ossBusyEndHour || 24)
  const busy = hour >= startHour && hour < endHour
  const ossRate = Number((busy ? p.ossBusyRmbPerGb : p.ossIdleRmbPerGb) || 0.5)
  const natRate = Number(p.trafficNatRmbPerGb || 1.1)
  const proxyRate = Number(p.trafficProxyRmbPerGb || 1.6)
  const ossRmb = gb * ossRate
  const natRmb = gb * natRate
  const proxyRmb = gb * proxyRate

  let total = evalRmb + previewRmbTokens + imageRmb + ossRmb + natRmb + proxyRmb
  const scaleMap: Record<string, number> = {'1x': 1, '2x': 2, '3x': 3, '4x': 4}
  const scale = scaleMap[settings.resolution] || 1
  if (p.upscaylEnabled && p.upscaylChargeByScale && scale > 1) {
    total = total * scale
  }
  return Math.round(total * coef * Number(p.pointsPerRmb || 100))
})

const badgeHidden = computed(() => estimatedPoints.value === null || promptEmpty.value)

const themeVars = useThemeVars()
const badgeStyle = computed(() => ({
  backgroundColor: themeVars.value.primaryColor,
  color: settings.darkMode ? '#000000' : '#ffffff',
  borderColor: settings.darkMode ? 'rgba(0,0,0,0.3)' : 'rgba(255,255,255,0.4)'
}))

function doReset() {
  settings.reset()
  uploadList.value.forEach((it: any) => {
    if (it?.url?.startsWith('blob:')) URL.revokeObjectURL(it.url)
  })
  uploadList.value = []
  files.value = []
  selectedIndex.value = null
  previewShow.value = false
  previewSrc.value = ''
}

function openPromptGuide() {
  promptGuideShow.value = true
}

function openIdeaPicker() {
  ideaPickerShow.value = true
  if (!ideaPickerLoaded.value) loadIdeaPicker()
}

const pickerCategoryIds = ref<number[] | null>(null)
const pickerTagIds = ref<number[] | null>(null)
const pickerSortBy = ref<'updated' | 'hot'>('updated')
const pickerTemplates = ref<IdeaTemplate[]>([])
const pickerCatOptions = ref<any[]>([])
const pickerTagOptions = ref<any[]>([])
const ideaPickerLoaded = ref(false)
const pickerSortOptions = [
  {label: t('left.idea_picker.sort_updated'), value: 'updated'},
  {label: t('left.idea_picker.sort_hot'), value: 'hot'}
]

async function loadIdeaPicker() {
  try {
    const [cats, tags] = await Promise.all([listIdeaCategories(), listIdeaTags()])
    pickerCatOptions.value = (cats || []).map((c: any) => ({label: c.name, value: c.id}))
    pickerTagOptions.value = (tags || []).map((t: any) => ({label: t.name, value: t.id}))
    const params: any = {sortBy: pickerSortBy.value, order: 'desc'}
    if (pickerCategoryIds.value && pickerCategoryIds.value.length) params.categoryIds = pickerCategoryIds.value
    if (pickerTagIds.value && pickerTagIds.value.length) params.tagIds = pickerTagIds.value
    pickerTemplates.value = await listIdeaTemplates(params)
    await prefetchTemplateImageUrls()
    ideaPickerLoaded.value = true
  } catch {
  }
}

watch([pickerCategoryIds, pickerTagIds, pickerSortBy], () => {
  loadIdeaPicker()
})

function toggleCategory(id: number, checked: boolean) {
  const arr = pickerCategoryIds.value || []
  const set = new Set(arr)
  if (checked) set.add(id)
  else set.delete(id)
  pickerCategoryIds.value = Array.from(set)
}

function toggleTag(id: number, checked: boolean) {
  const arr = pickerTagIds.value || []
  const set = new Set(arr)
  if (checked) set.add(id)
  else set.delete(id)
  pickerTagIds.value = Array.from(set)
}

async function prefetchTemplateImageUrls() {
  const list = pickerTemplates.value || []
  const promises: Promise<void>[] = []
  for (const it of list) {
    if (it.effectImageUri) {
      promises.push((async () => {
        try {
          it.effectImageUrl = await getIdeaImageUrl(it.effectImageUri!)
        } catch {
          it.effectImageUrl = `/api/generative-image?ref=${encodeURIComponent(it.effectImageUri!)}`
        }
      })())
    }
    const raws = it.originalImageUris || []
    if (raws.length) {
      promises.push((async () => {
        const urls: string[] = []
        for (const r of raws) {
          try {
            urls.push(await getIdeaImageUrl(r))
          } catch {
            urls.push(`/api/generative-image?ref=${encodeURIComponent(r)}`)
          }
        }
        it.originalImageUrls = urls
      })())
    }
  }
  if (promises.length) await Promise.all(promises)
}

function parseTime(s?: string): number {
  if (!s) return 0
  const iso = s.includes('T') ? s : s.replace(' ', 'T')
  const t = Date.parse(iso)
  return Number.isNaN(t) ? 0 : t
}

const NEW_DAYS = 7
const HOT_COUNT = 20

function newBadge(it: IdeaTemplate): boolean {
  if (it.isNew === true) return true
  const now = Date.now()
  const ts = parseTime(it.updateTime) || parseTime(it.createTime)
  if (!ts) return false
  const diffDays = (now - ts) / (1000 * 60 * 60 * 24)
  return diffDays <= NEW_DAYS
}

function hotBadge(it: IdeaTemplate): boolean {
  if (it.isHot === true) return true
  const cnt = Number(it.usageCount || 0)
  return cnt >= HOT_COUNT
}

function applyTemplate(it: IdeaTemplate) {
  prompt.value = it.promptText || ''
  ideaPickerShow.value = false
  markIdeaTemplateUsed(it.id).catch(() => {
  })
}

function handlePaste(e: ClipboardEvent) {
  if (generating.value) return
  const items = e.clipboardData?.items || []
  const pastedFiles: File[] = []
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item.kind === 'file') {
      const file = item.getAsFile()
      if (file && allowedTypes.has(file.type)) pastedFiles.push(file)
    }
  }
  if (pastedFiles.length) {
    const nextCount = uploadList.value.length + pastedFiles.length
    if (nextCount > MAX_FILES) {
      dialog.error({title: t('upload.error.title'), content: t('upload.error.max_files')})
      return
    }
    const currentSize = files.value.reduce((s: number, f: File) => s + (f?.size || 0), 0)
    const addSize = pastedFiles.reduce((s: number, f: File) => s + (f?.size || 0), 0)
    if (currentSize + addSize > MAX_TOTAL_SIZE) {
      dialog.error({title: t('upload.error.title'), content: t('upload.error.max_total')})
      return
    }
    for (const file of pastedFiles) {
      uploadList.value.push({name: file.name, status: 'finished', url: URL.createObjectURL(file)})
      files.value.push(file)
    }
  } else {
    message.error(t('left.image_input.only_png_jpg'))
  }
}

function onThumbClick(url: string, i: number) {
  selectedIndex.value = i
  previewSrc.value = url
  previewShow.value = true
}

function beforeUpload(data: any) {
  const f = data?.file?.file || data?.file
  const t = f?.type
  const okType = allowedTypes.has(t)
  if (!okType) {
    message.error(t('left.image_input.only_png_jpg'))
    return false
  }
  if (uploadList.value.length >= MAX_FILES) {
    dialog.error({title: t('upload.error.title'), content: t('upload.error.max_files')})
    return false
  }
  const currentSize = files.value.reduce((s: number, file: File) => s + (file?.size || 0), 0)
  if (currentSize + (f?.size || 0) > MAX_TOTAL_SIZE) {
    dialog.error({title: t('upload.error.title'), content: t('upload.error.max_total')})
    return false
  }
  return true
}

function clearPrompt() {
  prompt.value = ''
}

function clearSystemPrompt() {
  systemPrompt.value = ''
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

const advancedExpandedNames = computed(() => (settings.advancedExpanded ? ['advanced'] : []))

function onAdvancedExpandChange(names: Array<string | number>) {
  settings.advancedExpanded = Array.isArray(names) && names.includes('advanced')
}

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


.badge-fan {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(24, 160, 88, 0.5);
  clip-path: polygon(100% 0, 0 0, 100% 100%);
}

:deep(.dark) .badge-fan {
  background-color: rgba(42, 148, 125, 0.45)
}

.badge-num {
  position: absolute;
  top: 2px;
  right: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.4)
}

:deep(.dark) .badge-num {
  color: #ffffff
}

.scale-enter-active,
.scale-leave-active {
  transition: transform 200ms ease-out, opacity 200ms ease-out
}

.scale-enter-from,
.scale-leave-to {
  transform: scale(0.6);
  opacity: 0
}

.scale-enter-to,
.scale-leave-from {
  transform: scale(1);
  opacity: 1
}

.bubble-enter-active,
.bubble-leave-active {
  transition: transform 160ms cubic-bezier(0.22, 1.0, 0.36, 1.0), opacity 160ms ease-out
}

.bubble-enter-from,
.bubble-leave-to {
  transform: scale(0.7);
  opacity: 0
}

.bubble-enter-to,
.bubble-leave-from {
  transform: scale(1);
  opacity: 1
}
</style>
<style scoped>
.filter-tags :deep(.n-tag) {
  font-size: 11px;
  line-height: 18px;
  height: 20px;
  padding: 0 6px;
}

.chips-sm :deep(.n-tag) {
  font-size: 11px;
  line-height: 18px;
  height: 20px;
  padding: 0 6px;
}

.apply-btn :deep(.n-button__content) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
}

.apply-btn {
  padding: 0 8px;
  height: 22px;
}

.square-box {
  position: relative;
  width: 100%;
  padding-top: 100%;
}
</style>
