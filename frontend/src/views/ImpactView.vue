<script setup lang="ts">
import { ref, watch, onMounted, computed, onUnmounted } from 'vue'
import axios from 'axios'
import VChart from 'vue-echarts'
import { getFactors, getCorrelation, getScatter as fetchScatter, getWarehouses, useAbortController } from '@/api'
import type { FactorRankVO, CorrelationMatrixVO, ScatterPointVO, WarehouseVO } from '@/types/api'

const loading = ref(false)
const error = ref<string | null>(null)
const { getSignal, abort: abortRequests } = useAbortController()
const factors = ref<FactorRankVO[]>([])
const correlation = ref<CorrelationMatrixVO | null>(null)
const warehouses = ref<WarehouseVO[]>([])

// 散点图
const selectedFactor = ref<string | undefined>(undefined)
const scatterData = ref<ScatterPointVO[]>([])
const scatterLoading = ref(false)

// 双仓对比
const compareA = ref<string | undefined>(undefined)
const compareB = ref<string | undefined>(undefined)
const compareLoading = ref(false)
const factorsA = ref<FactorRankVO[]>([])
const factorsB = ref<FactorRankVO[]>([])
const selectedWarehouse = ref<string>('')

async function loadData() {
  if (!selectedWarehouse.value) return
  loading.value = true
  error.value = null
  const signal = getSignal()
  try {
    const [f, c] = await Promise.all([getFactors(selectedWarehouse.value, signal), getCorrelation(selectedWarehouse.value, signal)])
    factors.value = f
    correlation.value = c
    if (f.length > 0 && !selectedFactor.value) {
      selectedFactor.value = f[0].factorName
    }
  } catch (e) {
    if (!axios.isCancel(e)) {
      error.value = '影响因素数据加载失败，请重试'
    }
  } finally {
    loading.value = false
  }
}

async function loadScatter() {
  if (!selectedWarehouse.value || !selectedFactor.value) return
  scatterLoading.value = true
  try {
    scatterData.value = await fetchScatter(selectedWarehouse.value, selectedFactor.value, getSignal())
  } catch {
    // handled
  } finally {
    scatterLoading.value = false
  }
}

async function handleCompare() {
  if (!compareA.value || !compareB.value) return
  compareLoading.value = true
  try {
    const [a, b] = await Promise.all([getFactors(compareA.value), getFactors(compareB.value)])
    factorsA.value = a
    factorsB.value = b
  } catch {
    // handled
  } finally {
    compareLoading.value = false
  }
}

// 梯队颜色
function tierColor(tier: number) {
  if (tier === 1) return '#f5222d'
  if (tier === 2) return '#fa8c16'
  return '#8c8c8c'
}

// 因素排序图
const factorOption = computed(() => {
  const sorted = [...factors.value].sort((a, b) => Math.abs(a.correlation) - Math.abs(b.correlation))
  return {
    tooltip: {
      trigger: 'axis' as const,
      formatter: (params: { name: string; value: number }[]) => {
        const p = params[0]
        const f = factors.value.find((x) => x.factorName === p.name)
        return `${p.name}<br/>相关系数: ${p.value?.toFixed(4)}<br/>梯队: ${f?.tierLabel || '-'}<br/>${f?.applicationTip || f?.description || ''}`
      },
    },
    grid: { left: 140, right: 40, top: 10, bottom: 20 },
    xAxis: { type: 'value' as const },
    yAxis: { type: 'category' as const, data: sorted.map((d) => d.factorName) },
    series: [{
      type: 'bar' as const,
      data: sorted.map((d) => ({
        value: d.correlation,
        itemStyle: { color: tierColor(d.tier) },
      })),
    }],
  }
})

