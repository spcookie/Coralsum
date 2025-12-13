Å
<template>
  <div class="flex-1 overflow-y-auto p-4 space-y-4">
    <n-card :title="t('ideas.categories.title')">
      <div class="flex items-center gap-2 mb-2 flex-wrap">
        <n-input v-model:value="catName" :placeholder="t('ideas.categories.placeholder_name')"
                 style="max-width: 220px"/>
        <n-input v-model:value="catDesc" :placeholder="t('ideas.categories.placeholder_desc')"
                 style="max-width: 320px"/>
        <n-tree-select v-model:value="catParentId" :options="catTreeOptions"
                       :placeholder="t('ideas.categories.placeholder_parent')" clearable style="width: 220px"/>
        <n-button type="primary" @click="addCategory">{{ t('ideas.actions.add') }}</n-button>
        <n-button :disabled="!catSelectedId" @click="saveCategory">{{ t('ideas.actions.save') }}</n-button>
        <n-button @click="reloadCategories">{{ t('ideas.actions.refresh') }}</n-button>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <n-data-table :columns="catColumns" :data="viewCategories" :row-key="r=>r.id">
            <template #empty>
              <div class="py-8 text-center text-neutral-500">{{ t('common.empty') }}</div>
            </template>
          </n-data-table>
          <div class="mt-2 flex justify-end">
            <n-pagination :item-count="catTotal" :page="catPage" :page-size="catSize" @update:page="onCatPage"
                          @update:page-size="onCatSize"/>
          </div>
        </div>
        <div>
          <n-card :title="t('ideas.categories.tree_title')" size="small">
            <n-tree :data="catTree" :default-expanded-keys="catExpandedKeys" key-field="id" label-field="name"/>
          </n-card>
        </div>
      </div>
    </n-card>

    <n-card :title="t('ideas.tags.title')">
      <div class="flex items-center gap-2 mb-2">
        <n-input v-model:value="tagName" :placeholder="t('ideas.tags.placeholder_name')" style="max-width: 220px"/>
        <n-button type="primary" @click="addTag">{{ t('ideas.actions.add') }}</n-button>
        <n-button @click="loadTags">{{ t('ideas.actions.refresh') }}</n-button>
      </div>
      <n-data-table :columns="tagColumns" :data="viewTags" :row-key="r=>r.id">
        <template #empty>
          <div class="py-8 text-center text-neutral-500">{{ t('common.empty') }}</div>
        </template>
      </n-data-table>
      <div class="mt-2 flex justify-end">
        <n-pagination :item-count="tagTotal" :page="tagPage" :page-size="tagSize" @update:page="onTagPage"
                      @update:page-size="onTagSize"/>
      </div>
    </n-card>

    <n-card :title="t('ideas.templates.title')">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-2 mb-2">
        <div class="flex items-center gap-2 flex-wrap">
          <n-tree-select v-model:value="filterCategoryIds" :options="catTreeOptions"
                         :placeholder="t('ideas.templates.filter_category')" clearable multiple style="width: 220px"/>
          <n-select v-model:value="filterTagIds" :options="tagOptions" :placeholder="t('ideas.templates.filter_tags')"
                    clearable multiple style="width: 180px"/>
          <n-select v-model:value="sortBy" :options="sortOptions" style="width: 140px"/>
          <n-button @click="queryTemplates">{{ t('common.query') }}</n-button>
        </div>
        <div class="flex items-center gap-2 flex-wrap justify-end">
          <n-button type="primary" @click="openCreate">{{ t('ideas.templates.create') }}</n-button>
          <n-button @click="openImport">{{ t('ideas.templates.import_json') }}</n-button>
          <n-button @click="resetTemplates">{{ t('ideas.actions.reset') }}</n-button>
          <n-button @click="loadTemplates">{{ t('ideas.actions.refresh') }}</n-button>
        </div>
      </div>
      <n-data-table :columns="tplColumns" :data="viewTemplates" :row-key="r=>r.id">
        <template #empty>
          <div class="py-8 text-center text-neutral-500">{{ t('common.empty') }}</div>
        </template>
      </n-data-table>
      <div class="mt-2 flex justify-end">
        <n-pagination :item-count="tplTotal" :page="tplPage" :page-size="tplSize" @update:page="onTplPage"
                      @update:page-size="onTplSize"/>
      </div>
    </n-card>

    <n-modal v-model:show="showEdit" :style="{ width: 'min(880px, 92vw)' }" :title="form.id ? t('ideas.templates.edit_title') : t('ideas.templates.create_title')"
             preset="card">
      <n-form :label-width="140" label-placement="left">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
          <n-form-item :label="t('ideas.templates.form.name')">
            <n-input v-model:value="form.name" :placeholder="t('ideas.templates.form.name')"/>
          </n-form-item>
          <n-form-item :label="t('ideas.templates.form.category')">
            <n-tree-select v-model:value="form.categoryId" :options="catTreeOptions"
                           :placeholder="t('ideas.templates.form.category_placeholder')" clearable/>
          </n-form-item>
        </div>
        <n-form-item :label="t('ideas.templates.form.description')">
          <n-input v-model:value="form.description" :placeholder="t('ideas.templates.form.description_placeholder')"/>
        </n-form-item>
        <n-form-item :label="t('ideas.templates.form.prompt')">
          <n-input v-model:value="form.promptText" :autosize="{minRows: 6, maxRows: 14}" :placeholder="t('ideas.templates.form.prompt_placeholder')"
                   type="textarea"/>
        </n-form-item>
        <n-form-item :label="t('ideas.templates.form.tips')">
          <div class="segmented grid grid-cols-1 md:grid-cols-3 gap-2">
            <n-button :type="form.tipsNeedImage?'primary':'default'" class="w-full" size="small"
                      @click="form.tipsNeedImage=!form.tipsNeedImage">{{ t('ideas.templates.form.need_image') }}
            </n-button>
            <n-button :type="form.tipsNeedProModel?'primary':'default'" class="w-full" size="small"
                      @click="form.tipsNeedProModel=!form.tipsNeedProModel">{{
                t('ideas.templates.form.need_pro_model')
              }}
            </n-button>
            <n-button :type="form.tipsNeedEditPrompt?'primary':'default'" class="w-full" size="small"
                      @click="form.tipsNeedEditPrompt=!form.tipsNeedEditPrompt">
              {{ t('ideas.templates.form.need_edit_prompt') }}
            </n-button>
          </div>
        </n-form-item>
        <n-form-item :label="t('ideas.templates.form.tags')">
          <n-checkbox-group v-model:value="form.tagIds">
            <div class="flex flex-wrap gap-2">
              <n-checkbox v-for="t0 in tags" :key="t0.id" :value="t0.id">{{ t0.name }}</n-checkbox>
            </div>
          </n-checkbox-group>
        </n-form-item>
        <n-divider/>
        <div class="space-y-2">
          <div class="text-sm">{{ t('ideas.templates.upload_original_title') }}</div>
          <n-upload v-model:file-list="originalFileList" :show-file-list="false" accept="image/png,image/jpeg,image/webp" directory-dnd
                    multiple @update:file-list="onUpdateOriginalFiles">
            <n-button>{{ t('ideas.templates.upload_select') }}</n-button>
          </n-upload>
          <div class="text-xs text-neutral-500">{{
              t('ideas.templates.upload_original_hint', {count: originalCount})
            }}
          </div>
          <div class="grid grid-cols-3 sm:grid-cols-4 gap-2">
            <div v-for="(u, idx) in originalImages" :key="idx"
                 class="square-box relative group rounded border border-neutral-200 dark:border-neutral-700 bg-neutral-50 dark:bg-neutral-900/40">
              <img v-if="originalShareLinks[idx]" :src="originalShareLinks[idx]" class="absolute inset-0 w-full h-full object-contain cursor-zoom-in"
                   loading="lazy" @click="openPreviewRef(u, idx)"
                   @error="onOriginalError(idx)"/>
              <n-button class="img-action absolute top-1 right-1 opacity-0 group-hover:opacity-100" quaternary
                        size="tiny"
                        @click.stop="removeOriginal(u, idx)">{{ t('ideas.actions.delete') }}
              </n-button>
            </div>
          </div>
          <div v-if="originalPendingUrls.length" class="grid grid-cols-3 sm:grid-cols-4 gap-2 mt-1">
            <div v-for="(u, idx) in originalPendingUrls" :key="`p-${idx}`"
                 class="square-box relative group rounded border border-neutral-200 dark:border-neutral-700 bg-neutral-50 dark:bg-neutral-900/40">
              <img :src="u" class="absolute inset-0 w-full h-full object-contain" loading="lazy"/>
              <n-button class="img-action absolute top-1 right-1 opacity-0 group-hover:opacity-100" quaternary
                        size="tiny"
                        @click.stop="removePendingOriginal(idx)">{{ t('ideas.actions.delete') }}
              </n-button>
            </div>
          </div>
        </div>
        <div class="space-y-2">
          <div class="text-sm">{{ t('ideas.templates.upload_effect_title') }}</div>
          <n-upload v-model:file-list="effectFileList" :max="1" :show-file-list="false"
                    accept="image/png,image/jpeg,image/webp" @update:file-list="onUpdateEffectFiles">
            <n-button>{{ t('ideas.templates.upload_select') }}</n-button>
          </n-upload>
          <div class="text-xs text-neutral-500">{{ effectLabel }}</div>
          <div v-if="effectShareLink || effectPendingUrl"
               class="mt-1 relative group rounded border border-neutral-200 dark:border-neutral-700 bg-neutral-50 dark:bg-neutral-900/40 w-[220px] h-[220px] mx-auto md:mx-0">
            <img v-if="effectShareLink" :src="effectShareLink" class="absolute inset-0 w-full h-full object-contain cursor-zoom-in"
                 loading="lazy"
                 @click="openPreviewRef(form.effectImageRef)" @error="onEffectError"/>
            <img v-else-if="effectPendingUrl" :src="effectPendingUrl" class="absolute inset-0 w-full h-full object-contain"
                 loading="lazy"/>
            <n-button v-if="form.effectImageRef" class="img-action absolute top-1 right-1 opacity-0 group-hover:opacity-100" quaternary
                      size="tiny"
                      @click.stop="removeEffect()">{{ t('ideas.actions.delete') }}
            </n-button>
            <n-button v-else-if="effectPendingUrl" class="img-action absolute top-1 right-1 opacity-0 group-hover:opacity-100" quaternary
                      size="tiny"
                      @click.stop="clearEffectPending()">{{ t('ideas.actions.delete') }}
            </n-button>
          </div>
          <div v-if="form.effectImageRef && !effectShareLink" class="mt-1">
            <n-button class="img-action" quaternary size="tiny" @click="removeEffect()">{{
                t('ideas.actions.delete')
              }}
            </n-button>
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-2">
          <n-button @click="showEdit=false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" @click="saveTemplate">{{ t('ideas.actions.save') }}</n-button>
        </div>
      </n-form>
    </n-modal>

    <n-modal v-model:show="showImport" :style="{ width: 'min(680px, 92vw)' }" :title="t('ideas.templates.import_modal.title')"
             preset="card">
      <div class="space-y-3">
        <div class="text-xs text-neutral-600">{{ t('ideas.templates.import_modal.desc') }}</div>
        <n-input v-model:value="importText" :autosize="{minRows: 10, maxRows: 20}" :placeholder="t('ideas.templates.import_modal.placeholder')"
                 type="textarea"/>
        <div class="flex justify-end gap-2">
          <n-button @click="showImport=false">{{ t('ideas.templates.import_modal.cancel') }}</n-button>
          <n-button type="primary" @click="doImport">{{ t('ideas.templates.import_modal.confirm') }}</n-button>
        </div>
      </div>
    </n-modal>
    <ImagePreviewer v-model:modelValue="previewShow" :src="previewSrc"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, h, onMounted, ref, watch} from 'vue'
