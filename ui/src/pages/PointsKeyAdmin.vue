<template>
  <div class="flex-1 overflow-y-auto p-4 space-y-4">
    <n-collapse :default-expanded-names="[]">
      <n-collapse-item name="cfg">
        <template #header>
          <div class="text-[16px] leading-6 font-medium">{{ t('keys.config.title') }}</div>
        </template>
        <n-form ref="formRef" :model="form" :rules="rules" require-mark-placement="right" show-require-mark>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <n-form-item :label="t('keys.config.name')" path="name">
              <n-input v-model:value="form.name" :placeholder="t('keys.config.placeholder_name')" maxlength="20"
                       show-count/>
            </n-form-item>
            <div class="col-span-1 md:col-span-2 flex items-start gap-3">
              <n-form-item :label="t('keys.config.permanent')" class="flex-1" path="permanentPoints">
                <n-input-number v-model:value="form.permanentPoints" :precision="2" min="0"/>
              </n-form-item>
              <n-form-item :label="t('keys.config.subscribe')" class="flex-1" path="subscribePoints">
                <n-input-number v-model:value="form.subscribePoints" :precision="2" min="0"/>
              </n-form-item>
            </div>
            <n-form-item :label="t('keys.config.subscribe_type')" class="max-w-[220px]">
              <n-select v-model:value="form.subscribeType" :options="subscribeOptions" class="w-[200px]"/>
            </n-form-item>
            <n-form-item :label="t('keys.config.period_unit')" class="max-w-[220px]">
              <n-select v-model:value="form.periodUnit" :options="periodUnitOptions" class="w-[200px]"/>
            </n-form-item>
            <n-form-item :label="t('keys.config.period_count')" path="periodCount">
              <n-input-number v-model:value="form.periodCount" :precision="0" min="0"/>
            </n-form-item>
            <n-form-item :label="t('keys.config.gift_period_unit')" class="max-w-[220px]">
              <n-select v-model:value="form.giftPeriodUnit" :options="periodUnitOptions" class="w-[200px]"/>
            </n-form-item>
            <n-form-item :label="t('keys.config.gift_period_count')" path="giftPeriodCount">
              <n-input-number v-model:value="form.giftPeriodCount" :precision="0" min="0"/>
            </n-form-item>
            <n-form-item :label="t('keys.config.gift')" class="flex-1" path="giftPoints">
              <n-input-number v-model:value="form.giftPoints" :precision="2" min="0"/>
            </n-form-item>
          </div>
          <div class="flex gap-2 justify-end mt-2">
            <n-button :disabled="saving || genLoading" :loading="saving" @click="saveConfig">{{
                t('keys.config.save')
              }}
            </n-button>
          </div>
        </n-form>
      </n-collapse-item>
    </n-collapse>

    <n-card :title="t('keys.config.list')">
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2">
          <div class="text-xs text-neutral-500">{{ t('keys.config.current') }}：{{ selectedConfigInfo }}</div>
          <n-input v-model:value="cfgName" :placeholder="t('keys.config.query_by_name')" style="width: 180px"/>
          <n-select v-model:value="cfgSortBy" :options="cfgSortOptions" style="width: 140px"/>
          <n-select v-model:value="cfgOrder" :options="orderOptions" style="width: 120px"/>
          <n-button @click="queryConfigs">{{ t('common.query') || '查询' }}</n-button>
        </div>
        <div class="flex items-center gap-2">
          <n-input-number v-model:value="genCount" :disabled="genLoading || saving" :precision="0" max="200" min="1"
                          style="width: 120px"/>
          <n-button :disabled="!selectedConfigId || genLoading || saving" :loading="genLoading" type="primary"
                    @click="generateKeys">{{ t('keys.generate') }}
          </n-button>
          <n-button @click="resetConfigs">{{ t('left.actions.reset') }}</n-button>
          <n-button @click="loadConfigs">{{ t('keys.config.refresh') }}</n-button>
        </div>
      </div>
      <n-data-table :columns="cfgColumns" :data="cfgRows" :row-key="r=>r.id">
        <template #empty>
          <div class="py-8 text-center text-neutral-500">{{ t('keys.config.empty') }}</div>
        </template>
      </n-data-table>
      <div class="mt-2 flex justify-end">
        <n-pagination :item-count="cfgTotal" :page="cfgPage" :page-size="cfgSize" @update:page="onCfgPage"
                      @update:page-size="onCfgSize"/>
      </div>
    </n-card>

    <n-card :title="t('keys.list.title')">
      <div class="flex gap-2 mb-2 items-center">
        <n-input v-model:value="keyQuery" :placeholder="t('keys.list.query_by_key')" style="width: 200px"/>
        <n-select v-model:value="keySortBy" :options="keySortOptions" style="width: 140px"/>
        <n-select v-model:value="keyOrder" :options="orderOptions" style="width: 120px"/>
        <n-button @click="queryKeys">{{ t('common.query') || '查询' }}</n-button>
        <n-button :disabled="selectedIds.length===0" @click="batchToggle(true)">{{
            t('keys.list.enable_batch')
          }}
        </n-button>
        <n-button :disabled="selectedIds.length===0" @click="batchToggle(false)">{{
            t('keys.list.disable_batch')
          }}
        </n-button>
        <n-button @click="loadKeys">{{ t('keys.list.refresh') }}</n-button>
      </div>
      <n-data-table :columns="columns" :data="rows" :row-key="r=>r.id" @update:checked-row-keys="onCheck">
        <template #empty>
          <div class="py-8 text-center text-neutral-500">{{ t('common.empty') || '暂无数据' }}</div>
        </template>
      </n-data-table>
      <div class="mt-2 flex justify-end">
        <n-pagination :item-count="keyTotal" :page="keyPage" :page-size="keySize" @update:page="onKeyPage"
                      @update:page-size="onKeySize"/>
      </div>
    </n-card>
  </div>

