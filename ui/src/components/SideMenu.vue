<template>
  <div
      class="hidden sm:flex w-14 border-r border-neutral-200 dark:border-neutral-800 flex-col items-center py-3 gap-4 glass bg-white/40 dark:bg-black/30">
    <n-tooltip placement="right">
      <template #trigger>
        <n-button circle quaternary @click="openProfile">
          <Icon class="text-xl" icon="ph:user-gear"/>
        </n-button>
      </template>
      用户信息
    </n-tooltip>
    <n-tooltip placement="right">
      <template #trigger>
        <n-button circle quaternary @click="showRedeem = true">
          <Icon class="text-xl" icon="ph:key"/>
        </n-button>
      </template>
      兑换密钥
    </n-tooltip>

    <n-modal v-model:show="showRedeem" :style="{ width: '420px', maxWidth: '92vw' }" preset="card" title="兑换积分密钥">
      <div class="space-y-3">
        <n-input v-model:value="redeemKey" placeholder="输入密钥"/>
        <div class="flex gap-2 justify-end">
          <n-button @click="showRedeem=false">取消</n-button>
          <n-button type="primary" @click="redeem">兑换</n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import {ref} from 'vue'
import {Icon} from '@iconify/vue'
import {useUserStore} from '@/stores/user'
import {NTooltip, NButton, NModal, NInput, useMessage} from 'naive-ui'
import {redeemPoints} from '@/api'

const user = useUserStore()
const message = useMessage()

function openProfile() {
  user.requireProfile()
}

const showRedeem = ref(false)
const redeemKey = ref('')


function redeem() {
  if (!user.profileReady) {
    message.error('请先登录')
    user.requireLogin()
    return
  }
  if (redeemKey.value.trim()) {
    redeemPoints(user.email, redeemKey.value.trim())
        .then(res => {
          user.addPoints(res.pointsAdded)
          redeemKey.value = ''
          showRedeem.value = false
        })
        .catch((e: any) => {
          const status = e?.status
          if (status === 401 || status === 403) {
            message.error('未登录或未授权，请先登录')
            user.requireLogin()
          } else if (status === 500) {
            message.error('服务异常，请稍后重试')
          } else {
            message.error(e?.message || '兑换失败')
          }
        })
  }
}
</script>

<style scoped></style>