import {useUserStore} from '@/stores/user'
import {useRouter} from 'vue-router'
import {
  NButton,
  NCard,
  NCheckbox,
  NCheckboxGroup,
  NDataTable,
  NInput,
  NModal,
  NPagination,
  NPopconfirm,
  NSelect,
  NTooltip,
  NForm,
  NFormItem,
  NDivider,
  NUpload,
  NTree,
  NInputNumber,
  NTreeSelect,
  useMessage
} from 'naive-ui'
import {useI18n} from 'vue-i18n'
import {getIdeaImageUrl} from '@/api/ideas'
import {
  createIdeaCategory,
  createIdeaTag,
  createIdeaTemplate,
  deleteIdeaCategory,
  deleteIdeaTemplate,
  importIdeaTemplates,
  listIdeaCategories,
  listIdeaCategoryTree,
  listIdeaTags,
  listIdeaTemplates,
  updateIdeaCategory,
  updateIdeaTag,
  updateIdeaTemplate,
  uploadIdeaEffectImage,
  uploadIdeaOriginalImage
} from '@/api/ideas'
import {deleteIdeaOriginalImage, deleteIdeaEffectImage} from '@/api/ideas'
import ImagePreviewer from '@/components/ImagePreviewer.vue'

const message = useMessage()
const {t} = useI18n()
const user = useUserStore()
const router = useRouter()
onMounted(() => {
  try {
    const hasCtl = (user.permissions || []).includes('CTL')
    if (!hasCtl) {
      message.error('无权限访问')
      router.push('/')
      return
    }
  } catch {
  }
  reloadCategories();
  loadTags();
  loadTemplates()
})
const tab = ref<'categories' | 'tags' | 'templates'>('templates')

