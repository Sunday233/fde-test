<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue'
import axios from 'axios'
import VChart from 'vue-echarts'
import { calculate, getEstimateDefaults, getWarehouseDetail, getWarehouses } from '@/api'
import type { EstimateRequest, EstimateResultVO, WarehouseDetailVO, WarehouseVO } from '@/types/api'
import type { Rule } from 'ant-design-vue/es/form'

const formRef = ref()
const loading = ref(false)
const defaultsLoading = ref(false)
const error = ref<string | null>(null)
const result = ref<EstimateResultVO | null>(null)
const historyData = ref<WarehouseDetailVO | null>(null)
const warehouses = ref<WarehouseVO[]>([])
const selectedWarehouse = ref<string>('')

const form = reactive<EstimateRequest>({
  dailyOrders: 0,
  itemsPerOrder: 0,
  workDays: 26,
  laborEfficiency: 0,
  fixedLaborPrice: 0,
  tempLaborPrice: 0,
  forkliftLaborPrice: 0,
  fixedLaborRatio: 0,
  tempLaborRatio: 0,
  forkliftLaborRatio: 0,
  hoursPerDay: 8,
  itemsPerOrderBaseline: 0,
  enableItemCorrection: false,
  taxRate: 0.06,
})

const rules: Record<string, Rule[]> = {
  dailyOrders: [{ required: true, message: '请输入日均单量', type: 'number', min: 1 }],
  itemsPerOrder: [{ required: true, message: '请输入件单比', type: 'number', min: 0.01 }],
  workDays: [{ required: true, message: '请输入工作天数', type: 'number', min: 1, max: 31 }],
  laborEfficiency: [{ required: true, message: '请输入人效', type: 'number', min: 0.01 }],
  fixedLaborPrice: [{ required: true, message: '请输入固定劳务单价', type: 'number', min: 0.01 }],
  tempLaborPrice: [{ required: true, message: '请输入临时劳务单价', type: 'number', min: 0.01 }],
  fixedLaborRatio: [{ required: true, message: '请输入固临比', type: 'number', min: 0, max: 1 }],
  taxRate: [{ required: true, message: '请输入税率', type: 'number', min: 0, max: 1 }],
}