</template>

<script lang="ts" setup>
import {computed, h, onMounted, reactive, ref} from 'vue'
import type {FormInst, FormRules} from 'naive-ui'
import {
  NButton,
  NCard,
  NCollapse,
  NCollapseItem,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NPagination,
  NPopconfirm,
  NPopover,
  NSelect,
  NTag,
  useMessage
} from 'naive-ui'
import {useI18n} from 'vue-i18n'
import {
  createPointsKeyConfig,
  generatePointsKeys,
  listPointsKeyConfigs,
  listPointsKeys,
  togglePointsKeyConfig,
  togglePointsKeys
} from '@/api'

const message = useMessage()
const {t} = useI18n()

const form = reactive({
  name: '',
  permanentPoints: 0,
  subscribePoints: 0,
  giftPoints: 0,
  subscribeType: 'MONTHLY',
  periodUnit: null,
  periodCount: 0,
  giftPeriodUnit: null,
  giftPeriodCount: 0,

})
const genCount = ref(1)
const canGenerate = computed(() => (form.name || '').trim().length > 0)
const saving = ref(false)
const genLoading = ref(false)

const formRef = ref<FormInst | null>(null)
const rules: FormRules = {
  name: [
    {
      validator: (_r, v: any) => {
        const s = String(v ?? '').trim()
        if (s.length === 0) return new Error(t('messages.check_inputs'))
        if (s.length > 20) return new Error(t('messages.check_inputs'))
        return true
      },
      trigger: ['input', 'blur']
    }
  ],
  permanentPoints: [
    {
      validator: (_r, v: any) => (v ?? 0) >= 0 || new Error(t('messages.check_inputs')),
      trigger: ['change', 'blur']
    }
  ],
  subscribePoints: [
    {
      validator: (_r, v: any) => {
        if (!form.subscribeType) return true
        return (v ?? 0) >= 0 || new Error(t('messages.check_inputs'))
      },
      trigger: ['change', 'blur']
    }
  ],
  giftPoints: [
    {
      validator: (_r, v: any) => (v ?? 0) >= 0 || new Error(t('messages.check_inputs')),
      trigger: ['change', 'blur']
    }
  ],
  periodCount: [
    {
      validator: (_r, v: any) => {
        const val = Number(v ?? 0)
        if ((form.subscribePoints ?? 0) <= 0) return true
        return (Number.isFinite(val) && val > 0) || new Error(t('messages.check_inputs'))
      },
      trigger: ['change', 'blur']
    }
  ],
  periodUnit: [
    {
      validator: (_r, v: any) => {
        if ((form.subscribePoints ?? 0) <= 0) return true
        const s = String(v ?? '').trim()
        return s.length > 0 || new Error(t('messages.check_inputs'))
      },
      trigger: ['change', 'blur']
    }
  ],
  giftPeriodCount: [
    {
      validator: (_r, v: any) => {
        const val = Number(v ?? 0)
        if ((form.giftPoints ?? 0) <= 0) return true
        return (Number.isFinite(val) && val > 0) || new Error(t('messages.check_inputs'))
      },
      trigger: ['change', 'blur']
    }
  ],
  giftPeriodUnit: [
    {
      validator: (_r, v: any) => {
        if ((form.giftPoints ?? 0) <= 0) return true
        const s = String(v ?? '').trim()
        return s.length > 0 || new Error(t('messages.check_inputs'))
      },
      trigger: ['change', 'blur']
    }
  ],
}