// 分类
const catName = ref('')
const catDesc = ref('')
const catParentId = ref<number | null>(null)
const catSelectedId = ref<number | null>(null)
const categories = ref<any[]>([])
const catTree = ref<any[]>([])
const catExpandedKeys = ref<any[]>([])
const catPage = ref(1)
const catSize = ref(10)
const catTotal = computed(() => categories.value.length)
const viewCategories = computed(() => {
  const start = (catPage.value - 1) * catSize.value
  const end = start + catSize.value
  return categories.value.slice(start, end)
})

function onCatPage(p: number) {
  catPage.value = p
}

function onCatSize(s: number) {
  catSize.value = s;
  catPage.value = 1
}

const catColumns = [
  {title: t('ideas.columns.id'), key: 'id', width: 80},
  {title: t('ideas.columns.name'), key: 'name'},
  {title: t('ideas.columns.description'), key: 'description'},
  {
    title: t('ideas.columns.parent'), key: 'parentId', render(row: any) {
      const pid = Number(row.parentId ?? 0)
      const name = (categories.value || []).find((c: any) => Number(c.id) === pid)?.name || ''
      return h('div', {}, name)
    }
  },
  {
    title: t('ideas.columns.action'), key: 'action', width: 180, render(row: any) {
      const editBtn = h(NButton, {
        size: 'small',
        tertiary: true,
        onClick: () => editCategory(row)
      }, {default: () => t('ideas.actions.edit')})
      const delBtn = h(NPopconfirm, {onPositiveClick: () => removeCategory(row)}, {
        default: () => t('ideas.categories.confirm_delete'),
        trigger: () => h(NButton, {
          size: 'small',
          quaternary: true,
          class: 'text-red-600'
        }, {default: () => t('ideas.actions.delete')})
      })
      return h('div', {class: 'flex items-center gap-2'}, [editBtn, delBtn])
    }
  }
]

