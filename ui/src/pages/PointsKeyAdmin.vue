<template>
  <div class="flex-1 overflow-y-auto p-4 space-y-4">
    <n-card title="生成密钥配置">
      <n-form :model="form" :rules="rules" ref="formRef">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
          <n-form-item label="配置名称">
            <n-input v-model:value="form.name" placeholder="如：新春活动礼包"/>
          </n-form-item>
          <div class="col-span-1 md:col-span-2 flex items-start gap-3">
            <n-form-item class="flex-1" label="永久积分">
              <n-input-number v-model:value="form.permanentPoints" :precision="2" min="0"/>
            </n-form-item>
            <n-form-item class="flex-1" label="订阅积分">
              <n-input-number v-model:value="form.subscribePoints" :precision="2" min="0"/>
            </n-form-item>
          </div>
          <n-form-item class="max-w-[220px]" label="订阅类型">
            <n-select v-model:value="form.subscribeType" :options="subscribeOptions" class="w-[200px]"/>
          </n-form-item>
          <n-form-item class="max-w-[220px]" label="周期单位">
            <n-select v-model:value="form.periodUnit" :options="periodUnitOptions" class="w-[200px]"/>
          </n-form-item>
          <n-form-item label="周期数量">
            <n-input-number v-model:value="form.periodCount" :precision="0" min="0"/>
          </n-form-item>
          <n-form-item label="结束时间">
            <n-date-picker v-model:value="form.validTo" placeholder="选择结束时间" type="datetime"/>
          </n-form-item>
        </div>
        <div class="flex gap-2 justify-end mt-2">
          <n-button @click="saveConfig">保存配置</n-button>
          <n-button :disabled="!canGenerate" type="primary" @click="generateKeys">生成密钥</n-button>
          <n-input-number v-model:value="genCount" :precision="0" max="200" min="1"/>
      </div>
      </n-form>
    </n-card>

    <n-card title="配置列表">
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2">
          <div class="text-xs text-neutral-500">当前选择：{{ selectedConfigInfo }}</div>
          <n-input v-model:value="cfgName" placeholder="按名称查询" style="width: 180px"/>
          <n-select v-model:value="cfgSortBy" :options="cfgSortOptions" style="width: 140px"/>
          <n-select v-model:value="cfgOrder" :options="orderOptions" style="width: 120px"/>
          <n-button @click="queryConfigs">查询</n-button>
        </div>
        <div class="flex items-center gap-2">
          <n-button @click="resetConfigs">重置</n-button>
          <n-button @click="loadConfigs">刷新配置</n-button>
        </div>
      </div>
      <n-data-table :columns="cfgColumns" :data="cfgRows" :row-key="r=>r.id">
        <template #empty>
          <div class="py-8 text-center text-neutral-500">暂无配置</div>
        </template>
      </n-data-table>
      <div class="mt-2 flex justify-end">
        <n-pagination :item-count="cfgTotal" :page="cfgPage" :page-size="cfgSize" @update:page="onCfgPage"
                      @update:page-size="onCfgSize"/>
      </div>
    </n-card>

    <n-card title="密钥列表">
      <div class="flex gap-2 mb-2 items-center">
        <n-input v-model:value="keyQuery" placeholder="按Key查询" style="width: 200px"/>
        <n-select v-model:value="keyOrder" :options="orderOptions" style="width: 120px"/>
        <n-button @click="queryKeys">查询</n-button>
        <n-button :disabled="selectedIds.length===0" @click="batchToggle(true)">批量启用</n-button>
        <n-button :disabled="selectedIds.length===0" @click="batchToggle(false)">批量禁用</n-button>
        <n-button @click="loadKeys">刷新</n-button>
      </div>
      <n-data-table :columns="columns" :data="rows" :row-key="r=>r.id" @update:checked-row-keys="onCheck">
        <template #empty>
          <div class="py-8 text-center text-neutral-500">暂无数据</div>
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
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  useMessage
} from 'naive-ui'
import {createPointsKeyConfig, generatePointsKeys, listPointsKeyConfigs, listPointsKeys, togglePointsKeys} from '@/api'
import type { FormRules, FormInst } from 'naive-ui'

const message = useMessage()

const form = reactive({
  name: '',
  permanentPoints: 0,
  subscribePoints: 0,
  subscribeType: 'MONTHLY',
  periodUnit: 'MONTH',
  periodCount: 0,
  validTo: null as any,
})
const genCount = ref(10)
const canGenerate = computed(() => (form.name || '').trim().length > 0)