async function loadDefaults() {
  const wh = selectedWarehouse.value
  if (!wh) return
  error.value = null
  defaultsLoading.value = true
  try {
    const defaults = await getEstimateDefaults(wh)
    const { workDays: _, ...rest } = defaults
    Object.assign(form, rest)
  } catch (e) {
    if (!axios.isCancel(e)) {
      error.value = '默认参数加载失败，请重试'
    }
  } finally {
    defaultsLoading.value = false
  }
  try {
    historyData.value = await getWarehouseDetail(wh)
  } catch {
    historyData.value = null
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

async function handleCalculate() {
  try {
    await formRef.value.validateFields()
  } catch {
    return
  }
  loading.value = true
  try {
    result.value = await calculate({ ...form })
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

// 历史对比图
const compareOption = computed(() => {
  if (!result.value) return null
  const dims = ['月度费用', '日均费用', '单均成本', '件均成本']
  const estValues = [result.value.monthlyFee, result.value.dailyFee, result.value.costPerOrder, result.value.costPerItem]
  const histValues = historyData.value
    ? [historyData.value.totalFee, historyData.value.totalFee / (historyData.value.totalOrders > 0 ? 22 : 1), historyData.value.costPerOrder, historyData.value.costPerItem]
    : [0, 0, 0, 0]
  return {
    tooltip: { trigger: 'axis' as const },
    legend: { bottom: 0, data: ['估算值', '历史值'] },
    grid: { left: 80, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category' as const, data: dims },
    yAxis: { type: 'value' as const },
    series: [
      { name: '估算值', type: 'bar' as const, data: estValues, itemStyle: { color: '#1890ff' } },
      { name: '历史值', type: 'bar' as const, data: histValues, itemStyle: { color: '#52c41a' } },
    ],
  }
})

onMounted(async () => {
  await loadWarehouseOptions()
  await loadDefaults()
})
watch(selectedWarehouse, () => {
  result.value = null
  loadDefaults()
})

</script>

<template>
  <div class="p-6">
    <a-result v-if="error" status="error" :title="error">
      <template #extra>
        <a-button type="primary" @click="loadDefaults">重试</a-button>
      </template>
    </a-result>
    <template v-else>
    <a-spin :spinning="defaultsLoading" tip="加载默认参数...">
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

    <a-row :gutter="24" class="mb-6">
      <!-- 左侧：参数表单 -->
      <a-col :xs="24" :md="12">
        <a-card title="参数输入">
          <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="日均单量" name="dailyOrders">
                  <a-input-number v-model:value="form.dailyOrders" :min="0" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="件单比" name="itemsPerOrder">
                  <a-input-number v-model:value="form.itemsPerOrder" :min="0" :step="0.1" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="工作天数" name="workDays">
                  <a-input-number v-model:value="form.workDays" :min="1" :max="31" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="人效 (件/人时)" name="laborEfficiency">
                  <a-input-number v-model:value="form.laborEfficiency" :min="0" :step="0.1" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="每日工时 (h)" name="hoursPerDay">
                  <a-input-number v-model:value="form.hoursPerDay" :min="1" :max="24" :step="0.5" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="税率" name="taxRate">
                  <a-input-number v-model:value="form.taxRate" :min="0" :max="1" :step="0.01" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-divider>劳务单价</a-divider>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="固定 (元/h)" name="fixedLaborPrice">
                  <a-input-number v-model:value="form.fixedLaborPrice" :min="0" :step="0.5" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="临时 (元/h)" name="tempLaborPrice">
                  <a-input-number v-model:value="form.tempLaborPrice" :min="0" :step="0.5" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="叉车 (元/h)" name="forkliftLaborPrice">
                  <a-input-number v-model:value="form.forkliftLaborPrice" :min="0" :step="0.5" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-divider>人力占比</a-divider>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="固定占比" name="fixedLaborRatio">
                  <a-input-number v-model:value="form.fixedLaborRatio" :min="0" :max="1" :step="0.05" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="临时占比" name="tempLaborRatio">
                  <a-input-number v-model:value="form.tempLaborRatio" :min="0" :max="1" :step="0.05" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="叉车占比" name="forkliftLaborRatio">
                  <a-input-number v-model:value="form.forkliftLaborRatio" :min="0" :max="1" :step="0.05" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-divider>件单比修正</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="启用件单比修正">
                  <a-switch v-model:checked="form.enableItemCorrection" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="基线件单比" name="itemsPerOrderBaseline">
                  <a-input-number v-model:value="form.itemsPerOrderBaseline" :min="0" :step="0.1" :disabled="!form.enableItemCorrection" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" :loading="loading" @click="handleCalculate">计算</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <!-- 右侧：计算结果 -->
      <a-col :xs="24" :md="12">
        <a-card title="计算结果">
          <div v-if="!result" class="text-center py-8 text-gray-400">请填写参数后点击计算</div>
          <a-descriptions v-else bordered :column="1" size="small">
            <a-descriptions-item label="预估人数">{{ result.estimatedHeadcount.toFixed(1) }} 人</a-descriptions-item>
            <a-descriptions-item label="件单比修正系数">{{ result.itemCorrectionFactor.toFixed(4) }}</a-descriptions-item>
            <a-descriptions-item label="预估总工时">{{ result.estimatedTotalHours.toFixed(1) }} h</a-descriptions-item>
            <a-descriptions-item label="加权平均单价">¥{{ result.weightedUnitPrice.toFixed(2) }}/h</a-descriptions-item>
            <a-descriptions-item label="月度费用">¥{{ result.monthlyFee.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item label="日均费用">¥{{ result.dailyFee.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item label="单均成本">¥{{ result.costPerOrder.toFixed(2) }}/单</a-descriptions-item>
            <a-descriptions-item label="件均成本">¥{{ result.costPerItem.toFixed(2) }}/件</a-descriptions-item>
            <a-descriptions-item v-if="result.baselineDailyFee > 0" label="基线日均费用">¥{{ result.baselineDailyFee.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item v-if="result.baselineDailyFee > 0" label="基线偏差">
              <span :style="{ color: result.baselineDeviation > 0 ? '#ff4d4f' : '#52c41a' }">
                {{ result.baselineDeviation > 0 ? '+' : '' }}{{ (result.baselineDeviation * 100).toFixed(1) }}%
              </span>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>

    <!-- 历史对比图 -->
    <a-card title="与历史数据对比">
      <div v-if="!compareOption" class="text-center py-8 text-gray-400">
        {{ !result ? '请先完成计算' : (!historyData ? '暂无历史数据' : '') }}
      </div>
      <v-chart v-else :option="compareOption" style="height: 320px" autoresize />
    </a-card>
    </a-spin>
    </template>
  </div>
</template>