async function loadCategories() {
  try {
    categories.value = await listIdeaCategories()
  } catch {
  }
}

async function loadCategoryTree() {
  try {
    catTree.value = await listIdeaCategoryTree()
    catExpandedKeys.value = (catTree.value || []).map((n: any) => n.id)
  } catch {
  }
}

function reloadCategories() {
  loadCategories();
  loadCategoryTree()
}

async function addCategory() {
  if (!catName.value.trim()) return message.error(t('ideas.categories.input_name'))
  try {
    const parent = catParentId.value
    await createIdeaCategory(
        catName.value.trim(),
        catDesc.value.trim() || undefined,
        parent == null ? undefined : Number(parent)
    )
    catSelectedId.value = null
    catName.value = ''
    catDesc.value = ''
    catParentId.value = null

    message.success(t('ideas.msg.add_success'))
    reloadCategories()
  } catch (e: any) {
    message.error(e?.message || t('ideas.msg.add_failed'))
  }
}

function editCategory(row: any) {
  catSelectedId.value = Number(row.id)
  catName.value = row.name || ''
  catDesc.value = row.description || ''
  catParentId.value = row.parentId ?? null

}

async function saveCategory() {
  if (!catSelectedId.value) return
  try {
    const parent = catParentId.value
    const normalizedParent = parent == null ? undefined : Number(parent)
    const finalParent = normalizedParent === catSelectedId.value ? undefined : normalizedParent
    await updateIdeaCategory(
        catSelectedId.value,
        catName.value.trim(),
        catDesc.value.trim() || undefined,
        finalParent
    )
    catSelectedId.value = null
    catName.value = ''
    catDesc.value = ''
    catParentId.value = null

    message.success(t('ideas.msg.save_success'))
    reloadCategories()
  } catch (e: any) {
    message.error(e?.message || t('ideas.msg.save_failed'))
  }
}

async function removeCategory(row: any) {
  try {
    await deleteIdeaCategory(row.id);
    message.success(t('ideas.msg.delete_success'));
    reloadCategories()
  } catch (e: any) {
    message.error(e?.message || t('ideas.msg.delete_failed'))
  }
}

// 标签
const tagName = ref('')
const tags = ref<any[]>([])
const tagPage = ref(1)
const tagSize = ref(10)
const tagTotal = computed(() => tags.value.length)
const viewTags = computed(() => {
  const start = (tagPage.value - 1) * tagSize.value
  const end = start + tagSize.value
  return tags.value.slice(start, end)
})

function onTagPage(p: number) {
  tagPage.value = p
}

function onTagSize(s: number) {
  tagSize.value = s;
  tagPage.value = 1
}

const tagColumns = [
  {title: t('ideas.columns.id'), key: 'id', width: 80},
  {title: t('ideas.columns.name'), key: 'name'},
  {
    title: t('ideas.columns.action'), key: 'action', width: 160, render(row: any) {
      const editBtn = h(NButton, {
        size: 'small',
        tertiary: true,
        onClick: () => editTag(row)
      }, {default: () => t('ideas.actions.edit')})
      const delBtn = h(NPopconfirm, {onPositiveClick: () => removeTag(row)}, {
        default: () => t('ideas.tags.confirm_delete'),
        trigger: () => h(NButton, {
          size: 'small',
          quaternary: true,
          class: 'text-red-600'
        }, {default: () => t('ideas.actions.delete')})
      })
      return h('div', {class: 'flex items-center gap-2'}, [editBtn, delBtn])
    }
  }
]

async function loadTags() {
  try {
    tags.value = await listIdeaTags()
  } catch {
  }
}

async function addTag() {
  if (!tagName.value.trim()) return message.error(t('ideas.tags.input_name'));
  try {
    await createIdeaTag(tagName.value.trim());
    tagName.value = '';
    message.success(t('ideas.msg.add_success'));
    loadTags()
  } catch (e: any) {
    message.error(e?.message || t('ideas.msg.add_failed'))
  }
}

function editTag(row: any) {
  tagName.value = row.name || ''
}

async function removeTag(row: any) {
  try {
    await deleteIdeaTag(row.id);
    message.success(t('ideas.msg.delete_success'));
    loadTags()
  } catch (e: any) {
    message.error(e?.message || t('ideas.msg.delete_failed'))
  }
}

// 模版
const templates = ref<any[]>([])