const formRef = ref<FormInst | null>(null)
const rules: FormRules = {
  name: [
    { required: true, message: '请填写配置名称', trigger: ['input', 'blur'] },
    {
      validator: (_r, v: any) => typeof v === 'string' && v.trim().length > 0 || new Error('配置名称不能为空'),
      trigger: ['input', 'blur']
    }
  ],
  permanentPoints: [
    {
      validator: (_r, v: any) => (v ?? 0) >= 0 || new Error('永久积分需≥0'),
      trigger: ['change', 'blur']
    }
  ],
  subscribePoints: [
    {
      validator: (_r, v: any) => {
        if (!form.subscribeType) return true
        return (v ?? 0) >= 0 || new Error('订阅积分需≥0')
      },
      trigger: ['change', 'blur']
    }
  ],
  periodCount: [
    {
      validator: (_r, v: any) => {
        const val = Number(v ?? 0)
        if (!form.periodUnit) return true
        return (Number.isFinite(val) && val > 0) || new Error('设置周期单位时，周期数量需>0')
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
    await formRef.value?.validate()
    const payload: any = {...form}
    if (payload.validTo) {
      const d = new Date(payload.validTo)
      // 发送为 LocalDateTime 格式：yyyy-MM-dd'T'HH:mm:ss（不带时区）
      const iso = d.toISOString().slice(0, 19)
      payload.validTo = iso
    }
    const saved = await createPointsKeyConfig(payload)
    selectedConfigId.value = saved.id
    await loadConfigs()
    message.success('配置已保存')
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || '保存失败'
    message.error(msg)
  }
}

async function generateKeys() {
  try {
    const id = selectedConfigId.value
    if (!id) {
      message.warning('请先选择配置或保存配置')
      return
    }
    const c = Number(genCount.value)
    if (!Number.isFinite(c) || c < 1 || c > 200) {
      message.warning('生成数量需在 1-200 之间')
      return
    }
    await generatePointsKeys(id, genCount.value)
    message.success('密钥生成成功')
    await loadKeys()
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || '生成失败'
    message.error(msg)
  }
}

function parseProblemMessage(err: any): string | null {
  try {
    const data = err?.response?.data
    if (!data || typeof data !== 'object') return null
    // Micronaut Problem JSON: violations[] or message/detail/title
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

const rows = ref<any[]>([])
const selectedIds = ref<number[]>([])
const columns = [
  {type: 'selection'},
  {title: 'ID', key: 'id', width: 80},
  {title: 'Key', key: 'keyCode'},
  {title: '配置快照', key: 'configJson'},
  {title: '可用', key: 'enabled'},
  {title: '已使用', key: 'used'},
  {title: '使用时间', key: 'usedTime'},
  {title: '使用IP', key: 'usedIp'},
  {title: '使用UID', key: 'usedUid'},
]

const keyPage = ref(1)
const keySize = ref(10)
const keyTotal = ref(0)
const keyQuery = ref('')
const keyOrder = ref<'asc' | 'desc'>('desc')

function onCheck(keys: any[]) {
  selectedIds.value = (keys || []).map((k: any) => Number(k))
}

async function batchToggle(enabled: boolean) {
  try {
    await togglePointsKeys(selectedIds.value, enabled)
    message.success(enabled ? '已启用选中密钥' : '已禁用选中密钥')
    await loadKeys()
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || '操作失败'
    message.error(msg)
  }
}

async function loadKeys() {
  try {
    const res = await listPointsKeys({
      page: keyPage.value,
      size: keySize.value,
      key: keyQuery.value.trim() || undefined,
      order: keyOrder.value
    })
    rows.value = (res.items || []).map(mapKeyRow)
    keyTotal.value = Number(res.total || 0)
  } catch (e: any) {
    const msg = parseProblemMessage(e) || e?.message || '加载失败'
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
const cfgSortBy = ref<'id' | 'end'>('id')
const cfgOrder = ref<'asc' | 'desc'>('desc')
const cfgSortOptions = [
  {label: '按ID', value: 'id'},
  {label: '按结束时间', value: 'end'},
]
const orderOptions = [
  {label: '升序', value: 'asc'},
  {label: '降序', value: 'desc'},
]
const cfgColumns = [
  {title: 'ID', key: 'id', width: 80},
  {title: '名称', key: 'name'},
  {title: '永久积分', key: 'permanentPoints'},
  {title: '订阅积分', key: 'subscribePoints'},
  {title: '订阅类型', key: 'subscribeType'},
  {title: '周期单位', key: 'periodUnit'},
  {title: '周期数量', key: 'periodCount'},
  {title: '结束时间', key: 'validTo'},
  {title: '禁用', key: 'disabled'},
  {
    title: '操作',
    key: 'action',
    render(row: any) {
      return h(
          NButton,
          {size: 'small', onClick: () => selectConfig(row)},
          {default: () => '选择'}
      )
    }
  }
]

function selectConfig(row: any) {
  selectedConfigId.value = Number(row.id)
}

const selectedConfigInfo = computed(() => {
  const id = selectedConfigId.value
  if (!id) return '未选择'
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
    const msg = parseProblemMessage(e) || e?.message || '加载配置失败'
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
    subscribeType: r.subscribe_type ?? r.subscribeType,
    periodUnit: r.period_unit ?? r.periodUnit,
    periodCount: r.period_count ?? r.periodCount,
    validTo: r.valid_to ?? r.validTo,
    disabled: !!(r.disabled ?? r.disabled),
  }
}
</script>

<style scoped></style>
