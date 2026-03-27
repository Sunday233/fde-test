<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import axios from 'axios'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import {
  compareWarehouses,
  getLatestBaselineMonth,
  getMonthlyBaseline,
  getOperationFeeDetail,
  getWarehouses,
  useAbortController,
} from '@/api'
import type {
  CompareResultVO,
  MonthlyBaselineVO,
  OperationFeeDetailVO,
  PageResult,
  WarehouseVO,
} from '@/types/api'

const loading = ref(false)
const detailLoading = ref(false)
const compareLoading = ref(false)
const error = ref<string | null>(null)
const detailDimension = ref<'day' | 'month'>('month')

const baselineData = ref<MonthlyBaselineVO[]>([])
const detailData = ref<OperationFeeDetailVO[]>([])
const compareData = ref<CompareResultVO[]>([])
const warehouses = ref<WarehouseVO[]>([])

const pagination = ref({ current: 1, pageSize: 20, total: 0 })
const { getSignal, abort: abortRequests } = useAbortController()

const filterStartMonth = ref<dayjs.Dayjs | null>(null)
const filterEndMonth = ref<dayjs.Dayjs | null>(null)
const filterWarehouseCode = ref<string | undefined>()

const compareA = ref<string | undefined>()
const compareB = ref<string | undefined>()
const selectedMetric = ref<'totalFee' | 'totalOrders' | 'costPerOrder' | 'costPerItem' | 'avgHeadcount'>('totalFee')

const metricOptions = [
  { label: '总费用', value: 'totalFee' },
  { label: '总单量', value: 'totalOrders' },
  { label: '单均成本', value: 'costPerOrder' },
  { label: '件均成本', value: 'costPerItem' },
  { label: '平均人数', value: 'avgHeadcount' },
] as const

const columns = [
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 160, fixed: 'left' as const },
  { title: '年份', dataIndex: 'year', key: 'year', width: 70 },
  { title: '月份', dataIndex: 'month', key: 'month', width: 70 },
  { title: '日均费用', dataIndex: 'dailyAvgFee', key: 'dailyAvgFee', width: 120, customRender: ({ text }: { text: number }) => formatCurrency(text) },
  { title: '总费用', dataIndex: 'totalFee', key: 'totalFee', width: 130, customRender: ({ text }: { text: number }) => formatCurrency(text) },
  { title: '总单量', dataIndex: 'totalOrders', key: 'totalOrders', width: 100, customRender: ({ text }: { text: number }) => formatInteger(text) },
  { title: '总件数', dataIndex: 'totalItems', key: 'totalItems', width: 100, customRender: ({ text }: { text: number }) => formatInteger(text) },
  { title: '件单比', dataIndex: 'itemsPerOrder', key: 'itemsPerOrder', width: 90, customRender: ({ text }: { text: number }) => formatDecimal(text) },
  { title: '日均单量', dataIndex: 'dailyAvgOrders', key: 'dailyAvgOrders', width: 100, customRender: ({ text }: { text: number }) => formatDecimal(text, 0) },
  { title: '单均成本', dataIndex: 'costPerOrder', key: 'costPerOrder', width: 110, customRender: ({ text }: { text: number }) => formatCurrency(text) },
  { title: '件均成本', dataIndex: 'costPerItem', key: 'costPerItem', width: 110, customRender: ({ text }: { text: number }) => formatCurrency(text) },
  { title: '平均人数', dataIndex: 'avgHeadcount', key: 'avgHeadcount', width: 100, customRender: ({ text }: { text: number }) => formatDecimal(text, 1) },
  { title: '人效', dataIndex: 'laborEfficiency', key: 'laborEfficiency', width: 120, customRender: ({ text }: { text: number }) => formatEfficiency(text) },
  { title: '固临比', dataIndex: 'fixedTempRatio', key: 'fixedTempRatio', width: 90, customRender: ({ text }: { text: number }) => formatDecimal(text) },
  { title: '总工时', dataIndex: 'totalWorkHours', key: 'totalWorkHours', width: 100, customRender: ({ text }: { text: number }) => formatHours(text) },
  { title: '加权单价', dataIndex: 'weightedUnitPrice', key: 'weightedUnitPrice', width: 120, customRender: ({ text }: { text: number }) => formatHourlyCurrency(text) },
]

