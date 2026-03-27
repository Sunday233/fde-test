<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import dayjs from 'dayjs'
import MarkdownIt from 'markdown-it'
import { getReportList, generateReport, getReportDetail, getWarehouses, useAbortController } from '@/api'
import type { ReportVO, WarehouseVO, ReportGenerateRequest, PageResult } from '@/types/api'
import { message } from 'ant-design-vue'

const md = new MarkdownIt({ html: false })

const loading = ref(false)
const error = ref<string | null>(null)
const { getSignal, abort: abortRequests } = useAbortController()
const reports = ref<ReportVO[]>([])
const warehouses = ref<WarehouseVO[]>([])
const pagination = ref({ current: 1, pageSize: 20, total: 0 })

// 生成表单
const modalVisible = ref(false)
const generateLoading = ref(false)
const generateForm = ref<ReportGenerateRequest>({
  warehouseCode: '',
  startMonth: '',
  endMonth: '',
})
const genStartMonth = ref<dayjs.Dayjs | null>(null)
const genEndMonth = ref<dayjs.Dayjs | null>(null)

// 预览
const drawerVisible = ref(false)
const previewLoading = ref(false)
const previewHtml = ref('')
const previewReport = ref<ReportVO | null>(null)

const columns = [
  { title: '报告标题', dataIndex: 'title', key: 'title' },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 160 },
  { title: '开始月份', dataIndex: 'startMonth', key: 'startMonth', width: 110 },
  { title: '结束月份', dataIndex: 'endMonth', key: 'endMonth', width: 110 },
  { title: '生成时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 150 },
]

async function loadReports() {
  loading.value = true
  error.value = null
  try {
    const res = await getReportList(pagination.value.current, pagination.value.pageSize, getSignal())
    if ('records' in res) {
      reports.value = (res as PageResult<ReportVO>).records
      pagination.value.total = (res as PageResult<ReportVO>).total
    } else {
      reports.value = res as ReportVO[]
      pagination.value.total = reports.value.length
    }
  } catch (e) {
    if (!axios.isCancel(e)) {
      error.value = '报告列表加载失败，请重试'
    }
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: { current?: number; pageSize?: number }) {
  pagination.value.current = pag.current ?? 1
  pagination.value.pageSize = pag.pageSize ?? 20
  loadReports()
}

function openGenerateModal() {
  generateForm.value = { warehouseCode: '', startMonth: '', endMonth: '' }
  genStartMonth.value = null
  genEndMonth.value = null
  modalVisible.value = true
}

async function handleGenerate() {
  if (!generateForm.value.warehouseCode) {
    message.warning('请选择仓库')
    return
  }
  if (!genStartMonth.value || !genEndMonth.value) {
    message.warning('请选择起止月份')
    return
  }
  if (genEndMonth.value.isBefore(genStartMonth.value)) {
    message.warning('结束月份不能早于开始月份')
    return
  }
  generateForm.value.startMonth = genStartMonth.value.format('YYYY-MM')
  generateForm.value.endMonth = genEndMonth.value.format('YYYY-MM')

  generateLoading.value = true
  try {
    const report = await generateReport(generateForm.value)
    message.success('报告生成成功')
    modalVisible.value = false
    await loadReports()
    // 自动预览新报告
    handlePreview(report)
  } catch {
    // handled
  } finally {
    generateLoading.value = false
  }
}

async function handlePreview(record: ReportVO) {
  drawerVisible.value = true
  previewLoading.value = true
  previewReport.value = record
  try {
    const detail = await getReportDetail(record.id)
    previewHtml.value = md.render(detail.content || '')
  } catch {
    previewHtml.value = '<p>加载失败</p>'
  } finally {
    previewLoading.value = false
  }
}

async function handleDownload(record: ReportVO) {
  try {
    const detail = await getReportDetail(record.id)
    const htmlContent = md.render(detail.content || '')
    const fullHtml = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${record.title}</title>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 900px; margin: 0 auto; padding: 40px 20px; color: #333; line-height: 1.6; }
  h1 { border-bottom: 2px solid #1890ff; padding-bottom: 8px; }
  h2 { border-bottom: 1px solid #eee; padding-bottom: 4px; margin-top: 24px; }
  table { border-collapse: collapse; width: 100%; margin: 16px 0; }
  th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
  th { background: #fafafa; font-weight: 600; }
  code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; }
  pre { background: #f5f5f5; padding: 16px; border-radius: 4px; overflow-x: auto; }
  blockquote { border-left: 4px solid #1890ff; margin: 16px 0; padding: 8px 16px; background: #f0f5ff; }
</style>
</head>
<body>
${htmlContent}
</body>
</html>`
    const blob = new Blob([fullHtml], { type: 'text/html;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${record.title}_${record.warehouseName}_${record.startMonth}-${record.endMonth}.html`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    message.error('下载失败')
  }
}

onMounted(async () => {
  try {
    warehouses.value = await getWarehouses()
  } catch { /* ignore */ }
  loadReports()
})

onUnmounted(abortRequests)
</script>

<template>
  <div class="p-4">
    <a-result v-if="error" status="error" :title="error">
      <template #extra>
        <a-button type="primary" @click="loadReports">重试</a-button>
      </template>
    </a-result>
    <template v-else>
    <!-- 操作栏 -->
    <a-card class="mb-4">
      <a-button type="primary" @click="openGenerateModal">生成报告</a-button>
    </a-card>

    <!-- 报告列表 -->
    <a-card title="报告列表">
      <a-table
        :columns="columns"
        :data-source="reports"
        :loading="loading"
        row-key="id"
        :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, showSizeChanger: true }"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handlePreview(record as ReportVO)">预览</a-button>
              <a-button type="link" size="small" @click="handleDownload(record as ReportVO)">下载</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 生成报告弹窗 -->
    <a-modal v-model:open="modalVisible" title="生成报告" @ok="handleGenerate" :confirm-loading="generateLoading">
      <a-form layout="vertical">
        <a-form-item label="仓库" required>
          <a-select
            v-model:value="generateForm.warehouseCode"
            placeholder="选择仓库"
            :options="warehouses.map((w) => ({ value: w.warehouseCode, label: w.warehouseName }))"
          />
        </a-form-item>
        <a-form-item label="开始月份" required>
          <a-date-picker v-model:value="genStartMonth" picker="month" format="YYYY-MM" style="width: 100%" />
        </a-form-item>
        <a-form-item label="结束月份" required>
          <a-date-picker v-model:value="genEndMonth" picker="month" format="YYYY-MM" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 预览抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="previewReport?.title || '报告预览'"
      width="700"
      placement="right"
    >
      <a-spin :spinning="previewLoading">
        <div v-html="previewHtml" class="prose" />
      </a-spin>
    </a-drawer>
    </template>
  </div>
</template>