function mapTplRow(r: any) {
  return {
    id: Number(r.id),
    name: r.name,
    description: r.description,
    promptText: r.prompt_text ?? r.promptText,
    categoryId: r.category_id ?? r.categoryId,
    originalImageRefs: (r.original_image_refs ?? r.originalImageRefs ?? '[]'),
    effectImageRef: (r.effect_image_ref ?? r.effectImageRef ?? ''),
    tagIds: Array.isArray(r.tag_ids) ? (r.tag_ids as any[]).map((x) => Number(x)) : ((r.tagIds || []) as any[]).map((x) => Number(x)),
    tagNames: Array.isArray(r.tag_names) ? r.tag_names : (r.tagNames || []),
    tipsNeedImage: !!(r.tips_need_image ?? r.tipsNeedImage),
    tipsNeedProModel: !!(r.tips_need_pro_model ?? r.tipsNeedProModel),
    tipsNeedEditPrompt: !!(r.tips_need_edit_prompt ?? r.tipsNeedEditPrompt),
    usageCount: Number(r.usage_count ?? r.usageCount ?? 0),
    lastUsedTime: r.last_used_time ?? r.lastUsedTime,
    updateTime: r.update_time ?? r.updateTime
  }
}

const tplColumns = [
  {title: t('ideas.columns.id'), key: 'id', width: 80},
  {
    title: t('ideas.columns.name'), key: 'name', width: 220, render(row: any) {
      const trigger = h('div', {class: 'truncate max-w-[220px]'}, row.name)
      return h(NTooltip, {placement: 'top'}, {default: () => String(row.name || ''), trigger: () => trigger})
    }
  },
  {
    title: t('ideas.columns.category'), key: 'categoryId', width: 140, render(row: any) {
      const name = catNameMap.value[Number(row.categoryId)] || String(row.categoryId ?? '')
      return h('div', {}, name)
    }
  },
  {
    title: t('ideas.columns.tags'), key: 'tagIds', width: 220, render(row: any) {
      const byNames: string[] = Array.isArray(row.tagNames) ? row.tagNames : []
      const byIds: number[] = Array.isArray(row.tagIds) ? row.tagIds : []
      const names = byNames.length ? byNames : byIds.map((id) => tagNameMap.value[Number(id)]).filter((x) => !!x)
      return h('div', {class: 'flex items-center gap-1 flex-wrap max-w-[220px]'}, names.map((n) => h('span', {class: 'px-1 py-0.5 rounded bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 border border-neutral-200 dark:border-neutral-700 text-[11px]'}, n)))
    }
  },
  {
    title: t('ideas.columns.prompt'), key: 'promptText', width: 300, render(row: any) {
      const trigger = h('div', {class: 'truncate max-w-[300px]'}, row.promptText)
      return h(NTooltip, {placement: 'top'}, {default: () => String(row.promptText || ''), trigger: () => trigger})
    }
  },
  {
    title: t('ideas.columns.description'), key: 'description', width: 300, render(row: any) {
      const trigger = h('div', {class: 'truncate max-w-[300px]'}, row.description)
      return h(NTooltip, {placement: 'top'}, {default: () => String(row.description || ''), trigger: () => trigger})
    }
  },
  {
    title: t('ideas.columns.tips'), key: 'tips', width: 160, render(row: any) {
      const items: string[] = []
      if (row.tipsNeedImage) items.push(t('left.idea_picker.badge_need_image'))
      if (row.tipsNeedProModel) items.push(t('left.idea_picker.badge_need_pro'))
      if (row.tipsNeedEditPrompt) items.push(t('left.idea_picker.badge_need_edit'))
      return h('div', {class: 'flex items-center gap-1'}, items.map((txt) => h('span', {class: 'px-1 py-0.5 rounded bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 border border-neutral-200 dark:border-neutral-700 text-[11px]'}, txt)))
    }
  },
  {title: t('ideas.columns.usage_count'), key: 'usageCount', width: 100},
  {
    title: t('ideas.columns.last_used'), key: 'lastUsedTime', width: 160, render(row: any) {
      return formatDateTime(row.lastUsedTime)
    }
  },
  {
    title: t('ideas.columns.updated'), key: 'updateTime', width: 160, render(row: any) {
      return formatDateTime(row.updateTime)
    }
  },
  {
    title: t('ideas.columns.action'), key: 'action', width: 200, render(row: any) {
      const editBtn = h(NButton, {
        size: 'small',
        tertiary: true,
        onClick: () => editTemplate(row)
      }, {default: () => t('ideas.actions.edit')})
      const delBtn = h(NPopconfirm, {onPositiveClick: () => removeTemplate(row)}, {
        default: () => t('ideas.templates.confirm_delete'),
        trigger: () => h(NButton, {
          size: 'small',
          quaternary: true,
          class: 'text-red-600'
        }, {default: () => t('ideas.actions.delete')})
      })
      return h('div', {class: 'flex items-center gap-2 whitespace-nowrap'}, [editBtn, delBtn])
    }
  }
]
const filterCategoryIds = ref<number[] | null>(null)
const filterTagIds = ref<number[] | null>(null)
const sortBy = ref<'updated' | 'hot'>('updated')
const sortOptions = computed(() => [
  {label: t('ideas.templates.sort.updated'), value: 'updated'},
  {label: t('ideas.templates.sort.hot'), value: 'hot'}
])
const catOptions = computed(() => (categories.value || []).map((c: any) => ({label: c.name, value: c.id})))