const detailColumns = computed(() => {
  const dateCol = detailDimension.value === 'day'
    ? { title: '日期', dataIndex: 'date', key: 'date', width: 120, fixed: 'left' as const }
    : { title: '月份', dataIndex: 'monthLabel', key: 'monthLabel', width: 110, fixed: 'left' as const }

  return [
    dateCol,
    { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 160 },
    { title: '操作大类', dataIndex: 'operationCategory', key: 'operationCategory', width: 140 },
    { title: '操作量', dataIndex: 'operationCount', key: 'operationCount', width: 110, customRender: ({ text }: { text: number }) => formatInteger(text) },
    { title: '费用占比', dataIndex: 'feeRatio', key: 'feeRatio', width: 110, customRender: ({ text }: { text: number }) => `${(Number(text || 0) * 100).toFixed(2)}%` },
    { title: '估算费用', dataIndex: 'estimatedFee', key: 'estimatedFee', width: 140, customRender: ({ text }: { text: number }) => formatCurrency(text) },
  ]
})

function formatCurrency(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `¥${Number(value).toFixed(2)}`
}

function formatHourlyCurrency(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `¥${Number(value).toFixed(2)}/h`
}

function formatInteger(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return Number(value).toLocaleString()
}

function formatDecimal(value?: number | null, digits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return Number(value).toFixed(digits)
}

function formatHours(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${Number(value).toFixed(1)} h`
}

function formatEfficiency(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${Number(value).toFixed(2)} 件/人时`
}

function getMonthRange() {
  return {
    startMonth: filterStartMonth.value?.format('YYYY-MM'),
    endMonth: filterEndMonth.value?.format('YYYY-MM'),
  }
}

function baselineRowKey(record: MonthlyBaselineVO) {
  return `${record.warehouseCode}_${record.year}_${record.month}`
}

function detailRowKey(record: OperationFeeDetailVO) {
  const datePart = detailDimension.value === 'day' ? (record.date ?? 'unknown') : record.monthLabel
  return `${record.warehouseCode}_${datePart}_${record.operationCategory}`
}

function formatPaginationTotal(total: number) {
  return `共 ${total} 条`
}

async function loadBaselineData() {
  loading.value = true
  error.value = null
  try {
    const signal = getSignal()
    const { startMonth, endMonth } = getMonthRange()
    const result = await getMonthlyBaseline(
      filterWarehouseCode.value,
      startMonth,
      endMonth,
      pagination.value.current,
      pagination.value.pageSize,
      signal,
    )

    if ('records' in result) {
      baselineData.value = (result as PageResult<MonthlyBaselineVO>).records
      pagination.value.total = (result as PageResult<MonthlyBaselineVO>).total
    } else {
      baselineData.value = result as MonthlyBaselineVO[]
      pagination.value.total = baselineData.value.length
    }
  } catch (err) {
    if (!axios.isCancel(err)) {
      error.value = '基线数据加载失败，请重试'
    }
  } finally {
    loading.value = false
  }
}

async function loadOperationFeeDetail() {
  detailLoading.value = true
  try {
    const signal = getSignal()
    const { startMonth, endMonth } = getMonthRange()
    detailData.value = await getOperationFeeDetail(filterWarehouseCode.value, startMonth, endMonth, detailDimension.value, signal)
  } catch (err) {
    if (!axios.isCancel(err)) {
      detailData.value = []
    }
  } finally {
    detailLoading.value = false
  }
}

async function loadPageData() {
  await Promise.all([loadBaselineData(), loadOperationFeeDetail()])
}

function handleQuery() {
  if (filterStartMonth.value && filterEndMonth.value && filterStartMonth.value.isAfter(filterEndMonth.value)) {
    error.value = '开始月份不能晚于结束月份'
    return
  }
  pagination.value.current = 1
  loadPageData()
}

function handleTableChange(pag: { current?: number; pageSize?: number }) {
  pagination.value.current = pag.current ?? 1
  pagination.value.pageSize = pag.pageSize ?? 20
  loadBaselineData()
}

