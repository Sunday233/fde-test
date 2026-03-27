<script setup lang="ts">
import { ref, watch, onMounted, computed, onUnmounted } from 'vue'
import axios from 'axios'
import VChart from 'vue-echarts'
import { getOverview, getTrend, getAvailableMonths, getWarehouses, useAbortController } from '@/api'
import type { DashboardOverviewVO, TrendDataVO, WarehouseVO } from '@/types/api'

const loading = ref(false)
const error = ref<string | null>(null)
const overview = ref<DashboardOverviewVO | null>(null)
const { getSignal, abort: abortRequests } = useAbortController()

const warehouses = ref<WarehouseVO[]>([])
const selectedWarehouse = ref<string>('')

// 月份选择
const availableMonths = ref<string[]>([])
const selectedMonth = ref<string>('')

// 趋势数据
const trendData = ref<TrendDataVO[]>([])
const feeBreakdownData = ref<TrendDataVO[]>([])
const workloadData = ref<TrendDataVO[]>([])

async function loadMonths() {
  try {
    const months = await getAvailableMonths()
    availableMonths.value = months
    if (months.length > 0 && !selectedMonth.value) {
      selectedMonth.value = months[0]
    }
  } catch {
    // fallback: use current month
    const d = new Date()
    const cur = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    availableMonths.value = [cur]
    if (!selectedMonth.value) selectedMonth.value = cur
  }
}

async function loadWarehouseOptions() {
  try {
    warehouses.value = await getWarehouses()
    if (warehouses.value.length > 0 && !selectedWarehouse.value) {
      selectedWarehouse.value = warehouses.value[0].warehouseCode
    }
  } catch {
    warehouses.value = []
    selectedWarehouse.value = ''
  }
}

async function loadData() {
  if (!selectedWarehouse.value || !selectedMonth.value) return
  loading.value = true
  error.value = null
  const signal = getSignal()
  const month = selectedMonth.value
  try {
    const [ov, trend, fee, workload] = await Promise.all([
      getOverview(selectedWarehouse.value, month, signal),
      getTrend(selectedWarehouse.value, month, month, 'outbound_orders', signal),
      getTrend(selectedWarehouse.value, month, month, 'monthly_fee_breakdown', signal),
      getTrend(selectedWarehouse.value, month, month, 'workload_distribution', signal),
    ])
    overview.value = ov
    trendData.value = trend
    feeBreakdownData.value = fee
    workloadData.value = workload
  } catch (e) {
    if (!axios.isCancel(e)) {
      error.value = '数据加载失败，请重试'
    }
  } finally {
    loading.value = false
  }
}

// ─── 折线图 option ───
const trendOption = computed(() => {
  const grouped = new Map<string, { dates: string[]; values: number[] }>()
  for (const item of trendData.value) {
    const key = item.warehouseName || item.warehouseCode
    if (!grouped.has(key)) grouped.set(key, { dates: [], values: [] })
    const g = grouped.get(key)!
    g.dates.push(item.date)
    g.values.push(item.value)
  }
  const allDates = [...new Set(trendData.value.map((d) => d.date))].sort()
  return {
    tooltip: { trigger: 'axis' as const },
    legend: { bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category' as const, data: allDates },
    yAxis: { type: 'value' as const },
    series: [...grouped.entries()].map(([name, g]) => ({
      name,
      type: 'line' as const,
      data: g.values,
      smooth: true,
    })),
  }
})

// ─── 费用构成柱状图 option ───
const feeOption = computed(() => {
  const types = [...new Set(feeBreakdownData.value.map((d) => d.type))]
  const months = [...new Set(feeBreakdownData.value.map((d) => d.date))].sort()
  return {
    tooltip: { trigger: 'axis' as const },
    legend: { bottom: 0, data: types },
    grid: { left: 80, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category' as const, data: months },
    yAxis: { type: 'value' as const, axisLabel: { formatter: '¥{value}' } },
    series: types.map((t) => ({
      name: t,
      type: 'bar' as const,
      stack: 'fee',
      data: months.map((m) => {
        const item = feeBreakdownData.value.find((d) => d.type === t && d.date === m)
        return item ? item.value : 0
      }),
    })),
  }
})

// ─── 饼图 option ───
const workloadOption = computed(() => ({
  tooltip: {
    trigger: 'item' as const,
    formatter: '{b}: {c} ({d}%)',
  },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie' as const,
      radius: ['40%', '70%'],
      data: workloadData.value.map((d) => ({
        name: d.type,
        value: d.value,
      })),
    },
  ],
}))

onMounted(async () => {
  await loadWarehouseOptions()
  await loadMonths()
  await loadData()
})
watch(selectedWarehouse, loadData)
watch(selectedMonth, loadData)
onUnmounted(abortRequests)
</script>

<template>
  <div class="p-4">
    <a-result v-if="error" status="error" :title="error">
      <template #extra>
        <a-button type="primary" @click="loadData">重试</a-button>
      </template>
    </a-result>
    <a-spin v-else :spinning="loading">
      <!-- 月份选择器 -->
      <div class="mb-4 flex items-center gap-2">
        <span>仓库：</span>
        <a-select v-model:value="selectedWarehouse" style="width: 220px" placeholder="选择仓库" :options="warehouses.map((w) => ({ value: w.warehouseCode, label: w.warehouseName }))" />
        <span>数据月份：</span>
        <a-select v-model:value="selectedMonth" style="width: 160px" placeholder="选择月份">
          <a-select-option v-for="m in availableMonths" :key="m" :value="m">{{ m }}</a-select-option>
        </a-select>
      </div>
      <!-- KPI 卡片 -->
      <a-row :gutter="16" class="mb-4">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="总单量" :value="overview?.totalOrders ?? '-'" suffix="单" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="总工时" :value="overview?.totalWorkHours ?? '-'" suffix="h" :precision="1" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="月度费用" :value="overview?.monthlyFee ?? '-'" prefix="¥" :precision="2" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="人效" :value="overview?.laborEfficiency ?? '-'" suffix="件/人时" :precision="2" />
          </a-card>
        </a-col>
      </a-row>
      <a-row :gutter="16" class="mb-4">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="日均费用" :value="overview?.dailyAvgFee ?? '-'" prefix="¥" :precision="2" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="出勤天数" :value="overview?.workDays ?? '-'" suffix="天" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="件单比" :value="overview?.itemsPerOrder ?? '-'" :precision="2" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card>
            <a-statistic title="单均成本" :value="overview?.avgCostPerOrder ?? '-'" prefix="¥" :precision="2" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 图表区域 -->
      <a-row :gutter="16" class="mb-4">
        <a-col :span="24">
          <a-card title="日出库单量趋势">
            <a-empty v-if="trendData.length === 0 && !loading" description="暂无趋势数据" />
            <v-chart v-else :option="trendOption" style="height: 320px" autoresize />
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :xs="24" :md="12">
          <a-card title="月度费用构成">
            <a-empty v-if="feeBreakdownData.length === 0 && !loading" description="暂无费用构成数据" />
            <v-chart v-else :option="feeOption" style="height: 320px" autoresize />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-card title="操作类型工作量分布">
            <a-empty v-if="workloadData.length === 0 && !loading" description="暂无工作量分布数据" />
            <v-chart v-else :option="workloadOption" style="height: 320px" autoresize />
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>