const subscribeOptions = [
  {label: 'MONTHLY', value: 'MONTHLY'},
  {label: 'YEARLY', value: 'YEARLY'},
  {label: 'TRIAL', value: 'TRIAL'},
]

const periodUnitOptions = [
  {label: 'DAY', value: 'DAY'},
  {label: 'MONTH', value: 'MONTH'},
  {label: 'YEAR', value: 'YEAR'},
]

async function saveConfig() {
  try {
    saving.value = true
    const ok = await formRef.value?.validate().then(() => true).catch(() => false)
    if (!ok) {
      message.warning(t('messages.check_inputs'));
      return
    }
    const payload: any = {...form}
    payload.name = String(payload.name ?? '').trim()
    if ((payload.subscribePoints ?? 0) <= 0) {
      payload.periodUnit = undefined
      payload.periodCount = 0
    }
    if ((payload.giftPoints ?? 0) <= 0) {
      payload.giftPeriodUnit = undefined
      payload.giftPeriodCount = 0
    }
    const saved = await createPointsKeyConfig(payload)
    selectedConfigId.value = saved.id
    await loadConfigs()
    message.success(t('keys.msg.save_success'))
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || t('keys.msg.save_failed')
    message.error(msg)
  } finally {
    saving.value = false
  }
}

async function generateKeys() {
  try {
    genLoading.value = true
    const id = selectedConfigId.value
    if (!id) {
      message.warning(t('keys.msg.select_or_save_first'))
      return
    }
    const c = Number(genCount.value)
    if (!Number.isFinite(c) || c < 1 || c > 200) {
      message.warning(t('keys.msg.gen_count_range'))
      return
    }
    await generatePointsKeys(id, genCount.value)
    message.success(t('keys.msg.generate_success'))
    await loadKeys()
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || t('keys.msg.generate_failed')
    message.error(msg)
  } finally {
    genLoading.value = false
  }
}

function parseProblemMessage(err: any): string | null {
  try {
    const data = err?.response?.data
    if (!data || typeof data !== 'object') return null
    const violations = (data as any).violations
    if (Array.isArray(violations) && violations.length > 0) {
      const first = violations[0]
      const field = first?.field || first?.path || ''
      const m = first?.message || ''
      return String(m || field || '')
    }
    const message = (data as any).message || (data as any).detail || (data as any).title
    return message ? String(message) : null
  } catch {
    return null
  }
}