async function handleCompare() {
  if (!compareA.value || !compareB.value) {
    return
  }
  compareLoading.value = true
  try {
    const { startMonth, endMonth } = getMonthRange()
    compareData.value = await compareWarehouses([compareA.value, compareB.value], startMonth, endMonth, 'month')
  } finally {
    compareLoading.value = false
  }
}

const compareOption = computed(() => {
  if (!compareData.value.length) {
    return null
  }

  const monthKeys = [...new Set(compareData.value
    .map((item) => `${item.year}-${String(item.month).padStart(2, '0')}`))]
    .sort()
  const compareCodes = [compareA.value, compareB.value].filter((code): code is string => Boolean(code))

  const getMetricValue = (item: CompareResultVO) => {
    switch (selectedMetric.value) {
      case 'totalFee':
        return Number(item.totalFee) || 0
      case 'totalOrders':
        return Number(item.totalOrders) || 0
      case 'costPerOrder':
        return Number(item.costPerOrder) || 0
      case 'costPerItem':
        return Number(item.costPerItem) || 0
      case 'avgHeadcount':
        return Number(item.avgHeadcount) || 0
      default:
        return 0
    }
  }

  const series = compareCodes.map((code) => {
    const warehouseRows = compareData.value.filter((item) => item.warehouseCode === code)
    const warehouseName = warehouseRows[0]?.warehouseName ?? code
    const rowByMonth = new Map(warehouseRows.map((item) => [`${item.year}-${String(item.month).padStart(2, '0')}`, item]))
    return {
      name: warehouseName,
      type: 'line' as const,
      smooth: true,
      connectNulls: false,
      data: monthKeys.map((month) => {
        const row = rowByMonth.get(month)
        return row ? getMetricValue(row) : null
      }),
    }
  })

  const tooltipFormatter = (value: number) => {
    if (selectedMetric.value === 'totalOrders' || selectedMetric.value === 'avgHeadcount') {
      return formatInteger(value)
    }
    if (selectedMetric.value === 'costPerOrder' || selectedMetric.value === 'costPerItem' || selectedMetric.value === 'totalFee') {
      return formatCurrency(value)
    }
    return formatDecimal(value)
  }

  return {
    tooltip: {
      trigger: 'axis' as const,
      formatter: (params: Array<{ axisValueLabel: string; seriesName: string; value: number }>) => {
        const lines = params
          .filter((item) => item.value !== null && item.value !== undefined)
          .map((item) => `${item.seriesName}：${tooltipFormatter(item.value)}`)
        return `${params[0]?.axisValueLabel ?? ''}<br/>${lines.join('<br/>')}`
      },
    },
    legend: {
      top: 8,
      itemGap: 18,
    },
    grid: {
      left: 72,
      right: 28,
      top: 64,
      bottom: 96,
      containLabel: true,
    },
    xAxis: {
      type: 'category' as const,
      data: monthKeys,
      axisLabel: {
        rotate: 30,
        margin: 16,
      },
    },
    yAxis: {
      type: 'value' as const,
      axisLabel: {
        margin: 12,
      },
      splitLine: {
        lineStyle: {
          type: 'dashed' as const,
        },
      },
    },
    series,
  }
})

onMounted(async () => {
  try {
    warehouses.value = await getWarehouses()
  } catch {
    warehouses.value = []
  }

  try {
    const latestMonth = await getLatestBaselineMonth()
    if (latestMonth) {
      filterEndMonth.value = dayjs(latestMonth, 'YYYY-MM')
      filterStartMonth.value = dayjs(latestMonth, 'YYYY-MM').subtract(11, 'month')
    }
  } catch {
    const fallback = dayjs()
    filterEndMonth.value = fallback
    filterStartMonth.value = fallback.subtract(11, 'month')
  }

  loadPageData()
})

onUnmounted(() => {
  abortRequests()
})
</script>