function toTreeOptions(nodes: any[]): any[] {
  return (nodes || []).map((n: any) => ({
    label: n.name,
    key: n.id,
    children: toTreeOptions(n.children || [])
  }))
}

const catTreeOptions = computed(() => toTreeOptions(catTree.value || []))
const tagOptions = computed(() => (tags.value || []).map((t: any) => ({label: t.name, value: t.id})))
const catNameMap = computed(() => {
  const m: Record<number, string> = {}
  try {
    for (const c of (categories.value || [])) {
      m[Number(c.id)] = String(c.name || '')
    }
  } catch {
  }
  return m
})
const tagNameMap = computed(() => {
  const m: Record<number, string> = {}
  try {
    for (const t of (tags.value || [])) {
      m[Number(t.id)] = String(t.name || '')
    }
  } catch {
  }
  return m
})

async function loadTemplates() {
  const params: any = {sortBy: sortBy.value, order: 'desc'}
  if (filterCategoryIds.value && filterCategoryIds.value.length) params.categoryIds = filterCategoryIds.value
  if (filterTagIds.value && filterTagIds.value.length) params.tagIds = filterTagIds.value
  try {
    const list = await listIdeaTemplates(params)
    templates.value = list.map(mapTplRow)
  } catch {
  }
}

function queryTemplates() {
  loadTemplates()
}

function resetTemplates() {
  filterCategoryIds.value = null;
  filterTagIds.value = null;
  sortBy.value = 'updated';
  loadTemplates()
}

const tplPage = ref(1)
const tplSize = ref(10)
const tplTotal = computed(() => templates.value.length)
const viewTemplates = computed(() => {
  const start = (tplPage.value - 1) * tplSize.value
  const end = start + tplSize.value
  return templates.value.slice(start, end)
})

function onTplPage(p: number) {
  tplPage.value = p
}

function onTplSize(s: number) {
  tplSize.value = s;
  tplPage.value = 1
}