// 热力图
const heatmapOption = computed(() => {
  if (!correlation.value) return null
  const { factors: names, matrix } = correlation.value
  const data: [number, number, number][] = []
  for (let i = 0; i < matrix.length; i++) {
    for (let j = 0; j < matrix[i].length; j++) {
      data.push([j, i, matrix[i][j]])
    }
  }
  return {
    tooltip: {
      formatter: (p: { data: [number, number, number] }) => {
        const [x, y, v] = p.data
        return `${names[y]} × ${names[x]}<br/>相关系数: ${v?.toFixed(4)}`
      },
    },
    grid: { left: 140, right: 60, top: 10, bottom: 60 },
    xAxis: { type: 'category' as const, data: names, axisLabel: { rotate: 45 } },
    yAxis: { type: 'category' as const, data: names },
    visualMap: { min: -1, max: 1, calculable: true, orient: 'vertical' as const, right: 0, top: 'center', inRange: { color: ['#2166ac', '#f7f7f7', '#b2182b'] } },
    series: [{
      type: 'heatmap' as const,
      data,
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } },
    }],
  }
})

// 散点图
const scatterOption = computed(() => ({
  tooltip: {
    formatter: (p: { data: [number, number, string] }) => `${p.data[2]}<br/>${selectedFactor.value}: ${p.data[0]}<br/>工时: ${p.data[1]}`,
  },
  grid: { left: 60, right: 20, top: 10, bottom: 40 },
  xAxis: { type: 'value' as const, name: selectedFactor.value },
  yAxis: { type: 'value' as const, name: '工时' },
  series: [{
    type: 'scatter' as const,
    data: scatterData.value.map((d) => [d.factorValue, d.workHours, d.date]),
    symbolSize: 8,
  }],
}))

// 双仓对比图工厂
function compareFactorOption(data: FactorRankVO[], title: string) {
  const sorted = [...data].sort((a, b) => Math.abs(a.correlation) - Math.abs(b.correlation))
  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' as const },
    grid: { left: 120, right: 20, top: 30, bottom: 10 },
    xAxis: { type: 'value' as const },
    yAxis: { type: 'category' as const, data: sorted.map((d) => d.factorName) },
    series: [{
      type: 'bar' as const,
      data: sorted.map((d) => ({
        value: d.correlation,
        itemStyle: { color: d.correlation >= 0 ? '#1890ff' : '#ff4d4f' },
      })),
    }],
  }
}

const compareOptionA = computed(() => {
  const wh = warehouses.value.find((w) => w.warehouseCode === compareA.value)
  return compareFactorOption(factorsA.value, wh?.warehouseName || '仓库 A')
})
const compareOptionB = computed(() => {
  const wh = warehouses.value.find((w) => w.warehouseCode === compareB.value)
  return compareFactorOption(factorsB.value, wh?.warehouseName || '仓库 B')
})

onMounted(async () => {
  try {
    loading.value = true
    warehouses.value = await getWarehouses()
    if (warehouses.value.length > 0) {
      selectedWarehouse.value = warehouses.value[0].warehouseCode
    }
  } catch { /* ignore */} finally {
    await loadData()
  }
})

watch(selectedWarehouse, () => {
  selectedFactor.value = undefined
  loadData()
})
watch(selectedFactor, loadScatter)

onUnmounted(abortRequests)
</script>

