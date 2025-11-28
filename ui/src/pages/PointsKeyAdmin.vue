<template>
  <div class="flex-1 overflow-y-auto p-4 space-y-4">
    <n-card title="生成密钥配置">
      <n-form :model="form">
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
          <n-form-item label="订阅时长（天）">
            <n-input-number v-model:value="form.subscribeDurationDays" :precision="0" min="0"/>
          </n-form-item>
          <n-form-item label="开始时间">
            <n-date-picker v-model:value="form.validFrom" placeholder="选择开始时间" type="datetime"/>
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

    <n-card title="密钥列表">
      <div class="flex gap-2 mb-2">
        <n-button :disabled="selectedIds.length===0" @click="batchToggle(true)">批量启用</n-button>
        <n-button :disabled="selectedIds.length===0" @click="batchToggle(false)">批量禁用</n-button>
        <n-button @click="loadKeys">刷新</n-button>
      </div>
      <n-data-table :columns="columns" :data="rows" :row-key="r=>r.id" @update:checked-row-keys="onCheck">
        <template #empty>
          <div class="py-8 text-center text-neutral-500">暂无数据</div>
        </template>
      </n-data-table>
    </n-card>
  </div>

</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue'
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
import {createPointsKeyConfig, generatePointsKeys, listPointsKeys, togglePointsKeys} from '@/api'

const message = useMessage()

const form = reactive({
  name: '',
  permanentPoints: 0,
  subscribePoints: 0,
  subscribeType: 'MONTHLY',
  subscribeDurationDays: 0,
  validFrom: null as any,
  validTo: null as any,
})
const genCount = ref(10)
const canGenerate = computed(() => (form.name || '').trim().length > 0)

const subscribeOptions = [
  {label: 'MONTHLY', value: 'MONTHLY'},
  {label: 'YEARLY', value: 'YEARLY'},
  {label: 'TRIAL', value: 'TRIAL'},
]

async function saveConfig() {
  try {
    const payload: any = {...form}
    if (payload.validFrom) payload.validFrom = new Date(payload.validFrom).toISOString()
    if (payload.validTo) payload.validTo = new Date(payload.validTo).toISOString()
    await createPointsKeyConfig(payload)
    message.success('配置已保存')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  }
}

async function generateKeys() {
  try {
    // 简化：先保存配置再生成，实际应选择已有配置ID，这里为了便捷使用刚保存的配置
    const payload: any = {...form}
    if (payload.validFrom) payload.validFrom = new Date(payload.validFrom).toISOString()
    if (payload.validTo) payload.validTo = new Date(payload.validTo).toISOString()
    const cfg = await createPointsKeyConfig(payload)
    await generatePointsKeys(cfg.id, genCount.value)
    message.success('密钥生成成功')
    await loadKeys()
  } catch (e: any) {
    message.error(e?.message || '生成失败')
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

function onCheck(keys: any[]) {
  selectedIds.value = (keys || []).map((k: any) => Number(k))
}

async function batchToggle(enabled: boolean) {
  try {
    await togglePointsKeys(selectedIds.value, enabled)
    message.success(enabled ? '已启用选中密钥' : '已禁用选中密钥')
    await loadKeys()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

async function loadKeys() {
  try {
    rows.value = await listPointsKeys()
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  }
}

onMounted(loadKeys)
</script>

<style scoped></style>