const showEdit = ref(false)
const form = ref<any>({
  id: null,
  name: '',
  description: '',
  promptText: '',
  categoryId: null,
  tipsNeedImage: false,
  tipsNeedProModel: false,
  tipsNeedEditPrompt: false,
  tagIds: [],
  originalImageRefs: '[]',
  effectImageRef: ''
})
const originalPendingFiles = ref<File[]>([])
const originalPendingUrls = ref<string[]>([])
const effectPendingFile = ref<File | null>(null)
const effectPendingUrl = ref<string>('')
const originalCount = computed(() => {
  try {
    const arr = JSON.parse(form.value.originalImageRefs || '[]')
    const exist = Array.isArray(arr) ? arr.length : 0
    const pending = Array.isArray(originalPendingFiles.value) ? originalPendingFiles.value.length : 0
    return exist + pending
  } catch {
    return Array.isArray(originalPendingFiles.value) ? originalPendingFiles.value.length : 0
  }
})
const effectLabel = computed(() => form.value.effectImageRef ? t('ideas.templates.uploaded') : t('ideas.templates.not_uploaded'))
const originalImages = computed<string[]>(() => {
  try {
    const arr = JSON.parse(form.value.originalImageRefs || '[]')
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})
const previewShow = ref(false)
const previewSrc = ref('')

function buildPreviewUrl(refPath: string): string {
  try {
    return new URL(`/api/generative-image?ref=${encodeURIComponent(refPath)}`, window.location.origin).toString()
  } catch {
    return `/api/generative-image?ref=${encodeURIComponent(refPath)}`
  }
}

const originalShareLinks = ref<Record<number, string>>({})
const effectShareLink = ref<string>('')

function openPreviewRef(refPath: string, idx?: number) {
  const link = typeof idx === 'number' ? (originalShareLinks.value[idx] || '') : (effectShareLink.value || '')
  previewSrc.value = link || ''
  previewShow.value = true
}

async function refreshOriginalShareLinks() {
  const map: Record<number, string> = {}
  for (let i = 0; i < originalImages.value.length; i++) {
    const refPath = originalImages.value[i]
    try {
      const link = await getIdeaImageUrl(refPath)
      if (link) map[i] = link
    } catch {
    }
  }
  originalShareLinks.value = map
}

async function refreshEffectShareLink() {
  const refPath = form.value.effectImageRef
  if (!refPath) {
    effectShareLink.value = '';
    return
  }
  try {
    effectShareLink.value = await getIdeaImageUrl(refPath)
  } catch {
    effectShareLink.value = ''
  }
}

watch(() => showEdit.value, (v) => {
  if (v) {
    refreshOriginalShareLinks();
    refreshEffectShareLink()
  }
})

function openCreate() {
  form.value = {
    id: null,
    name: '',
    description: '',
    promptText: '',
    categoryId: null,
    tipsNeedImage: false,
    tipsNeedProModel: false,
    tipsNeedEditPrompt: false,
    tagIds: [],
    originalImageRefs: '[]',
    effectImageRef: ''
  }
  originalShareLinks.value = {}
  effectShareLink.value = ''
  showEdit.value = true
}

function editTemplate(row: any) {
  form.value = {...row, tagIds: (row.tagIds || [])};
  showEdit.value = true
}

async function removeTemplate(row: any) {
  try {
    await deleteIdeaTemplate(row.id);
    message.success(t('ideas.msg.delete_success'));
    loadTemplates()
  } catch (e: any) {
    message.error(e?.message || t('ideas.msg.delete_failed'))
  }
}

async function saveTemplate() {
  try {
    const payload = {
      name: form.value.name,
      description: form.value.description || undefined,
      promptText: form.value.promptText,
      categoryId: form.value.categoryId || undefined,
      tipsNeedImage: !!form.value.tipsNeedImage,
      tipsNeedProModel: !!form.value.tipsNeedProModel,
      tipsNeedEditPrompt: !!form.value.tipsNeedEditPrompt,
      tagIds: form.value.tagIds || []
    }
    if (form.value.id) await updateIdeaTemplate(form.value.id, payload)
    else {
      const res = await createIdeaTemplate(payload)
      form.value.id = res?.id
    }
    if (form.value.id) {
      for (const f of originalPendingFiles.value) {
        try {
          const uri = await uploadIdeaOriginalImage(form.value.id, f)
          const arr = (() => {
            try {
              return JSON.parse(form.value.originalImageRefs || '[]')
            } catch {
              return []
            }
          })()
          arr.push(uri)
          form.value.originalImageRefs = JSON.stringify(arr)
        } catch {
        }
      }
      if (effectPendingFile.value) {
        try {
          const uri = await uploadIdeaEffectImage(form.value.id, effectPendingFile.value)
          form.value.effectImageRef = uri
        } catch {
        }
      }
    }
    originalPendingFiles.value = []
    originalPendingUrls.value.forEach(u => {
      try {
        if (u && u.startsWith('blob:')) URL.revokeObjectURL(u)
      } catch {
      }
    })
    originalPendingUrls.value = []
    if (effectPendingUrl.value && effectPendingUrl.value.startsWith('blob:')) {
      try {
        URL.revokeObjectURL(effectPendingUrl.value)
      } catch {
      }
    }
    effectPendingFile.value = null
    effectPendingUrl.value = ''
    message.success(t('ideas.msg.save_success'))
    showEdit.value = false
    loadTemplates()
  } catch (e: any) {
    message.error(e?.message || t('ideas.msg.save_failed'))
  }
}

const originalFileList = ref<any[]>([])
const effectFileList = ref<any[]>([])

function onUpdateOriginalFiles(files: any[]) {
  const existingLen = (() => {
    try {
      const arr = JSON.parse(form.value.originalImageRefs || '[]');
      return Array.isArray(arr) ? arr.length : 0
    } catch {
      return 0
    }
  })()
  const remaining = Math.max(0, 10 - (existingLen + (originalPendingFiles.value?.length || 0)))
  const pick = (files || []).slice(0, remaining)
  originalPendingFiles.value = []
  originalPendingUrls.value.forEach(u => {
    try {
      if (u && u.startsWith('blob:')) URL.revokeObjectURL(u)
    } catch {
    }
  })
  originalPendingUrls.value = []
  for (const f of pick) {
    const raw: File | undefined = f?.file
    if (!raw) continue
    if (raw.size > 1_000_000) {
      message.error(t('ideas.templates.error_file_too_large'));
      continue
    }
    originalPendingFiles.value.push(raw)
    try {
      originalPendingUrls.value.push(URL.createObjectURL(raw))
    } catch {
    }
  }
}

function onUpdateEffectFiles(files: any[]) {
  const list = files || []
  const f = list[0]
  const raw: File | undefined = f?.file
  // clear previous
  effectPendingFile.value = null
  if (effectPendingUrl.value && effectPendingUrl.value.startsWith('blob:')) {
    try {
      URL.revokeObjectURL(effectPendingUrl.value)
    } catch {
    }
  }
  effectPendingUrl.value = ''
  // control upload file list to avoid max=1 blockage
  if (!raw) {
    effectFileList.value = [];
    return
  }
  if (raw.size > 1_000_000) {
    effectFileList.value = []
    message.error(t('ideas.templates.error_file_too_large'))
    return
  }
  effectFileList.value = [f]
  effectPendingFile.value = raw
  try {
    effectPendingUrl.value = URL.createObjectURL(raw)
  } catch {
  }
}

function clearEffectPending() {
  try {
    if (effectPendingUrl.value && effectPendingUrl.value.startsWith('blob:')) {
      URL.revokeObjectURL(effectPendingUrl.value)
    }
  } catch {
  }
  effectPendingFile.value = null
  effectPendingUrl.value = ''
  effectFileList.value = []
}

async function removeOriginal(key: string, idx: number) {
  try {
    if (!form.value.id) return
    const ok = await deleteIdeaOriginalImage(form.value.id, key)
    if (!ok) return message.error(t('ideas.msg.delete_failed'))
    const arr = (() => {
      try {
        return JSON.parse(form.value.originalImageRefs || '[]')
      } catch {
        return []
      }
    })()
    const next = (arr as any[]).filter((x) => x !== key)
    form.value.originalImageRefs = JSON.stringify(next)
    const old = originalShareLinks.value
    const reindexed: Record<number, string> = {}
    const max = Object.keys(old).length
    for (let i = 0; i < max; i++) {
      if (i === idx) continue
      const target = i > idx ? i - 1 : i
      const link = (old as any)[i]
      if (link) reindexed[target] = link
    }
    originalShareLinks.value = reindexed
    try {
      const payload = {
        name: form.value.name,
        description: form.value.description || undefined,
        promptText: form.value.promptText,
        categoryId: form.value.categoryId || undefined,
        tipsNeedImage: !!form.value.tipsNeedImage,
        tipsNeedProModel: !!form.value.tipsNeedProModel,
        tipsNeedEditPrompt: !!form.value.tipsNeedEditPrompt,
        tagIds: form.value.tagIds || []
      }
      await updateIdeaTemplate(form.value.id, payload)
    } catch {
    }
    message.success(t('ideas.msg.delete_success'))
  } catch (e: any) {
    message.error(e?.message || t('ideas.msg.delete_failed'))
  }
}

async function removeEffect() {
  try {
    if (!form.value.id) return
    const ok = await deleteIdeaEffectImage(form.value.id)
    if (!ok) return message.error(t('ideas.msg.delete_failed'))
    form.value.effectImageRef = null
    effectShareLink.value = ''
    try {
      const payload = {
        name: form.value.name,
        description: form.value.description || undefined,
        promptText: form.value.promptText,
        categoryId: form.value.categoryId || undefined,
        tipsNeedImage: !!form.value.tipsNeedImage,
        tipsNeedProModel: !!form.value.tipsNeedProModel,
        tipsNeedEditPrompt: !!form.value.tipsNeedEditPrompt,
        tagIds: form.value.tagIds || []
      }
      await updateIdeaTemplate(form.value.id, payload)
    } catch {
    }
    message.success(t('ideas.msg.delete_success'))
  } catch (e: any) {
    message.error(e?.message || t('ideas.msg.delete_failed'))
  }
}

function onOriginalError(idx: number) {
  try {
    const arr = (() => {
      try {
        return JSON.parse(form.value.originalImageRefs || '[]')
      } catch {
        return []
      }
    })()
    if (!Array.isArray(arr)) return
    arr.splice(idx, 1)
    form.value.originalImageRefs = JSON.stringify(arr)
    const old = originalShareLinks.value
    const reindexed: Record<number, string> = {}
    const max = Object.keys(old).length
    for (let i = 0; i < max; i++) {
      if (i === idx) continue
      const target = i > idx ? i - 1 : i
      const link = (old as any)[i]
      if (link) reindexed[target] = link
    }
    originalShareLinks.value = reindexed
  } catch {
  }
}

function onEffectError() {
  try {
    effectShareLink.value = ''
  } catch {
  }
}

function removePendingOriginal(idx: number) {
  try {
    const urls = originalPendingUrls.value
    const files = originalPendingFiles.value
    const u = urls[idx]
    if (u && u.startsWith('blob:')) {
      try {
        URL.revokeObjectURL(u)
      } catch {
      }
    }
    originalPendingUrls.value = urls.filter((_, i) => i !== idx)
    originalPendingFiles.value = files.filter((_, i) => i !== idx)
  } catch {
  }
}

const showImport = ref(false)
const importText = ref('')

function openImport() {
  showImport.value = true
}

async function doImport() {
  try {
    let items: any[]
    try {
      items = JSON.parse(importText.value || '[]')
    } catch {
      return message.error(t('ideas.templates.import_modal.parse_failed'))
    }
    const cnt = await importIdeaTemplates(items)
    message.success(t('ideas.templates.import_modal.success', {count: cnt}))
    showImport.value = false
    importText.value = ''
    loadTemplates()
  } catch (e: any) {
    message.error(e?.message || t('ideas.templates.import_modal.failed'))
  }
}

function newBadge(it: any) {
  try {
    const ct = new Date(it.createTime || it.create_time || Date.now())
    const diffDays = (Date.now() - ct.getTime()) / (1000 * 60 * 60 * 24)
    return diffDays <= 7
  } catch {
    return false
  }
}

function hotBadge(it: any) {
  return Number(it.usageCount ?? it.usage_count ?? 0) >= 5
}

function formatDateTime(input: any): string {
  try {
    if (!input) return ''
    const d = typeof input === 'string' ? new Date(input) : input
    if (!d || Number.isNaN(d.getTime())) return String(input)
    const pad = (n: number) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch {
    return String(input ?? '')
  }
}


</script>

<style scoped>
:root {
}

.square-box {
  position: relative;
  width: 100%;
  padding-top: 100%;
}

:deep(.img-action.n-button) {
  background-color: rgba(0, 0, 0, 0.35) !important;
  color: #fff !important;
  border: none !important;
  box-shadow: none !important;
  backdrop-filter: blur(2px);
}

:deep(.img-action.n-button:hover) {
  background-color: rgba(0, 0, 0, 0.45) !important;
}

:deep(.img-action.n-button .n-button__border) {
  display: none !important;
}
</style>