<template>
  <div class="p-6 baseline-page">
    <a-result v-if="error" status="error" :title="error">
      <template #extra>
        <a-button type="primary" @click="loadPageData">重试</a-button>
      </template>
    </a-result>
    <template v-else>
      <a-card class="mb-6">
        <a-space wrap>
          <span class="text-gray-600">月份区间：</span>
          <a-date-picker
            v-model:value="filterStartMonth"
            picker="month"
            format="YYYY-MM"
            placeholder="开始月份"
          />
          <span class="text-gray-400">至</span>
          <a-date-picker
            v-model:value="filterEndMonth"
            picker="month"
            format="YYYY-MM"
            placeholder="结束月份"
          />
          <a-select
            v-model:value="filterWarehouseCode"
            allow-clear
            placeholder="全部仓库"
            style="width: 220px"
            :options="warehouses.map((warehouse) => ({ value: warehouse.warehouseCode, label: warehouse.warehouseName }))"
          />
          <a-button type="primary" :loading="loading || detailLoading" @click="handleQuery">
            查询
          </a-button>
        </a-space>
      </a-card>

      <a-card title="月度基线数据" class="mb-6">
        <a-table
          :columns="columns"
          :data-source="baselineData"
          :loading="loading"
          :row-key="baselineRowKey"
          :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, showSizeChanger: true }"
          :scroll="{ x: 2000 }"
          size="small"
          @change="handleTableChange"
        />
      </a-card>

      <a-card :title="detailDimension === 'day' ? '日度操作大类费用明细' : '月度操作大类费用明细'" class="mb-6">
        <div class="mb-4">
          <a-radio-group v-model:value="detailDimension" button-style="solid" size="small" @change="loadOperationFeeDetail">
            <a-radio-button value="day">按日汇总</a-radio-button>
            <a-radio-button value="month">按月汇总</a-radio-button>
          </a-radio-group>
        </div>
        <a-skeleton active :loading="detailLoading && detailData.length === 0" :paragraph="{ rows: 6 }">
          <a-empty v-if="detailData.length === 0 && !detailLoading" :description="detailDimension === 'day' ? '暂无日度费用明细数据' : '暂无月度费用明细数据'" />
          <a-table
            v-else
            :columns="detailColumns"
            :data-source="detailData"
            :loading="detailLoading"
            :row-key="detailRowKey"
            :pagination="{ pageSize: 30, showSizeChanger: true, showTotal: formatPaginationTotal }"
            :scroll="{ x: 960 }"
            size="small"
          />
        </a-skeleton>
      </a-card>

      <a-card title="双仓月度趋势对比" class="mb-6">
        <a-space wrap>
          <a-select
            v-model:value="compareA"
            placeholder="仓库 A"
            style="width: 220px"
            :options="warehouses.map((warehouse) => ({ value: warehouse.warehouseCode, label: warehouse.warehouseName }))"
          />
          <a-select
            v-model:value="compareB"
            placeholder="仓库 B"
            style="width: 220px"
            :options="warehouses.map((warehouse) => ({ value: warehouse.warehouseCode, label: warehouse.warehouseName }))"
          />
          <a-button
            type="primary"
            :loading="compareLoading"
            :disabled="!compareA || !compareB || compareA === compareB"
            @click="handleCompare"
          >
            对比
          </a-button>
        </a-space>

        <template v-if="compareData.length > 0">
          <div class="mt-4 mb-4">
            <span class="mr-3 text-gray-600">指标：</span>
            <a-radio-group v-model:value="selectedMetric" button-style="solid" size="small">
              <a-radio-button v-for="option in metricOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </a-radio-button>
            </a-radio-group>
          </div>
          <a-skeleton active :loading="compareLoading" :paragraph="{ rows: 6 }">
            <v-chart v-if="compareOption" :option="compareOption" style="height: 360px" autoresize />
          </a-skeleton>
        </template>
        <div v-else-if="!compareLoading" class="py-8 text-center text-gray-400">
          请选择两个不同仓库后点击对比，将展示月份区间内月度趋势
        </div>
      </a-card>
    </template>
  </div>
</template>

<style scoped>
.baseline-page :deep(.ant-card) {
  border-radius: 10px;
}

.baseline-page :deep(.ant-card-head) {
  min-height: 52px;
}

.baseline-page :deep(.ant-card-head-title) {
  padding: 14px 0;
}

.baseline-page :deep(.ant-card-body) {
  padding: 18px 22px;
}
</style>