function formatDateTime(input: any): string {
  try {
    if (!input) return '--'
    const d = typeof input === 'string' ? new Date(input) : input
    if (!d || Number.isNaN(d.getTime())) return String(input)
    const pad = (n: number) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch {
    return String(input ?? '--')
  }
}

function copyText(text: string) {
  try {
    navigator?.clipboard?.writeText(text)
    message.success(t('messages.copied'))
  } catch {
    message.warning(t('messages.copy_failed'))
  }
}

function parseMaybeJson(raw: any): any {
  let cur = raw
  for (let i = 0; i < 3; i++) {
    if (typeof cur === 'string') {
      const t = cur.trim()
      try {
        cur = JSON.parse(t)
      } catch {
        break
      }
    } else {
      break
    }
  }
  return cur
}

function isDateArray(val: any): boolean {
  if (!Array.isArray(val)) return false
  if (val.length < 3) return false
  return val.every((n) => typeof n === 'number' && Number.isFinite(n))
}

function toDateFromArray(arr: number[]): Date {
  const [y, m, d, hh = 0, mm = 0, ss = 0] = arr
  return new Date(y, (m || 1) - 1, d || 1, hh, mm, ss)
}

function normalizeDates(input: any): any {
  if (input == null) return input
  if (isDateArray(input)) {
    const dt = toDateFromArray(input as number[])
    return formatDateTime(dt)
  }
  if (Array.isArray(input)) return input.map((v) => normalizeDates(v))
  if (typeof input === 'object') {
    const out: any = Array.isArray(input) ? [] : {}
    for (const k of Object.keys(input)) {
      const v = (input as any)[k]
      out[k] = normalizeDates(v)
    }
    return out
  }
  return input
}

function safeNormalizeAndStringify(raw: any): string {
  try {
    const normalized = normalizeDates(raw)
    return JSON.stringify(normalized, null, 2)
  } catch {
    try {
      return JSON.stringify(raw, null, 2)
    } catch {
      return String(raw ?? '')
    }
  }
}

function renderConfigTip(row: any) {
  const raw = row.configJson
  const obj = parseMaybeJson(raw)
  const pretty = safeNormalizeAndStringify(obj ?? raw)
  const name = obj?.name ?? '--'
  const perm = obj?.permanent_points ?? obj?.permanentPoints ?? '--'
  const subs = obj?.subscribe_points ?? obj?.subscribePoints ?? '--'
  const gift = obj?.gift_points ?? obj?.giftPoints ?? '--'
  const st = obj?.subscribe_type ?? obj?.subscribeType ?? '--'
  const pu = obj?.period_unit ?? obj?.periodUnit ?? '--'
  const pc = obj?.period_count ?? obj?.periodCount ?? '--'
  const gpu = obj?.gift_period_unit ?? obj?.giftPeriodUnit ?? '--'
  const gpc = obj?.gift_period_count ?? obj?.giftPeriodCount ?? '--'
  const disabled = obj?.disabled ?? '--'
  const copyBtn = h(NButton, {size: 'tiny', onClick: () => copyText(pretty)}, {default: () => t('keys.copy.json')})
  return h('div', {class: 'space-y-2 w-[460px] max-w-[70vw]'}, [
    h('div', {class: 'text-xs text-neutral-400'}, t('keys.config.summary')),
    h('div', {class: 'grid grid-cols-2 gap-x-3 gap-y-1 text-sm'}, [
      h('div', {}, `${t('keys.columns.name')}：${name}`),
      h('div', {}, `${t('keys.columns.permanent')}：${perm}`),
      h('div', {}, `${t('keys.columns.subscribe')}：${subs}`),
      h('div', {}, `${t('keys.columns.gift')}：${gift}`),
      h('div', {}, `${t('keys.columns.subscribe_type')}：${st}`),
      h('div', {}, `${t('keys.columns.period_unit')}：${pu}`),
      h('div', {}, `${t('keys.columns.period_count')}：${pc}`),
      h('div', {}, `${t('keys.columns.gift_period_unit')}：${gpu}`),
      h('div', {}, `${t('keys.columns.gift_period_count')}：${gpc}`),
      h('div', {}, `${t('keys.columns.status')}：${String(!!disabled)}`),
    ]),
    h('div', {class: 'flex items-center justify-between'}, [
      h('div', {class: 'text-xs text-neutral-400'}, t('keys.config.raw_json')), copyBtn
    ]),
    h('pre', {class: 'text-xs bg-neutral-800 text-neutral-100 p-2 rounded overflow-auto max-h-[240px]'}, pretty),
  ])
}

const rows = ref<any[]>([])
const selectedIds = ref<number[]>([])
const columns = [
  {type: 'selection'},
  {title: t('keys.list.columns.id'), key: 'id', width: 80},
  {title: t('keys.list.columns.key'), key: 'keyCode'},
  {
    title: t('keys.list.columns.status'),
    key: 'status',
    render(row: any) {
      const used = !!row.used
      const enabled = !!row.enabled
      const type = used ? 'error' : (enabled ? 'success' : 'warning')
      const label = used ? t('keys.list.status.used') : (enabled ? t('keys.list.status.enabled') : t('keys.list.status.disabled'))
      return h(NTag, {type, size: 'small'}, {default: () => label})
    }
  },
  {
    title: t('keys.list.columns.create_time'), key: 'createTime', render(row: any) {
      return formatDateTime(row.createTime)
    }
  },
  {
    title: t('keys.list.columns.used_time'), key: 'usedTime', render(row: any) {
      return formatDateTime(row.usedTime)
    }
  },
  {title: t('keys.list.columns.used_ip'), key: 'usedIp'},
  {title: t('keys.list.columns.used_uid'), key: 'usedUid'},
  {
    title: t('keys.list.columns.action'),
    key: 'action',
    render(row: any) {
      const viewBtn = h(
          NPopover,
          {trigger: 'click'},
          {
            default: () => renderConfigTip(row),
            trigger: () => h(NButton, {size: 'small', tertiary: true}, {default: () => t('keys.view_config')})
          }
      )
      return h('div', {class: 'flex items-center gap-2'}, [viewBtn])
    }
  }
]

const keyPage = ref(1)
const keySize = ref(10)
const keyTotal = ref(0)
const keyQuery = ref('')
const keyOrder = ref<'asc' | 'desc'>('desc')
const keySortBy = ref<'status' | 'used'>('used')
const keySortOptions = [
  {label: t('keys.list.sort_by_status'), value: 'status'},
  {label: t('keys.list.sort_by_used_time'), value: 'used'},
]

function onCheck(keys: any[]) {
  selectedIds.value = (keys || []).map((k: any) => Number(k))
}

async function batchToggle(enabled: boolean) {
  try {
    await togglePointsKeys(selectedIds.value, enabled)
    message.success(enabled ? t('keys.list.enable_selected_success') : t('keys.list.disable_selected_success'))
    await loadKeys()
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || t('common.action_failed')
    message.error(msg)
  }
}

async function loadKeys() {
  try {
    const res = await listPointsKeys({
      page: keyPage.value,
      size: keySize.value,
      key: keyQuery.value.trim() || undefined,
      sortBy: keySortBy.value,
      order: keyOrder.value
    })
    rows.value = (res.items || []).map(mapKeyRow)
    keyTotal.value = Number(res.total || 0)
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || t('messages.service_error')
    message.error(msg)
  }
}

function queryKeys() {
  keyPage.value = 1
  loadKeys()
}

function onKeyPage(p: number) {
  keyPage.value = p
  loadKeys()
}

function onKeySize(s: number) {
  keySize.value = s
  keyPage.value = 1
  loadKeys()
}

const cfgRows = ref<any[]>([])
const selectedConfigId = ref<number | null>(null)
const cfgPage = ref(1)
const cfgSize = ref(10)
const cfgTotal = ref(0)
const cfgName = ref('')
const cfgSortBy = ref<'id' | 'status'>('id')
const cfgOrder = ref<'asc' | 'desc'>('desc')
const cfgSortOptions = [
  {label: t('keys.config.sort_by_id'), value: 'id'},
  {label: t('keys.config.sort_by_status'), value: 'status'},
]
const orderOptions = [
  {label: t('keys.order.asc'), value: 'asc'},
  {label: t('keys.order.desc'), value: 'desc'},
]
const cfgColumns = [
  {title: t('keys.columns.id'), key: 'id', width: 80},
  {title: t('keys.columns.name'), key: 'name'},
  {title: t('keys.columns.permanent'), key: 'permanentPoints'},
  {title: t('keys.columns.subscribe'), key: 'subscribePoints'},
  {title: t('keys.columns.gift'), key: 'giftPoints'},
  {title: t('keys.columns.subscribe_type'), key: 'subscribeType'},
  {title: t('keys.columns.period_unit'), key: 'periodUnit'},
  {title: t('keys.columns.period_count'), key: 'periodCount'},
  {title: t('keys.columns.gift_period_unit'), key: 'giftPeriodUnit'},
  {title: t('keys.columns.gift_period_count'), key: 'giftPeriodCount'},
  {
    title: t('keys.columns.status'),
    key: 'status',
    render(row: any) {
      const disabled = !!row.disabled
      const type = disabled ? 'error' : 'success'
      const label = disabled ? t('keys.status.disabled') : t('keys.status.enabled')
      return h(NTag, {type, size: 'small'}, {default: () => label})
    }
  },
  {
    title: t('keys.columns.action'),
    key: 'action',
    render(row: any) {
      const chooseBtn = h(NButton, {
        size: 'small',
        onClick: () => selectConfig(row)
      }, {default: () => t('keys.config.choose')})
      const disabled = !!row.disabled
      const toggleText = disabled ? t('keys.status.enabled') : t('keys.status.disabled')
      const confirmText = disabled ? t('keys.config.confirm_enable') : t('keys.config.confirm_disable')
      const toggleBtn = h(
          NPopconfirm,
          {onPositiveClick: () => toggleConfigStatus(row, !disabled)},
          {
            default: () => confirmText,
            trigger: () => h(NButton, {size: 'small', tertiary: true}, {default: () => toggleText})
          }
      )
      return h('div', {class: 'flex items-center gap-2'}, [chooseBtn, toggleBtn])
    }
  }
]

function selectConfig(row: any) {
  selectedConfigId.value = Number(row.id)
}

async function toggleConfigStatus(row: any, toDisabled: boolean) {
  try {
    await togglePointsKeyConfig(Number(row.id), toDisabled)
    message.success(toDisabled ? t('keys.msg.config_disabled') : t('keys.msg.config_enabled'))
    await loadConfigs()
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || t('common.action_failed')
    message.error(msg)
  }
}

const selectedConfigInfo = computed(() => {
  const id = selectedConfigId.value
  if (!id) return t('keys.config.none_selected')
  const row = cfgRows.value.find(r => Number(r.id) === id)
  if (!row) return String(id)
  return `${row.id} / ${row.name}`
})

async function loadConfigs() {
  try {
    const res = await listPointsKeyConfigs({
      page: cfgPage.value,
      size: cfgSize.value,
      name: cfgName.value.trim() || undefined,
      sortBy: cfgSortBy.value,
      order: cfgOrder.value
    })
    cfgRows.value = (res.items || []).map(mapCfgRow)
    cfgTotal.value = Number(res.total || 0)
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || t('messages.service_error')
    message.error(msg)
  }
}

function queryConfigs() {
  cfgPage.value = 1
  loadConfigs()
}

function resetConfigs() {
  cfgName.value = ''
  cfgSortBy.value = 'id'
  cfgOrder.value = 'desc'
  cfgPage.value = 1
  cfgSize.value = 10
  loadConfigs()
}

function onCfgPage(p: number) {
  cfgPage.value = p
  loadConfigs()
}

function onCfgSize(s: number) {
  cfgSize.value = s
  cfgPage.value = 1
  loadConfigs()
}

onMounted(async () => {
  await loadConfigs()
  await loadKeys()
})

function mapKeyRow(r: any) {
  return {
    id: Number(r.id),
    keyCode: r.key_code ?? r.keyCode,
    configJson: r.config_json ?? r.configJson,
    enabled: !!(r.enabled ?? r.enabled),
    used: !!(r.used ?? r.used),
    usedTime: r.used_time ?? r.usedTime,
    createTime: r.create_time ?? r.createTime,
    usedIp: r.used_ip ?? r.usedIp,
    usedUid: r.used_uid ?? r.usedUid,
  }
}

function mapCfgRow(r: any) {
  return {
    id: Number(r.id),
    name: r.name,
    permanentPoints: r.permanent_points ?? r.permanentPoints,
    subscribePoints: r.subscribe_points ?? r.subscribePoints,
    giftPoints: r.gift_points ?? r.giftPoints,
    subscribeType: r.subscribe_type ?? r.subscribeType,
    periodUnit: r.period_unit ?? r.periodUnit,
    periodCount: r.period_count ?? r.periodCount,
    giftPeriodUnit: r.gift_period_unit ?? r.giftPeriodUnit,
    giftPeriodCount: r.gift_period_count ?? r.giftPeriodCount,
    disabled: !!(r.disabled ?? r.disabled),
  }
}
</script>

<style scoped>
:deep(.n-form-item-label__asterisk),
:deep(.n-form-item-asterisk) {
  color: #16a34a;
}
</style>
