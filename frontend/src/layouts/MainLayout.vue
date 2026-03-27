<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DashboardOutlined,
  BarChartOutlined,
  ExperimentOutlined,
  CalculatorOutlined,
  FileTextOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const collapsed = ref(false)

const selectedKeys = computed(() => {
  const path = route.path
  return [path === '/' ? '/' : path]
})

function onMenuClick({ key }: { key: string }) {
  router.push(key)
}
</script>

<template>
  <a-layout class="main-layout-root">
    <a-layout-sider v-model:collapsed="collapsed" class="main-sider" collapsible breakpoint="md">
      <div
        style="
          height: 32px;
          margin: 16px;
          color: #fff;
          font-size: 14px;
          font-weight: bold;
          text-align: center;
          line-height: 32px;
          white-space: nowrap;
          overflow: hidden;
        "
      >
        {{ collapsed ? '费用' : '仓内操作费用分析' }}
      </div>
      <a-menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        @click="onMenuClick"
      >
        <a-menu-item key="/">
          <DashboardOutlined />
          <span>数据看板</span>
        </a-menu-item>
        <a-menu-item key="/baseline">
          <BarChartOutlined />
          <span>费用基线</span>
        </a-menu-item>
        <a-menu-item key="/impact">
          <ExperimentOutlined />
          <span>影响因素</span>
        </a-menu-item>
        <a-menu-item key="/estimate">
          <CalculatorOutlined />
          <span>成本估算</span>
        </a-menu-item>
        <a-menu-item key="/report">
          <FileTextOutlined />
          <span>报告</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout class="main-content-layout">
      <a-layout-content class="main-content-scroll">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.main-layout-root {
  min-height: 100vh;
  max-height: 100vh;
  overflow: hidden;
}

.main-sider {
  position: sticky;
  top: 0;
  left: 0;
  height: 100vh;
}

.main-content-layout {
  min-height: 100vh;
}

.main-content-scroll {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