<template>
  <div class="p-6">
    <a-result v-if="error" status="error" :title="error">
      <template #extra>
        <a-button type="primary" @click="loadData">重试</a-button>
      </template>
    </a-result>
    <a-spin v-else :spinning="loading" tip="加载中..." style="min-height: 400px; display: block">
      <a-card class="mb-6">
        <a-space>
          <span>仓库：</span>
          <a-select
            v-model:value="selectedWarehouse"
            placeholder="选择仓库"
            style="width: 220px"
            :options="warehouses.map((w) => ({ value: w.warehouseCode, label: w.warehouseName }))"
          />
        </a-space>
      </a-card>

      <!-- 因素重要性排序 -->
      <a-card title="影响因素重要性排序（三梯队）" class="mb-6">
        <a-empty v-if="factors.length === 0 && !loading" description="暂无影响因素数据" />
        <template v-else>
          <v-chart :option="factorOption" style="height: 360px" autoresize />
          <div class="mt-2" style="display:flex;gap:16px;justify-content:center">
            <span><span style="display:inline-block;width:12px;height:12px;background:#f5222d;border-radius:2px;margin-right:4px"></span>核心驱动 (|r|≥0.7)</span>
            <span><span style="display:inline-block;width:12px;height:12px;background:#fa8c16;border-radius:2px;margin-right:4px"></span>重要影响 (0.4≤|r|&lt;0.7)</span>
            <span><span style="display:inline-block;width:12px;height:12px;background:#8c8c8c;border-radius:2px;margin-right:4px"></span>结构影响 (|r|&lt;0.4)</span>
          </div>
        </template>
      </a-card>

      <!-- 因素详情表 -->
      <a-card title="影响因素详情" class="mb-6" v-if="factors.length > 0">
        <a-table :data-source="factors" :pagination="false" size="small" row-key="factorName">
          <a-table-column title="排名" data-index="rank" width="60" />
          <a-table-column title="因素" data-index="factorName" width="140" />
          <a-table-column title="相关系数" data-index="correlation" width="100">
            <template #default="{ text }">{{ (text as number)?.toFixed(4) }}</template>
          </a-table-column>
          <a-table-column title="梯队" data-index="tierLabel" width="100">
            <template #default="{ record }">
              <a-tag :color="record.tier === 1 ? 'red' : record.tier === 2 ? 'orange' : 'default'">{{ record.tierLabel }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="应用建议" data-index="applicationTip" />
        </a-table>
      </a-card>

      <!-- Pearson 热力图 -->
      <a-card title="Pearson 相关系数热力图" class="mb-6">
        <a-empty v-if="!heatmapOption && !loading" description="暂无相关性数据" />
        <v-chart v-else-if="heatmapOption" :option="heatmapOption" style="height: 400px" autoresize />
      </a-card>

      <!-- 单因素散点图 -->
      <a-card title="单因素散点图" class="mb-6">
        <a-select
          v-model:value="selectedFactor"
          placeholder="选择因素"
          style="width: 200px"
          class="mb-4"
          :options="factors.map((f) => ({ value: f.factorName, label: f.factorName }))"
        />
        <a-spin :spinning="scatterLoading">
          <a-empty v-if="scatterData.length === 0 && !scatterLoading" description="暂无散点图数据" />
          <v-chart v-else :option="scatterOption" style="height: 320px" autoresize />
        </a-spin>
      </a-card>

      <!-- 双仓因素对比 -->
      <a-card title="双仓因素对比">
        <a-space wrap class="mb-4">
          <a-select v-model:value="compareA" placeholder="仓库 A" style="width: 200px" :options="warehouses.map((w) => ({ value: w.warehouseCode, label: w.warehouseName }))" />
          <a-select v-model:value="compareB" placeholder="仓库 B" style="width: 200px" :options="warehouses.map((w) => ({ value: w.warehouseCode, label: w.warehouseName }))" />
          <a-button type="primary" :loading="compareLoading" :disabled="!compareA || !compareB" @click="handleCompare">对比</a-button>
        </a-space>
        <a-row v-if="factorsA.length > 0 || factorsB.length > 0" :gutter="16">
          <a-col :xs="24" :md="12">
            <a-empty v-if="factorsA.length === 0" description="暂无数据" />
            <v-chart v-else :option="compareOptionA" style="height: 360px" autoresize />
          </a-col>
          <a-col :xs="24" :md="12">
            <a-empty v-if="factorsB.length === 0" description="暂无数据" />
            <v-chart v-else :option="compareOptionB" style="height: 360px" autoresize />
          </a-col>
        </a-row>
        <div v-else class="text-center py-8 text-gray-400">请选择两个仓库后点击对比</div>
      </a-card>
    </a-spin>
  </div>
</template>
